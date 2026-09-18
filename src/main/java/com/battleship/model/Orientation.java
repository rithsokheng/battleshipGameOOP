package com.battleship.model;

import java.util.Random;

/**
 * Ship and launcher orientation — replaces raw boolean flags.
 * Self-documenting at call sites and extensible (e.g. DIAGONAL variants),
 * unlike a bare {@code boolean horizontal} whose meaning must be guessed.
 */
public enum Orientation {

    HORIZONTAL,
    VERTICAL;

    public boolean isHorizontal() { return this == HORIZONTAL; }

    public Orientation toggle() {
        return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }

    public static Orientation random(Random rng) {
        return rng.nextBoolean() ? HORIZONTAL : VERTICAL;
    }
}