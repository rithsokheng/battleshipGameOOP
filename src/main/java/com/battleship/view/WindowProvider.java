package com.battleship.view;

import javafx.stage.Window;

/**
 * Supplies the owning window to screens that need to anchor modal dialogs.
 *
 * <p>Extracted from {@link ViewNavigator} (SRP / ISP audit): leaking
 * {@code javafx.stage.Stage} out of the navigator meant every screen could reach
 * the whole application window, whether or not it had any business doing so.</p>
 */
@FunctionalInterface
public interface WindowProvider {

    Window window();
}
