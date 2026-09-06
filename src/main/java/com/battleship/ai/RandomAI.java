package com.battleship.ai;

import com.battleship.model.Board;
import com.battleship.model.Coordinate;
import com.battleship.model.ShotResult;

import java.security.SecureRandom;
import java.util.List;

/** Ensign (Easy) difficulty: uniform random selection over unshot cells. */
public class RandomAI implements AIStrategy {

    private final SecureRandom random = new SecureRandom();

    @Override
    public Coordinate chooseTarget(Board enemyBoard) {
        List<Coordinate> unshot = enemyBoard.getUnshotCells();
        return unshot.get(random.nextInt(unshot.size()));
    }

    @Override
    public void notifyResult(ShotResult result) {
        // No state to update.
    }
}
