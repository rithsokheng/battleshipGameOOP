package com.battleship.ai;

import com.battleship.model.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Admiral (Hard) difficulty: builds a probability density map over unshot
 * cells (how many valid remaining-ship placements would cover each cell)
 * and fires at the highest-probability cell. Falls back to target-following
 * behavior right after a hit, since that's more precise than pure probability
 * once a ship has been found.
 *
 * Uses composition (TargetingQueue) instead of inheriting from HuntTargetAI.
 */
public class SmartAI implements AIStrategy {

    private final TargetingQueue targetQueue = new TargetingQueue();
    private final SecureRandom random = new SecureRandom();
    private int lastBoardSize = -1;

    @Override
    public Coordinate chooseTarget(Board enemyBoard) {
        lastBoardSize = enemyBoard.getSize();

        // If we're actively finishing off a located ship, defer to the queue.
        Coordinate queued = targetQueue.nextTarget(enemyBoard);
        if (queued != null) return queued;

        int size = enemyBoard.getSize();
        int[][] density = new int[size][size];
        List<ShipType> remaining = new ArrayList<>();
        for (Ship s : enemyBoard.getShips()) {
            if (!s.isSunk()) remaining.add(s.getType());
        }
        if (remaining.isEmpty()) {
            return fallbackHunt(enemyBoard);
        }

        for (ShipType type : remaining) {
            int len = type.getSize();
            for (int r = 0; r < size; r++) {
                for (int c = 0; c <= size - len; c++) {
                    if (fits(enemyBoard, r, c, len, true)) {
                        for (int i = 0; i < len; i++) density[r][c + i]++;
                    }
                }
            }
            for (int c = 0; c < size; c++) {
                for (int r = 0; r <= size - len; r++) {
                    if (fits(enemyBoard, r, c, len, false)) {
                        for (int i = 0; i < len; i++) density[r + i][c]++;
                    }
                }
            }
        }

        int best = -1;
        List<Coordinate> bestCells = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Coordinate coord = new Coordinate(r, c);
                CellStatus status = enemyBoard.getCellStatus(coord);
                if (status != CellStatus.EMPTY && status != CellStatus.SHIP) continue;
                if (density[r][c] > best) {
                    best = density[r][c];
                    bestCells.clear();
                    bestCells.add(coord);
                } else if (density[r][c] == best) {
                    bestCells.add(coord);
                }
            }
        }

        if (bestCells.isEmpty()) return fallbackHunt(enemyBoard);
        return bestCells.get(random.nextInt(bestCells.size()));
    }

    @Override
    public void notifyResult(ShotResult result) {
        if (!result.isHit()) return;
        if (result.outcome() == CellStatus.SUNK) {
            targetQueue.clear();
            return;
        }
        if (lastBoardSize > 0) {
            targetQueue.enqueueNeighbors(result.coordinate(), lastBoardSize);
        }
    }

    private Coordinate fallbackHunt(Board enemyBoard) {
        List<Coordinate> unshot = enemyBoard.getUnshotCells();
        List<Coordinate> parity = new ArrayList<>();
        for (Coordinate c : unshot) {
            if ((c.getRow() + c.getCol()) % 2 == 0) parity.add(c);
        }
        List<Coordinate> pool = parity.isEmpty() ? unshot : parity;
        return pool.get(random.nextInt(pool.size()));
    }

    private boolean fits(Board board, int row, int col, int len, boolean horizontal) {
        for (int i = 0; i < len; i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;
            CellStatus status = board.getCellStatus(new Coordinate(r, c));
            // A cell already known as MISS or SUNK cannot host a live ship.
            if (status == CellStatus.MISS || status == CellStatus.SUNK) return false;
        }
        return true;
    }

    /**
     * Admiral AI spends its limited-ammo launchers deliberately: it looks for
     * the 2x3 (Nuclear) or 1x3 (Level 2) block with the most still-unshot cells
     * and only fires it if that block is mostly "fresh" — otherwise it saves
     * the ammo and falls back to a precise single Default shot.
     */
    @Override
    public AiShotPlan chooseShotPlan(Board enemyBoard, AmmoInventory ammo) {
        if (targetQueue.hasTargets()) {
            return new AiShotPlan(LauncherType.DEFAULT, chooseTarget(enemyBoard), true);
        }

        int size = enemyBoard.getSize();
        if (ammo.hasAmmo(LauncherType.NUCLEAR) && !ammo.isInfinite(LauncherType.NUCLEAR) && size >= 10) {
            AiShotPlan plan = bestBlock(enemyBoard, LauncherType.NUCLEAR);
            if (plan != null) return plan;
        }
        if (ammo.hasAmmo(LauncherType.LEVEL_2) && !ammo.isInfinite(LauncherType.LEVEL_2) && size >= 8) {
            AiShotPlan plan = bestBlock(enemyBoard, LauncherType.LEVEL_2);
            if (plan != null) return plan;
        }
        return new AiShotPlan(LauncherType.DEFAULT, chooseTarget(enemyBoard), true);
    }

    /** Finds the best-scoring placement for an area weapon; null if not worth the ammo. */
    private AiShotPlan bestBlock(Board board, LauncherType type) {
        int size = board.getSize();
        // Use the type's own cell count to derive dimensions
        int[][] dims = type == LauncherType.NUCLEAR
                ? new int[][]{{2, 3}, {3, 2}}
                : new int[][]{{1, 3}, {3, 1}};

        int bestScore = -1;
        Coordinate bestAnchor = null;
        boolean bestHorizontal = true;

        for (int[] dim : dims) {
            int rows = dim[0], cols = dim[1];
            boolean horizontal = rows <= cols; // matches LauncherType's horizontal convention
            for (int r = 0; r <= size - rows; r++) {
                for (int c = 0; c <= size - cols; c++) {
                    int score = 0;
                    for (int dr = 0; dr < rows; dr++) {
                        for (int dc = 0; dc < cols; dc++) {
                            CellStatus s = board.getCellStatus(new Coordinate(r + dr, c + dc));
                            if (s == CellStatus.EMPTY || s == CellStatus.SHIP) score++;
                        }
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        bestAnchor = new Coordinate(r, c);
                        bestHorizontal = horizontal;
                    }
                }
            }
        }

        int totalCells = type.getCellCount();
        // Only worth the ammo if at least half the covered cells are still unshot.
        if (bestAnchor == null || bestScore < (totalCells / 2 + 1)) return null;
        return new AiShotPlan(type, bestAnchor, bestHorizontal);
    }
}
