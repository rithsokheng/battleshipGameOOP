package com.battleship.model;

import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A player's weapon system: what is in the magazine, which weapon is armed, and
 * which way it points.
 *
 * <p>Fixes the SRP half of the {@code Player} audit. The participant entity used to
 * be an accumulator of unrelated concerns — identity, board ownership, weapon
 * selection, aiming direction, ammunition bookkeeping and resupply. Weapon state
 * now lives here, and {@link Player} only forwards the few commands the rest of
 * the game is allowed to issue.</p>
 *
 * <p>The class is package-private: the mutable stock cannot be reached from the
 * controller, the view or the AI, which is exactly the encapsulation the old
 * public {@code AmmoInventory} gave away.</p>
 */
final class Arsenal implements AmmoReadout {

    /** Weapon -> rounds left. Keyed by identity; weapons are stateless singletons. */
    private final Map<Weapon, Integer> stock = new LinkedHashMap<>();
    private final int boardSize;

    private Weapon selected = WeaponCatalog.standard();
    private Orientation orientation = Orientation.HORIZONTAL;

    Arsenal(int boardSize) {
        this.boardSize = boardSize;
        for (Weapon weapon : WeaponCatalog.all()) {
            stock.put(weapon, weapon.startingAmmo(boardSize));
        }
    }

    @Override
    public int ammoCount(Weapon weapon) {
        Integer rounds = stock.get(weapon);
        if (rounds != null) return rounds;
        // A weapon the catalog knows but this battle never stocked (e.g. a plugin
        // registered mid-game) behaves as "infinite" or "empty", never as ammo 0.
        return weapon.hasInfiniteAmmo() ? Integer.MAX_VALUE : 0;
    }

    @Override
    public boolean hasAmmo(Weapon weapon) {
        return ammoCount(weapon) > 0;
    }

    @Override
    public boolean isAmmoInfinite(Weapon weapon) {
        return weapon.hasInfiniteAmmo();
    }

    /** A weapon may be armed when the battlefield offers it and rounds are left. */
    boolean canSelect(Weapon weapon) {
        return weapon.availableFor(boardSize) && hasAmmo(weapon);
    }

    /** Arms a weapon; leaves the previous selection untouched when it is not selectable. */
    boolean select(Weapon weapon) {
        if (!canSelect(weapon)) return false;
        this.selected = weapon;
        return true;
    }

    void toggleOrientation() {
        this.orientation = orientation.toggle();
    }

    /** Rule 1: the weapon must be actively re-selected after every shot. */
    void resetAfterShot() {
        this.selected = WeaponCatalog.standard();
    }

    /** Arms a specific weapon + orientation for an automated or networked shot. */
    void arm(Weapon weapon, Orientation orientation) {
        this.selected = weapon;
        this.orientation = orientation;
    }

    /** Spends one round; no-op for infinite weapons. */
    void consume(Weapon weapon) {
        if (weapon.hasInfiniteAmmo()) return;
        int current = ammoCount(weapon);
        if (current <= 0) {
            throw new IllegalStateException("No ammo remaining for " + weapon.displayName());
        }
        stock.put(weapon, current - 1);
    }

    /** Adds rounds (e.g. the nuclear resupply after the launch-code quiz). */
    void resupply(Weapon weapon, int amount) {
        if (weapon.hasInfiniteAmmo() || amount <= 0) return;
        stock.put(weapon, ammoCount(weapon) + amount);
    }

    Weapon selected() {
        return selected;
    }

    Orientation orientation() {
        return orientation;
    }
}
