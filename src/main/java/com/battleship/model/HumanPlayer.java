package com.battleship.model;

import java.util.Map;

/**
 * A person at the keyboard (or two, in hotseat). Supplies shots through the UI
 * and learns nothing automatically — see {@link Player#decideAutonomousShot()}
 * and {@link Player#observeOwnShot(ShotResult)}, whose default implementations
 * are exactly this class's behaviour.
 */
public final class HumanPlayer extends Player {

    public HumanPlayer(String name, int boardSize, Map<ShipType, Integer> enemyFleetComposition) {
        super(name, boardSize, enemyFleetComposition);
    }

    /** Convenience for the standard match flow: board size and roster come from the theater. */
    public HumanPlayer(String name, Theater theater) {
        this(name, theater.getBoardSize(), theater.getFleetComposition());
    }

    @Override
    public boolean isAutonomous() {
        return false;
    }
}
