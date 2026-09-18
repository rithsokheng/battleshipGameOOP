package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Player;
import com.battleship.net.NetworkGameSession;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application entry point. Owns the primary Stage/Scene and implements
 * {@link ViewNavigator} — views receive the navigator abstraction (never this
 * concrete Application class), so navigation can be mocked in tests.
 */
public class MainApp extends Application implements ViewNavigator {

    private Stage stage;
    private Scene scene;
    private final GameController controller = new GameController();

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.scene = new Scene(new StackPane(), 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/battleship.css").toExternalForm());
        stage.setTitle("Battleship: Naval Command");
        stage.setScene(scene);
        SoundManager.getInstance().playMenuMusic();
        showMainMenu();
        stage.show();
    }

    private void setRoot(javafx.scene.Parent root) {
        scene.setRoot(root);
    }

    /** Lets standalone screens (e.g. the network multiplayer flow) push themselves directly. */
    public void setScreen(javafx.scene.Parent root) {
        setRoot(root);
    }

    public void showMainMenu() {
        setRoot(new MainMenuView(this).build());
    }

    public void showModeSelect() {
        controller.goToModeSelect();
        setRoot(new GameModeSelectView(this, controller).build());
    }

    public void showMultiplayerLobby() {
        setRoot(new MultiplayerLobbyView(this, controller).build());
    }

    public void showBoardSelect() {
        setRoot(new BoardSelectView(this, controller).build());
    }

    public void showShipPlacement() {
        setRoot(new ShipPlaceView(this, controller).build());
    }

    public void showPassScreen(Runnable onContinue) {
        setRoot(new PassScreen(controller.getPlacingPlayer().getName(), onContinue).build());
    }

    public void showBattle() {
        setRoot(new LocalBattleView(this, controller).build());
    }

    public void showGameOver(Player winner) {
        setRoot(new GameOverView(this, controller, winner).build());
    }

    // ---------- Network flow (ViewNavigator contract) ----------

    @Override
    public void showNetworkShipPlacement(NetworkGameSession session) {
        setRoot(new NetworkShipPlaceView(this, controller, session).build());
    }

    @Override
    public void showNetworkBattle(NetworkGameSession session) {
        setRoot(new NetworkBattleView(this, controller, session).build());
    }

    @Override
    public void showNetworkGameOver(NetworkGameSession session, boolean won) {
        setRoot(new NetworkGameOverView(this, session, won).build());
    }

    public Stage getStage() { return stage; }
    public GameController getController() { return controller; }

    /** Views play sounds through the GameAudio abstraction, never the singleton. */
    @Override
    public GameAudio getAudio() { return SoundManager.getInstance(); }

    public static void main(String[] args) {
        launch(args);
    }
}
