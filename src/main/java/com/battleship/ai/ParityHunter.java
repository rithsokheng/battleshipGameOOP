package com.battleship.ai;

import com.battleship.model.Board;
import com.battleship.model.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stateless HUNT-mode searcher: picks a random unshot cell using checkerboard
 * parity — exploiting the fact that the smallest ship occupies 2 cells, so at
 * least one cell of every ship lies on a (row + col)-even square.
 *
 * <p>Shared by {@link HuntTargetAI} and {@link SmartAI} so the search heuristic
 * lives in exactly one place (DRY — closes the residual duplication flagged as
 * Issue 6 in Implementation7, of which the neighbour-enqueue half was already
 * solved by {@link TargetingQueue}).</p>
 */
public final class ParityHunter {

    private ParityHunter() { }

    /**
     * Picks a random cell to hunt on, preferring even (row + col) parity.
     *
     * @param enemyBoard the attacker's knowledge of the defender's board
     * @param random     the strategy's randomness source
     * @return an unshot coordinate; even-parity when any remains available
     */
    public static Coordinate pick(Board enemyBoard, Random random) {
        List<Coordinate> unshot = enemyBoard.getUnshotCells();
        List<Coordinate> parity = new ArrayList<>();
        for (Coordinate c : unshot) {
            if ((c.getRow() + c.getCol()) % 2 == 0) parity.add(c);
        }
        List<Coordinate> pool = parity.isEmpty() ? unshot : parity;
        return pool.get(random.nextInt(pool.size()));
    }
}
