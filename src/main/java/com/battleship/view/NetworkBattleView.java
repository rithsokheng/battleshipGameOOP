package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.controller.LauncherFireResult;
import com.battleship.controller.ShotResolver;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.model.ShotResult;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkBattleMediator;
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
    private final NetworkBattleMediator mediator;

    private Label turnLabel;
    private Label logLabel;
    private Label orientationLabel;
    private Label fleetStatusLabel;

    public NetworkBattleView(ViewNavigator nav, GameController controller, NetworkGameSession netSession) {
        super(nav, controller);
        this.netSession = netSession;
        this.me = netSession.getMe();
        this.mediator = new NetworkBattleMediator(netSession);
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
    protected String ghostStyleClass() {
        return BoardGridPane.GHOST_TARGET;
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
        turnLabel.getStyleClass().addAll("app-title", "screen-title-md");

        logLabel = new Label("Select a weapon, then a target on the enemy grid.");
        logLabel.getStyleClass().addAll("info-text", "battle-log");

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
        fleetStatusLabel.getStyleClass().add("fleet-status-label");
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
            case NetMessage.Fire f        -> handleIncomingFire(f);
            case NetMessage.FireResult fr -> handleFireResult(fr);
            default -> { /* lobby-phase messages ignored during battle */ }
        }
    }

    private void handleDisconnect() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Disconnected");
        alert.setHeaderText(null);
        alert.setContentText("Your opponent disconnected.");
        alert.showAndWait();
        nav.showMainMenu();
    }

    /** I am the defender: delegate incoming fire resolution and response to mediator, then render. */
    private void handleIncomingFire(NetMessage.Fire fire) {
        NetworkBattleMediator.IncomingFireOutcome outcome = mediator.resolveAndReply(fire);

        for (ShotResult r : outcome.resolution().results()) {
            if (r.outcome() != CellStatus.SUNK) ownGrid.renderShot(r.coordinate(), r.outcome());
        }
        for (Ship s : outcome.resolution().sunkShips()) ownGrid.renderSunkShip(s);

        refreshFleetStatus();

        if (outcome.lost()) {
            goToGameOver(false);
            return;
        }

        playResultAudio(outcome.anyHit(), outcome.anySunk());
        logLabel.setText(outcome.anyHit() ? "Incoming fire \u2014 you took damage!" : "Incoming fire \u2014 they missed.");
        netSession.beginMyTurn();
        audio.playTurnStart();
        turnLabel.setText("YOUR TURN");
        enemyGrid.setDisable(false);
    }

    /** I am the attacker: apply the result the defender reported for my shot. */
    private void handleFireResult(NetMessage.FireResult result) {
        boolean anyHit = applyCellResults(result.results());
        String sunkLog = applySunkShips(result.sunkShips());

        logLabel.setText(sunkLog.isEmpty()
                ? (anyHit ? "Direct hit!" : "Nothing but spray \u2014 miss.")
                : sunkLog.trim());
        playResultAudio(anyHit, !sunkLog.isEmpty());
        refreshFleetStatus();

        if (result.defenderLost()) {
            goToGameOver(true);
            return;
        }
        handTurnToOpponent();
    }

    /**
     * Records and renders every cell the defender reported.
     * @return {@code true} if any reported cell was a hit or part of a sunk ship
     */
    private boolean applyCellResults(List<NetMessage.CellResult> results) {
        boolean anyHit = false;
        for (NetMessage.CellResult cr : results) {
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
                anyHit = true; // cell rendering handled via the sunkShips list below
            }
        }
        return anyHit;
    }

    /**
     * Records and renders every ship reported sunk.
     * @return the attack-log fragment for the sunk ships, or an empty string if none
     */
    private String applySunkShips(List<NetMessage.SunkShipInfo> sunkShips) {
        if (sunkShips == null || sunkShips.isEmpty()) return "";
        StringBuilder log = new StringBuilder();
        for (NetMessage.SunkShipInfo si : sunkShips) {
            Ship ship = netSession.getEnemyTracker().recordSunk(si.shipType(), si.cells());
            enemyGrid.renderSunkShip(ship);
            log.append(si.shipType().name().replace('_', ' ')).append(" has been sent to the bottom! ");
        }
        return log.toString();
    }

    /** My shot is resolved — hand the turn back to the opponent. */
    private void handTurnToOpponent() {
        netSession.beginOpponentTurn();
        turnLabel.setText("OPPONENT'S TURN");
        enemyGrid.setDisable(true);
    }

    private void goToGameOver(boolean won) {
        nav.showNetworkGameOver(netSession, won);
    }

    // ---------- Shot resolution (network) ----------

    @Override
    protected void resolveShot(Coordinate anchor) {
        LauncherType type = me.getSelectedLauncher();

        // V9: the view no longer does ammo bookkeeping on the raw inventory —
        // it simply tells the domain object the shot happened; the Player owns
        // its ammo (consume is a no-op for the infinite DEFAULT launcher).
        me.consumeAmmo(type);
        me.resetLauncherAfterShot();

        if (type == LauncherType.NUCLEAR && !me.hasAmmo(LauncherType.NUCLEAR)) {
            NuclearResupplyDialog.show(nav.getStage(), () -> {
                me.resupplyAmmo(LauncherType.NUCLEAR, 1);
                refreshLauncherBar();
            });
        }

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
    }

    private void updateOrientationLabel() {
        orientationLabel.setText(orientationLabelText());
    }

    @Override
    protected void onOrientationChanged() {
        updateOrientationLabel();
    }
}