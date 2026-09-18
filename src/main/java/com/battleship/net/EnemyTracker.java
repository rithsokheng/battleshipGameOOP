package com.battleship.net;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Ship;
import com.battleship.model.ShipType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * In network play we never receive the opponent's real ship layout (that would
 * defeat the game) — we only ever learn cell outcomes from FIRE_RESULT messages.
 * This tracks exactly that, in a shape the existing BoardGridPane renderer can
 * consume (renderShot / renderSunkShip).
 */
public class EnemyTracker {

    private final int size;
    private final CellStatus[][] grid;
    private final List<Ship> knownSunkShips = new ArrayList<>();

    public EnemyTracker(int size) {
        this.size = size;
        this.grid = new CellStatus[size][size];
        for (CellStatus[] row : grid) Arrays.fill(row, CellStatus.EMPTY);
    }

    public void recordMiss(Coordinate c) { grid[c.getRow()][c.getCol()] = CellStatus.MISS; }
    public void recordHit(Coordinate c) { grid[c.getRow()][c.getCol()] = CellStatus.HIT; }

    public Ship recordSunk(ShipType type, List<Coordinate> cells) {
        for (Coordinate c : cells) grid[c.getRow()][c.getCol()] = CellStatus.SUNK;
        Ship ship = new Ship(type, cells, Orientation.HORIZONTAL);
        knownSunkShips.add(ship);
        return ship;
    }

    public CellStatus getStatus(Coordinate c) { return grid[c.getRow()][c.getCol()]; }
    public List<Ship> getKnownSunkShips() { return Collections.unmodifiableList(knownSunkShips); }
    public int getSize() { return size; }
}
