package com.battleship.controller;

import com.battleship.model.Ship;
import com.battleship.model.ShotResult;

import java.util.List;

/**
 * Outcome of one launcher shot, which may cover multiple cells (Level 2, Nuclear)
 * and therefore may register multiple hits/misses and sink multiple ships at once.
 */
public record LauncherFireResult(List<ShotResult> results, List<Ship> sunkShips) {

    public boolean anyHit() {
        return results.stream().anyMatch(ShotResult::isHit);
    }
}
