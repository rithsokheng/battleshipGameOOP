package com.battleship.controller;

import com.battleship.ai.AiShotPlan;
import com.battleship.ai.AIFactory;
import com.battleship.ai.AIStrategy;
import com.battleship.model.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Consumer;

/**
 * Application-service layer: orchestrates the full game flow
 * (menu -> mode select -> board select -> ship placement -> battle -> game over)
 * and mediates between View and Model via callbacks.
 */
public class GameController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private GameMode selectedMode;
    private Theater selectedTheater;
    private GameState state = GameState.MAIN_MENU;

    private Player player1;
    private Player player2;
    private int currentPlayerIndex; // whose turn it is in BATTLE
    private int placingPlayerIndex; // whose turn it is in SHIP_PLACEMENT (hotseat only)
    private AIStrategy aiStrategy;

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

        aiStrategy = hotseat ? null : AIFactory.create(selectedMode);
        currentPlayerIndex = 0;
        placingPlayerIndex = 0;

        player1.initLauncherAmmo(size);
        player2.initLauncherAmmo(size);
    }

    // ---------- Flow: Ship placement ----------

    public Player getPlacingPlayer() {
        return placingPlayerIndex == 0 ? player1 : player2;
    }

    /** Ship types still needed for the placing player, keyed by type, with remaining count. */
    public Map<ShipType, Integer> getRemainingShipCounts(Player player) {
        Map<ShipType, Integer> remaining = new LinkedHashMap<>(selectedTheater.getFleetComposition());
        for (Ship s : player.getOwnBoard().getShips()) {
            remaining.merge(s.getType(), -1, Integer::sum);
        }
        remaining.values().removeIf(v -> v <= 0);
        remaining.entrySet().removeIf(e -> e.getValue() <= 0);
        return remaining;
    }

    public boolean placeShip(Player player, ShipType type, Coordinate start, boolean horizontal) {
        Map<ShipType, Integer> remaining = getRemainingShipCounts(player);
        if (!remaining.containsKey(type) || remaining.get(type) <= 0) return false;
        return player.getOwnBoard().placeShip(type, start, horizontal);
    }

    public boolean canPlace(Player player, ShipType type, Coordinate start, boolean horizontal) {
        return player.getOwnBoard().isValidPlacement(type, start, horizontal);
    }

    /** Pulls an already-placed ship back off the board and into the dock ("put out"). */
    public boolean removeShip(Player player, Ship ship) {
        return player.getOwnBoard().removeShip(ship);
    }

    public boolean isPlacementComplete(Player player) {
        return player.getOwnBoard().getShips().size() == selectedTheater.getTotalShipCount();
    }

    public void resetPlacement(Player player) {
        player.getOwnBoard().clearShips();
    }

    /** Randomly places all remaining ships for the player (spec 4.2, retry until success). */
    public void autoPlaceRemaining(Player player) {
        Map<ShipType, Integer> remaining = getRemainingShipCounts(player);
        int size = selectedTheater.getBoardSize();
        for (Map.Entry<ShipType, Integer> entry : remaining.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                boolean placed = false;
                for (int attempt = 0; attempt < 10_000 && !placed; attempt++) {
                    int row = RANDOM.nextInt(size);
                    int col = RANDOM.nextInt(size);
                    boolean horizontal = RANDOM.nextBoolean();
                    placed = player.getOwnBoard().placeShip(entry.getKey(), new Coordinate(row, col), horizontal);
                }
            }
        }
    }

    /** Called when the placing player hits READY. Advances placement or starts battle. */
    public void confirmReady() {
        if (selectedMode == GameMode.HOTSEAT) {
            if (placingPlayerIndex == 0) {
                placingPlayerIndex = 1;
                changeState(GameState.PASS_SCREEN);
                return;
            } else {
                startBattle();
                return;
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
        rollInitiative();
        changeState(GameState.BATTLE);
    }

    // ---------- Flow: Battle ----------

    /** Cryptographically fair coin flip determines who fires first. */
    public Player rollInitiative() {
        currentPlayerIndex = RANDOM.nextBoolean() ? 0 : 1;
        return getCurrentPlayer();
    }

    public Player getCurrentPlayer() { return currentPlayerIndex == 0 ? player1 : player2; }
    public Player getOpponent() { return currentPlayerIndex == 0 ? player2 : player1; }
    public boolean isAiTurn() { return selectedMode != GameMode.HOTSEAT && getCurrentPlayer() == player2; }

    // ---------- Launcher system ----------

    /** Attempts to select a launcher for the given player; fails silently (returns false) if unavailable/out of ammo. */
    public boolean setSelectedLauncher(Player player, LauncherType type) {
        int size = selectedTheater.getBoardSize();
        if (!type.isAvailableFor(size)) return false;
        if (type == LauncherType.LEVEL_2 && player.getLevel2Ammo() <= 0) return false;
        if (type == LauncherType.NUCLEAR && player.getNuclearAmmo() <= 0) return false;
        player.setSelectedLauncher(type);
        return true;
    }

    public void toggleLauncherOrientation(Player player) {
        player.setLauncherHorizontal(!player.isLauncherHorizontal());
    }

    public int getAmmoRemaining(Player player, LauncherType type) {
        return switch (type) {
            case DEFAULT -> Integer.MAX_VALUE;
            case LEVEL_2 -> player.getLevel2Ammo();
            case NUCLEAR -> player.getNuclearAmmo();
        };
    }

    /**
     * Fires the current player's selected launcher, anchored at the given cell.
     * Already-shot and out-of-bounds cells within the pattern are skipped, but
     * ammo is still consumed once for the whole shot. Advances the turn afterward.
     */
    public LauncherFireResult fireLauncher(Coordinate anchor) {
        Player attacker = getCurrentPlayer();
        Player defender = getOpponent();
        LauncherType type = attacker.getSelectedLauncher();
        int size = defender.getOwnBoard().getSize();

        List<Coordinate> cells = LauncherLogic.getTargetCells(type, anchor, attacker.isLauncherHorizontal());
        List<ShotResult> results = new ArrayList<>();
        LinkedHashSet<Ship> sunk = new LinkedHashSet<>();

        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            CellStatus existing = defender.getOwnBoard().getCellStatus(c);
            if (existing == CellStatus.HIT || existing == CellStatus.MISS || existing == CellStatus.SUNK) continue;
            ShotResult r = defender.getOwnBoard().receiveShot(c);
            results.add(r);
            if (r.outcome() == CellStatus.SUNK) sunk.add(r.shipSunk());
        }

        if (type == LauncherType.LEVEL_2) attacker.setLevel2Ammo(attacker.getLevel2Ammo() - 1);
        if (type == LauncherType.NUCLEAR) attacker.setNuclearAmmo(attacker.getNuclearAmmo() - 1);
        attacker.setSelectedLauncher(LauncherType.DEFAULT); // must actively re-select each turn (rule 1)

        if (aiStrategy != null && attacker == player2) {
            for (ShotResult r : results) aiStrategy.notifyResult(r);
        }

        LauncherFireResult fireResult = new LauncherFireResult(results, new ArrayList<>(sunk));

        if (defender.hasLost()) {
            changeState(GameState.GAME_OVER);
            if (onGameOver != null) onGameOver.accept(attacker);
        } else {
            currentPlayerIndex = 1 - currentPlayerIndex;
        }
        return fireResult;
    }

    /** Has the AI choose a weapon + target, applies the selection, and fires it. */
    public LauncherFireResult fireAiLauncher() {
        AiShotPlan plan = aiStrategy.chooseShotPlan(player1.getOwnBoard(), player2.getLevel2Ammo(), player2.getNuclearAmmo());
        player2.setSelectedLauncher(plan.type());
        player2.setLauncherHorizontal(plan.horizontal());
        return fireLauncher(plan.anchor());
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
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
}
