package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.controller.LauncherFireResult;
import com.battleship.controller.LauncherLogic;
import com.battleship.model.*;
import com.battleship.view.quiz.NuclearLaunchDialog;
import com.battleship.view.quiz.NuclearResupplyDialog;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Battle screen: own board (left, read-only) and enemy board (right, click to fire).
 * Restyled as a "Fleet Command" naval command center: a top command bar with a
 * turn badge and commander chips, glass-panel board cards with a live
 * ships-remaining readout, and a right-hand side console with a live enemy
 * fleet status list (per-ship damage bars) and a scrolling attack log.
 * Also keeps the 3-type launcher system: a weapon bar to pick Default/Level 2/
 * Nuclear (with live ammo counts), an orientation toggle for the area weapons,
 * a hover ghost preview of the shot pattern, and multi-cell LauncherFireResult
 * handling (a single shot can hit/sink more than one cell or ship at once).
 */
public class BattleView {

    private final MainApp app;
    private final GameController controller;

    private BoardGridPane ownGrid;
    private BoardGridPane enemyGrid;
    private Label turnLabel;
    private Label orientationLabel;
    private Label ownShipsLeftLabel;
    private Label enemyShipsLeftLabel;
    private VBox shipStatusBar;
    private VBox attackLogList;
    private HBox launcherBar;

    private final List<int[]> ghostCells = new ArrayList<>();

    public BattleView(MainApp app, GameController controller) {
        this.app = app;
        this.controller = controller;
    }

    public StackPane build() {
        // Vs AI: ALWAYS view from the human's (player1) perspective, even when
        // the AI won initiative — otherwise the boards render swapped and shots
        // land on the wrong cells. Hotseat keeps the rotating per-player view.
        boolean vsAi = controller.getSelectedMode() != GameMode.HOTSEAT;
        Player current = vsAi ? controller.getPlayer1() : controller.getCurrentPlayer();
        Player opponent = vsAi ? controller.getPlayer2() : controller.getOpponent();

        HBox commandBar = buildCommandBar(current, opponent);
        if (controller.isAiTurn()) {
            updateTurnBadge("ENEMY TURN", "turn-badge-enemy");
        } else {
            updateTurnBadge(current.getName() + "'S TURN", "turn-badge-player");
        }

        launcherBar = new HBox(10);
        launcherBar.setAlignment(Pos.CENTER);
        refreshLauncherBar();

        orientationLabel = new Label();
        orientationLabel.getStyleClass().add("dim-text");
        updateOrientationLabel();

        VBox weaponsCard = new VBox(8, launcherBar, orientationLabel);
        weaponsCard.setAlignment(Pos.CENTER);
        weaponsCard.getStyleClass().add("side-card");

        ownGrid = new BoardGridPane(current.getOwnBoard().getSize());
        renderExistingShots(ownGrid, current.getOwnBoard());
        for (Ship s : current.getOwnBoard().getShips()) {
            if (!s.isSunk()) ownGrid.renderShip(s);
        }

        enemyGrid = new BoardGridPane(opponent.getOwnBoard().getSize());
        renderExistingShots(enemyGrid, opponent.getOwnBoard());
        attachFireHandlers();

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

        VBox layout = new VBox(16, commandBar, content);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(20));

        StackPane root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root, 0.0);
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

        SoundManager.getInstance().playBattleMusic();

        if (controller.isAiTurn()) {
            enemyGrid.setDisable(true);
            runAiTurnAfterDelay();
        }
        return root;
    }

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText(null);
        alert.setContentText("Leave this battle and return to the main menu? Progress will be lost.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SoundManager.getInstance().stopBgm();
            SoundManager.getInstance().playMenuMusic();
            app.showMainMenu();
        }
    }

    // ---------- Command bar (top: turn badge / commander chips / exit) ----------

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

        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> { SoundManager.getInstance().playClick(); confirmExit(); });

        HBox rightWrap = new HBox(exit);
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

    // ---------- Board cards ----------

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

    // ---------- Launcher weapon bar ----------

    private void refreshLauncherBar() {
        launcherBar.getChildren().clear();
        Player current = controller.getCurrentPlayer();
        for (LauncherType type : LauncherType.values()) {
            launcherBar.getChildren().add(buildLauncherButton(type, current));
        }
    }

    private Button buildLauncherButton(LauncherType type, Player player) {
        int size = controller.getSelectedTheater().getBoardSize();
        boolean available = type.isAvailableFor(size);
        int ammo = controller.getAmmoRemaining(player, type);
        boolean hasAmmo = type == LauncherType.DEFAULT || ammo > 0;
        boolean enabled = available && hasAmmo;

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
        boolean selected = player.getSelectedLauncher() == type;

        String base = "-fx-background-radius:8; -fx-border-radius:8; -fx-border-width:1.5; -fx-font-size:12px; -fx-font-weight:bold; ";
        if (!enabled) {
            b.setStyle(base + "-fx-background-color:#122032; -fx-text-fill:#4a5c70; -fx-border-color:#233246;");
            b.setDisable(true);
        } else if (selected) {
            b.setStyle(base + "-fx-background-color:#ffd166; -fx-text-fill:#081a2d; -fx-border-color:#ffd166;");
        } else {
            b.setStyle(base + "-fx-background-color:#123a58; -fx-text-fill:#dff1ff; -fx-border-color:#2e5d87;");
        }

        b.setOnAction(e -> {
            controller.setSelectedLauncher(player, type);
            refreshLauncherBar();
        });
        return b;
    }

    private void toggleOrientation() {
        controller.toggleLauncherOrientation(controller.getCurrentPlayer());
        updateOrientationLabel();
    }

    private void updateOrientationLabel() {
        boolean horizontal = controller.getCurrentPlayer().isLauncherHorizontal();
        orientationLabel.setText("Orientation: " + (horizontal ? "HORIZONTAL" : "VERTICAL") +
                "  (R or Right-Click to rotate — affects Level 2 / Nuclear)");
    }

    // ---------- Board rendering ----------

    private void renderExistingShots(BoardGridPane grid, Board board) {
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

    private void attachFireHandlers() {
        int size = enemyGrid.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                final int row = r, col = c;
                StackPane cell = enemyGrid.getCell(r, c);
                cell.setOnMouseClicked(e -> handleFire(new Coordinate(row, col)));
                cell.setOnMouseEntered(e -> showGhost(row, col));
                cell.setOnMouseExited(e -> clearGhost());
            }
        }
    }

    private void showGhost(int row, int col) {
        clearGhost();
        Player player = controller.getCurrentPlayer();
        LauncherType type = player.getSelectedLauncher();
        List<Coordinate> cells = LauncherLogic.getTargetCells(type, new Coordinate(row, col), player.isLauncherHorizontal());
        int size = enemyGrid.getSize();
        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            enemyGrid.getCell(c).setStyle(BoardGridPane.BASE_STYLE + "-fx-background-color: rgba(255,209,102,0.35);");
            ghostCells.add(new int[]{c.getRow(), c.getCol()});
        }
    }

    private void clearGhost() {
        Board opponentBoard = controller.getOpponent().getOwnBoard();
        for (int[] rc : ghostCells) {
            Coordinate c = new Coordinate(rc[0], rc[1]);
            CellStatus status = opponentBoard.getCellStatus(c);
            if (status == CellStatus.HIT || status == CellStatus.MISS) {
                enemyGrid.renderShot(c, status);
            } else {
                enemyGrid.resetCellStyle(rc[0], rc[1]);
            }
        }
        ghostCells.clear();
        for (Ship s : opponentBoard.getShips()) {
            if (s.isSunk()) enemyGrid.renderSunkShip(s);
        }
    }

    // ---------- Firing ----------

    private void handleFire(Coordinate anchor) {
        if (controller.isAiTurn()) return;

        Player attacker = controller.getCurrentPlayer();
        Board opponentBoard = controller.getOpponent().getOwnBoard();
        List<Coordinate> pattern = LauncherLogic.getTargetCells(
                attacker.getSelectedLauncher(), anchor, attacker.isLauncherHorizontal());

        boolean anyLiveCell = pattern.stream().anyMatch(c ->
                c.isWithinBounds(opponentBoard.getSize()) &&
                opponentBoard.getCellStatus(c) != CellStatus.HIT &&
                opponentBoard.getCellStatus(c) != CellStatus.MISS &&
                opponentBoard.getCellStatus(c) != CellStatus.SUNK);

        if (!anyLiveCell) {
            addLogEntry("That area is already fully shelled, Admiral.", "info");
            return;
        }

        if (attacker.getSelectedLauncher() == LauncherType.NUCLEAR) {
            boolean authorized = NuclearLaunchDialog.askAndAwaitAuthorization(enemyGrid.getScene().getWindow());
            if (!authorized) {
                controller.setSelectedLauncher(attacker, LauncherType.DEFAULT);
                addLogEntry("Launch codes rejected. Nuclear strike aborted — Default weapon re-armed.", "info");
                refreshLauncherBar();
                return;
            }
        }

        if (attacker.getSelectedLauncher() == LauncherType.NUCLEAR) {
            SoundManager.getInstance().playNuclear();
        } else {
            SoundManager.getInstance().playFire();
        }

        clearGhost();
        int nuclearBefore = attacker.getNuclearAmmo();
        LauncherFireResult result = controller.fireLauncher(anchor);
        applyResult(enemyGrid, result);
        refreshLauncherBar();

        if (controller.getState() == GameState.GAME_OVER) {
            SoundManager.getInstance().stopBgm();
            app.showGameOver(attacker);
            return;
        }

        // Only start the resupply countdown if the battle is still running.
        maybeTriggerNuclearResupply(attacker, nuclearBefore);

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
            int nuclearBefore = attacker.getNuclearAmmo();
            LauncherFireResult result = controller.fireAiLauncher();
            applyResult(ownGrid, result);

            if (controller.getState() == GameState.GAME_OVER) {
                SoundManager.getInstance().stopBgm();
                app.showGameOver(attacker);
                return;
            }

            maybeTriggerNuclearResupply(attacker, nuclearBefore);

            SoundManager.getInstance().playTurnStart();
            updateTurnBadge(controller.getCurrentPlayer().getName() + "'S TURN", "turn-badge-player");
            refreshLauncherBar();
            enemyGrid.setDisable(false);
        });
        pause.play();
    }

    private void applyResult(BoardGridPane grid, LauncherFireResult result) {
        for (ShotResult r : result.results()) {
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

    /** If this fire just used the player's last Nuclear shot, start the 30s auto-resupply countdown. */
    private void maybeTriggerNuclearResupply(Player player, int nuclearAmmoBefore) {
        if (nuclearAmmoBefore > 0 && player.getNuclearAmmo() == 0) {
            NuclearResupplyDialog.show(app.getStage(), () -> {
                player.setNuclearAmmo(player.getNuclearAmmo() + 1);
                refreshLauncherBar();
            });
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
}
