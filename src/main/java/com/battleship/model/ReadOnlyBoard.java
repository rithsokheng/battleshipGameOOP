package com.battleship.model;

import java.util.List;

/**
 * Read-only view of a player's board (fixes F1). Views receive this interface
 * instead of the mutable {@link Board} aggregate, so cell/ship state can be
 * queried but never mutated (no placeShip / receiveShot / clearShips).
 */
public interface ReadOnlyBoard {

    int getSize();

    CellStatus getCellStatus(Coordinate c);

    List<Ship> getShips();

    boolean isAllShipsSunk();

    List<Coordinate> getUnshotCells();
}
