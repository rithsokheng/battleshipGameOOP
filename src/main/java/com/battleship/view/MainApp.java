package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Player;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application entry point. Owns the primary Stage/Scene and exposes
 * navigation methods used by every screen to move to the next one.
 */
public class MainApp extends Application {

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
        setRoot(new BattleView(this, controller).build());
    }

    public void showGameOver(Player winner) {
        setRoot(new GameOverView(this, controller, winner).build());
    }

    public Stage getStage() { return stage; }
    public GameController getController() { return controller; }

    public static void main(String[] args) {
        launch(args);
    }
}
