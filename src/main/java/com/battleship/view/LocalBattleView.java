package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.controller.LauncherFireResult;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.GameMode;
import com.battleship.model.GameState;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.view.quiz.NuclearResupplyDialog;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Battle screen for local play (vs AI or hotseat), rendered from the human
 * player's perspective. Extends {@link AbstractBattleView}, so all shared
 * battle-screen machinery (weapon bar, ghost preview, fire pipeline, exit)
 * is inherited — this class only contributes the local command bar, the
 * fleet-status side console, and the local/AI shot resolution.
 */
public class LocalBattleView extends AbstractBattleView {

    private Label turnLabel;
    private Label orientationLabel;
    private Label ownShipsLeftLabel;
    private Label enemyShipsLeftLabel;
    private VBox shipStatusBar;
    private VBox attackLogList;

    public LocalBattleView(MainApp app, GameController controller) {
        super(app, controller);
    }

    // ---------- Perspective helpers ----------

    private boolean vsAi() { return controller.getSelectedMode() != GameMode.HOTSEAT; }

    private Player viewPerspective() {
        return vsAi() ? controller.getPlayer1() : controller.getCurrentPlayer();
    }

    private Player viewOpponent() {
        return vsAi() ? controller.getPlayer2() : controller.getOpponent();
    }

    // ---------- AbstractBattleView hooks ----------

    @Override
    protected Player firingPlayer() { return controller.getCurrentPlayer(); }

    @Override
    protected boolean canFireNow() { return !controller.isAiTurn(); }

    @Override
    protected int targetBoardSize() {
        return controller.getOpponent().getOwnBoard().getSize();
    }

    @Override
    protected boolean isCellAlreadyResolved(Coordinate c) {
        CellStatus s = controller.getOpponent().getOwnBoard().getCellStatus(c);
        return s == CellStatus.HIT || s == CellStatus.MISS || s == CellStatus.SUNK;
    }

    @Override
    protected String ghostStyle() {
        return "-fx-background-color: rgba(255,209,102,0.35);";
    }

    @Override
    protected void repaintGhostCell(int row, int col) {
        Coordinate c = new Coordinate(row, col);
        CellStatus status = controller.getOpponent().getOwnBoard().getCellStatus(c);
        if (status == CellStatus.HIT || status == CellStatus.MISS) {
            enemyGrid.renderShot(c, status);
        } else {
            enemyGrid.resetCellStyle(row, col);
        }
    }

    @Override
    protected void selectLauncher(LauncherType type) {
        controller.selectLauncher(firingPlayer(), type);
    }

    @Override
    protected void reportBlockedShot() {
        addLogEntry("That area is already fully shelled, Admiral.", "info");
    }

    @Override
    protected void onNuclearRejected() {
        controller.selectLauncher(controller.getCurrentPlayer(), LauncherType.DEFAULT);
        addLogEntry("Launch codes rejected. Nuclear strike aborted \u2014 Default weapon re-armed.", "info");
        refreshLauncherBar();
    }

    @Override
    protected String launcherButtonStyle(LauncherButtonState state) {
        return switch (state) {
            case DISABLED -> "-fx-background-color:#122032; -fx-text-fill:#4a5c70; -fx-border-color:#233246;";
            case SELECTED -> "-fx-background-color:#ffd166; -fx-text-fill:#081a2d; -fx-border-color:#ffd166;";
            case ENABLED  -> "-fx-background-color:#123a58; -fx-text-fill:#dff1ff; -fx-border-color:#2e5d87;";
        };
    }

    @Override
    protected String exitPrompt() {
        return "Leave this battle and return to the main menu? Progress will be lost.";
    }

    @Override
    protected void onOrientationChanged() {
        updateOrientationLabel();
    }

    @Override
    protected BoardGridPane createOwnGrid() {
        Player current = viewPerspective();
        BoardGridPane grid = new BoardGridPane(current.getOwnBoard().getSize());
        renderExistingShots(grid, current.getOwnBoard());
        for (Ship s : current.getOwnBoard().getShips()) {
            if (!s.isSunk()) grid.renderShip(s);
        }
        return grid;
    }

    @Override
    protected BoardGridPane createEnemyGrid() {
        return new BoardGridPane(viewOpponent().getOwnBoard().getSize());
    }

    @Override
    protected Pane assembleLayout() {
        Player current = viewPerspective();
        Player opponent = viewOpponent();

        orientationLabel = new Label();
        orientationLabel.getStyleClass().add("dim-text");
        updateOrientationLabel();

        VBox weaponsCard = new VBox(8, launcherBar, orientationLabel);
        weaponsCard.setAlignment(Pos.CENTER);
        weaponsCard.getStyleClass().add("side-card");

        ownShipsLeftLabel = new Label();
        ownShipsLeftLabel.getStyleClass().add("ships-left-badge");
        enemyShipsLeftLabel = new Label();
        enemyShipsLeftLabel.getStyleClass().add("ships-left-badge");

        VBox ownBox = buildBoardCard("YOUR FLEET", ownGrid, ownShipsLeftLabel);
        VBox enemyBox = buildBoardCard("ENEMY WATERS", enemyGrid, enemyShipsLeftLabel);
        refreshShipsLeftLabels();

        HBox boardsRow = new HBox(24, ownBox, enemyBox);
        boardsRow.setAlignment(Pos.TOP_CENTER);

        VBox leftColumn = new VBox(16, weaponsCard, boardsRow);
        leftColumn.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        VBox sidePanel = buildSidePanel();
        refreshShipStatusBar();
        addLogEntry("Select a weapon, then a target on the enemy grid.", "info");

        HBox content = new HBox(24, leftColumn, sidePanel);
        content.setAlignment(Pos.TOP_CENTER);

        HBox commandBar = buildCommandBar(current, opponent);
        VBox layout = new VBox(16, commandBar, content);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(20));
        return layout;
    }

    @Override
    protected StackPane decorateRoot(Pane layout) {
        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root, 0.0);
        root.getChildren().add(ocean);
        root.getChildren().add(layout);
        return root;
    }

    @Override
    protected void onViewShown() {
        if (controller.isAiTurn()) {
            updateTurnBadge("ENEMY TURN", "turn-badge-enemy");
            enemyGrid.setDisable(true);
            runAiTurnAfterDelay();
        } else {
            updateTurnBadge(viewPerspective().getName() + "'S TURN", "turn-badge-player");
        }
    }

    // ---------- Firing (local + AI) ----------

    @Override
    protected void resolveShot(Coordinate anchor) {
        Player attacker = controller.getCurrentPlayer();
        boolean hadNuclearAmmo = attacker.getAmmo().hasAmmo(LauncherType.NUCLEAR);
        LauncherFireResult result = controller.fireLauncher(anchor);
        applyResult(enemyGrid, result);
        refreshLauncherBar();

        if (controller.getState() == GameState.GAME_OVER) {
            SoundManager.getInstance().stopBgm();
            app.showGameOver(attacker);
            return;
        }

        // Only start the resupply countdown if the battle is still running.
        maybeTriggerNuclearResupply(attacker, hadNuclearAmmo);

        if (controller.getSelectedMode() == GameMode.HOTSEAT) {
            SoundManager.getInstance().stopBgm();
            app.showPassScreen(() -> app.showBattle());
        } else if (controller.isAiTurn()) {
            updateTurnBadge("ENEMY TURN", "turn-badge-enemy");
            enemyGrid.setDisable(true);
            runAiTurnAfterDelay();
        }
    }

    private void runAiTurnAfterDelay() {
        updateTurnBadge("COMPUTING TRAJECTORY...", "turn-badge-thinking");
        PauseTransition pause = new PauseTransition(Duration.millis(900));
        pause.setOnFinished(e -> {
            SoundManager.getInstance().playFire();
            Player attacker = controller.getCurrentPlayer();
            boolean hadNuclearAmmo = attacker.getAmmo().hasAmmo(LauncherType.NUCLEAR);
            LauncherFireResult result = controller.fireAiLauncher();
            applyResult(ownGrid, result);

            if (controller.getState() == GameState.GAME_OVER) {
                SoundManager.getInstance().stopBgm();
                app.showGameOver(attacker);
                return;
            }

            maybeTriggerNuclearResupply(attacker, hadNuclearAmmo);

            SoundManager.getInstance().playTurnStart();
            updateTurnBadge(controller.getCurrentPlayer().getName() + "'S TURN", "turn-badge-player");
            refreshLauncherBar();
            enemyGrid.setDisable(false);
        });
        pause.play();
    }

    private void applyResult(BoardGridPane grid, LauncherFireResult result) {
        for (com.battleship.model.ShotResult r : result.results()) {
            if (r.outcome() != CellStatus.SUNK) {
                grid.renderShot(r.coordinate(), r.outcome());
            }
        }
        boolean anySunk = !result.sunkShips().isEmpty();
        for (Ship sunkShip : result.sunkShips()) {
            grid.renderSunkShip(sunkShip);
            addLogEntry(sunkShip.getType().name().replace('_', ' ') + " SUNK!", "sunk");
        }
        if (anySunk) {
            SoundManager.getInstance().playSunk();
        } else {
            boolean anyHit = result.anyHit();
            if (anyHit) {
                SoundManager.getInstance().playHit();
                addLogEntry("Direct hit!", "hit");
            } else {
                SoundManager.getInstance().playMiss();
                addLogEntry("Nothing but spray — miss.", "miss");
            }
        }
        refreshShipStatusBar();
        refreshShipsLeftLabels();
    }

    // ---------- Local chrome: command bar / side console ----------

    private HBox buildCommandBar(Player current, Player opponent) {
        turnLabel = new Label();
        turnLabel.getStyleClass().addAll("turn-badge", "turn-badge-player");

        HBox leftWrap = new HBox(turnLabel);
        leftWrap.setAlignment(Pos.CENTER_LEFT);
        leftWrap.setPrefWidth(180);

        Label vs = new Label("VS");
        vs.getStyleClass().add("vs-divider");

        HBox chips = new HBox(20,
                chipText(current.getName(), "COMMANDER"),
                vs,
                chipText(opponent.getName(),
                        controller.getSelectedMode() != GameMode.HOTSEAT ? "HOSTILE FLEET" : "COMMANDER"));
        chips.setAlignment(Pos.CENTER);

        HBox centerWrap = new HBox(chips);
        centerWrap.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerWrap, Priority.ALWAYS);

        HBox rightWrap = new HBox(buildExitButton());
        rightWrap.setAlignment(Pos.CENTER_RIGHT);
        rightWrap.setPrefWidth(180);

        HBox bar = new HBox(leftWrap, centerWrap, rightWrap);
        bar.getStyleClass().add("battle-command-bar");
        bar.setAlignment(Pos.CENTER);
        return bar;
    }

    private VBox chipText(String name, String role) {
        Label n = new Label(name.toUpperCase());
        n.getStyleClass().add("fleet-chip-name");
        Label r = new Label(role);
        r.getStyleClass().add("fleet-chip-role");
        VBox box = new VBox(2, n, r);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void updateTurnBadge(String text, String styleClass) {
        turnLabel.setText(text);
        turnLabel.getStyleClass().removeAll("turn-badge-player", "turn-badge-enemy", "turn-badge-thinking");
        turnLabel.getStyleClass().add(styleClass);
    }

    private VBox buildBoardCard(String title, BoardGridPane grid, Label shipsLeftLabel) {
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("board-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(titleLbl, spacer, shipsLeftLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(14, titleRow, grid);
        card.getStyleClass().add("board-card");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private void refreshShipsLeftLabels() {
        Player current = controller.getCurrentPlayer();
        Player opponent = controller.getOpponent();

        long ownLeft = current.getOwnBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        long ownTotal = current.getOwnBoard().getShips().size();
        ownShipsLeftLabel.setText(ownLeft + "/" + ownTotal + " AFLOAT");

        long enemyLeft = opponent.getOwnBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        long enemyTotal = opponent.getOwnBoard().getShips().size();
        enemyShipsLeftLabel.setText(enemyLeft + "/" + enemyTotal + " REMAINING");
    }

    // ---------- Side console (fleet status + attack log) ----------

    private VBox buildSidePanel() {
        StackPane radar = DecorUtil.animatedRadarSweep(150);
        VBox radarCard = new VBox(radar);
        radarCard.setAlignment(Pos.CENTER);
        radarCard.getStyleClass().add("side-card");

        Label fleetTitle = new Label("ENEMY FLEET STATUS");
        fleetTitle.getStyleClass().add("side-card-title");
        shipStatusBar = new VBox(6);
        VBox fleetCard = new VBox(12, fleetTitle, shipStatusBar);
        fleetCard.getStyleClass().add("side-card");

        Label logTitle = new Label("ATTACK LOG");
        logTitle.getStyleClass().add("side-card-title");
        attackLogList = new VBox(6);
        attackLogList.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(attackLogList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(240);
        scroll.setPannable(true);
        scroll.getStyleClass().add("attack-log-scroll");

        VBox logCard = new VBox(12, logTitle, scroll);
        logCard.getStyleClass().add("side-card");
        VBox.setVgrow(logCard, Priority.ALWAYS);

        VBox side = new VBox(16, radarCard, fleetCard, logCard);
        side.setPrefWidth(260);
        side.setMinWidth(260);
        side.setMaxWidth(260);
        return side;
    }

    private void addLogEntry(String text, String type) {
        if (attackLogList == null) return;
        Label entry = new Label(text);
        entry.setWrapText(true);
        entry.setMaxWidth(220);
        entry.getStyleClass().addAll("log-entry", "log-entry-" + type);
        attackLogList.getChildren().add(0, entry);
        while (attackLogList.getChildren().size() > 50) {
            attackLogList.getChildren().remove(attackLogList.getChildren().size() - 1);
        }
    }

    private void refreshShipStatusBar() {
        shipStatusBar.getChildren().clear();
        Player opponent = controller.getOpponent();
        for (Ship s : opponent.getOwnBoard().getShips()) {
            shipStatusBar.getChildren().add(buildFleetStatusRow(s));
        }
    }

    private HBox buildFleetStatusRow(Ship s) {
        boolean sunk = s.isSunk();
        int size = s.getType().getSize();
        int hits = Math.min(s.getHits(), size);

        Label name = new Label(s.getType().name().replace('_', ' '));
        name.getStyleClass().add(sunk ? "fleet-status-name-sunk" : "fleet-status-name");
        name.setPrefWidth(92);

        double trackWidth = 70;
        Region track = new Region();
        track.getStyleClass().add("fleet-bar-track");
        track.setPrefSize(trackWidth, 5);
        track.setMaxSize(trackWidth, 5);

        double fillWidth = size == 0 ? 0 : trackWidth * ((double) hits / size);
        Region fill = new Region();
        fill.getStyleClass().add(sunk ? "fleet-bar-fill-sunk" : "fleet-bar-fill");
        fill.setPrefSize(fillWidth, 5);
        fill.setMaxSize(fillWidth, 5);
        fill.setMinSize(fillWidth, 5);

        StackPane barStack = new StackPane(track, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label countLabel = new Label(hits + "/" + size);
        countLabel.getStyleClass().add("dim-text");
        countLabel.setPrefWidth(34);

        HBox row = new HBox(8, name, barStack, countLabel);
        row.getStyleClass().add("fleet-status-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void renderExistingShots(BoardGridPane grid, com.battleship.model.Board board) {
        for (int r = 0; r < board.getSize(); r++) {
            for (int c = 0; c < board.getSize(); c++) {
                Coordinate coord = new Coordinate(r, c);
                CellStatus status = board.getCellStatus(coord);
                if (status == CellStatus.HIT || status == CellStatus.MISS) {
                    grid.renderShot(coord, status);
                }
            }
        }
        for (Ship s : board.getShips()) {
            if (s.isSunk()) grid.renderSunkShip(s);
        }
    }

    private void updateOrientationLabel() {
        Orientation orientation = firingPlayer().getLauncherOrientation();
        orientationLabel.setText("Orientation: " + (orientation.isHorizontal() ? "HORIZONTAL" : "VERTICAL")
                + "  (R or Right-Click to rotate — affects Level 2 / Nuclear)");
    }

    /**
     * If this shot just consumed the player's last Nuclear round, start the 30s
     * auto-resupply countdown (quiz-gated) and top the stock back up on success.
     */
    private void maybeTriggerNuclearResupply(Player player, boolean hadNuclearAmmoBefore) {
        if (hadNuclearAmmoBefore && !player.getAmmo().hasAmmo(LauncherType.NUCLEAR)) {
            NuclearResupplyDialog.show(app.getStage(), () -> {
                player.getAmmo().resupply(LauncherType.NUCLEAR, 1);
                refreshLauncherBar();
            });
        }
    }
}