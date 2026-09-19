package com.battleship.model;

/**
 * A participant in the game — human or AI-controlled.
 * ownBoard holds this player's ships; shots are resolved against the opponent's Board.
 *
 * The launcher state is encapsulated: outside code may only *ask* this object
 * to perform an action (select a weapon, rotate it, arm it for an AI shot) —
 * it can never reach in and mutate the fields directly.
 */
public class Player {

    private final String name;
    private final boolean isHuman;
    private final Board ownBoard;

    // --- Launcher system ---
    private AmmoInventory ammo;
    private LauncherType selectedLauncher = LauncherType.DEFAULT;
    private Orientation launcherOrientation = Orientation.HORIZONTAL;

    public Player(String name, boolean isHuman, Board ownBoard) {
        this.name = name;
        this.isHuman = isHuman;
        this.ownBoard = ownBoard;
    }

    public boolean hasLost() {
        return ownBoard.isAllShipsSunk();
    }

    /** Sets starting ammo for the launcher system based on the battle's board size. */
    public void initLauncherAmmo(int boardSize) {
        this.ammo = new AmmoInventory(boardSize);
        this.selectedLauncher = LauncherType.DEFAULT;
        this.launcherOrientation = Orientation.HORIZONTAL;
    }

    /**
     * Attempts to select a launcher; fails (and leaves state untouched) when the
     * weapon is unavailable on this battlefield or out of ammo.
     */
    public boolean selectLauncher(LauncherType type, int boardSize) {
        if (!type.isAvailableFor(boardSize)) return false;
        if (ammo != null && !ammo.hasAmmo(type)) return false;
        this.selectedLauncher = type;
        return true;
    }

    /** Toggles launcher orientation between HORIZONTAL and VERTICAL. */
    public void toggleLauncherOrientation() {
        this.launcherOrientation = launcherOrientation.toggle();
    }

    /** Resets launcher to DEFAULT after a shot (per game rules). */
    public void resetLauncherAfterShot() {
        this.selectedLauncher = LauncherType.DEFAULT;
    }

    /**
     * Arms a specific launcher + orientation for an automated (AI) shot.
     * Used only by the AI shot pipeline, which has already validated ammo.
     */
    public void prepareShot(LauncherType type, Orientation orientation) {
        this.selectedLauncher = type;
        this.launcherOrientation = orientation;
    }

    // --- Read-only accessors ---

    public String getName() { return name; }
    public boolean isHuman() { return isHuman; }

    /**
     * Read-only view of this player's board (fixes F1). Views must render from
     * this — mutation methods (placeShip, receiveShot, clearShips) are not on
     * the interface.
     */
    public ReadOnlyBoard getOwnBoard() { return ownBoard; }

    /**
     * Service-layer escape hatch (fixes F1): PlacementService, BattleService,
     * ShotResolver and NetworkBattleMediator legitimately need to mutate the
     * board. UI code must use {@link #getOwnBoard()} instead.
     */
    public Board getMutableBoard() { return ownBoard; }

    // --- Ammo access, delegated (fixes V1: getAmmo() no longer leaks the mutable AmmoInventory) ---

    /** Returns the current ammo count for the given type. */
    public int getAmmoCount(LauncherType type) {
        return ammo != null ? ammo.getAmmo(type) : 0;
    }

    /** Returns true if the player has at least one shot of this type. */
    public boolean hasAmmo(LauncherType type) {
        return ammo != null && ammo.hasAmmo(type);
    }

    /** True if this ammo type is infinite (e.g., DEFAULT). */
    public boolean isAmmoInfinite(LauncherType type) {
        return ammo != null && ammo.isInfinite(type);
    }

    /** Consumes one unit of the given ammo type. */
    public void consumeAmmo(LauncherType type) {
        if (ammo != null) ammo.consume(type);
    }

    /** Adds ammo (e.g., nuclear resupply after quiz). */
    public void resupplyAmmo(LauncherType type, int amount) {
        if (ammo != null) ammo.resupply(type, amount);
    }

    public LauncherType getSelectedLauncher() { return selectedLauncher; }
    public Orientation getLauncherOrientation() { return launcherOrientation; }
}
