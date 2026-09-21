package com.battleship.model.projection;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;

import java.util.List;

/**
 * Immutable projection of a placed ship, safe to hand to UI or AI layers.
 *
 * <p>Fixes V1.2: the mutable {@code Ship} domain entity is never exposed outside
 * {@code com.battleship.model} any more. Callers used to be able to write
 * {@code readOnlyBoard.getShips().get(0).registerHit(c)} and corrupt the game.
 * This record has no mutating methods, and its coordinate list is defensively
 * copied, so it satisfies the read-only contract it is handed out under
 * (Liskov Substitution Principle).</p>
 *
 * @param type       the ship class
 * @param cells      every occupied coordinate, in hull order
 * @param orientation the hull's orientation
 * @param hitCount   how many of the occupied cells have been hit
 * @param isSunk     whether the whole hull has been destroyed
 */
public record ShipSnapshot(
        ShipType type,
        List<Coordinate> cells,
        Orientation orientation,
        int hitCount,
        boolean isSunk
) {
    public ShipSnapshot {
        if (cells == null) {
            throw new IllegalArgumentException("A ship snapshot requires its occupied cells.");
        }
        cells = List.copyOf(cells);
    }

    /** Convenience: the hull length in cells. */
    public int length() {
        return cells.size();
    }
}
