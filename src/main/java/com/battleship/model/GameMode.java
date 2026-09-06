package com.battleship.model;

/** Selected game mode from GameModeSelectView. */
public enum GameMode {
    AI_EASY("vs AI - Easy"),
    AI_NORMAL("vs AI - Normal"),
    AI_HARD("vs AI - Hard"),
    HOTSEAT("1v1 Hotseat");

    private final String label;

    GameMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean isVsAi() { return this != HOTSEAT; }
}
