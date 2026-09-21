package com.battleship.view;

import com.battleship.model.Player;
import com.battleship.net.NetworkGameSession;
import javafx.scene.Parent;

/**
 * Narrow role interface dedicated strictly to screen and view routing.
 *
 * <p>Satisfies the <strong>Interface Segregation Principle (ISP)</strong> by allowing
 * screens and dialogs to depend solely on navigation capabilities without forcing a
 * dependency on window stages or audio managers.</p>
 */
public interface ScreenNavigator {

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
    void setScreen(Parent root);
}
