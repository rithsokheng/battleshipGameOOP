package com.battleship.model;

/** Ship classes with their fixed length in cells. */
public enum ShipType {
    PATROL_BOAT(2),
    DESTROYER(2),
    SUBMARINE(3),
    CRUISER(3),
    BATTLESHIP(4),
    CARRIER(5);

    private final int size;

    ShipType(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
