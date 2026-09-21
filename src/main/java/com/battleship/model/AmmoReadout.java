package com.battleship.model;

import com.battleship.model.weapon.Weapon;

/**
 * Read-only view of a combatant's ammunition, handed to AI strategies and the UI
 * so they can *plan* around weapon stock without ever mutating it (fixes V1:
 * the mutable inventory never leaves {@link Player}).
 */
public interface AmmoReadout {

    /** Current rounds for this weapon (or 0 when the weapon is not stocked). */
    int ammoCount(Weapon weapon);

    /** True when at least one round is available. */
    boolean hasAmmo(Weapon weapon);

    /** True for weapons that never run out. */
    boolean isAmmoInfinite(Weapon weapon);
}
