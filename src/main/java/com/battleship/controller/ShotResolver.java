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
 * Reusable utility that resolves one launcher shot against a target board
 * (fixes V8). Used by both {@link BattleService} (local match) and the
 * defending side of a network match ({@code NetworkBattleView}), so the
 * fire-resolution rules live in exactly one place instead of being
 * copy-pasted into a view class (DRY + SRP).
 */
public final class ShotResolver {

    private ShotResolver() { }

    /**
     * Fires the given launcher pattern against the target board.
     * Already-resolved cells and out-of-bounds cells within the pattern are
     * skipped; ammo bookkeeping is the caller's responsibility.
     *
     * @return the aggregated result (per-cell outcomes + ships sunk this shot)
     */
    public static LauncherFireResult resolve(
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
