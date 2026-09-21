package com.battleship.controller;

import com.battleship.ai.AIFactory;
import com.battleship.model.Coordinate;
import com.battleship.model.FleetReadout;
import com.battleship.model.GameMode;
import com.battleship.model.GameState;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import com.battleship.model.Turn;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;
import com.battleship.persistence.GameSaveDTO;
import com.battleship.persistence.GameSaveMapper;
import com.battleship.persistence.SaveGameService;

import java.io.IOException;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Application-service layer: orchestrates the game flow
 * (menu -> mode select -> board select -> ship placement -> battle -> game over)
 * and mediates between View and Model via callbacks.
 *
 * <p>It is deliberately a thin mediator (SRP): all placement logic lives in
 * {@link PlacementService}, all turn/firing logic in {@link BattleService}.
 * Views never receive mutable domain objects — they get player names, fleet
 * read-outs and tracking grids.</p>
 */
public class GameController {

    private final PlacementService placementService;
    private final BattleService battleService;
    private final NetworkFireService networkFireService;
    private final SaveGameService saveGameService;

    /** Testable constructor — inject services (DIP). */
    public GameController(PlacementService placementService, BattleService battleService, SaveGameService saveGameService) {
        this.placementService = placementService;
        this.battleService = battleService;
        this.networkFireService = new NetworkFireService();
        this.saveGameService = saveGameService;
    }

    public GameController(PlacementService placementService, BattleService battleService) {
        this(placementService, battleService, new SaveGameService());
    }

    /** Production convenience constructor. */
    public GameController() {
        this(new PlacementService(), new BattleService(), new SaveGameService());
    }

    private GameMode selectedMode;
    private Theater selectedTheater;
    private GameState state = GameState.MAIN_MENU;

    private Player player1;
    private Player player2;
    private Turn placingTurn; // whose turn it is in SHIP_PLACEMENT (hotseat only)

    private Consumer<GameState> onStateChanged;
    private Consumer<Player> onGameOver;

    // ---------- Flow: Mode & Theater selection ----------

    public void setMode(GameMode mode) {
        this.selectedMode = mode;
        changeState(GameState.BOARD_SELECT);
    }

    public void setTheater(Theater theater) {
        this.selectedTheater = theater;
        initializeGame();
        changeState(GameState.SHIP_PLACEMENT);
    }

    private void initializeGame() {
        // Polymorphic players replace the old isHuman boolean (V2.2): the mode
        // decides which subclass is instantiated, not a flag inside one class.
        boolean vsHuman = selectedMode == GameMode.HOTSEAT || selectedMode == GameMode.ONLINE || selectedMode == null;
        boolean hotseat = selectedMode == GameMode.HOTSEAT;
        player1 = new HumanPlayer(hotseat ? "Admiral 1" : "Admiral (You)", selectedTheater);
        player2 = vsHuman
                ? new HumanPlayer("Admiral 2", selectedTheater)
                : new com.battleship.model.AiPlayer("Enemy AI", selectedTheater, AIFactory.create(selectedMode));

        battleService.init(player1, player2);
        placingTurn = Turn.PLAYER_1;
    }

    // ---------- Flow: Ship placement (delegates to PlacementService) ----------

    public Player getPlacingPlayer() {
        return placingTurn == Turn.PLAYER_1 ? player1 : player2;
    }

    /** Ship types still needed for the player, keyed by type, with remaining count. */
    public Map<ShipType, Integer> getRemainingShipCounts(Player player) {
        return placementService.getRemainingShipCounts(player, selectedTheater);
    }

    public boolean placeShip(Player player, ShipType type, Coordinate start, Orientation orientation) {
        return placementService.deploy(player, selectedTheater, type, start, orientation);
    }

    public boolean canPlace(Player player, ShipType type, Coordinate start, Orientation orientation) {
        return placementService.canDeploy(player, type, start, orientation);
    }

    /** Pulls an already-deployed ship at the given coordinate back into the dock ("put back"). */
    public boolean removeShipAt(Player player, Coordinate c) {
        return placementService.undeployAt(player, c);
    }

    public boolean isPlacementComplete(Player player) {
        return placementService.isDeploymentComplete(player, selectedTheater);
    }

    public void resetPlacement(Player player) {
        placementService.resetDeployment(player);
    }

    /** Randomly deploys all remaining ships for the player (spec 4.2, retry until success). */
    public void autoPlaceRemaining(Player player) {
        placementService.autoDeployAll(player, selectedTheater);
    }

    /** Called when the placing player hits READY. Advances placement or starts battle. */
    public void confirmReady() {
        if (selectedMode == GameMode.HOTSEAT) {
            if (placingTurn == Turn.PLAYER_1) {
                placingTurn = Turn.PLAYER_2;
                changeState(GameState.PASS_SCREEN);
            } else {
                startBattle();
            }
        } else {
            // vs AI: auto-deploy the AI's fleet, then start battle.
            autoPlaceRemaining(player2);
            startBattle();
        }
    }

    /** Called from PassScreen "continue" to resume ship placement for player 2. */
    public void resumePlacementAfterPass() {
        changeState(GameState.SHIP_PLACEMENT);
    }

    private void startBattle() {
        battleService.rollInitiative();
        changeState(GameState.BATTLE);
    }

    // ---------- Flow: Battle (delegates to BattleService) ----------

    public Player rollInitiative() { return battleService.rollInitiative(); }
    public Player getCurrentPlayer() { return battleService.getCurrentPlayer(); }
    public Player getOpponent() { return battleService.getOpponent(); }

    public boolean isAiTurn() {
        return selectedMode != null && selectedMode.isVsAi() && battleService.isAiTurn();
    }

    public boolean selectWeapon(Player player, Weapon weapon) {
        return battleService.selectWeapon(player, weapon);
    }

    public void toggleWeaponOrientation(Player player) {
        battleService.toggleOrientation(player);
    }

    public int getAmmoRemaining(Player player, Weapon weapon) {
        return battleService.getAmmoRemaining(player, weapon);
    }

    /**
     * Fires the current player's selected weapon, anchored at the given cell.
     * Delegates to the BattleService; reacts to game-over if the shot ended the match.
     */
    public LauncherFireResult fireLauncher(Coordinate anchor) {
        LauncherFireResult fireResult = battleService.fire(anchor);
        reactToBattleEnd();
        return fireResult;
    }

    /** Has the current (machine) player choose a weapon + target, then fires it. */
    public LauncherFireResult fireAiLauncher() {
        LauncherFireResult fireResult = battleService.fireAiLauncher();
        reactToBattleEnd();
        return fireResult;
    }

    /**
     * Single place that recognises a finished match, for both human and AI shots.
     * On a decisive shot the BattleService keeps the turn with the winner, so
     * {@link BattleService#getCurrentPlayer()} is the winning player.
     */
    private void reactToBattleEnd() {
        if (battleService.isBattleOver()) {
            changeState(GameState.GAME_OVER);
            if (onGameOver != null) onGameOver.accept(battleService.getCurrentPlayer());
        }
    }

    // ---------- State plumbing ----------

    private void changeState(GameState newState) {
        this.state = newState;
        if (onStateChanged != null) onStateChanged.accept(newState);
    }

    public void setOnStateChanged(Consumer<GameState> callback) { this.onStateChanged = callback; }
    public void setOnGameOver(Consumer<Player> callback) { this.onGameOver = callback; }

    public void goToMainMenu() { changeState(GameState.MAIN_MENU); }
    public void goToModeSelect() { changeState(GameState.MODE_SELECT); }

    public GameState getState() { return state; }
    public GameMode getSelectedMode() { return selectedMode; }
    public Theater getSelectedTheater() { return selectedTheater; }

    // ---------- Read-only player queries (fixes F2: views never receive mutable domain objects) ----------

    /** Name of player 1 or 2 (1-indexed). */
    public String getPlayerName(int index) {
        return index == 1 ? player1.name() : player2.name();
    }

    /** Read-only fleet projection of player 1 or 2 (1-indexed). */
    public FleetReadout getPlayerFleet(int index) {
        return index == 1 ? player1 : player2;
    }

    /** The knowledge grid of player 1 or 2 (1-indexed) — the only enemy model a view may read. */
    public TrackingGrid getTrackingGrid(int index) {
        return index == 1 ? player1.trackingGrid() : player2.trackingGrid();
    }

    /** Identity check for game-over reporting: is the given player player 1? */
    public boolean isFirstPlayer(Player player) {
        return player == player1;
    }

    // ---------- Network fire pipeline (fixes F7: view no longer mutates the domain) ----------

    /**
     * Applies the domain bookkeeping for a network shot (ammo consumption,
     * weapon reset) and returns the order to transmit over the wire.
     */
    public NetworkFireService.NetworkShotOrder fireNetworkShot(Player shooter, Weapon weapon,
                                                               Coordinate anchor, Orientation orientation) {
        return networkFireService.fireNetworkShot(shooter, weapon, anchor, orientation);
    }

    /** Tops the shooter's nuclear ammo back up after a successful quiz resupply. */
    public void resupplyNuclearAmmo(Player shooter) {
        networkFireService.resupplyNuclear(shooter);
    }

    // ---------- Persistence (Smell 5.3: connects SaveGameService to controller) ----------

    /**
     * Serializes the current match state and writes it to a timestamped JSON file.
     *
     * @param directory the folder to save into
     * @return the saved file's path
     * @throws IOException if disk write fails
     */
    public Path saveGame(Path directory) throws IOException {
        if (player1 == null || player2 == null) {
            throw new IllegalStateException("Cannot save a game that has not been initialized.");
        }
        int size = selectedTheater != null ? selectedTheater.getBoardSize() : player1.size();
        int currentIndex = (battleService.getCurrentPlayer() == player1) ? 1 : 2;

        GameSaveDTO dto = GameSaveMapper.toDTO(player1, player2, size, state, currentIndex);
        return saveGameService.save(dto, directory);
    }

    /** Loads a previously saved game DTO from disk. */
    public GameSaveDTO loadGame(Path file) throws IOException {
        return saveGameService.load(file);
    }

    // ---------- Mutable access (package-private; controller-internal/tests only — fixes F2) ----------


    Player getPlayer1() { return player1; }
    Player getPlayer2() { return player2; }
}
