package com.battleship.ai;

import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShotResult;

import java.security.SecureRandom;

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

        // HUNT mode: checkerboard parity over unshot cells (shared heuristic, DRY).
        return ParityHunter.pick(enemyBoard, random);
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
    public AiShotPlan chooseShotPlan(Board enemyBoard, Player firingPlayer) {
        boolean hunting = !targetQueue.hasTargets();
        if (hunting && firingPlayer.hasAmmo(LauncherType.LEVEL_2)
                && !firingPlayer.isAmmoInfinite(LauncherType.LEVEL_2)
                && random.nextInt(4) == 0) {
            // Same shared HUNT heuristic as chooseTarget — no duplicated parity block.
            Coordinate anchor = ParityHunter.pick(enemyBoard, random);
            return new AiShotPlan(LauncherType.LEVEL_2, anchor, Orientation.random(random));
        }
        return AIStrategy.super.chooseShotPlan(enemyBoard, firingPlayer);
    }
}
