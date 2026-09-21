package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

import java.util.List;

/**
 * The deployment commands a fleet owner accepts during ship placement.
 *
 * <p>Fixes Smell 5.1 (Law of Demeter): {@code PlacementService} used to do
 * {@code player.getMutableBoard().placeShip(...)}, reaching through the player's
 * guts into the board. It now talks to this narrow command interface, which
 * {@link Player} implements on behalf of its private grid — so the service has
 * no idea a {@code Player} (or a {@code PrimaryGrid}) exists, and no caller can
 * obtain a board and wipe it.</p>
 */
public interface FleetDeployment {

    int size();

    /** Validates a deployment without mutating anything. */
    boolean canDeploy(ShipType type, Coordinate start, Orientation orientation);

    /** Places a ship if the position is legal; returns whether it was placed. */
    boolean deploy(ShipType type, Coordinate start, Orientation orientation);

    /** Pulls back whatever hull occupies the given cell. */
    boolean undeployAt(Coordinate c);

    /** Removes every hull (RESET in the placement UI). */
    void clearDeployment();

    /** The hulls currently deployed, as immutable snapshots. */
    List<ShipSnapshot> fleet();
}
