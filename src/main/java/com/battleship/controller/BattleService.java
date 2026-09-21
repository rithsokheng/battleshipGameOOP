package com.battleship.controller;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.Turn;
import com.battleship.model.fog.MarkerStatus;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;

import java.security.SecureRandom;

/**
 * Encapsulates turn management, weapon selection and the firing pipeline.
 * Extracted from GameController so the controller can stay a thin mediator (SRP).
 *
 * <p>Two architectural fixes:</p>
 * <ul>
 *   <li>The AI branch {@code if (aiStrategy != null && attacker == player2)} is gone:
 *       whoever holds the turn is asked for a {@link ShotOrder} through the
 *       polymorphic {@link Player#decideAutonomousShot()} (V2.2).</li>
 *   <li>Every shot updates the <em>shooter's</em> {@link TrackingGrid} — the only
 *       fog-of-war model in the game — instead of the shooter reading the
 *       defender's grid (V1.3 / Smell 5.2).</li>
 * </ul>
 */
public class BattleService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Shot-resolution strategy, injectable for tests (fixes F5). */
    private final ShotResolution shotResolution;

    private Player player1;
    private Player player2;
    private Turn currentTurn;

    /** True only between a fire() that ended the match and the controller reacting to it. */
    private boolean battleOver;

    /** Production constructor — uses the standard shot resolver. */
    public BattleService() {
        this(ShotResolver.STANDARD);
    }

    /** Testable constructor — inject the shot-resolution strategy (DIP, fixes F5). */
    public BattleService(ShotResolution shotResolution) {
        this.shotResolution = shotResolution;
    }

    public void init(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentTurn = Turn.PLAYER_1;
        this.battleOver = false;
    }

    /** Cryptographically fair coin flip determines who fires first. */
    public Player rollInitiative() {
        currentTurn = RANDOM.nextBoolean() ? Turn.PLAYER_1 : Turn.PLAYER_2;
        return getCurrentPlayer();
    }

    public Player getCurrentPlayer() { return currentTurn == Turn.PLAYER_1 ? player1 : player2; }
    public Player getOpponent() { return currentTurn == Turn.PLAYER_1 ? player2 : player1; }

    /** True when the player holding the turn acts on its own (no UI click expected). */
    public boolean isAiTurn() {
        return getCurrentPlayer().isAutonomous();
    }

    public boolean selectWeapon(Player player, Weapon weapon) {
        return player.selectWeapon(weapon);
    }

    public void toggleOrientation(Player player) {
        player.toggleWeaponOrientation();
    }

    public int getAmmoRemaining(Player player, Weapon weapon) {
        return player.ammoCount(weapon);
    }

    /**
     * Fires the current player's selected weapon, anchored at the given cell.
     * The pattern resolution is delegated to the shared {@link ShotResolver};
     * this method owns only the turn-level concerns: knowledge bookkeeping,
     * ammo consumption, weapon reset, shooter feedback and turn advancement
     * (unless the defender just lost).
     */
    public LauncherFireResult fire(Coordinate anchor) {
        battleOver = false;
        Player attacker = getCurrentPlayer();
        Player defender = getOpponent();
        Weapon weapon = attacker.selectedWeapon();

        LauncherFireResult result = shotResolution.resolve(
                defender, weapon, anchor, attacker.weaponOrientation());

        recordObservedOutcome(attacker, result);
        attacker.consumeAmmo(weapon);          // infinite weapons: no-op
        attacker.resetWeaponAfterShot();       // must actively re-select each turn (rule 1)
        for (ShotResult shot : result.results()) {
            attacker.observeOwnShot(shot);     // polymorphic: only a machine learns
        }

        if (defender.isFleetDestroyed()) {
            battleOver = true; // turn stays with the winner for game-over reporting
        } else {
            currentTurn = currentTurn.next();
        }
        return result;
    }

    /** Has the current player choose a weapon + target on its own, then fires it. */
    public LauncherFireResult fireAiLauncher() {
        Player attacker = getCurrentPlayer();
        ShotOrder order = attacker.decideAutonomousShot().orElseThrow(() ->
                new IllegalStateException(attacker.name() + " needs a human to choose a shot."));
        attacker.armWeapon(order.weapon(), order.orientation());
        return fire(order.anchor());
    }

    /** Copies everything the shooter just observed into their own knowledge grid. */
    private void recordObservedOutcome(Player attacker, LauncherFireResult result) {
        TrackingGrid knowledge = attacker.trackingGrid();
        for (ShotResult shot : result.results()) {
            knowledge.recordShotOutcome(shot.coordinate(), markerFor(shot.outcome()));
        }
        for (ShipSnapshot sunk : result.sunkShips()) {
            knowledge.recordWreck(sunk.type(), sunk.cells());
        }
    }

    private static MarkerStatus markerFor(CellStatus outcome) {
        return switch (outcome) {
            case HIT -> MarkerStatus.HIT;
            case SUNK -> MarkerStatus.SUNK;
            default -> MarkerStatus.MISS;
        };
    }

    public boolean isBattleOver() { return battleOver; }
}
