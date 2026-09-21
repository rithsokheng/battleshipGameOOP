package com.battleship.view;

import com.battleship.model.Player;
import com.battleship.net.NetworkGameSession;

/**
 * Abstraction for screen navigation (fixes V6 / C1). Views depend on this
 * interface — never on the concrete {@code MainApp} — so they can be
 * unit-tested without a JavaFX runtime, and the Application class no longer
 * has to be known to every screen.
 *
 * <p>{@link MainApp} is the production implementation; tests may supply a stub.
 * By extending {@link AudioProvider} and {@link WindowProvider}, the navigator
 * also satisfies segregated clients that only need sound playback or a dialog owner window.</p>
 */
public interface ViewNavigator extends ScreenNavigator, AudioProvider, WindowProvider {

    /** The window, needed only by modal dialogs owned by deep screens. */
    javafx.stage.Stage getStage();

    /** Game audio facade, so views never touch the concrete sound manager. */
    GameAudio getAudio();

    @Override
    default GameAudio audio() {
        return getAudio();
    }

    @Override
    default javafx.stage.Window window() {
        return getStage();
    }
}
