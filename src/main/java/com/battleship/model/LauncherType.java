package com.battleship.model;

/**
 * The 3 launcher types a player can select before firing.
 * DEFAULT is always available with infinite ammo; LEVEL_2 is a limited-ammo
 * weapon that only unlocks on larger boards. NUCLEAR is now available on
 * every battlefield size (5x5, 8x8, 10x10), starting with a single warhead
 * on each, which auto-resupplies after use.
 */
public enum LauncherType {

    DEFAULT("Default", 1),
    LEVEL_2("Level 2", 3),
    NUCLEAR("Nuclear", 6);

    private final String label;
    private final int cellCount;

    LauncherType(String label, int cellCount) {
        this.label = label;
        this.cellCount = cellCount;
    }

    public String getLabel() { return label; }
    public int getCellCount() { return cellCount; }

    /** Whether this launcher can be used at all on a board of the given size. */
    public boolean isAvailableFor(int boardSize) {
        return switch (this) {
            case DEFAULT -> true;
            case LEVEL_2 -> boardSize >= 8;
            case NUCLEAR -> true; // unlocked on all battlefields: 5x5, 8x8, 10x10
        };
    }

    /** Starting ammo for a battle on a board of the given size. */
    public int getStartingAmmo(int boardSize) {
        return switch (this) {
            case DEFAULT -> Integer.MAX_VALUE; // infinite
            case LEVEL_2 -> boardSize >= 10 ? 3 : (boardSize >= 8 ? 2 : 0);
            case NUCLEAR -> 1; // one warhead to start on every battlefield size
        };
    }
}
