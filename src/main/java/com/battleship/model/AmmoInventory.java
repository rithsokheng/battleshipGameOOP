package com.battleship.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages ammunition counts for every {@link LauncherType}.
 * DEFAULT ammo is treated as infinite and never decremented.
 */
public class AmmoInventory {

    private final Map<LauncherType, Integer> ammo = new EnumMap<>(LauncherType.class);

    /** Initializes starting ammo for all launcher types based on the board size. */
    public AmmoInventory(int boardSize) {
        for (LauncherType type : LauncherType.values()) {
            ammo.put(type, type.getStartingAmmo(boardSize));
        }
    }

    /** Returns the current ammo count for the given type (Integer.MAX_VALUE for infinite). */
    public int getAmmo(LauncherType type) {
        return ammo.getOrDefault(type, 0);
    }

    /** Returns true if the player has at least one shot of this type. */
    public boolean hasAmmo(LauncherType type) {
        return getAmmo(type) > 0;
    }

    /** Returns true if this type has infinite ammo (e.g. DEFAULT). */
    public boolean isInfinite(LauncherType type) {
        return getAmmo(type) == Integer.MAX_VALUE;
    }

    /**
     * Consumes one unit of ammo. No-op for infinite-ammo types.
     * @throws IllegalStateException if ammo is already 0.
     */
    public void consume(LauncherType type) {
        int current = getAmmo(type);
        if (current == Integer.MAX_VALUE) return; // infinite — don't decrement
        if (current <= 0) {
            throw new IllegalStateException("No ammo remaining for " + type);
        }
        ammo.put(type, current - 1);
    }

    /** Adds the given amount of ammo for the given type. */
    public void resupply(LauncherType type, int amount) {
        int current = getAmmo(type);
        if (current == Integer.MAX_VALUE) return; // infinite — no-op
        ammo.put(type, current + amount);
    }
}

