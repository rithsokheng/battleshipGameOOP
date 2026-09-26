package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.controller.LauncherFireResult;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.FleetReadout;
import com.battleship.model.GameMode;
import com.battleship.model.GameState;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.ShotResult;
import com.battleship.model.fog.MarkerStatus;
import com.battleship.model.fog.TrackingGrid;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.weapon.Weapon;
import com.battleship.model.weapon.WeaponCatalog;
import com.battleship.view.battle.BattleLog;
import com.battleship.view.quiz.NuclearResupplyDialog;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Map;

/**
 * Battle screen for local play (vs AI or hotseat), rendered from the current
 * admiral's perspective. Extends {@link AbstractBattleView}, so all shared
 * machinery (weapon console, ghost preview, fire pipeline, exit) is inherited.
 *
 * <p>Strict Fog of War (V1.3 / Smell 5.2): the enemy grid is painted exclusively
 * from the attacker's {@link TrackingGrid}, never from the opponent's private
 * fleet. Enemy status rows report confirmed losses against the known fleet
 * composition instead of cheating on un-sunk damage.</p>
 */
public class LocalBattleView extends AbstractBattleView {

    private Label turnLabel;
    private Label orientationLabel;
    private Label ownShipsLeftLabel;
    private Label enemyShipsLeftLabel;
    private VBox shipStatusBar;
    private final BattleLog battleLog = new BattleLog();


    private final com.battleship.view.battle.BattlePerspective perspective;

    public LocalBattleView(ViewNavigator nav, GameController controller) {
        this(nav, controller, controller.getSelectedMode() == GameMode.HOTSEAT
                ? new com.battleship.view.battle.AlternatingPerspective(controller)
                : new com.battleship.view.battle.FixedPerspective(controller));
    }

    public LocalBattleView(ViewNavigator nav, GameController controller, com.battleship.view.battle.BattlePerspective perspective) {
        super(nav, controller);
        this.perspective = java.util.Objects.requireNonNull(perspective, "BattlePerspective is required.");
    }

    // ---------- Perspective helpers (Strategy Pattern — satisfies OCP) ----------

    private String perspectiveName() { return perspective.perspectiveName(); }
    private FleetReadout perspectiveFleet() { return perspective.perspectiveFleet(); }
    private String opponentName() { return perspective.opponentName(); }
    private TrackingGrid opponentKnowledge() { return perspective.opponentKnowledge(); }

    // ---------- AbstractBattleView hooks ----------

    @Override
    protected Player firingPlayer() { return controller.getCurrentPlayer(); }

    @Override
    protected boolean canFireNow() { return !controller.isAiTurn(); }

    @Override
    protected int targetBoardSize() {
        return opponentKnowledge().size();
    }

    @Override
    protected boolean isCellAlreadyResolved(Coordinate c) {
        return opponentKnowledge().isAlreadyShelled(c);
    }

    @Override
    protected String ghostStyleClass() {
        return BoardGridPane.GHOST_TARGET;
    }

    @Override
    protected void repaintGhostCell(int row, int col) {
        Coordinate c = new Coordinate(row, col);
        MarkerStatus status = opponentKnowledge().observedStatus(c);
        if (status == MarkerStatus.HIT) {
            enemyGrid.renderShot(c, CellStatus.HIT);
        } else if (status == MarkerStatus.MISS) {
            enemyGrid.renderShot(c, CellStatus.MISS);
        } else {
            enemyGrid.resetCellStyle(row, col);
        }
    }

    @Override
    protected void selectWeapon(Weapon weapon) {
        controller.selectWeapon(firingPlayer(), weapon);
    }

    @Override
    protected void reportBlockedShot() {
        addLogEntry("That area is already fully shelled, Admiral.", "info");
    }

    @Override
    protected void onNuclearRejected() {
        controller.selectWeapon(controller.getCurrentPlayer(), WeaponCatalog.defaultWeapon());
        addLogEntry("Launch codes rejected. Nuclear strike aborted \u2014 Default weapon re-armed.", "info");
        refreshLauncherBar();
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
        FleetReadout ownFleet = perspectiveFleet();
        BoardGridPane grid = new BoardGridPane(ownFleet.size());
        renderExistingShots(grid, ownFleet);
        for (ShipSnapshot s : ownFleet.fleet()) {
            if (!s.isSunk()) grid.renderShip(s);
        }
        return grid;
    }

    @Override
    protected BoardGridPane createEnemyGrid() {
        TrackingGrid knowledge = opponentKnowledge();
        BoardGridPane grid = new BoardGridPane(knowledge.size());
        for (int r = 0; r < knowledge.size(); r++) {
            for (int c = 0; c < knowledge.size(); c++) {
                Coordinate coord = new Coordinate(r, c);
                MarkerStatus st = knowledge.observedStatus(coord);
                if (st == MarkerStatus.HIT) grid.renderShot(coord, CellStatus.HIT);
                else if (st == MarkerStatus.MISS) grid.renderShot(coord, CellStatus.MISS);
            }
        }
        for (TrackingGrid.DiscoveredWreck wreck : knowledge.confirmedSunk()) {
            grid.renderSunkShip(wreck.cells());
        }
        return grid;
    }

    @Override
    protected Pane assembleLayout() {
        orientationLabel = new Label();
        orientationLabel.getStyleClass().add("dim-text");
        updateOrientationLabel();

        VBox leftColumn = buildLeftColumn();
        VBox sidePanel = buildSidePanel();
        refreshShipStatusBar();
        addLogEntry("Select a weapon, then a target on the enemy grid.", "info");

        HBox content = new HBox(24, leftColumn, sidePanel);
        content.setAlignment(Pos.CENTER);
        VBox.setVgrow(content, Priority.ALWAYS);

        HBox commandBar = buildCommandBar(perspectiveName(), opponentName());
        VBox layout = new VBox(16, commandBar, content);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(16, 20, 16, 20));
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    /** Weapon bar stacked above the two board cards. */
    private VBox buildLeftColumn() {
        VBox weaponsCard = new VBox(8, launcherBar, orientationLabel);
        weaponsCard.setAlignment(Pos.CENTER);
        weaponsCard.getStyleClass().add("side-card");

        HBox boardsRow = buildBoardsRow();
        VBox.setVgrow(boardsRow, Priority.ALWAYS);
        boardsRow.setAlignment(Pos.CENTER);

        VBox leftColumn = new VBox(16, weaponsCard, boardsRow);
        leftColumn.setAlignment(Pos.CENTER);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        return leftColumn;
    }

    /** Own fleet beside enemy waters, each with its own afloat counter. */
    private HBox buildBoardsRow() {
        ownShipsLeftLabel = new Label();
        ownShipsLeftLabel.getStyleClass().add("ships-left-badge");
        enemyShipsLeftLabel = new Label();
        enemyShipsLeftLabel.getStyleClass().add("ships-left-badge");

        VBox ownBox = buildBoardCard("YOUR FLEET", ownGrid, ownShipsLeftLabel);
        VBox enemyBox = buildBoardCard("ENEMY WATERS", enemyGrid, enemyShipsLeftLabel);
        refreshShipsLeftLabels();

        HBox boardsRow = new HBox(24, ownBox, enemyBox);
        boardsRow.setAlignment(Pos.CENTER);
        return boardsRow;
    }

    @Override
    protected StackPane decorateRoot(Pane layout) {
        StackPane root = new StackPane();
        Canvas ocean = DecorUtil.animatedOceanScene(root, 0.0);
        root.getChildren().add(ocean);
        root.getChildren().add(layout);

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener((o, oldW, newW) -> adjustGridSizes(newW.doubleValue(), newScene.getHeight()));
                newScene.heightProperty().addListener((o, oldH, newH) -> adjustGridSizes(newScene.getWidth(), newH.doubleValue()));
                adjustGridSizes(newScene.getWidth(), newScene.getHeight());
            }
        });

        return root;
    }

    private void adjustGridSizes(double width, double height) {
        if (width <= 0 || height <= 0 || ownGrid == null || enemyGrid == null) return;
        int size = ownGrid.getSize();

        // Screen height minus top bar (~55px), weapons card (~65px), card header/padding (~68px), outer spacing (~52px)
        double availH = height - 250;
        // Screen width minus sidePanel (240px), column spacing (24px), layout padding (40px),
        // board gap (24px), card chrome (76px for both cards) -> ~404px
        double availW = (width - 404) / 2.0;

        double maxGridPx = Math.min(availW, availH);
        maxGridPx = Math.max(370.0, Math.min(maxGridPx, 540.0));

        double newCellPx = Math.floor(maxGridPx / size);
        ownGrid.setCellSize(newCellPx);
        enemyGrid.setCellSize(newCellPx);
    }

    @Override
    protected void onViewShown() {
        if (nav.getStage() != null && nav.getStage().getScene() != null) {
            adjustGridSizes(nav.getStage().getScene().getWidth(), nav.getStage().getScene().getHeight());
        }
        if (controller.isAiTurn()) {
            updateTurnBadge("ENEMY TURN", "turn-badge-enemy");
            enemyGrid.setDisable(true);
            runAiTurnAfterDelay();
        } else {
            updateTurnBadge(perspectiveName() + "'S TURN", "turn-badge-player");
        }
    }

    // ---------- Firing (local + AI) ----------

    @Override
    protected void resolveShot(Coordinate anchor) {
        Player attacker = controller.getCurrentPlayer();
        boolean hadNuclearAmmo = attacker.hasAmmo(WeaponCatalog.nuclearWarhead());
        LauncherFireResult result = controller.fireLauncher(anchor);
        applyResult(enemyGrid, result);
        refreshLauncherBar();

        if (controller.getState() == GameState.GAME_OVER) {
            NuclearResupplyDialog.dismissActive();
            audio.stopBgm();
            nav.showGameOver(attacker);
            return;
        }

        // Only start the resupply countdown if the battle is still running.
        maybeTriggerNuclearResupply(attacker, hadNuclearAmmo);

        if (controller.getSelectedMode() == GameMode.HOTSEAT) {
            audio.stopBgm();
            nav.showPassScreen(() -> nav.showBattle());
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
            audio.playFire();
            Player attacker = controller.getCurrentPlayer();
            boolean hadNuclearAmmo = attacker.hasAmmo(WeaponCatalog.nuclearWarhead());
            LauncherFireResult result = controller.fireAiLauncher();
            applyResult(ownGrid, result);

            if (controller.getState() == GameState.GAME_OVER) {
                NuclearResupplyDialog.dismissActive();
                audio.stopBgm();
                nav.showGameOver(attacker);
                return;
            }

            maybeTriggerNuclearResupply(attacker, hadNuclearAmmo);

            audio.playTurnStart();
            updateTurnBadge(controller.getCurrentPlayer().name() + "'S TURN", "turn-badge-player");
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
        for (ShipSnapshot sunkShip : result.sunkShips()) {
            grid.renderSunkShip(sunkShip.cells());
            addLogEntry(sunkShip.type().name().replace('_', ' ') + " SUNK!", "sunk");
        }
        boolean anyHit = result.anyHit();
        playResultAudio(anyHit, anySunk);
        if (!anySunk) {
            if (anyHit) {
                addLogEntry("Direct hit!", "hit");
            } else {
                addLogEntry("Nothing but spray \u2014 miss.", "miss");
            }
        }
        refreshShipStatusBar();
        refreshShipsLeftLabels();
    }

    // ---------- Local chrome: command bar / side console ----------

    private HBox buildCommandBar(String currentName, String opponentName) {
        turnLabel = new Label();
        turnLabel.getStyleClass().addAll("turn-badge", "turn-badge-player");

        HBox leftWrap = new HBox(turnLabel);
        leftWrap.setAlignment(Pos.CENTER_LEFT);
        leftWrap.setPrefWidth(180);

        Label vs = new Label("VS");
        vs.getStyleClass().add("vs-divider");

        HBox chips = new HBox(20,
                chipText(currentName, "COMMANDER"),
                vs,
                chipText(opponentName,
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

        long ownLeft = current.fleet().stream().filter(s -> !s.isSunk()).count();
        long ownTotal = current.fleet().size();
        ownShipsLeftLabel.setText(ownLeft + "/" + ownTotal + " AFLOAT");

        int enemyLeft = current.trackingGrid().shipsRemaining();
        int enemyTotal = current.trackingGrid().totalEnemyShips();
        enemyShipsLeftLabel.setText(enemyLeft + "/" + enemyTotal + " REMAINING");
    }

    // ---------- Side console (fleet status + attack log) ----------

    private VBox buildSidePanel() {
        VBox side = new VBox(12, buildRadarCard(), buildFleetStatusCard(), buildAttackLogCard());
        side.setPrefWidth(240);
        side.setMinWidth(240);
        side.setMaxWidth(240);
        side.setAlignment(Pos.CENTER);
        return side;
    }

    /** Decorative radar sweep. */
    private VBox buildRadarCard() {
        StackPane radar = DecorUtil.animatedRadarSweep(130);
        VBox radarCard = new VBox(radar);
        radarCard.setAlignment(Pos.CENTER);
        radarCard.getStyleClass().add("side-card");
        return radarCard;
    }

    /** Fleet composition and confirmed losses for the enemy fleet (Fog of War safe). */
    private VBox buildFleetStatusCard() {
        Label fleetTitle = new Label("ENEMY FLEET STATUS");
        fleetTitle.getStyleClass().add("side-card-title");
        shipStatusBar = new VBox(6);
        VBox fleetCard = new VBox(12, fleetTitle, shipStatusBar);
        fleetCard.getStyleClass().add("side-card");
        return fleetCard;
    }

    /** Scrolling, newest-first attack log. */
    private VBox buildAttackLogCard() {
        return battleLog.node();
    }

    private void addLogEntry(String text, String type) {
        battleLog.add(text, type);
    }


    private void refreshShipStatusBar() {
        shipStatusBar.getChildren().clear();
        Player current = controller.getCurrentPlayer();
        TrackingGrid knowledge = current.trackingGrid();
        for (Map.Entry<ShipType, Integer> entry : knowledge.enemyFleetComposition().entrySet()) {
            shipStatusBar.getChildren().add(buildFleetStatusRow(entry.getKey(), entry.getValue(), knowledge));
        }
    }

    private HBox buildFleetStatusRow(ShipType type, int totalCount, TrackingGrid knowledge) {
        long sunkCount = knowledge.confirmedSunk().stream().filter(w -> w.type() == type).count();
        boolean allSunk = sunkCount >= totalCount;

        Label name = new Label(type.name().replace('_', ' '));
        name.getStyleClass().add(allSunk ? "fleet-status-name-sunk" : "fleet-status-name");
        name.setPrefWidth(84);

        double trackWidth = 58;
        Region track = new Region();
        track.getStyleClass().add("fleet-bar-track");
        track.setPrefSize(trackWidth, 5);
        track.setMaxSize(trackWidth, 5);

        double fillWidth = totalCount == 0 ? 0 : trackWidth * ((double) sunkCount / totalCount);
        Region fill = new Region();
        fill.getStyleClass().add(allSunk ? "fleet-bar-fill-sunk" : "fleet-bar-fill");
        fill.setPrefSize(fillWidth, 5);
        fill.setMaxSize(fillWidth, 5);
        fill.setMinSize(fillWidth, 5);

        StackPane barStack = new StackPane(track, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label countLabel = new Label(sunkCount + "/" + totalCount + " SUNK");
        countLabel.getStyleClass().add("dim-text");
        countLabel.setPrefWidth(54);

        HBox row = new HBox(6, name, barStack, countLabel);
        row.getStyleClass().add("fleet-status-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void renderExistingShots(BoardGridPane grid, FleetReadout fleet) {
        for (int r = 0; r < fleet.size(); r++) {
            for (int c = 0; c < fleet.size(); c++) {
                Coordinate coord = new Coordinate(r, c);
                CellStatus status = fleet.cellStatus(coord);
                if (status == CellStatus.HIT || status == CellStatus.MISS) {
                    grid.renderShot(coord, status);
                }
            }
        }
        for (ShipSnapshot s : fleet.fleet()) {
            if (s.isSunk()) grid.renderSunkShip(s.cells());
        }
    }

    private void updateOrientationLabel() {
        orientationLabel.setText(orientationLabelText());
    }

    /**
     * If this shot just consumed the player's last Nuclear round, start the 30s
     * auto-resupply countdown (quiz-gated) and top the stock back up on success.
     */
    private void maybeTriggerNuclearResupply(Player player, boolean hadNuclearAmmoBefore) {
        Weapon nuclear = WeaponCatalog.nuclearWarhead();
        if (hadNuclearAmmoBefore && !player.hasAmmo(nuclear)) {
            NuclearResupplyDialog.show(nav.getStage(), () -> {
                // Fix 2/F7: resupply goes through the controller-owned service, never the raw Player.
                controller.resupplyNuclearAmmo(player);
                refreshLauncherBar();
            });
        }
    }
}