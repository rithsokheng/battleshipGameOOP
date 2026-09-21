package com.battleship.model;

/**
 * The defensive surface of a fleet: the only operations an opponent's shot is
 * allowed to perform against someone's primary grid.
 *
 * <p>This is the "pass commands to the aggregate" half of the V1.1 fix. The old
 * API handed out a fully mutable {@code Board} so that services could place
 * ships, remove ships, shell cells and read the whole layout. A shot actually
 * needs exactly one command — {@link #receiveShot(Coordinate)} — plus the
 * ability to skip cells that were already resolved.</p>
 */
public interface ShotTarget {

    int size();

    /** True when the cell has already been HIT, MISS or SUNK. */
    boolean isCellResolved(Coordinate c);

    /** Resolves a shot: the grid applies its own hit/sink invariants and reports the outcome. */
    ShotResult receiveShot(Coordinate c);
}
