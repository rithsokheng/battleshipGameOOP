package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A placed ship: tracks its occupied coordinates and hit state.
 *
 * <p><strong>Package-private on purpose.</strong> Fixes V1.2 — the hull used to be
 * public and was handed to views and AI through {@code ReadOnlyBoard.getShips()},
 * letting any caller run {@code registerHit(...)} and corrupt the defender's
 * fleet. Outside this package a ship is only ever seen as a {@link ShipSnapshot}.</p>
 */
final class Ship {

    private final ShipType type;
    private final List<Coordinate> occupiedCells;
    private final Orientation orientation;
    private final Set<Coordinate> hitCells = new HashSet<>();

    Ship(ShipType type, List<Coordinate> occupiedCells, Orientation orientation) {
        if (occupiedCells.size() != type.getSize()) {
            throw new IllegalArgumentException(
                    "Expected " + type.getSize() + " cells for " + type + ", got " + occupiedCells.size());
        }
        this.type = type;
        this.occupiedCells = new ArrayList<>(occupiedCells);
        this.orientation = orientation;
    }

    /** Registers a hit at the given coordinate if it belongs to this ship. Idempotent. */
    boolean registerHit(Coordinate c) {
        if (occupiedCells.contains(c)) {
            hitCells.add(c);
            return true;
        }
        return false;
    }

    boolean isSunk() {
        return hitCells.size() >= type.getSize();
    }

    /** Immutable projection of this hull, safe to hand outside the domain. */
    ShipSnapshot snapshot() {
        return new ShipSnapshot(type, occupiedCells, orientation, hitCells.size(), isSunk());
    }

    ShipType type() {
        return type;
    }

    List<Coordinate> occupiedCells() {
        return Collections.unmodifiableList(occupiedCells);
    }
}
