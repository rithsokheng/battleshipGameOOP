package com.battleship.ai;

import com.battleship.model.GameMode;

/** Factory that instantiates the correct AIStrategy for a given Difficulty or GameMode. */
public class AIFactory {

    private AIFactory() { } // prevent instantiation

    public static AIStrategy create(Difficulty difficulty) {
        return switch (difficulty) {
            case ENSIGN -> new RandomAI();
            case LIEUTENANT -> new HuntTargetAI();
            case ADMIRAL -> new SmartAI();
        };
    }

    /** Returns null for HOTSEAT, ONLINE, or null input — no AI needed. Prefer {@link #createOptional(GameMode)}. */
    public static AIStrategy create(GameMode mode) {
        if (mode == null || mode == GameMode.HOTSEAT || mode == GameMode.ONLINE) {
            return null;
        }
        return switch (mode) {
            case AI_EASY -> new RandomAI();
            case AI_NORMAL -> new HuntTargetAI();
            case AI_HARD -> new SmartAI();
            default -> null;
        };
    }

    /** Returns an Optional AIStrategy, empty for HOTSEAT. */
    public static java.util.Optional<AIStrategy> createOptional(GameMode mode) {
        return java.util.Optional.ofNullable(create(mode));
    }
}

