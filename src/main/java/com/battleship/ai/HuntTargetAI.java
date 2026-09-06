package com.battleship.ai;

import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.ShotResult;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lieutenant (Normal) difficulty: HUNT/TARGET state machine.
 * HUNT  -> checkerboard-parity random search (exploits min ship size = 2).
 * TARGET -> on hit, queue orthogonal neighbors and follow the line.
 */
public class HuntTargetAI implements AIStrategy {

    protected final Deque<Coordinate> queue = new ArrayDeque<>();
    protected final SecureRandom random = new SecureRandom();
    private int lastBoardSize = -1;

    @Override
    public Coordinate chooseTarget(Board enemyBoard) {
        lastBoardSize = enemyBoard.getSize();

        // TARGET mode: drain queue, skip any coordinate already shot at.
        while (!queue.isEmpty()) {
            Coordinate c = queue.poll();
            CellStatus status = enemyBoard.getCellStatus(c);
            if (status == CellStatus.EMPTY || status == CellStatus.SHIP) {
                return c;
            }
        }

        // HUNT mode: checkerboard parity over unshot cells.
        List<Coordinate> unshot = enemyBoard.getUnshotCells();
        List<Coordinate> parity = new ArrayList<>();
        for (Coordinate c : unshot) {
            if ((c.getRow() + c.getCol()) % 2 == 0) parity.add(c);
        }
        List<Coordinate> pool = parity.isEmpty() ? unshot : parity;
        return pool.get(random.nextInt(pool.size()));
    }

    @Override
    public void notifyResult(ShotResult result) {
        if (!result.isHit()) return;
        if (result.outcome() == CellStatus.SUNK) {
            queue.clear();
            return;
        }
        Coordinate c = result.coordinate();
        int[][] deltas = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : deltas) {
            Coordinate n = new Coordinate(c.getRow() + d[0], c.getCol() + d[1]);
            if (lastBoardSize < 0 || n.isWithinBounds(lastBoardSize)) {
                queue.add(n);
            }
        }
    }

    /**
     * Normal AI stays precise once it has a lead (TARGET mode -> DEFAULT shots),
     * but while blind-searching (HUNT mode) it occasionally spends Level 2 ammo
     * to cover 3 cells at once instead of 1.
     */
    @Override
    public AiShotPlan chooseShotPlan(Board enemyBoard, int level2Ammo, int nuclearAmmo) {
        boolean hunting = queue.isEmpty();
        if (hunting && level2Ammo > 0 && random.nextInt(4) == 0) {
            List<Coordinate> unshot = enemyBoard.getUnshotCells();
            List<Coordinate> parity = new ArrayList<>();
            for (Coordinate c : unshot) {
                if ((c.getRow() + c.getCol()) % 2 == 0) parity.add(c);
            }
            List<Coordinate> pool = parity.isEmpty() ? unshot : parity;
            Coordinate anchor = pool.get(random.nextInt(pool.size()));
            return new AiShotPlan(LauncherType.LEVEL_2, anchor, random.nextBoolean());
        }
        return AIStrategy.super.chooseShotPlan(enemyBoard, level2Ammo, nuclearAmmo);
    }
}
