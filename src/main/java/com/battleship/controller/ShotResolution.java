package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShotTarget;
import com.battleship.model.weapon.Weapon;

/**
 * Injectable strategy for resolving one weapon shot against a target grid
 * (fixes F5, DIP). Production uses {@link ShotResolver#STANDARD}; tests can
 * substitute a stub/fake without any static coupling.
 *
 * <p>The target is a {@link ShotTarget} — the narrow "receive a shot" command
 * surface — rather than a mutable board (V1.1).</p>
 */
@FunctionalInterface
public interface ShotResolution {

    /**
     * Fires the given weapon's blast pattern against the target.
     * Already-resolved cells and out-of-bounds cells within the pattern are
     * skipped; ammunition bookkeeping is the caller's responsibility.
     *
     * @return the aggregated result (per-cell outcomes + hulls sunk by this shot)
     */
    LauncherFireResult resolve(ShotTarget target, Weapon weapon,
                               Coordinate anchor, Orientation orientation);
}
