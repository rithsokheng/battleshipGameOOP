package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.model.ShipType;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Optional;

import java.util.ArrayList;
import java.util.List;

/**
 * ISSUE 4 + 5: ship placement screen.
 * Left = dock, center = board (native drag-and-drop), right = status panel,
 * bottom = AUTO PLACE / RESET. Includes a live orientation label and a
 * fading rotate-hint tooltip.
 */
public class ShipPlaceView {

    private final ViewNavigator nav;
    private final GameController controller;
    private final Player player;

    private BoardGridPane boardGridPane;
    private ShipDockPane dockPane;
    private Label orientationLabel;
    private Label countLabel;
    private Button readyButton;

    private Orientation orientation = Orientation.HORIZONTAL;
    private final List<int[]> ghostCells = new ArrayList<>();

    public ShipPlaceView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
        this.player = controller.getPlacingPlayer();
    }

    public StackPane build() {
        Label title = new Label("DEPLOY YOUR FLEET");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 34));
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("\u2693  " + player.getName().toUpperCase() + " \u2014 POSITION YOUR SHIPS  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        subtitle.getStyleClass().add("app-subtitle");

        dockPane = new ShipDockPane(controller, player);
        dockPane.setOrientation(orientation);
        dockPane.getStyleClass().add("side-card");
        dockPane.setPrefWidth(190);

        boardGridPane = new BoardGridPane(controller.getSelectedTheater().getBoardSize());
        setupDragTargets();

        orientationLabel = new Label();
        orientationLabel.getStyleClass().add("accent-text");
        orientationLabel.setStyle("-fx-font-size:13px;");
        updateOrientationLabel();

        Label statusHeader = new Label("STATUS");
        statusHeader.getStyleClass().add("side-card-title");

        countLabel = new Label();
        countLabel.setStyle("-fx-text-fill:#f5f7fa; -fx-font-size:14px; -fx-font-weight:bold;");

        Label removeHint = new Label("Click a placed ship to return it to the dock");
        removeHint.setWrapText(true);
        removeHint.setMaxWidth(190);
        removeHint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        removeHint.getStyleClass().add("dim-text");

        readyButton = new Button("READY");
        readyButton.setPrefWidth(170);
        readyButton.setPrefHeight(46);
        readyButton.getStyleClass().add("primary-button");
        readyButton.setOnAction(e -> {
            nav.getAudio().playClick();
            controller.confirmReady();
            routeAfterReady();
        });

        VBox statusPanel = new VBox(16, statusHeader, countLabel, orientationLabel, removeHint, readyButton);
        statusPanel.setPadding(new Insets(24, 22, 24, 22));
        statusPanel.setAlignment(Pos.TOP_CENTER);
        statusPanel.getStyleClass().add("side-card");
        statusPanel.setPrefWidth(240);

        VBox boardCard = buildBoardCard();

        Button rotate = new Button("\u21bb  ROTATE SHIP");
        rotate.getStyleClass().add("ghost-button");
        rotate.setPrefHeight(40);
        rotate.setOnAction(e -> { nav.getAudio().playClick(); toggleOrientation(); });

        Button autoPlace = new Button("AUTO PLACE");
        autoPlace.getStyleClass().add("ghost-button");
        autoPlace.setPrefHeight(40);
        autoPlace.setOnAction(e -> {
            nav.getAudio().playClick();
            nav.getAudio().playPlaceShip();
            controller.autoPlaceRemaining(player);
            refreshAll();
        });
        Button reset = new Button("RESET");
        reset.getStyleClass().add("ghost-button");
        reset.setPrefHeight(40);
        reset.setOnAction(e -> {
            nav.getAudio().playClick();
            controller.resetPlacement(player);
            refreshAll();
        });
        HBox bottomBar = new HBox(16, rotate, autoPlace, reset);
        bottomBar.setAlignment(Pos.CENTER);

        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> { nav.getAudio().playClick(); confirmExit(); });

        VBox titleBlock = new VBox(6, title, subtitle);
        titleBlock.setAlignment(Pos.CENTER);
        StackPane titleRow = new StackPane(titleBlock, exit);
        StackPane.setAlignment(exit, Pos.CENTER_RIGHT);
        titleRow.setMaxWidth(Double.MAX_VALUE);

        HBox center = new HBox(26, dockPane, boardCard, statusPanel);
        center.setAlignment(Pos.CENTER);

        VBox layout = new VBox(24, titleRow, center, bottomBar);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(28, 24, 24, 24));
        layout.setMaxWidth(Double.MAX_VALUE);

        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.lightSeaScene(root);
        root.getChildren().add(ocean);
        root.getChildren().add(layout);
        root.setFocusTraversable(true);

        root.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("R")) toggleOrientation();
        });
        root.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) toggleOrientation();
        });
        root.requestFocus();

        // Same gentle entrance used on the mode-select screen, so navigating
        // into placement feels continuous rather than an abrupt cut.
        layout.setOpacity(0.0);
        layout.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(380), layout);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(380), layout);
        rise.setToY(0);
        fade.play();
        rise.play();

        showRotateHint(root);
        refreshAll();
        return root;
    }

    /** Wraps the board grid in the same "board-card" chrome used on the battle screen. */
    private VBox buildBoardCard() {
        Label heading = new Label("YOUR WATERS");
        heading.getStyleClass().add("board-card-title");
        heading.setStyle("-fx-font-size:15px;");

        VBox card = new VBox(16, heading, boardGridPane);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22));
        return card;
    }

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText(null);
        alert.setContentText("Leave this game and return to the main menu? Your fleet deployment will be lost.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            nav.getAudio().stopBgm();
            nav.getAudio().playMenuMusic();
            nav.showMainMenu();
        }
    }

    private void toggleOrientation() {
        orientation = orientation.toggle();
        updateOrientationLabel();
        dockPane.setOrientation(orientation);
    }

    private void updateOrientationLabel() {
        orientationLabel.setText("Current orientation: " + (orientation.isHorizontal() ? "HORIZONTAL" : "VERTICAL"));
    }

    private void showRotateHint(StackPane root) {
        Label hint = new Label("Press R or Right-Click to rotate ship");
        hint.getStyleClass().add("status-panel");
        hint.setStyle("-fx-text-fill:#ffd166; -fx-padding:8 14;");
        StackPane.setAlignment(hint, Pos.TOP_CENTER);
        StackPane.setMargin(hint, new Insets(16, 0, 0, 0));
        root.getChildren().add(hint);

        PauseTransition wait = new PauseTransition(Duration.seconds(3));
        FadeTransition fade = new FadeTransition(Duration.millis(500), hint);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        SequentialTransition seq = new SequentialTransition(wait, fade);
        seq.setOnFinished(e -> root.getChildren().remove(hint));
        seq.play();
    }

    private void setupDragTargets() {
        int size = boardGridPane.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                final int row = r, col = c;
                StackPane cell = boardGridPane.getCell(r, c);

                cell.setOnDragOver(event -> {
                    if (event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });

                cell.setOnDragEntered(event -> {
                    if (!event.getDragboard().hasString()) return;
                    ShipType type = ShipType.valueOf(event.getDragboard().getString());
                    showGhost(row, col, type);
                });

                cell.setOnDragExited(event -> clearGhost());

                cell.setOnMouseClicked(event -> {
                    if (event.getButton() != MouseButton.PRIMARY) return;
                    Ship ship = player.getOwnBoard().getShipAt(new Coordinate(row, col));
                    if (ship != null) {
                        nav.getAudio().playRemoveShip();
                        controller.removeShip(player, ship);
                        refreshAll();
                    }
                });

                cell.setOnDragDropped(event -> {
                    if (!event.getDragboard().hasString()) { event.setDropCompleted(false); event.consume(); return; }
                    ShipType type = ShipType.valueOf(event.getDragboard().getString());
                    clearGhost();
                    boolean placed = controller.placeShip(player, type, new Coordinate(row, col), orientation);
                    if (placed) {
                        nav.getAudio().playPlaceShip();
                        refreshAll();
                    } else {
                        shakeCell(cell);
                    }
                    event.setDropCompleted(placed);
                    event.consume();
                });
            }
        }
    }

    private void showGhost(int row, int col, ShipType type) {
        clearGhost();
        boolean valid = controller.canPlace(player, type, new Coordinate(row, col), orientation);
        String color = valid ? "-fx-background-color: rgba(232,213,163,0.4);" : "-fx-background-color: rgba(200,58,58,0.5);";
        for (int i = 0; i < type.getSize(); i++) {
            int gr = orientation.isHorizontal() ? row : row + i;
            int gc = orientation.isHorizontal() ? col + i : col;
            if (gr < 0 || gr >= boardGridPane.getSize() || gc < 0 || gc >= boardGridPane.getSize()) continue;
            boardGridPane.getCell(gr, gc).setStyle(BoardGridPane.BASE_STYLE + color);
            ghostCells.add(new int[]{gr, gc});
        }
    }

    private void clearGhost() {
        for (int[] rc : ghostCells) {
            boardGridPane.resetCellStyle(rc[0], rc[1]);
        }
        ghostCells.clear();
        // Re-render any already-placed ships that may have been under the ghost.
        for (Ship s : player.getOwnBoard().getShips()) {
            boardGridPane.renderShip(s);
        }
    }

    private void shakeCell(StackPane cell) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), cell);
        shake.setByX(4);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    private void refreshAll() {
        boardGridPane.clearAll();
        for (Ship s : player.getOwnBoard().getShips()) {
            boardGridPane.renderShip(s);
        }
        dockPane.refresh();
        int placed = player.getOwnBoard().getShips().size();
        int total = controller.getSelectedTheater().getTotalShipCount();
        countLabel.setText("Ships placed: " + placed + " / " + total);
        readyButton.setDisable(!controller.isPlacementComplete(player));
    }

    private void routeAfterReady() {
        switch (controller.getState()) {
            case PASS_SCREEN -> nav.showPassScreen(() -> {
                controller.resumePlacementAfterPass();
                nav.showShipPlacement();
            });
            case BATTLE -> nav.showBattle();
            default -> nav.showShipPlacement();
        }
    }
}
