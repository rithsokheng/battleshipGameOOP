package com.battleship.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The 3 launcher types a player can select before firing.
 * DEFAULT is always available with infinite ammo; LEVEL_2 is a limited-ammo
 * weapon that only unlocks on larger boards. NUCLEAR is now available on
 * every battlefield size (5x5, 8x8, 10x10), starting with a single warhead
 * on each, which auto-resupplies after use.
 *
 * Each constant implements its own blast-pattern logic via
 * {@link #getTargetCells(Coordinate, boolean)}.
 */
public enum LauncherType {

    DEFAULT("Default", 1) {
        @Override
        public List<Coordinate> getTargetCells(Coordinate anchor, boolean horizontal) {
            return List.of(anchor);
        }
    },
    LEVEL_2("Level 2", 3) {
        @Override
        public List<Coordinate> getTargetCells(Coordinate anchor, boolean horizontal) {
            List<Coordinate> cells = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                int r = horizontal ? anchor.getRow() : anchor.getRow() + i;
                int c = horizontal ? anchor.getCol() + i : anchor.getCol();
                cells.add(new Coordinate(r, c));
            }
            return cells;
        }
    },
    NUCLEAR("Nuclear", 6) {
        @Override
        public List<Coordinate> getTargetCells(Coordinate anchor, boolean horizontal) {
            int rows = horizontal ? 2 : 3;
            int cols = horizontal ? 3 : 2;
            List<Coordinate> cells = new ArrayList<>(6);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    cells.add(new Coordinate(anchor.getRow() + r, anchor.getCol() + c));
                }
            }
            return cells;
        }
    };

    private final String label;
    private final int cellCount;

    LauncherType(String label, int cellCount) {
        this.label = label;
        this.cellCount = cellCount;
    }

    public String getLabel() { return label; }
    public int getCellCount() { return cellCount; }

    /**
     * Computes the list of coordinates that this launcher's blast pattern covers,
     * given an anchor coordinate and orientation.
     *
     * @param anchor     the cell the player clicked (top-left corner for area weapons)
     * @param horizontal true = line/rect grows rightward; false = grows downward
     */
    public abstract List<Coordinate> getTargetCells(Coordinate anchor, boolean horizontal);

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
