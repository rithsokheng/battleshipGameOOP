package com.battleship.ai;

import com.battleship.model.AmmoInventory;
import com.battleship.model.Board;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.ShotResult;

/**
 * Strategy pattern contract for AI opponents. Pure logic — no JavaFX dependency.
 */
public interface AIStrategy {

    /** Chooses the next coordinate to fire at, given the enemy board as known so far. */
    Coordinate chooseTarget(Board enemyBoardKnowledge);

    /** Optional hook to let stateful strategies (e.g. Hunt/Target) update after each shot. */
    void notifyResult(ShotResult result);

    /**
     * Chooses a full shot plan (launcher + anchor + orientation) for this turn.
     * Default implementation always fires the infinite-ammo DEFAULT launcher via
     * chooseTarget(), so Easy AI (and any strategy that doesn't override this)
     * never touches the special launchers.
     */
    default AiShotPlan chooseShotPlan(Board enemyBoard, AmmoInventory ammo) {
        return new AiShotPlan(LauncherType.DEFAULT, chooseTarget(enemyBoard), Orientation.HORIZONTAL);
    }
}
