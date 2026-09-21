package com.battleship.model;

import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A participant in the game — the aggregate root for one admiral.
 *
 * <p>Two structural fixes live here:</p>
 * <ul>
 *   <li><strong>V1.1 / Smell 5.1</strong> — the primary grid is private and has no
 *       getter. Code that wants to deploy hulls, take a shot or read a grid talks
 *       to the narrow {@link FleetDeployment}, {@link ShotTarget} and
 *       {@link FleetReadout} command surfaces this class implements. The public
 *       {@code getMutableBoard()} backdoor is gone for good.</li>
 *   <li><strong>V2.2</strong> — the {@code isHuman} primitive is gone: behaviour
 *       that differs between a person and a machine (who supplies the next shot,
 *       whether results are learned from) is expressed by {@link HumanPlayer} and
 *       {@link AiPlayer} overriding {@link #decideAutonomousShot()} and
 *       {@link #observeOwnShot(ShotResult)}.</li>
 * </ul>
 *
 * <p><strong>SRP</strong>: identity + fleet + knowledge + arsenal. Weapon stocking,
 * arming and aiming were extracted into {@link Arsenal}, which this class forwards
 * to so the rest of the game cannot mutate ammunition directly.</p>
 */
public abstract class Player implements FleetReadout, FleetDeployment, ShotTarget, AmmoReadout {

    private final String name;
    /** Never exposed — the whole point of the V1.1 fix. */
    private final PrimaryGrid primaryGrid;
    private final TrackingGrid trackingGrid;
    private final Arsenal arsenal;

    /**
     * @param name                  display name
     * @param boardSize             battlefield edge length
     * @param enemyFleetComposition the opposing roster (published by the rules of the game)
     */
    protected Player(String name, int boardSize, Map<ShipType, Integer> enemyFleetComposition) {
        this.name = Objects.requireNonNull(name, "A player needs a name.");
        this.primaryGrid = new PrimaryGrid(boardSize);
        this.trackingGrid = new TrackingGrid(boardSize, enemyFleetComposition);
        this.arsenal = new Arsenal(boardSize);
    }

    public String name() {
        return name;
    }

    public String getName() {
        return name;
    }

    /**
     * What this player knows about the enemy — safe to expose, because a tracking
     * grid cannot answer questions about unobserved ship positions (V1.3).
     */
    public TrackingGrid trackingGrid() {
        return trackingGrid;
    }

    // ---------- Polymorphic turn behaviour (replaces the isHuman flag) ----------

    /** True for machine-controlled players, which act without a UI click. */
    public abstract boolean isAutonomous();

    /** Convenience query: true if this player is controlled by a human. */
    public boolean isHuman() {
        return !isAutonomous();
    }

    /**
     * Produces this player's next shot on its own, or {@link Optional#empty()} when
     * a human must click a cell. {@link AiPlayer} overrides this; {@link HumanPlayer}
     * inherits the empty answer.
     */
    public Optional<ShotOrder> decideAutonomousShot() {
        return Optional.empty();
    }

    /** Learning hook: called for the shooter after their own shot was resolved. */
    public void observeOwnShot(ShotResult result) {
        // Nothing to learn for a human.
    }

    // ---------- Weapon commands (delegated to the Arsenal component) ----------

    /** Arms a weapon if the battlefield offers it and ammunition remains. */
    public boolean selectWeapon(Weapon weapon) {
        return arsenal.select(weapon);
    }

    public boolean aimDefault() {
        return selectWeapon(com.battleship.model.weapon.WeaponCatalog.defaultWeapon());
    }

    public boolean aimNuclear() {
        return selectWeapon(com.battleship.model.weapon.WeaponCatalog.nuclear());
    }

    public void toggleWeaponOrientation() {
        arsenal.toggleOrientation();
    }

    /** Rule 1: after firing, the admiral must actively re-select a weapon. */
    public void resetWeaponAfterShot() {
        arsenal.resetAfterShot();
    }

    /** Arms a weapon + orientation for an automated or networked shot. */
    public void armWeapon(Weapon weapon, Orientation orientation) {
        arsenal.arm(weapon, orientation);
    }

    public Weapon selectedWeapon() {
        return arsenal.selected();
    }

    public Orientation weaponOrientation() {
        return arsenal.orientation();
    }

    public void consumeAmmo(Weapon weapon) {
        arsenal.consume(weapon);
    }

    public void resupplyAmmo(Weapon weapon, int amount) {
        arsenal.resupply(weapon, amount);
    }

    // ---------- AmmoReadout ----------

    @Override
    public int ammoCount(Weapon weapon) {
        return arsenal.ammoCount(weapon);
    }

    @Override
    public boolean hasAmmo(Weapon weapon) {
        return arsenal.hasAmmo(weapon);
    }

    @Override
    public boolean isAmmoInfinite(Weapon weapon) {
        return arsenal.isAmmoInfinite(weapon);
    }

    // ---------- FleetReadout ----------

    @Override
    public int size() {
        return primaryGrid.size();
    }

    @Override
    public CellStatus cellStatus(Coordinate c) {
        return primaryGrid.cellStatus(c);
    }

    @Override
    public List<ShipSnapshot> fleet() {
        return primaryGrid.fleet();
    }

    @Override
    public boolean isFleetDestroyed() {
        return primaryGrid.isFleetDestroyed();
    }

    // ---------- FleetDeployment ----------

    @Override
    public boolean canDeploy(ShipType type, Coordinate start, Orientation orientation) {
        return primaryGrid.canDeploy(type, start, orientation);
    }

    @Override
    public boolean deploy(ShipType type, Coordinate start, Orientation orientation) {
        return primaryGrid.deploy(type, start, orientation);
    }

    @Override
    public boolean undeployAt(Coordinate c) {
        return primaryGrid.undeployAt(c);
    }

    @Override
    public void clearDeployment() {
        primaryGrid.clearDeployment();
    }

    // ---------- ShotTarget ----------

    @Override
    public boolean isCellResolved(Coordinate c) {
        return primaryGrid.isCellResolved(c);
    }

    @Override
    public ShotResult receiveShot(Coordinate c) {
        return primaryGrid.receiveShot(c);
    }

    /** Hulls currently deployed — the placement counter uses it. */
    public int deployedShipCount() {
        return primaryGrid.deployedShipCount();
    }
}
