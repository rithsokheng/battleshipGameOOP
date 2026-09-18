package com.battleship.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root representing one player's grid: cell states, placed ships,
 * and shot resolution. Pure domain object — no JavaFX dependency.
 */
public class Board {

    private final int size;
    private final CellStatus[][] grid;
    private final Ship[][] shipGrid; // null where no ship
    private final List<Ship> ships;

    public Board(int size) {
        this.size = size;
        this.grid = new CellStatus[size][size];
        this.shipGrid = new Ship[size][size];
        this.ships = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = CellStatus.EMPTY;
            }
        }
    }

    private List<Coordinate> computeCells(ShipType type, Coordinate start, Orientation orientation) {
        boolean horizontal = orientation.isHorizontal();
        List<Coordinate> cells = new ArrayList<>();
        for (int i = 0; i < type.getSize(); i++) {
            int r = horizontal ? start.getRow() : start.getRow() + i;
            int c = horizontal ? start.getCol() + i : start.getCol();
            cells.add(new Coordinate(r, c));
        }
        return cells;
    }

    /** Validates ship placement without mutating state. */
    public boolean isValidPlacement(ShipType type, Coordinate start, Orientation orientation) {
        List<Coordinate> cells = computeCells(type, start, orientation);
        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) return false;
            if (shipGrid[c.getRow()][c.getCol()] != null) return false;
        }
        return true;
    }

    /** Places a ship of the given type/orientation if valid; returns false otherwise. */
    public boolean placeShip(ShipType type, Coordinate start, Orientation orientation) {
        if (!isValidPlacement(type, start, orientation)) return false;
        List<Coordinate> cells = computeCells(type, start, orientation);
        Ship ship = new Ship(type, cells, orientation);
        for (Coordinate c : cells) {
            shipGrid[c.getRow()][c.getCol()] = ship;
            grid[c.getRow()][c.getCol()] = CellStatus.SHIP;
        }
        ships.add(ship);
        return true;
    }

    /** Removes a single already-placed ship (used by "put ship back" in placement UI). */
    public boolean removeShip(Ship ship) {
        if (ship == null || !ships.contains(ship)) return false;
        for (Coordinate c : ship.getOccupiedCells()) {
            shipGrid[c.getRow()][c.getCol()] = null;
            grid[c.getRow()][c.getCol()] = CellStatus.EMPTY;
        }
        ships.remove(ship);
        return true;
    }

    /** Removes all placed ships (used by RESET in placement UI). */
    public void clearShips() {
        ships.clear();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                shipGrid[r][c] = null;
                grid[r][c] = CellStatus.EMPTY;
            }
        }
    }

    /** Resolves a shot at the given coordinate and updates grid state. */
    public ShotResult receiveShot(Coordinate c) {
        if (!c.isWithinBounds(size)) {
            throw new IllegalArgumentException("Coordinate " + c + " is out of bounds for board of size " + size);
        }
        // Guard: if cell already resolved, return current status as no-op.
        CellStatus existing = grid[c.getRow()][c.getCol()];
        if (existing == CellStatus.HIT || existing == CellStatus.MISS || existing == CellStatus.SUNK) {
            return new ShotResult(c, existing, null);
        }

        Ship ship = shipGrid[c.getRow()][c.getCol()];
        if (ship == null) {
            grid[c.getRow()][c.getCol()] = CellStatus.MISS;
            return new ShotResult(c, CellStatus.MISS, null);
        }
        ship.registerHit(c);
        if (ship.isSunk()) {
            for (Coordinate sc : ship.getOccupiedCells()) {
                grid[sc.getRow()][sc.getCol()] = CellStatus.SUNK;
            }
            return new ShotResult(c, CellStatus.SUNK, ship);
        } else {
            grid[c.getRow()][c.getCol()] = CellStatus.HIT;
            return new ShotResult(c, CellStatus.HIT, null);
        }
    }

    public List<Coordinate> getUnshotCells() {
        List<Coordinate> result = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                CellStatus status = grid[r][c];
                if (status == CellStatus.EMPTY || status == CellStatus.SHIP) {
                    result.add(new Coordinate(r, c));
                }
            }
        }
        return result;
    }

    public boolean isAllShipsSunk() {
        if (ships.isEmpty()) return false;
        return ships.stream().allMatch(Ship::isSunk);
    }

    public int getSize() { return size; }

    public CellStatus getCellStatus(Coordinate c) {
        if (!c.isWithinBounds(size)) {
            throw new IllegalArgumentException("Coordinate " + c + " is out of bounds for board of size " + size);
        }
        return grid[c.getRow()][c.getCol()];
    }

    public List<Ship> getShips() { return Collections.unmodifiableList(ships); }

    public Ship getShipAt(Coordinate c) {
        if (!c.isWithinBounds(size)) {
            throw new IllegalArgumentException("Coordinate " + c + " is out of bounds for board of size " + size);
        }
        return shipGrid[c.getRow()][c.getCol()];
    }
}
