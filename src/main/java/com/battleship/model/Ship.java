package com.battleship.model;

import java.util.ArrayList;
import java.util.List;

/** A placed ship: tracks its occupied coordinates and hit state. */
public class Ship {

    private final ShipType type;
    private final List<Coordinate> occupiedCells;
    private final boolean isHorizontal;
    private int hits;

    public Ship(ShipType type, List<Coordinate> occupiedCells, boolean isHorizontal) {
        this.type = type;
        this.occupiedCells = new ArrayList<>(occupiedCells);
        this.isHorizontal = isHorizontal;
        this.hits = 0;
    }

    /** Registers a hit at the given coordinate if it belongs to this ship. */
    public boolean registerHit(Coordinate c) {
        if (occupiedCells.contains(c)) {
            hits++;
            return true;
        }
        return false;
    }

    public boolean isSunk() {
        return hits >= type.getSize();
    }

    public ShipType getType() { return type; }
    public List<Coordinate> getOccupiedCells() { return occupiedCells; }
    public boolean isHorizontal() { return isHorizontal; }
    public int getHits() { return hits; }
}
