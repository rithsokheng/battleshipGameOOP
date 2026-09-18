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
 * The {@link #getAudio()} accessor lets views play sounds through the
 * {@link GameAudio} abstraction instead of the static {@code SoundManager}
 * singleton.</p>
 */
public interface ViewNavigator {

    // ---------- Local (vs AI / hotseat) flow ----------

    void showMainMenu();
    void showModeSelect();
    void showBoardSelect();
    void showShipPlacement();
    void showPassScreen(Runnable onContinue);
    void showBattle();
    void showGameOver(Player winner);
    void showMultiplayerLobby();

    // ---------- Network ("Play With a Friend") flow ----------

    void showNetworkShipPlacement(NetworkGameSession session);
    void showNetworkBattle(NetworkGameSession session);
    void showNetworkGameOver(NetworkGameSession session, boolean won);

    /** Lets standalone screens push themselves directly. */
    void setScreen(javafx.scene.Parent root);

    /** The window, needed only by modal dialogs owned by deep screens. */
    javafx.stage.Stage getStage();

    /** Game audio facade, so views never touch the concrete sound manager. */
    GameAudio getAudio();
}
