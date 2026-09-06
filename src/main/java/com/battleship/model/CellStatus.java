package com.battleship.model;

/** State of a single grid cell. */
public enum CellStatus {
    EMPTY,
    SHIP,
    HIT,
    MISS,
    SUNK
}
