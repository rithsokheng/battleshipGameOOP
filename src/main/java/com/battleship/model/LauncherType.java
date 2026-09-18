package com.battleship.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The 3 launcher types a player can select before firing.
 * DEFAULT is always available with infinite ammo; LEVEL_2 is a limited-ammo
 * weapon that only unlocks on larger boards. NUCLEAR is available on every
 * battlefield size (5x5, 8x8, 10x10), starting with a single warhead on each,
 * which auto-resupplies after use.
 *
 * Every constant implements its own behavior via abstract-method overrides
 * (availability, starting ammo, blast pattern, pattern dimensions) — adding a
 * new launcher requires adding exactly one enum constant and nothing else
 * (Open/Closed Principle).
 */
public enum LauncherType {

    DEFAULT("Default", 1) {
        @Override
        public List<Coordinate> getTargetCells(Coordinate anchor, Orientation orientation) {
            return List.of(anchor);
        }
        @Override
        public boolean isAvailableFor(int boardSize) { return true; }
        @Override
        public int getStartingAmmo(int boardSize) { return Integer.MAX_VALUE; } // infinite
        @Override
        public int[][] patternDimensions() { return new int[][]{{1, 1}}; }
    },
    LEVEL_2("Level 2", 3) {
        @Override
        public List<Coordinate> getTargetCells(Coordinate anchor, Orientation orientation) {
            boolean horizontal = orientation.isHorizontal();
            List<Coordinate> cells = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                int r = horizontal ? anchor.getRow() : anchor.getRow() + i;
                int c = horizontal ? anchor.getCol() + i : anchor.getCol();
                cells.add(new Coordinate(r, c));
            }
            return cells;
        }
        @Override
        public boolean isAvailableFor(int boardSize) { return boardSize >= 8; }
        @Override
        public int getStartingAmmo(int boardSize) {
            return boardSize >= 10 ? 3 : (boardSize >= 8 ? 2 : 0);
        }
        @Override
        public int[][] patternDimensions() { return new int[][]{{1, 3}, {3, 1}}; }
    },
    NUCLEAR("Nuclear", 6) {
        @Override
        public List<Coordinate> getTargetCells(Coordinate anchor, Orientation orientation) {
            boolean horizontal = orientation.isHorizontal();
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
        @Override
        public boolean isAvailableFor(int boardSize) { return true; }
        @Override
        public int getStartingAmmo(int boardSize) { return 1; }
        @Override
        public int[][] patternDimensions() { return new int[][]{{2, 3}, {3, 2}}; }
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
     * @param anchor      the cell the player clicked (top-left corner for area weapons)
     * @param orientation HORIZONTAL = line/rect grows rightward; VERTICAL = grows downward
     */
    public abstract List<Coordinate> getTargetCells(Coordinate anchor, Orientation orientation);

    /** Whether this launcher can be used at all on a board of the given size. */
    public abstract boolean isAvailableFor(int boardSize);

    /** Starting ammo for a battle on a board of the given size. */
    public abstract int getStartingAmmo(int boardSize);

    /**
     * The blast-block dimensions this launcher covers, as {height, width} pairs —
     * one pair per orientation (HORIZONTAL first, then VERTICAL). Lets callers
     * (e.g. SmartAI) evaluate area coverage without hardcoding type-specific dims.
     */
    public abstract int[][] patternDimensions();
}
