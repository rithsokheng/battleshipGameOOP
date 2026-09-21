package com.battleship.model.fog;

import com.battleship.model.Coordinate;
import com.battleship.model.ShipType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything one player <em>knows</em> about the enemy waters — and nothing more.
 *
 * <p>Fixes V1.3 + Smell 5.2: before this class existed the local game handed the
 * opponent's live {@code Board} to the view and the AI (which could therefore
 * read the exact ship layout), while the network game used a bespoke
 * {@code EnemyTracker}. Both modes now share this single model of reality: it is
 * physically impossible to ask this class for an unobserved ship position,
 * because it only stores verified shot outcomes and announced wrecks.</p>
 *
 * <p>The enemy's fleet composition is known up front (standard Battleship rules
 * publish the fleet roster, not its positions), which lets the AI reason about
 * which hulls can still be afloat without ever touching the real grid.</p>
 */
public final class TrackingGrid {

    /** A hull the defender has publicly announced as sunk, with its cells. */
    public record DiscoveredWreck(ShipType type, List<Coordinate> cells) {
        public DiscoveredWreck {
            Objects.requireNonNull(type, "A wreck needs a ship type.");
            cells = List.copyOf(cells);
        }
    }

    private final int size;
    private final Map<ShipType, Integer> enemyFleet;
    private final Map<Coordinate, MarkerStatus> markers = new HashMap<>();
    private final List<DiscoveredWreck> confirmedSunk = new ArrayList<>();

    /**
     * @param size                  board edge length
     * @param enemyFleetComposition the enemy roster, e.g. {@code DESTROYER -> 2}
     */
    public TrackingGrid(int size, Map<ShipType, Integer> enemyFleetComposition) {
        if (size <= 0) {
            throw new IllegalArgumentException("Tracking grid size must be positive, was " + size);
        }
        this.size = size;
        this.enemyFleet = new LinkedHashMap<>(Objects.requireNonNull(enemyFleetComposition));
    }

    /** Empty knowledge of a board of the given size (fleet roster unknown). */
    public static TrackingGrid blind(int size) {
        return new TrackingGrid(size, Map.of());
    }

    /** Records one verified shot outcome. Only HIT, MISS and SUNK are acceptable. */
    public void recordShotOutcome(Coordinate coord, MarkerStatus outcome) {
        Objects.requireNonNull(coord, "A marker needs a coordinate.");
        Objects.requireNonNull(outcome, "A marker needs an outcome.");
        if (!outcome.isShelled()) {
            throw new IllegalArgumentException(
                    "A tracking grid only records verified outcomes (MISS/HIT/SUNK), got " + outcome);
        }
        if (!coord.isWithinBounds(size)) {
            throw new IllegalArgumentException("Coordinate " + coord + " is outside a " + size + " board.");
        }
        // A confirmed wreck outranks a bare hit marker, never the other way around.
        if (markers.get(coord) == MarkerStatus.SUNK) return;
        markers.put(coord, outcome);
    }

    /** Convenience overload that converts a domain CellStatus into MarkerStatus automatically. */
    public void recordShotOutcome(Coordinate coord, com.battleship.model.CellStatus outcome) {
        recordShotOutcome(coord, MarkerStatus.from(outcome));
    }


    /** Records an announced wreck: every cell becomes SUNK and the hull joins the loss board. */
    public void recordWreck(ShipType type, List<Coordinate> hullCells) {
        Objects.requireNonNull(type, "A wreck needs a ship type.");
        List<Coordinate> cells = List.copyOf(hullCells);
        for (Coordinate c : cells) {
            if (c.isWithinBounds(size)) markers.put(c, MarkerStatus.SUNK);
        }
        confirmedSunk.add(new DiscoveredWreck(type, cells));
    }

    /** What the owner of this grid knows about the cell — {@link MarkerStatus#UNKNOWN} by default. */
    public MarkerStatus observedStatus(Coordinate c) {
        if (!c.isWithinBounds(size)) {
            throw new IllegalArgumentException("Coordinate " + c + " is outside a " + size + " board.");
        }
        return markers.getOrDefault(c, MarkerStatus.UNKNOWN);
    }

    /** True once a shot has been resolved on this cell. */
    public boolean isAlreadyShelled(Coordinate c) {
        return observedStatus(c).isShelled();
    }

    /** Every cell that has not been shot at yet, in reading order. */
    public List<Coordinate> unshotCells() {
        List<Coordinate> result = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Coordinate coord = new Coordinate(r, c);
                if (!isAlreadyShelled(coord)) result.add(coord);
            }
        }
        return result;
    }

    /** Hulls the defender has announced as sunk, in the order they were announced. */
    public List<DiscoveredWreck> confirmedSunk() {
        return Collections.unmodifiableList(confirmedSunk);
    }

    /** The enemy fleet roster this grid was created with. */
    public Map<ShipType, Integer> enemyFleetComposition() {
        return Collections.unmodifiableMap(enemyFleet);
    }

    /**
     * One entry per enemy hull that is still believed afloat (roster minus announced wrecks).
     * The AI uses this for its probability density map without reading the real fleet.
     */
    public List<ShipType> remainingShipPool() {
        Map<ShipType, Integer> remaining = new LinkedHashMap<>(enemyFleet);
        for (DiscoveredWreck wreck : confirmedSunk) {
            remaining.computeIfPresent(wreck.type(), (type, count) -> count - 1);
        }
        List<ShipType> pool = new ArrayList<>();
        remaining.forEach((type, count) -> {
            for (int i = 0; i < count; i++) pool.add(type);
        });
        return pool;
    }

    /** Number of enemy hulls still believed afloat. */
    public int shipsRemaining() {
        return remainingShipPool().size();
    }

    /** Total enemy hulls this player set out to destroy (roster size). */
    public int totalEnemyShips() {
        return enemyFleet.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** True once every enemy hull has been announced as sunk. */
    public boolean isFleetFullyAccountedFor() {
        return !enemyFleet.isEmpty() && shipsRemaining() <= 0;
    }

    public int size() {
        return size;
    }
}

