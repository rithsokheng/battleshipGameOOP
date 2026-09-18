package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.model.ShipType;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ship placement for a network match. Visually identical to ShipPlaceView but
 * drives a READY/READY -&gt; (host decides) START handshake over the socket
 * instead of the local PASS_SCREEN flow.
 */
public class NetworkShipPlaceView {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ViewNavigator nav;
    private final GameController controller;
    private final NetworkGameSession netSession;
    private final Player player;

    private BoardGridPane boardGridPane;
    private ShipDockPane dockPane;
    private Label orientationLabel;
    private Label countLabel;
    private Label statusLabel;
    private Button readyButton;

    private Orientation orientation = Orientation.HORIZONTAL;
    private boolean localReady = false;
    private boolean opponentReady = false;
    private final List<int[]> ghostCells = new ArrayList<>();

    public NetworkShipPlaceView(ViewNavigator nav, GameController controller, NetworkGameSession netSession) {
        this.nav = nav;
        this.controller = controller;
        this.netSession = netSession;
        this.player = netSession.getMe();
    }

    public StackPane build() {
        Label title = new Label("DEPLOY YOUR FLEET \u2014 " + player.getName());
        title.getStyleClass().add("app-title");
        title.setStyle("-fx-font-size:24px;");

        dockPane = new ShipDockPane(controller, player);
        dockPane.setOrientation(orientation);
        dockPane.getStyleClass().add("card-panel");

        boardGridPane = new BoardGridPane(controller.getSelectedTheater().getBoardSize());
        setupDragTargets();

        orientationLabel = new Label();
        orientationLabel.getStyleClass().add("accent-text");
        orientationLabel.setStyle("-fx-font-size:13px;");
        updateOrientationLabel();

        countLabel = new Label();
        countLabel.setStyle("-fx-text-fill:#f5f7fa; -fx-font-size:14px;");

        statusLabel = new Label("Deploy your fleet, then hit READY.");
        statusLabel.getStyleClass().add("dim-text");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(180);

        readyButton = new Button("READY");
        readyButton.setPrefWidth(160);
        readyButton.setPrefHeight(44);
        readyButton.getStyleClass().add("primary-button");
        readyButton.setOnAction(e -> onReadyClicked());

        Label removeHint = new Label("Click a placed ship to put it back in the dock");
        removeHint.setWrapText(true);
        removeHint.setMaxWidth(180);
        removeHint.getStyleClass().add("dim-text");

        VBox statusPanel = new VBox(14, countLabel, orientationLabel, removeHint, readyButton, statusLabel);
        statusPanel.setPadding(new Insets(14));
        statusPanel.setAlignment(Pos.TOP_CENTER);
        statusPanel.getStyleClass().add("card-panel");
        statusPanel.setPrefWidth(200);

        Button rotate = new Button("ROTATE SHIP");
        rotate.getStyleClass().add("ghost-button");
        rotate.setOnAction(e -> toggleOrientation());

        Button autoPlace = new Button("AUTO PLACE");
        autoPlace.getStyleClass().add("ghost-button");
        autoPlace.setOnAction(e -> {
            controller.autoPlaceRemaining(player);
            refreshAll();
        });
        Button reset = new Button("RESET");
        reset.getStyleClass().add("ghost-button");
        reset.setOnAction(e -> {
            controller.resetPlacement(player);
            refreshAll();
        });
        HBox bottomBar = new HBox(16, rotate, autoPlace, reset);
        bottomBar.setAlignment(Pos.CENTER);

        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> confirmExit());

        HBox topBar = new HBox(title);
        topBar.setAlignment(Pos.CENTER);
        StackPane titleRow = new StackPane(topBar, exit);
        StackPane.setAlignment(exit, Pos.CENTER_RIGHT);

        HBox center = new HBox(24, dockPane, boardGridPane, statusPanel);
        center.setAlignment(Pos.CENTER);

        VBox layout = new VBox(20, titleRow, center, bottomBar);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));

        StackPane root = new StackPane(layout);
        root.setFocusTraversable(true);

        root.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("R")) toggleOrientation();
        });
        root.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) toggleOrientation();
        });
        root.requestFocus();

        netSession.getSession().setOnMessage(this::handleMessage);
        netSession.getSession().setOnDisconnected(this::handleDisconnect);

        refreshAll();
        return root;
    }

    private void handleMessage(NetMessage msg) {
        if (msg == null) return;
        if (msg instanceof NetMessage.Ready) {
            opponentReady = true;
            statusLabel.setText(localReady
                    ? "Both fleets deployed \u2014 starting battle\u2026"
                    : "Opponent is ready. Deploy your fleet!");
            maybeStartAsHost();
        } else if (msg instanceof NetMessage.Start start) {
            // Only the client ever receives this (the host sets its own turn locally
            // in maybeStartAsHost right before sending START).
            netSession.beginMatch("HOST".equals(start.firstPlayer()));
            goToBattle();
        }
    }

    private void handleDisconnect() {
        statusLabel.setText("Connection lost.");
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Disconnected");
        alert.setHeaderText(null);
        alert.setContentText("Your opponent disconnected.");
        alert.showAndWait();
        nav.showMainMenu();
    }

    private void onReadyClicked() {
        if (!controller.isPlacementComplete(player)) return;
        localReady = true;
        readyButton.setDisable(true);
        statusLabel.setText(opponentReady ? "Both fleets deployed \u2014 starting battle\u2026" : "Waiting for opponent to finish deploying\u2026");
        netSession.getSession().send(new NetMessage.Ready());
        maybeStartAsHost();
    }

    private void maybeStartAsHost() {
        if (netSession.isHost() && localReady && opponentReady) {
            boolean hostFirst = RANDOM.nextBoolean();
            netSession.beginMatch(hostFirst);
            netSession.getSession().send(new NetMessage.Start(hostFirst ? "HOST" : "CLIENT"));
            goToBattle();
        }
    }

    private void goToBattle() {
        nav.showNetworkBattle(netSession);
    }

    // ---------- Placement UI (mirrors ShipPlaceView) ----------

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText(null);
        alert.setContentText("Leave this match and return to the main menu? This will disconnect your opponent.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            netSession.getSession().close();
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
        readyButton.setDisable(!controller.isPlacementComplete(player) || localReady);
    }
}
