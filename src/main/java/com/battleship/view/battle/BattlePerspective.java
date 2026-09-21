package com.battleship.view.battle;

import com.battleship.model.FleetReadout;
import com.battleship.model.fog.TrackingGrid;

/**
 * Strategy interface defining the viewport perspective for local combat rendering.
 *
 * <p>Satisfies the <strong>Open/Closed Principle (OCP)</strong> by decoupling the
 * viewpoint determination (fixed vs. alternating hotseat vs. spectator) from the
 * battle screen drawing routines.</p>
 */
public interface BattlePerspective {

    /** The display name of the Admiral owning this viewport perspective. */
    String perspectiveName();

    /** Read-only view of the fleet defending this viewport's waters. */
    FleetReadout perspectiveFleet();

    /** The display name of the opposing commander. */
    String opponentName();

    /** The knowledge tracking grid containing observed enemy waters from this perspective. */
    TrackingGrid opponentKnowledge();
}
