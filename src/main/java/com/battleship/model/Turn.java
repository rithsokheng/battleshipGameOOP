package com.battleship.model;

/** Whose turn it is — replaces the raw int index (fixes P1, primitive obsession). */
public enum Turn {
    PLAYER_1,
    PLAYER_2;

    /** Self-documenting alternative to {@code index = 1 - index}. */
    public Turn next() {
        return this == PLAYER_1 ? PLAYER_2 : PLAYER_1;
    }
}
