package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.model.ShotResult;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import com.battleship.view.quiz.NuclearResupplyDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Battle screen for a network ("Play With a Friend") match. Extends
 * {@link AbstractBattleView} (shared weapon bar, ghost preview, fire pipeline)
 * and only contributes the network shot resolution: firing sends a FIRE
 * message and the defender resolves it locally and replies with FIRE_RESULT.
 * No ship layout is ever transmitted.
 */
public class NetworkBattleView extends AbstractBattleView {

    private final NetworkGameSession netSession;
    private final Player me;

    private Label turnLabel;
    private Label logLabel;
    private Label orientationLabel;
    private Label fleetStatusLabel;

    public NetworkBattleView(MainApp app, GameController controller, NetworkGameSession netSession) {
        super(app, controller);
        this.netSession = netSession;
        this.me = netSession.getMe();
    }

    // ---------- AbstractBattleView hooks ----------

    @Override
    protected Player firingPlayer() { return me; }

    @Override
    protected boolean canFireNow() { return netSession.isMyTurn(); }

    @Override
    protected boolean extraWeaponGate() { return netSession.isMyTurn(); }

    @Override
    protected int targetBoardSize() { return netSession.getEnemyTracker().getSize(); }

    @Override
    protected boolean isCellAlreadyResolved(Coordinate c) {
        CellStatus s = netSession.getEnemyTracker().getStatus(c);
        return s == CellStatus.HIT || s == CellStatus.MISS || s == CellStatus.SUNK;
    }

    @Override
    protected String ghostStyle() {
        return "-fx-background-color: rgba(232,213,163,0.35);";
    }

    @Override
    protected void repaintGhostCell(int row, int col) {
        Coordinate c = new Coordinate(row, col);
        CellStatus status = netSession.getEnemyTracker().getStatus(c);
        if (status == CellStatus.HIT || status == CellStatus.MISS) {
            enemyGrid.renderShot(c, status);
        } else {
            enemyGrid.resetCellStyle(row, col);
        }
    }

    @Override
    protected void selectLauncher(LauncherType type) {
        me.selectLauncher(type, controller.getSelectedTheater().getBoardSize());
    }

    @Override
    protected void reportBlockedShot() {
        logLabel.setText("That area is already fully shelled, Admiral.");
    }

    @Override
    protected void onNuclearRejected() {
        me.selectLauncher(LauncherType.DEFAULT, controller.getSelectedTheater().getBoardSize());
        logLabel.setText("Launch codes rejected. Nuclear strike aborted \u2014 Default weapon re-armed.");
        refreshLauncherBar();
    }

    @Override
    protected String launcherButtonStyle(LauncherButtonState state) {
        return switch (state) {
            case DISABLED -> "-fx-background-color:#122032; -fx-text-fill:#5c7c97; -fx-border-color:#233246;";
            case SELECTED -> "-fx-background-color:#ffd166; -fx-text-fill:#081a2d; -fx-border-color:#ffd166;";
            case ENABLED  -> "-fx-background-color:#2e5d87; -fx-text-fill:#f5f7fa; -fx-border-color:#44b8ff;";
        };
    }

    @Override
    protected String exitPrompt() {
        return "Leave this match and return to the main menu? This will disconnect your opponent.";
    }

    @Override
    protected void onExitConfirmed() {
        netSession.getSession().close();
    }


    @Override
    protected BoardGridPane createOwnGrid() {
        BoardGridPane grid = new BoardGridPane(me.getOwnBoard().getSize());
        for (Ship s : me.getOwnBoard().getShips()) {
            if (!s.isSunk()) grid.renderShip(s);
        }
        return grid;
    }

    @Override
    protected BoardGridPane createEnemyGrid() {
        return new BoardGridPane(netSession.getEnemyTracker().getSize());
    }

    @Override
    protected Pane assembleLayout() {
        turnLabel = new Label(netSession.isMyTurn() ? "YOUR TURN" : "OPPONENT'S TURN");
        turnLabel.getStyleClass().add("app-title");
        turnLabel.setStyle("-fx-font-size:24px;");

        logLabel = new Label("Select a weapon, then a target on the enemy grid.");
        logLabel.getStyleClass().add("info-text");
        logLabel.setStyle("-fx-font-size:13px;");

        orientationLabel = new Label();
        orientationLabel.getStyleClass().add("dim-text");
        updateOrientationLabel();

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

        HBox topBar = new HBox(turnLabel);
        topBar.setAlignment(Pos.CENTER);
        javafx.scene.control.Button exit = buildExitButton();
        StackPane titleRow = new StackPane(topBar, exit);
        StackPane.setAlignment(exit, Pos.CENTER_RIGHT);

        VBox layout = new VBox(12, titleRow, launcherBar, orientationLabel, boards, logLabel, fleetStatusLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));
        return layout;
    }

    @Override
    protected StackPane decorateRoot(Pane layout) {
        return new StackPane(layout); // network screen has no ocean backdrop
    }

    @Override
    protected void onViewShown() {
        netSession.getSession().setOnMessage(this::handleMessage);
        netSession.getSession().setOnDisconnected(this::handleDisconnect);
        enemyGrid.setDisable(!netSession.isMyTurn());
    }

    // ---------- Networking ----------

    private void handleMessage(NetMessage msg) {
        if (msg == null) return;
        switch (msg) {
            case NetMessage.Fire fire -> handleIncomingFire(fire);
            case NetMessage.FireResult result -> handleFireResult(result);
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
    private void handleIncomingFire(NetMessage.Fire fire) {
        List<Coordinate> cells = fire.launcherType().getTargetCells(fire.anchor(), fire.orientation());
        var myBoard = me.getOwnBoard();

        List<ShotResult> results = new ArrayList<>();
        var sunk = new java.util.LinkedHashSet<Ship>();
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

        List<NetMessage.CellResult> cellResults = new ArrayList<>();
        for (ShotResult r : results) {
            cellResults.add(new NetMessage.CellResult(r.coordinate(), r.outcome()));
        }
        List<NetMessage.SunkShipInfo> sunkInfos = new ArrayList<>();
        for (Ship s : sunk) {
            sunkInfos.add(new NetMessage.SunkShipInfo(s.getType(), s.getOccupiedCells()));
        }
        netSession.getSession().send(new NetMessage.FireResult(cellResults, sunkInfos, lost));

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
        netSession.beginMyTurn();
        SoundManager.getInstance().playTurnStart();
        turnLabel.setText("YOUR TURN");
        enemyGrid.setDisable(false);
    }

    /** I am the attacker: apply the result the defender reported for my shot. */
    private void handleFireResult(NetMessage.FireResult result) {
        StringBuilder log = new StringBuilder();
        boolean anyHit = false;
        for (NetMessage.CellResult cr : result.results()) {
            Coordinate c = cr.coordinate();
            CellStatus status = cr.outcome();
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
        if (result.sunkShips() != null) {
            for (NetMessage.SunkShipInfo si : result.sunkShips()) {
                Ship ship = netSession.getEnemyTracker().recordSunk(si.shipType(), si.cells());
                enemyGrid.renderSunkShip(ship);
                log.append(si.shipType().name().replace('_', ' ')).append(" has been sent to the bottom! ");
            }
        }
        if (log.isEmpty()) {
            log.append(anyHit ? "Direct hit!" : "Nothing but spray — miss.");
        }
        if (anyHit && (result.sunkShips() == null || result.sunkShips().isEmpty())) {
            SoundManager.getInstance().playHit();
        } else if (result.sunkShips() != null && !result.sunkShips().isEmpty()) {
            SoundManager.getInstance().playSunk();
        } else {
            SoundManager.getInstance().playMiss();
        }
        logLabel.setText(log.toString().trim());
        refreshFleetStatus();

        if (result.defenderLost()) {
            goToGameOver(true);
            return;
        }
        netSession.beginOpponentTurn();
        turnLabel.setText("OPPONENT'S TURN");
        enemyGrid.setDisable(true);
    }

    private void goToGameOver(boolean won) {
        app.setScreen(new NetworkGameOverView(app, netSession, won).build());
    }

    // ---------- Shot resolution (network) ----------

    @Override
    protected void resolveShot(Coordinate anchor) {
        LauncherType type = me.getSelectedLauncher();

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
        me.resetLauncherAfterShot();

        netSession.getSession().send(new NetMessage.Fire(type, anchor, firingOrientation()));

        netSession.beginOpponentTurn();
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

    private void updateOrientationLabel() {
        com.battleship.model.Orientation orientation = me.getLauncherOrientation();
        orientationLabel.setText("Orientation: " + (orientation.isHorizontal() ? "HORIZONTAL" : "VERTICAL") +
                "  (R or Right-Click to rotate — affects Level 2 / Nuclear)");
    }

    @Override
    protected void onOrientationChanged() {
        updateOrientationLabel();
    }
}