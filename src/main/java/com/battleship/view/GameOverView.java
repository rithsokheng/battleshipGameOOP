package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.*;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * ISSUE 6 (cont.): reveals both boards fully. Sunk ships use renderSunkShip()
 * (all cells recolored); surviving ships use the normal ship render.
 * Restyled to match the "Fleet Command" theme: full-bleed sea backdrop
 * (gold-tinted for a win, storm-tinted for a loss), board-card framed
 * reveal boards, and a pill-style stat readout.
 */
public class GameOverView {

    private final ViewNavigator nav;
    private final GameController controller;
    private final Player winner;

    public GameOverView(ViewNavigator nav, GameController controller, Player winner) {
        this.nav = nav;
        this.controller = controller;
        this.winner = winner;
    }

    public StackPane build() {
        boolean playerWon = winner == controller.getPlayer1();
        nav.getAudio().playGameOver(playerWon);

        Label banner = new Label(playerWon ? "\uD83C\uDFC6  VICTORY" : "\u2620  DEFEAT");
        banner.setFont(Font.font("Arial Black", FontWeight.BOLD, 52));
        banner.getStyleClass().add(playerWon ? "app-title" : "danger-text");
        if (!playerWon) {
            banner.setStyle("-fx-font-size:52px; -fx-effect: dropshadow(gaussian, rgba(255,92,92,0.55), 28, 0.35, 0, 0);");
        }

        Label subtitle = new Label(playerWon
                ? "\u2693  THE ENEMY FLEET HAS BEEN DESTROYED  \u2693"
                : "\u2693  YOUR FLEET HAS BEEN LOST  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        subtitle.getStyleClass().add("app-subtitle");

        VBox ownBoard = revealedBoardCard("YOUR FLEET", controller.getPlayer1().getOwnBoard());
        VBox enemyBoard = revealedBoardCard(
                controller.getPlayer2().getName().toUpperCase() + "'S FLEET", controller.getPlayer2().getOwnBoard());

        HBox boards = new HBox(28, ownBoard, enemyBoard);
        boards.setAlignment(Pos.CENTER);

        HBox stats = buildStats();

        Button reEngage = new Button("\u21bb  RE-ENGAGE");
        reEngage.setPrefWidth(190);
        reEngage.setPrefHeight(46);
        reEngage.getStyleClass().addAll("primary-button", "featured-button");
        reEngage.setOnAction(e -> { nav.getAudio().playClick(); nav.showBoardSelect(); });

        Button returnToPort = new Button("RETURN TO PORT");
        returnToPort.setPrefWidth(190);
        returnToPort.setPrefHeight(46);
        returnToPort.getStyleClass().add("ghost-button");
        returnToPort.setOnAction(e -> { nav.getAudio().playClick(); nav.getAudio().playMenuMusic(); nav.showMainMenu(); });

        HBox buttons = new HBox(16, reEngage, returnToPort);
        buttons.setAlignment(Pos.CENTER);

        VBox titleBlock = new VBox(6, banner, subtitle);
        titleBlock.setAlignment(Pos.CENTER);

        VBox layout = new VBox(22, titleBlock, boards, stats, buttons);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(28, 24, 28, 24));

        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root, 0.0);
        root.getChildren().add(ocean);

        // A soft mood wash over the sea: warm gold for a win, cool red for a loss.
        Region mood = new Region();
        mood.setStyle(playerWon
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

    private VBox revealedBoardCard(String label, Board board) {
        Label title = new Label(label);
        title.getStyleClass().add("board-card-title");

        BoardGridPane grid = new BoardGridPane(board.getSize());
        for (Ship s : board.getShips()) {
            if (s.isSunk()) {
                grid.renderSunkShip(s);
            } else {
                grid.renderShip(s);
            }
        }
        for (int r = 0; r < board.getSize(); r++) {
            for (int c = 0; c < board.getSize(); c++) {
                Coordinate coord = new Coordinate(r, c);
                if (board.getCellStatus(coord) == CellStatus.MISS) {
                    grid.renderShot(coord, CellStatus.MISS);
                }
            }
        }

        VBox card = new VBox(14, title, grid);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private HBox buildStats() {
        Board defenderBoard = winner == controller.getPlayer1()
                ? controller.getPlayer2().getOwnBoard()
                : controller.getPlayer1().getOwnBoard();

        int hits = 0, misses = 0;
        for (int r = 0; r < defenderBoard.getSize(); r++) {
            for (int c = 0; c < defenderBoard.getSize(); c++) {
                CellStatus status = defenderBoard.getCellStatus(new Coordinate(r, c));
                if (status == CellStatus.HIT || status == CellStatus.SUNK) hits++;
                if (status == CellStatus.MISS) misses++;
            }
        }
        int totalShots = hits + misses;
        double accuracy = totalShots == 0 ? 0 : (100.0 * hits / totalShots);
        long shipsSunk = defenderBoard.getShips().stream().filter(Ship::isSunk).count();

        HBox row = new HBox(0,
                statPill("SHOTS FIRED", String.valueOf(totalShots)),
                statDivider(),
                statPill("HITS", String.valueOf(hits)),
                statDivider(),
                statPill("ACCURACY", String.format("%.1f%%", accuracy)),
                statDivider(),
                statPill("SHIPS SUNK", String.valueOf(shipsSunk)));
        row.getStyleClass().add("side-card");
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(16, 26, 16, 26));
        return row;
    }

    private VBox statPill(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("accent-text");
        v.setStyle("-fx-font-size:20px;");
        Label l = new Label(label);
        l.getStyleClass().add("dim-text");
        VBox box = new VBox(4, v, l);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 22, 0, 22));
        return box;
    }

    private Region statDivider() {
        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setMaxWidth(1);
        divider.setStyle("-fx-background-color: rgba(46,93,135,0.6);");
        divider.setPrefHeight(34);
        return divider;
    }
}
