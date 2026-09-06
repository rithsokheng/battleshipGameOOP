package com.battleship.model;

/**
 * A participant in the game — human or AI-controlled.
 * ownBoard holds this player's ships; shots are resolved against the opponent's Board.
 */
public class Player {

    private final String name;
    private final boolean isHuman;
    private final Board ownBoard;

    // --- Launcher system ---
    private int level2Ammo;
    private int nuclearAmmo;
    private LauncherType selectedLauncher = LauncherType.DEFAULT;
    private boolean launcherHorizontal = true;

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
        this.level2Ammo = LauncherType.LEVEL_2.getStartingAmmo(boardSize);
        this.nuclearAmmo = LauncherType.NUCLEAR.getStartingAmmo(boardSize);
        this.selectedLauncher = LauncherType.DEFAULT;
        this.launcherHorizontal = true;
    }

    public String getName() { return name; }
    public boolean isHuman() { return isHuman; }
    public Board getOwnBoard() { return ownBoard; }

    public int getLevel2Ammo() { return level2Ammo; }
    public void setLevel2Ammo(int level2Ammo) { this.level2Ammo = level2Ammo; }

    public int getNuclearAmmo() { return nuclearAmmo; }
    public void setNuclearAmmo(int nuclearAmmo) { this.nuclearAmmo = nuclearAmmo; }

    public LauncherType getSelectedLauncher() { return selectedLauncher; }
    public void setSelectedLauncher(LauncherType selectedLauncher) { this.selectedLauncher = selectedLauncher; }

    public boolean isLauncherHorizontal() { return launcherHorizontal; }
    public void setLauncherHorizontal(boolean launcherHorizontal) { this.launcherHorizontal = launcherHorizontal; }
}
