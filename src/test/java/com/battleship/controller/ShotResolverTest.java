package com.battleship.controller;

import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.ShotResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validates the extracted {@link ShotResolver} shared by local and network play. */
class ShotResolverTest {

    @Test
    void singleShotOnEmptyCellIsAMiss() {
        Board board = new Board(10);

        LauncherFireResult result = ShotResolver.STANDARD.resolve(
                board, LauncherType.DEFAULT, new Coordinate(3, 4), Orientation.HORIZONTAL);

        assertEquals(1, result.results().size());
        assertEquals(CellStatus.MISS, result.results().get(0).outcome());
        assertTrue(result.sunkShips().isEmpty());
        assertFalse(result.anyHit());
    }

    @Test
    void alreadyResolvedAndOutOfBoundsCellsAreSkipped() {
        Board board = new Board(10);
        // LEVEL_2 anchored at (5,8) horizontally covers (5,8),(5,9),(5,10) — the last is OOB.
        LauncherFireResult first = ShotResolver.STANDARD.resolve(
                board, LauncherType.LEVEL_2, new Coordinate(5, 8), Orientation.HORIZONTAL);
        assertEquals(2, first.results().size());

        // Re-resolving the same pattern must yield nothing: all live cells were resolved.
        LauncherFireResult second = ShotResolver.STANDARD.resolve(
                board, LauncherType.LEVEL_2, new Coordinate(5, 8), Orientation.HORIZONTAL);
        assertTrue(second.results().isEmpty());
        assertTrue(second.sunkShips().isEmpty());
    }

    @Test
    void areaShotSinksShipAndReportsItOnce() {
        Board board = new Board(10);
        assertTrue(board.placeShip(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL));

        // Nuclear pattern (2x3 from anchor) covers both patrol boat cells.
        LauncherFireResult result = ShotResolver.STANDARD.resolve(
                board, LauncherType.NUCLEAR, new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertEquals(6, result.results().size());
        assertEquals(1, result.sunkShips().size());
        assertTrue(result.results().stream().anyMatch(r -> r.outcome() == CellStatus.SUNK));
        assertTrue(board.isAllShipsSunk());
    }

    @Test
    void shotResultsExposeCoordinate() {
        Board board = new Board(10);
        ShotResult r = ShotResolver.STANDARD.resolve(board, LauncherType.DEFAULT, new Coordinate(2, 2),
                Orientation.HORIZONTAL).results().get(0);
        assertEquals(new Coordinate(2, 2), r.coordinate());
    }
}
