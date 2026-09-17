package com.battleship.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A placed ship: tracks its occupied coordinates and hit state. */
public class Ship {

    private final ShipType type;
    private final List<Coordinate> occupiedCells;
    private final boolean isHorizontal;
    private final Set<Coordinate> hitCells = new HashSet<>();

    public Ship(ShipType type, List<Coordinate> occupiedCells, boolean isHorizontal) {
        if (occupiedCells.size() != type.getSize()) {
            throw new IllegalArgumentException(
                    "Expected " + type.getSize() + " cells for " + type + ", got " + occupiedCells.size());
        }
        this.type = type;
        this.occupiedCells = new ArrayList<>(occupiedCells);
        this.isHorizontal = isHorizontal;
    }

    /** Registers a hit at the given coordinate if it belongs to this ship. Idempotent. */
    public boolean registerHit(Coordinate c) {
        if (occupiedCells.contains(c)) {
            hitCells.add(c);
            return true;
        }
        return false;
    }

    public boolean isSunk() {
        return hitCells.size() >= type.getSize();
    }

    public ShipType getType() { return type; }
    public List<Coordinate> getOccupiedCells() { return Collections.unmodifiableList(occupiedCells); }
    public boolean isHorizontal() { return isHorizontal; }
    public int getHits() { return hitCells.size(); }
}
