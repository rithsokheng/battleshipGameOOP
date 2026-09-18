package com.battleship.ai;

import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;

/** An AI's chosen weapon, anchor cell, and orientation for its next shot. */
public record AiShotPlan(LauncherType type, Coordinate anchor, Orientation orientation) { }
