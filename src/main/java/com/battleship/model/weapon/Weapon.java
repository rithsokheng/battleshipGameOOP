package com.battleship.model.weapon;

import com.battleship.model.Orientation;

/**
 * A selectable weapon: its identity, its ammunition rules and its blast geometry.
 *
 * <p>Replaces the {@code LauncherType} enum (V3.1). Behavior now lives in
 * ordinary classes implementing this interface, so a plugin may add a
 * {@code Torpedo}, {@code SonarSweep} or {@code ClusterBomb} by shipping a new
 * implementation and registering it in {@link WeaponCatalog} — no core type is
 * recompiled (Open/Closed Principle).</p>
 */
public interface Weapon {

    /** Stable identifier used by the wire protocol and for persistence. */
    String id();

    /** Label shown on the weapon console. */
    String displayName();

    /** Ammunition granted for a battle on a board of the given size. */
    int startingAmmo(int boardSize);

    /** Whether this weapon is offered at all on a board of the given size. */
    boolean availableFor(int boardSize);

    /** {@code true} when the weapon never runs out (its stock is not decremented). */
    boolean hasInfiniteAmmo();

    /** The footprint this weapon covers from an anchor, per orientation. */
    BlastPattern blastPattern();

    /** The coordinates covered when this weapon is fired from an anchor. */
    default java.util.List<com.battleship.model.Coordinate> calculateBlastArea(
            com.battleship.model.Coordinate anchor, Orientation orientation) {
        return blastPattern().coverage(anchor, orientation);
    }
}
