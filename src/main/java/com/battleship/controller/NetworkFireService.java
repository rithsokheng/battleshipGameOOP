package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;

/**
 * Owns the domain mutations of a network shot (fixes F7 — Feature Envy in
 * {@code NetworkBattleView.resolveShot}): ammo consumption, launcher reset
 * and the quiz-gated nuclear resupply. The view keeps only UI and network
 * I/O responsibilities.
 */
public class NetworkFireService {

    /** Everything the attacker's client must transmit after a shot. */
    public record NetworkShotOrder(LauncherType launcherType, Coordinate anchor, Orientation orientation) {}

    /**
     * Applies the shot's domain bookkeeping to the shooter (consume ammo,
     * reset launcher per rule 1) and returns the order to send over the wire.
     * Consume is a no-op for the infinite DEFAULT launcher.
     */
    public NetworkShotOrder fireNetworkShot(Player shooter, LauncherType type,
                                            Coordinate anchor, Orientation orientation) {
        shooter.consumeAmmo(type);
        shooter.resetLauncherAfterShot();
        return new NetworkShotOrder(type, anchor, orientation);
    }

    /** Tops nuclear ammo back up after a successful quiz resupply. */
    public void resupplyNuclear(Player shooter) {
        shooter.resupplyAmmo(LauncherType.NUCLEAR, 1);
    }
}
