package com.battleship.ai;

import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Encapsulates the target-following queue used by Hunt/Target AI strategies.
 * Once a hit is registered, orthogonal neighbors are enqueued so the AI
 * "follows the line" on subsequent turns.
 */
public class TargetingQueue {

    private final Deque<Coordinate> queue = new ArrayDeque<>();

    /** Returns true if there are queued target cells to pursue. */
    public boolean hasTargets() {
        return !queue.isEmpty();
    }

    /**
     * Returns the next valid (unshot) target from the queue, or null if the
     * queue is exhausted (all queued cells have already been shot).
     */
    public Coordinate nextTarget(Board enemyBoard) {
        while (!queue.isEmpty()) {
            Coordinate c = queue.poll();
            CellStatus status = enemyBoard.getCellStatus(c);
            if (status == CellStatus.EMPTY || status == CellStatus.SHIP) {
                return c;
            }
        }
        return null;
    }

    /**
     * Enqueues the four orthogonal neighbors of the given coordinate,
     * filtering out any that fall outside the board.
     */
    public void enqueueNeighbors(Coordinate c, int boardSize) {
        int[][] deltas = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : deltas) {
            Coordinate n = new Coordinate(c.getRow() + d[0], c.getCol() + d[1]);
            if (n.isWithinBounds(boardSize)) {
                queue.add(n);
            }
        }
    }

    /** Clears all queued targets (e.g. after a ship is fully sunk). */
    public void clear() {
        queue.clear();
    }
}

