package com.battleship.model;

/**
 * Outcome of a single shot. shipSunk is null unless this shot sank a ship.
 */
public record ShotResult(Coordinate coordinate, CellStatus outcome, Ship shipSunk) {

    public boolean isHit() {
        return outcome == CellStatus.HIT || outcome == CellStatus.SUNK;
    }
}
