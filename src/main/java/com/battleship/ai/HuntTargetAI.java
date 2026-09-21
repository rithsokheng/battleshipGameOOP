package com.battleship.ai;

import com.battleship.model.AmmoReadout;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShotOrder;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.weapon.WeaponCatalog;

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
    public Coordinate chooseTarget(TrackingGrid knowledge) {
        lastBoardSize = knowledge.size();

        // TARGET mode: drain queue, skip any coordinate already shot at.
        Coordinate queued = targetQueue.nextTarget(knowledge);
        if (queued != null) return queued;

        // HUNT mode: checkerboard parity over unshot cells (shared heuristic, DRY).
        return ParityHunter.pick(knowledge, random);
    }

    @Override
    public void notifyResult(ShotResult result) {
        if (!result.isHit()) return;
        if (result.outcome() == com.battleship.model.CellStatus.SUNK) {
            targetQueue.clear();
            return;
        }
        targetQueue.enqueueNeighbors(result.coordinate(), lastBoardSize);
    }

    /**
     * Normal AI stays precise once it has a lead (TARGET mode -> standard shots),
     * but while blind-searching (HUNT mode) it occasionally spends salvo ammo to
     * cover 3 cells at once instead of 1.
     */
    @Override
    public ShotOrder chooseShotPlan(TrackingGrid knowledge, AmmoReadout ammo) {
        var salvo = WeaponCatalog.salvo();
        boolean hunting = !targetQueue.hasTargets();
        if (hunting && ammo.hasAmmo(salvo) && !ammo.isAmmoInfinite(salvo) && random.nextInt(4) == 0) {
            // Same shared HUNT heuristic as chooseTarget — no duplicated parity block.
            Coordinate anchor = ParityHunter.pick(knowledge, random);
            return new ShotOrder(salvo, anchor, Orientation.random(random));
        }
        return AIStrategy.super.chooseShotPlan(knowledge, ammo);
    }
}
