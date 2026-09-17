package com.battleship.ai;

import com.battleship.model.AmmoInventory;
import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.ShotResult;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Lieutenant (Normal) difficulty: HUNT/TARGET state machine.
 * HUNT  -> checkerboard-parity random search (exploits min ship size = 2).
 * TARGET -> on hit, queue orthogonal neighbors and follow the line.
 */
public class HuntTargetAI implements AIStrategy {

    private final TargetingQueue targetQueue = new TargetingQueue();
    private final SecureRandom random = new SecureRandom();
    private int lastBoardSize = -1;

    @Override
    public Coordinate chooseTarget(Board enemyBoard) {
        lastBoardSize = enemyBoard.getSize();

        // TARGET mode: drain queue, skip any coordinate already shot at.
        Coordinate queued = targetQueue.nextTarget(enemyBoard);
        if (queued != null) return queued;

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
            targetQueue.clear();
            return;
        }
        targetQueue.enqueueNeighbors(result.coordinate(), lastBoardSize);
    }

    /**
     * Normal AI stays precise once it has a lead (TARGET mode -> DEFAULT shots),
     * but while blind-searching (HUNT mode) it occasionally spends Level 2 ammo
     * to cover 3 cells at once instead of 1.
     */
    @Override
    public AiShotPlan chooseShotPlan(Board enemyBoard, AmmoInventory ammo) {
        boolean hunting = !targetQueue.hasTargets();
        if (hunting && ammo.hasAmmo(LauncherType.LEVEL_2) && !ammo.isInfinite(LauncherType.LEVEL_2)
                && random.nextInt(4) == 0) {
            List<Coordinate> unshot = enemyBoard.getUnshotCells();
            List<Coordinate> parity = new ArrayList<>();
            for (Coordinate c : unshot) {
                if ((c.getRow() + c.getCol()) % 2 == 0) parity.add(c);
            }
            List<Coordinate> pool = parity.isEmpty() ? unshot : parity;
            Coordinate anchor = pool.get(random.nextInt(pool.size()));
            return new AiShotPlan(LauncherType.LEVEL_2, anchor, random.nextBoolean());
        }
        return AIStrategy.super.chooseShotPlan(enemyBoard, ammo);
    }

    /** Expose targeting queue state for SmartAI composition. */
    protected boolean isTargeting() {
        return targetQueue.hasTargets();
    }
}
