package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates all ship-placement logic: fleet-remaining accounting, legality
 * checks, placement/removal, and random auto-deployment. Extracted from
 * GameController so the controller can stay a thin mediator (SRP).
 */
public class PlacementService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Ship types still needed for the player, keyed by type, with remaining count. */
    public Map<ShipType, Integer> getRemainingShipCounts(Player player, Theater theater) {
        Map<ShipType, Integer> remaining = new LinkedHashMap<>(theater.getFleetComposition());
        for (Ship s : player.getOwnBoard().getShips()) {
            remaining.merge(s.getType(), -1, Integer::sum);
        }
        remaining.entrySet().removeIf(e -> e.getValue() <= 0);
        return remaining;
    }

    /** Places a ship only if the fleet composition still allows it and the board does too. */
    public boolean placeShip(Player player, Theater theater,
                             ShipType type, Coordinate start, Orientation orientation) {
        Map<ShipType, Integer> remaining = getRemainingShipCounts(player, theater);
        if (!remaining.containsKey(type) || remaining.get(type) <= 0) return false;
        return player.getOwnBoard().placeShip(type, start, orientation);
    }

    /** Validates placement without mutating state. */
    public boolean canPlace(Player player, ShipType type, Coordinate start, Orientation orientation) {
        return player.getOwnBoard().isValidPlacement(type, start, orientation);
    }

    /** Pulls an already-placed ship back off the board ("put ship back"). */
    public boolean removeShip(Player player, Ship ship) {
        return player.getOwnBoard().removeShip(ship);
    }

    /** Pulls an already-placed ship at the given coordinate back off the board. */
    public boolean removeShipAt(Player player, Coordinate c) {
        return player.getOwnBoard().removeShipAt(c);
    }

    public boolean isPlacementComplete(Player player, Theater theater) {
        return player.getOwnBoard().getShips().size() == theater.getTotalShipCount();
    }

    public void resetPlacement(Player player) {
        player.getOwnBoard().clearShips();
    }

    /** Randomly places all remaining ships for the player (spec 4.2, retry until success). */
    public void autoPlaceAll(Player player, Theater theater) {
        Map<ShipType, Integer> remaining = getRemainingShipCounts(player, theater);
        int size = theater.getBoardSize();
        for (Map.Entry<ShipType, Integer> entry : remaining.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                boolean placed = false;
                for (int attempt = 0; attempt < 10_000 && !placed; attempt++) {
                    Coordinate start = new Coordinate(RANDOM.nextInt(size), RANDOM.nextInt(size));
                    placed = player.getOwnBoard().placeShip(entry.getKey(), start, Orientation.random(RANDOM));
                }
            }
        }
    }
}