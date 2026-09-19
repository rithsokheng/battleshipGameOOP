package com.battleship.controller;

import com.battleship.model.Board;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;

/**
 * Injectable strategy for resolving one launcher shot against a target board
 * (fixes F5, DIP). Production uses {@link ShotResolver#STANDARD}; tests can
 * substitute a stub/fake without any static coupling.
 */
@FunctionalInterface
public interface ShotResolution {

    /**
     * Fires the given launcher pattern against the target board.
     * Already-resolved cells and out-of-bounds cells within the pattern are
     * skipped; ammo bookkeeping is the caller's responsibility.
     *
     * @return the aggregated result (per-cell outcomes + ships sunk this shot)
     */
    LauncherFireResult resolve(Board targetBoard, LauncherType launcherType,
                               Coordinate anchor, Orientation orientation);
}
