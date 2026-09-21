package com.battleship.model;

import com.battleship.model.projection.ShipSnapshot;

import java.util.List;

/**
 * Read-only projection of <em>your own</em> grid, safe to hand to the UI.
 *
 * <p>Replaces {@code ReadOnlyBoard} (V1.2). Two things changed: the fleet is
 * projected as immutable {@link ShipSnapshot}s instead of live {@link Ship}
 * entities, and the mutating operations are not merely absent from the interface
 * but unreachable, because nothing hands out the underlying aggregate at all.</p>
 */
public interface FleetReadout {

    int size();

    /** Resolved state of one of your own cells (may legitimately be SHIP). */
    CellStatus cellStatus(Coordinate c);

    /** Your hulls as immutable snapshots. */
    List<ShipSnapshot> fleet();

    /** True once every one of your hulls has been destroyed. */
    boolean isFleetDestroyed();
}
