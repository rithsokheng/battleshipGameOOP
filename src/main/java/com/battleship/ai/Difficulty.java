package com.battleship.ai;

/** AI difficulty tiers, mapped to concrete AIStrategy implementations by AIFactory. */
public enum Difficulty {
    ENSIGN,     // Easy   -> RandomAI
    LIEUTENANT, // Normal -> HuntTargetAI
    ADMIRAL     // Hard   -> SmartAI
}
