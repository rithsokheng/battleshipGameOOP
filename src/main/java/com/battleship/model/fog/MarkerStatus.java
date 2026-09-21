package com.battleship.model.fog;

/**
 * What a player can know about one enemy cell.
 *
 * <p>Deliberately <em>not</em> {@code CellStatus}: the tracking grid can never
 * hold {@code SHIP} or {@code EMPTY}, because those are facts about the
 * opponent's real grid. Modelling knowledge with its own closed set of markers
 * makes the fog-of-war leak (V1.3) impossible to express.</p>
 */
public enum MarkerStatus {
    /** Never observed — fog of war. */
    UNKNOWN,
    /** A shot landed in open water. */
    MISS,
    /** A shot hit a hull that is still afloat. */
    HIT,
    /** Part of a hull that has been reported sunk. */
    SUNK;

    /** True once a shot has been resolved on this cell. */
    public boolean isShelled() {
        return this != UNKNOWN;
    }

    /** Maps a resolved grid cell outcome into a fog-of-war tracking marker. */
    public static MarkerStatus from(com.battleship.model.CellStatus status) {
        return switch (status) {
            case HIT -> HIT;
            case SUNK -> SUNK;
            default -> MISS;
        };
    }
}

