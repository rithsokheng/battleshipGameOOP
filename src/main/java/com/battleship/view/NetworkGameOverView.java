package com.battleship.view;

import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Ship;
import com.battleship.net.NetworkGameSession;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Game over screen for a network match. Unlike the local GameOverView, we
 * never learned the opponent's real ship layout — so their board only shows
 * the cells revealed by our own shots (identical to what enemyTracker knows).
 * Styled to match GameOverView: full-bleed sea backdrop tinted for win/loss,
 * board-card framed reveal boards.
 */
public class NetworkGameOverView {

    private final ViewNavigator nav;
    private final NetworkGameSession netSession;
    private final boolean won;

    public NetworkGameOverView(ViewNavigator nav, NetworkGameSession netSession, boolean won) {
        this.nav = nav;
        this.netSession = netSession;
        this.won = won;
    }

    public StackPane build() {
        if (netSession.getSession() != null) netSession.getSession().close();
        nav.getAudio().stopBgm();
        nav.getAudio().playGameOver(won);

        Label banner = new Label(won ? "\uD83C\uDFC6  VICTORY" : "\u2620  DEFEAT");
        banner.setFont(Font.font("Arial Black", FontWeight.BOLD, 52));
        banner.getStyleClass().add(won ? "app-title" : "danger-text");
        if (!won) {
            banner.setStyle("-fx-font-size:52px; -fx-effect: dropshadow(gaussian, rgba(255,92,92,0.55), 28, 0.35, 0, 0);");
        }

        Label subtitle = new Label(won
                ? "\u2693  THE ENEMY FLEET HAS BEEN DESTROYED  \u2693"
                : "\u2693  YOUR FLEET HAS BEEN LOST  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        subtitle.getStyleClass().add("app-subtitle");

        HBox boards = new HBox(28, myBoardCard(), enemyTrackerCard());
        boards.setAlignment(Pos.CENTER);

        Button returnToPort = new Button("RETURN TO PORT");
        returnToPort.setPrefWidth(190);
        returnToPort.setPrefHeight(46);
        returnToPort.getStyleClass().addAll("primary-button", "featured-button");
        returnToPort.setOnAction(e -> { nav.getAudio().playClick(); nav.getAudio().playMenuMusic(); nav.showMainMenu(); });

        VBox titleBlock = new VBox(6, banner, subtitle);
        titleBlock.setAlignment(Pos.CENTER);

        VBox layout = new VBox(22, titleBlock, boards, returnToPort);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(28, 24, 28, 24));

        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root, 0.0);
        root.getChildren().add(ocean);

        Region mood = new Region();
        mood.setStyle(won
                ? "-fx-background-color: radial-gradient(center 50% 15%, radius 90%, rgba(255,209,102,0.16) 0%, rgba(255,209,102,0.0) 70%);"
                : "-fx-background-color: radial-gradient(center 50% 15%, radius 90%, rgba(255,70,70,0.16) 0%, rgba(255,70,70,0.0) 70%);");
        mood.setMouseTransparent(true);
        mood.prefWidthProperty().bind(root.widthProperty());
        mood.prefHeightProperty().bind(root.heightProperty());
        root.getChildren().add(mood);

        root.getChildren().add(layout);

        layout.setOpacity(0.0);
        layout.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(420), layout);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), layout);
        rise.setToY(0);
        fade.play();
        rise.play();

        return root;
    }

    private VBox myBoardCard() {
        Label title = new Label("YOUR FLEET");
        title.getStyleClass().add("board-card-title");

        Board board = netSession.getMe().getOwnBoard();
        BoardGridPane grid = new BoardGridPane(board.getSize());
        for (Ship s : board.getShips()) {
            if (s.isSunk()) grid.renderSunkShip(s); else grid.renderShip(s);
        }
        for (int r = 0; r < board.getSize(); r++) {
            for (int c = 0; c < board.getSize(); c++) {
                Coordinate coord = new Coordinate(r, c);
                if (board.getCellStatus(coord) == CellStatus.MISS) grid.renderShot(coord, CellStatus.MISS);
                if (board.getCellStatus(coord) == CellStatus.HIT) grid.renderShot(coord, CellStatus.HIT);
            }
        }

        VBox card = new VBox(14, title, grid);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private VBox enemyTrackerCard() {
        Label title = new Label("ENEMY WATERS (AS OBSERVED)");
        title.getStyleClass().add("board-card-title");

        int size = netSession.getEnemyTracker().getSize();
        BoardGridPane grid = new BoardGridPane(size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Coordinate coord = new Coordinate(r, c);
                CellStatus status = netSession.getEnemyTracker().getStatus(coord);
                if (status == CellStatus.HIT || status == CellStatus.MISS) grid.renderShot(coord, status);
            }
        }
        for (Ship s : netSession.getEnemyTracker().getKnownSunkShips()) {
            grid.renderSunkShip(s);
        }

        VBox card = new VBox(14, title, grid);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        return card;
    }
}
