package com.battleship.model;

import com.battleship.model.weapon.Weapon;

/**
 * A complete firing order: which weapon, where, and in which orientation.
 *
 * <p>Used by the AI pipeline ("choose, then fire") and by the network layer when
 * a shot travels over the wire. Replaces the duplicated {@code AiShotPlan} record.</p>
 */
public record ShotOrder(Weapon weapon, Coordinate anchor, Orientation orientation) {
}
