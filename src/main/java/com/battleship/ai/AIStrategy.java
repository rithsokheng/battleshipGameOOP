package com.battleship.ai;

import com.battleship.model.AmmoReadout;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.weapon.WeaponCatalog;

/**
 * Strategy pattern contract for AI opponents. Pure logic — no JavaFX, and no
 * access to the opponent's real grid.
 *
 * <p>Fixes V1.3: the parameter is a {@link TrackingGrid} (what the admiral knows)
 * instead of the defender's {@code Board}. An AI physically cannot read the
 * enemy's ship layout any more; it reasons from shot outcomes, announced wrecks
 * and the published fleet roster exactly like a human does.</p>
 *
 * <p>Ammunition arrives as a read-only {@link AmmoReadout} (fixes V1), so a
 * strategy may plan around its stock but can never consume or resupply it.</p>
 */
public interface AIStrategy {

    /** Chooses the next cell to fire at, given everything known so far. */
    Coordinate chooseTarget(TrackingGrid knowledge);

    /** Optional hook so stateful strategies (Hunt/Target) can update after each shot. */
    void notifyResult(ShotResult result);

    /**
     * Chooses a full firing order (weapon + anchor + orientation) for this turn.
     * The default fires the infinite standard shell through {@link #chooseTarget},
     * so a strategy that never overrides this stays on the standard weapon.
     */
    default ShotOrder chooseShotPlan(TrackingGrid knowledge, AmmoReadout ammo) {
        return new ShotOrder(WeaponCatalog.standard(), chooseTarget(knowledge), Orientation.HORIZONTAL);
    }
}
