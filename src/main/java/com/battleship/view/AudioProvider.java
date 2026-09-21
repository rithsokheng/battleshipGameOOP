package com.battleship.view;

/**
 * Supplies the game's audio facade to a screen.
 *
 * <p>Extracted from {@link ViewNavigator} (SRP / ISP audit): a navigator should
 * navigate and nothing else. Views that need sound ask for this tiny capability
 * (a lambda or a stub in tests), and screens that have no sound simply do not
 * request it.</p>
 */
@FunctionalInterface
public interface AudioProvider {

    GameAudio audio();
}
