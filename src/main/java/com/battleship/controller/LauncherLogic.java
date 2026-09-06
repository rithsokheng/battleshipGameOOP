package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculation of which cells a launcher shot covers, given an anchor
 * coordinate and orientation. Does not check board bounds or cell state —
 * GameController is responsible for skipping out-of-bounds / already-shot cells.
 */
public class LauncherLogic {

    private LauncherLogic() { }

    /**
     * @param anchor     the cell the player clicked (top-left corner for area weapons)
     * @param horizontal true = line/rect grows rightward (2 rows x 3 cols for Nuclear);
     *                   false = line/rect grows downward (3 rows x 2 cols for Nuclear)
     */
    public static List<Coordinate> getTargetCells(LauncherType type, Coordinate anchor, boolean horizontal) {
        List<Coordinate> cells = new ArrayList<>();
        switch (type) {
            case DEFAULT -> cells.add(anchor);

            case LEVEL_2 -> {
                for (int i = 0; i < 3; i++) {
                    int r = horizontal ? anchor.getRow() : anchor.getRow() + i;
                    int c = horizontal ? anchor.getCol() + i : anchor.getCol();
                    cells.add(new Coordinate(r, c));
                }
            }

            case NUCLEAR -> {
                int rows = horizontal ? 2 : 3;
                int cols = horizontal ? 3 : 2;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        cells.add(new Coordinate(anchor.getRow() + r, anchor.getCol() + c));
                    }
                }
            }
        }
        return cells;
    }
}
