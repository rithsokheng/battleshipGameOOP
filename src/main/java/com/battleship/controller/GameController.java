package com.battleship.controller;

import com.battleship.ai.AIFactory;
import com.battleship.model.Board;
import com.battleship.model.Coordinate;
import com.battleship.model.GameMode;
import com.battleship.model.GameState;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ReadOnlyBoard;
import com.battleship.model.Ship;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import com.battleship.model.Turn;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Application-service layer: orchestrates the game flow
 * (menu -> mode select -> board select -> ship placement -> battle -> game over)
 * and mediates between View and Model via callbacks.
 *
 * It is deliberately a thin mediator (SRP): all placement logic lives in
 * {@link PlacementService}, all turn/firing logic in {@link BattleService}.
 */
public class GameController {

    private final PlacementService placementService;
    private final BattleService battleService;
    private final NetworkFireService networkFireService;

    /** Testable constructor — inject services (DIP). */
    public GameController(PlacementService placementService, BattleService battleService) {
        this.placementService = placementService;
        this.battleService = battleService;
        this.networkFireService = new NetworkFireService();
    }

    /** Production convenience constructor. */
    public GameController() {
        this(new PlacementService(), new BattleService());
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
        int size = selectedTheater.getBoardSize();
        boolean hotseat = selectedMode == GameMode.HOTSEAT;

        player1 = new Player("Admiral (You)", true, new Board(size));
        player2 = new Player(hotseat ? "Admiral 2" : "Enemy AI", hotseat, new Board(size));

        player1.initLauncherAmmo(size);
        player2.initLauncherAmmo(size);

        battleService.init(player1, player2, hotseat ? null : AIFactory.create(selectedMode));
        placingTurn = Turn.PLAYER_1;
    }


    // ---------- Flow: Ship placement (delegates to PlacementService) ----------

    public Player getPlacingPlayer() {
        return placingTurn == Turn.PLAYER_1 ? player1 : player2;
    }

    /** Ship types still needed for the placing player, keyed by type, with remaining count. */
    public Map<ShipType, Integer> getRemainingShipCounts(Player player) {
        return placementService.getRemainingShipCounts(player, selectedTheater);
    }

    public boolean placeShip(Player player, ShipType type, Coordinate start, Orientation orientation) {
        return placementService.placeShip(player, selectedTheater, type, start, orientation);
    }

    public boolean canPlace(Player player, ShipType type, Coordinate start, Orientation orientation) {
        return placementService.canPlace(player, type, start, orientation);
    }

    /** Pulls an already-placed ship back off the board and into the dock ("put out"). */
    public boolean removeShip(Player player, Ship ship) {
        return placementService.removeShip(player, ship);
    }

    /** Pulls an already-placed ship at the given coordinate back off the board and into the dock. */
    public boolean removeShipAt(Player player, Coordinate c) {
        return placementService.removeShipAt(player, c);
    }

    public boolean isPlacementComplete(Player player) {
        return placementService.isPlacementComplete(player, selectedTheater);
    }

    public void resetPlacement(Player player) {
        placementService.resetPlacement(player);
    }

    /** Randomly places all remaining ships for the player (spec 4.2, retry until success). */
    public void autoPlaceRemaining(Player player) {
        placementService.autoPlaceAll(player, selectedTheater);
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
            // vs AI: auto-place the AI's fleet, then start battle.
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
        return selectedMode != GameMode.HOTSEAT && battleService.isAiTurn();
    }

    public boolean selectLauncher(Player player, LauncherType type) {
        return battleService.selectLauncher(player, type, selectedTheater.getBoardSize());
    }

    public void toggleLauncherOrientation(Player player) {
        battleService.toggleOrientation(player);
    }

    public int getAmmoRemaining(Player player, LauncherType type) {
        return battleService.getAmmoRemaining(player, type);
    }

    /**
     * Fires the current player's selected launcher, anchored at the given cell.
     * Delegates to the BattleService; reacts to game-over if the shot ended the match.
     */
    public LauncherFireResult fireLauncher(Coordinate anchor) {
        LauncherFireResult fireResult = battleService.fire(anchor);
        reactToBattleEnd();
        return fireResult;
    }

    /** Has the AI choose a weapon + target, applies the selection, and fires it. */
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

    // ---------- Read-only player queries (fixes F2: views never receive mutable Players) ----------

    /** Name of player 1 or 2 (1-indexed). */
    public String getPlayerName(int index) {
        return index == 1 ? player1.getName() : player2.getName();
    }

    /** Read-only view of player 1 or 2's board (1-indexed). */
    public ReadOnlyBoard getPlayerBoard(int index) {
        return index == 1 ? player1.getOwnBoard() : player2.getOwnBoard();
    }

    /** Identity check for game-over reporting: is the given player player 1? */
    public boolean isFirstPlayer(Player player) {
        return player == player1;
    }

    // ---------- Network fire pipeline (fixes F7: view no longer mutates the domain) ----------

    /**
     * Applies the domain bookkeeping for a network shot (ammo consumption,
     * launcher reset) and returns the order to transmit over the wire.
     */
    public NetworkFireService.NetworkShotOrder fireNetworkShot(Player shooter, LauncherType type,
                                                               Coordinate anchor, Orientation orientation) {
        return networkFireService.fireNetworkShot(shooter, type, anchor, orientation);
    }

    /** Tops the shooter's nuclear ammo back up after a successful quiz resupply. */
    public void resupplyNuclearAmmo(Player shooter) {
        networkFireService.resupplyNuclear(shooter);
    }

    // ---------- Mutable access (package-private; controller-internal/tests only — fixes F2) ----------

    Player getPlayer1() { return player1; }
    Player getPlayer2() { return player2; }
}