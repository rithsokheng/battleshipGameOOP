package com.battleship.controller;

import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Ship;
import com.battleship.model.ShotResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Reusable resolver for one launcher shot against a target board
 * (fixes V8 + F5). Used by both {@link BattleService} (local match) and the
 * defending side of a network match ({@code NetworkBattleMediator}), so the
 * fire-resolution rules live in exactly one place instead of being
 * copy-pasted into a view class (DRY + SRP).
 *
 * Implements {@link ShotResolution} so it can be injected (and swapped/mocked)
 * via the {@link #STANDARD} singleton; the class is no longer a static-only
 * utility.
 */
public final class ShotResolver implements ShotResolution {

    /** Shared production instance for constructor/default injection. */
    public static final ShotResolver STANDARD = new ShotResolver();

    private ShotResolver() { }

    /**
     * Fires the given launcher pattern against the target board.
     * Already-resolved cells and out-of-bounds cells within the pattern are
     * skipped; ammo bookkeeping is the caller's responsibility.
     *
     * @return the aggregated result (per-cell outcomes + ships sunk this shot)
     */
    @Override
    public LauncherFireResult resolve(
            Board targetBoard,
            LauncherType launcherType,
            Coordinate anchor,
            Orientation orientation) {

        int size = targetBoard.getSize();
        List<Coordinate> cells = launcherType.getTargetCells(anchor, orientation);
        List<ShotResult> results = new ArrayList<>();
        LinkedHashSet<Ship> sunk = new LinkedHashSet<>();

        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            CellStatus existing = targetBoard.getCellStatus(c);
            if (existing == CellStatus.HIT || existing == CellStatus.MISS
                    || existing == CellStatus.SUNK) continue;
            ShotResult r = targetBoard.receiveShot(c);
            results.add(r);
            if (r.outcome() == CellStatus.SUNK) sunk.add(r.shipSunk());
        }

        return new LauncherFireResult(results, new ArrayList<>(sunk));
    }
}
