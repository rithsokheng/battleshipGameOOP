package com.battleship.ai;

import com.battleship.model.GameMode;

/** Factory that instantiates the correct AIStrategy for a given Difficulty or GameMode. */
public class AIFactory {

    public static AIStrategy create(Difficulty difficulty) {
        return switch (difficulty) {
            case ENSIGN -> new RandomAI();
            case LIEUTENANT -> new HuntTargetAI();
            case ADMIRAL -> new SmartAI();
        };
    }

    /** Returns null for HOTSEAT — no AI needed. */
    public static AIStrategy create(GameMode mode) {
        return switch (mode) {
            case AI_EASY -> new RandomAI();
            case AI_NORMAL -> new HuntTargetAI();
            case AI_HARD -> new SmartAI();
            case HOTSEAT -> null;
        };
    }
}
