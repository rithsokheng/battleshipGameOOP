package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.FleetDeployment;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import com.battleship.model.projection.ShipSnapshot;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates all ship-placement logic: fleet-remaining accounting, legality
 * checks, deployment/removal, and random auto-deployment. Extracted from
 * GameController so the controller can stay a thin mediator (SRP).
 *
 * <p>Fixes Smell 5.1 (Law of Demeter): this service used to reach through the
 * player to grab a mutable board —
 * {@code player.getMutableBoard().placeShip(...)} — which is textbook
 * Feature Envy. It now operates exclusively on the {@link FleetDeployment}
 * command interface, so it neither knows nor cares that players exist.</p>
 */
public class PlacementService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Ship types still needed, keyed by type, with remaining count. */
    public Map<ShipType, Integer> getRemainingShipCounts(FleetDeployment deployment, Theater theater) {
        Map<ShipType, Integer> remaining = new LinkedHashMap<>(theater.getFleetComposition());
        for (ShipSnapshot ship : deployment.fleet()) {
            remaining.merge(ship.type(), -1, Integer::sum);
        }
        remaining.entrySet().removeIf(e -> e.getValue() <= 0);
        return remaining;
    }

    /** Deploys a ship only if the fleet composition still allows it and the grid does too. */
    public boolean deploy(FleetDeployment deployment, Theater theater,
                          ShipType type, Coordinate start, Orientation orientation) {
        Map<ShipType, Integer> remaining = getRemainingShipCounts(deployment, theater);
        if (remaining.getOrDefault(type, 0) <= 0) return false;
        return deployment.deploy(type, start, orientation);
    }

    /** Validates a deployment without mutating state. */
    public boolean canDeploy(FleetDeployment deployment, ShipType type, Coordinate start, Orientation orientation) {
        return deployment.canDeploy(type, start, orientation);
    }

    /** Pulls an already-deployed hull at the given coordinate back into the dock. */
    public boolean undeployAt(FleetDeployment deployment, Coordinate c) {
        return deployment.undeployAt(c);
    }

    public boolean isDeploymentComplete(FleetDeployment deployment, Theater theater) {
        return deployment.fleet().size() == theater.getTotalShipCount();
    }

    public void resetDeployment(FleetDeployment deployment) {
        deployment.clearDeployment();
    }

    /** Randomly deploys all remaining ships (spec 4.2, retry until success). */
    public void autoDeployAll(FleetDeployment deployment, Theater theater) {
        Map<ShipType, Integer> remaining = getRemainingShipCounts(deployment, theater);
        int size = theater.getBoardSize();
        for (Map.Entry<ShipType, Integer> entry : remaining.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                boolean placed = false;
                for (int attempt = 0; attempt < 10_000 && !placed; attempt++) {
                    Coordinate start = new Coordinate(RANDOM.nextInt(size), RANDOM.nextInt(size));
                    placed = deployment.deploy(entry.getKey(), start, Orientation.random(RANDOM));
                }
            }
        }
    }
}
