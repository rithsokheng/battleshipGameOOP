package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.*;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import com.battleship.view.quiz.NuclearLaunchDialog;
import com.battleship.view.quiz.NuclearResupplyDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Battle screen for a network ("Play With a Friend") match. Visually mirrors
 * BattleView, but each side is authoritative only over its own board: firing
 * sends a FIRE message and the defender resolves it locally and replies with
 * FIRE_RESULT. No ship layout is ever transmitted.
 */
public class NetworkBattleView {

    private final MainApp app;
    private final GameController controller;
    private final NetworkGameSession netSession;
    private final Player me;

    private BoardGridPane ownGrid;
    private BoardGridPane enemyGrid;
    private Label turnLabel;
    private Label logLabel;
    private Label orientationLabel;
    private Label fleetStatusLabel;
    private HBox launcherBar;

    private final List<int[]> ghostCells = new ArrayList<>();

    public NetworkBattleView(MainApp app, GameController controller, NetworkGameSession netSession) {
        this.app = app;
        this.controller = controller;
        this.netSession = netSession;
        this.me = netSession.getMe();
    }

    public StackPane build() {
        turnLabel = new Label(netSession.isMyTurn() ? "YOUR TURN" : "OPPONENT'S TURN");
        turnLabel.getStyleClass().add("app-title");
        turnLabel.setStyle("-fx-font-size:24px;");

        logLabel = new Label("Select a weapon, then a target on the enemy grid.");
        logLabel.getStyleClass().add("info-text");
        logLabel.setStyle("-fx-font-size:13px;");

        orientationLabel = new Label();
        orientationLabel.getStyleClass().add("dim-text");
        updateOrientationLabel();

        launcherBar = new HBox(10);
        launcherBar.setAlignment(Pos.CENTER);
        refreshLauncherBar();

        ownGrid = new BoardGridPane(me.getOwnBoard().getSize());
        for (Ship s : me.getOwnBoard().getShips()) {
            if (!s.isSunk()) ownGrid.renderShip(s);
        }

        enemyGrid = new BoardGridPane(netSession.getEnemyTracker().getSize());
        attachFireHandlers();
        enemyGrid.setDisable(!netSession.isMyTurn());

        Label ownLabel = new Label("YOUR FLEET");
        ownLabel.getStyleClass().add("accent-text");
        Label enemyLabel = new Label("ENEMY WATERS");
        enemyLabel.getStyleClass().add("accent-text");

        VBox ownBox = new VBox(6, ownLabel, ownGrid);
        ownBox.setAlignment(Pos.CENTER);
        VBox enemyBox = new VBox(6, enemyLabel, enemyGrid);
        enemyBox.setAlignment(Pos.CENTER);

        fleetStatusLabel = new Label();
        refreshFleetStatus();

        HBox boards = new HBox(30, ownBox, enemyBox);
        boards.setAlignment(Pos.CENTER);

        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> { SoundManager.getInstance().playClick(); confirmExit(); });

        HBox topBar = new HBox(turnLabel);
        topBar.setAlignment(Pos.CENTER);
        StackPane titleRow = new StackPane(topBar, exit);
        StackPane.setAlignment(exit, Pos.CENTER_RIGHT);

        VBox layout = new VBox(12, titleRow, launcherBar, orientationLabel, boards, logLabel, fleetStatusLabel);
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

        SoundManager.getInstance().playBattleMusic();

        return root;
    }

    // ---------- Networking ----------

    private void handleMessage(NetMessage msg) {
        if (msg == null) return;
        switch (msg.type) {
            case "FIRE" -> handleIncomingFire(msg);
            case "FIRE_RESULT" -> handleFireResult(msg);
            default -> { /* ignore */ }
        }
    }

    private void handleDisconnect() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Disconnected");
        alert.setHeaderText(null);
        alert.setContentText("Your opponent disconnected.");
        alert.showAndWait();
        app.showMainMenu();
    }

    /** I am the defender: resolve the incoming shot against my real board and reply. */
    private void handleIncomingFire(NetMessage msg) {
        LauncherType type = LauncherType.valueOf(msg.launcherType);
        Coordinate anchor = new Coordinate(msg.anchorRow, msg.anchorCol);
        List<Coordinate> cells = type.getTargetCells(anchor, msg.horizontal);
        Board myBoard = me.getOwnBoard();

        List<ShotResult> results = new ArrayList<>();
        LinkedHashSet<Ship> sunk = new LinkedHashSet<>();
        for (Coordinate c : cells) {
            if (!c.isWithinBounds(myBoard.getSize())) continue;
            CellStatus existing = myBoard.getCellStatus(c);
            if (existing == CellStatus.HIT || existing == CellStatus.MISS || existing == CellStatus.SUNK) continue;
            ShotResult r = myBoard.receiveShot(c);
            results.add(r);
            if (r.outcome() == CellStatus.SUNK) sunk.add(r.shipSunk());
        }

        for (ShotResult r : results) {
            if (r.outcome() != CellStatus.SUNK) ownGrid.renderShot(r.coordinate(), r.outcome());
        }
        for (Ship s : sunk) ownGrid.renderSunkShip(s);

        boolean lost = myBoard.isAllShipsSunk();

        NetMessage result = NetMessage.of("FIRE_RESULT");
        result.results = new ArrayList<>();
        for (ShotResult r : results) {
            NetMessage.CellResult cr = new NetMessage.CellResult();
            cr.row = r.coordinate().getRow();
            cr.col = r.coordinate().getCol();
            cr.outcome = r.outcome().name();
            result.results.add(cr);
        }
        result.sunkShips = new ArrayList<>();
        for (Ship s : sunk) {
            NetMessage.SunkInfo si = new NetMessage.SunkInfo();
            si.shipType = s.getType().name();
            si.cells = new ArrayList<>();
            for (Coordinate c : s.getOccupiedCells()) si.cells.add(new int[]{c.getRow(), c.getCol()});
            result.sunkShips.add(si);
        }
        result.defenderLost = lost;
        netSession.getSession().send(result);

        refreshFleetStatus();

        if (lost) {
            goToGameOver(false);
            return;
        }

        boolean anyHit = results.stream().anyMatch(ShotResult::isHit);
        boolean anySunk = !sunk.isEmpty();
        if (anySunk) {
            SoundManager.getInstance().playSunk();
        } else if (anyHit) {
            SoundManager.getInstance().playHit();
        } else {
            SoundManager.getInstance().playMiss();
        }
        logLabel.setText(anyHit ? "Incoming fire — you took damage!" : "Incoming fire — they missed.");
        netSession.setMyTurn(true);
        SoundManager.getInstance().playTurnStart();
        turnLabel.setText("YOUR TURN");
        enemyGrid.setDisable(false);
    }

    /** I am the attacker: apply the result the defender reported for my shot. */
    private void handleFireResult(NetMessage msg) {
        StringBuilder log = new StringBuilder();
        boolean anyHit = false;
        for (NetMessage.CellResult cr : msg.results) {
            Coordinate c = new Coordinate(cr.row, cr.col);
            CellStatus status = CellStatus.valueOf(cr.outcome);
            if (status == CellStatus.HIT) {
                netSession.getEnemyTracker().recordHit(c);
                enemyGrid.renderShot(c, CellStatus.HIT);
                anyHit = true;
            } else if (status == CellStatus.MISS) {
                netSession.getEnemyTracker().recordMiss(c);
                enemyGrid.renderShot(c, CellStatus.MISS);
            } else if (status == CellStatus.SUNK) {
                anyHit = true; // cell rendering handled via sunkShips below
            }
        }
        if (msg.sunkShips != null) {
            for (NetMessage.SunkInfo si : msg.sunkShips) {
                ShipType type = ShipType.valueOf(si.shipType);
                List<Coordinate> cells = new ArrayList<>();
                for (int[] rc : si.cells) cells.add(new Coordinate(rc[0], rc[1]));
                Ship ship = netSession.getEnemyTracker().recordSunk(type, cells);
                enemyGrid.renderSunkShip(ship);
                log.append(type.name().replace('_', ' ')).append(" has been sent to the bottom! ");
            }
        }
        if (log.isEmpty()) {
            log.append(anyHit ? "Direct hit!" : "Nothing but spray — miss.");
        }
        if (anyHit && msg.sunkShips == null) {
            SoundManager.getInstance().playHit();
        } else if (msg.sunkShips != null && !msg.sunkShips.isEmpty()) {
            SoundManager.getInstance().playSunk();
        } else {
            SoundManager.getInstance().playMiss();
        }
        logLabel.setText(log.toString().trim());
        refreshFleetStatus();

        if (msg.defenderLost) {
            goToGameOver(true);
            return;
        }
        netSession.setMyTurn(false);
        turnLabel.setText("OPPONENT'S TURN");
        enemyGrid.setDisable(true);
    }

    private void goToGameOver(boolean won) {
        app.setScreen(new NetworkGameOverView(app, netSession, won).build());
    }

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText(null);
        alert.setContentText("Leave this match and return to the main menu? This will disconnect your opponent.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            netSession.getSession().close();
            SoundManager.getInstance().stopBgm();
            SoundManager.getInstance().playMenuMusic();
            app.showMainMenu();
        }
    }

    // ---------- Launcher weapon bar ----------

    private void refreshLauncherBar() {
        launcherBar.getChildren().clear();
        for (LauncherType type : LauncherType.values()) {
            launcherBar.getChildren().add(buildLauncherButton(type));
        }
    }

    private Button buildLauncherButton(LauncherType type) {
        int size = controller.getSelectedTheater().getBoardSize();
        boolean available = type.isAvailableFor(size);
        int ammo = controller.getAmmoRemaining(me, type);
        boolean hasAmmo = type == LauncherType.DEFAULT || ammo > 0;
        boolean enabled = available && hasAmmo && netSession.isMyTurn();

        String ammoText = type == LauncherType.DEFAULT ? "\u221E" : String.valueOf(ammo);
        Button b = new Button(type.getLabel() + "  (" + ammoText + ")");
        b.getStyleClass().add("weapon-button");
        Image icon = ImageResources.launcherIcon(type);
        if (icon != null) {
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(20);
            iv.setFitHeight(20);
            iv.setPreserveRatio(true);
            b.setGraphic(iv);
        }
        boolean selected = me.getSelectedLauncher() == type;

        String base = "-fx-background-radius:8; -fx-border-radius:8; -fx-border-width:1.5; -fx-font-size:12px; ";
        if (!enabled) {
            b.setStyle(base + "-fx-background-color:#122032; -fx-text-fill:#5c7c97; -fx-border-color:#233246;");
            b.setDisable(true);
        } else if (selected) {
            b.setStyle(base + "-fx-background-color:#ffd166; -fx-text-fill:#081a2d; -fx-border-color:#ffd166;");
        } else {
            b.setStyle(base + "-fx-background-color:#2e5d87; -fx-text-fill:#f5f7fa; -fx-border-color:#44b8ff;");
        }

        b.setOnAction(e -> {
            controller.setSelectedLauncher(me, type);
            refreshLauncherBar();
        });
        return b;
    }

    private void toggleOrientation() {
        controller.toggleLauncherOrientation(me);
        updateOrientationLabel();
    }

    private void updateOrientationLabel() {
        boolean horizontal = me.isLauncherHorizontal();
        orientationLabel.setText("Orientation: " + (horizontal ? "HORIZONTAL" : "VERTICAL") +
                "  (R or Right-Click to rotate — affects Level 2 / Nuclear)");
    }

    // ---------- Board rendering ----------

    private void attachFireHandlers() {
        int size = enemyGrid.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                final int row = r, col = c;
                StackPane cell = enemyGrid.getCell(r, c);
                cell.setOnMouseClicked(e -> handleFireClick(new Coordinate(row, col)));
                cell.setOnMouseEntered(e -> showGhost(row, col));
                cell.setOnMouseExited(e -> clearGhost());
            }
        }
    }

    private void showGhost(int row, int col) {
        if (!netSession.isMyTurn()) return;
        clearGhost();
        LauncherType type = me.getSelectedLauncher();
        List<Coordinate> cells = type.getTargetCells(new Coordinate(row, col), me.isLauncherHorizontal());
        int size = enemyGrid.getSize();
        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            enemyGrid.getCell(c).setStyle(BoardGridPane.BASE_STYLE + "-fx-background-color: rgba(232,213,163,0.35);");
            ghostCells.add(new int[]{c.getRow(), c.getCol()});
        }
    }

    private void clearGhost() {
        for (int[] rc : ghostCells) {
            Coordinate c = new Coordinate(rc[0], rc[1]);
            CellStatus status = netSession.getEnemyTracker().getStatus(c);
            if (status == CellStatus.HIT || status == CellStatus.MISS) {
                enemyGrid.renderShot(c, status);
            } else {
                enemyGrid.resetCellStyle(rc[0], rc[1]);
            }
        }
        ghostCells.clear();
        for (Ship s : netSession.getEnemyTracker().getKnownSunkShips()) {
            enemyGrid.renderSunkShip(s);
        }
    }

    // ---------- Firing ----------

    private void handleFireClick(Coordinate anchor) {
        if (!netSession.isMyTurn()) return;

        LauncherType type = me.getSelectedLauncher();
        boolean horizontal = me.isLauncherHorizontal();
        List<Coordinate> pattern = type.getTargetCells(anchor, horizontal);
        int size = netSession.getEnemyTracker().getSize();

        boolean anyLiveCell = pattern.stream().anyMatch(c ->
                c.isWithinBounds(size) &&
                netSession.getEnemyTracker().getStatus(c) != CellStatus.HIT &&
                netSession.getEnemyTracker().getStatus(c) != CellStatus.MISS &&
                netSession.getEnemyTracker().getStatus(c) != CellStatus.SUNK);

        if (!anyLiveCell) {
            logLabel.setText("That area is already fully shelled, Admiral.");
            return;
        }

        if (type == LauncherType.NUCLEAR) {
            boolean authorized = NuclearLaunchDialog.askAndAwaitAuthorization(enemyGrid.getScene().getWindow());
            if (!authorized) {
                controller.setSelectedLauncher(me, LauncherType.DEFAULT);
                logLabel.setText("Launch codes rejected. Nuclear strike aborted — Default weapon re-armed.");
                refreshLauncherBar();
                return;
            }
        }

        clearGhost();
        if (type == LauncherType.NUCLEAR) {
            SoundManager.getInstance().playNuclear();
        } else {
            SoundManager.getInstance().playFire();
        }
        if (type == LauncherType.LEVEL_2) me.getAmmo().consume(LauncherType.LEVEL_2);
        if (type == LauncherType.NUCLEAR) {
            me.getAmmo().consume(LauncherType.NUCLEAR);
            if (!me.getAmmo().hasAmmo(LauncherType.NUCLEAR)) {
                NuclearResupplyDialog.show(app.getStage(), () -> {
                    me.getAmmo().resupply(LauncherType.NUCLEAR, 1);
                    refreshLauncherBar();
                });
            }
        }
        me.setSelectedLauncher(LauncherType.DEFAULT);

        NetMessage fire = NetMessage.of("FIRE");
        fire.launcherType = type.name();
        fire.anchorRow = anchor.getRow();
        fire.anchorCol = anchor.getCol();
        fire.horizontal = horizontal;
        netSession.getSession().send(fire);

        netSession.setMyTurn(false);
        turnLabel.setText("AWAITING RESPONSE\u2026");
        enemyGrid.setDisable(true);
        refreshLauncherBar();
    }

    private void refreshFleetStatus() {
        int myTotal = me.getOwnBoard().getShips().size();
        long myLost = me.getOwnBoard().getShips().stream().filter(Ship::isSunk).count();
        int enemySunkKnown = netSession.getEnemyTracker().getKnownSunkShips().size();
        int enemyTotal = controller.getSelectedTheater().getTotalShipCount();
        fleetStatusLabel.setText("Your ships lost: " + myLost + " / " + myTotal +
                "     Enemy ships confirmed sunk: " + enemySunkKnown + " / " + enemyTotal);
        fleetStatusLabel.setStyle("-fx-text-fill:#f5f7fa; -fx-font-size:12px;");
    }
}
