package com.battleship.view.battle;

import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;

import java.util.List;

/**
 * How a target board previews and validates the admiral's next shot.
 *
 * <p>Supply by the battle screen: it knows the armed weapon (through the weapon
 * console) and the rules for "this pattern is fully shelled already".</p>
 */
public interface TargetModel {

    /** Cells the currently armed weapon would cover from {@code anchor}. */
    List<Coordinate> patternAt(Coordinate anchor);

    /** Current firing direction (R / right-click toggles it). */
    Orientation orientation();

    /** True when the local admiral may fire right now. */
    boolean canFireNow();
}
