package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.view.quiz.NuclearLaunchDialog;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Template Method base for both battle screens (local + network). Owns every
 * shared concern: the weapon bar, orientation toggle, hover ghost preview, the
 * fire-click pipeline (turn gating, live-cell validation, nuclear launch
 * authorization, sound triggers) and the exit confirmation.
 *
 * Subclasses supply only the layout chrome and the polymorphic shot
 * resolution ({@link #resolveShot}), eliminating the former ~60% copy-paste
 * between the two battle screens.
 */
public abstract class AbstractBattleView {

    /** Visual state of one weapon button; styling is subclass-supplied. */
    protected enum LauncherButtonState { DISABLED, SELECTED, ENABLED }

    protected final MainApp app;
    protected final GameController controller;

    protected BoardGridPane ownGrid;
    protected BoardGridPane enemyGrid;
    protected HBox launcherBar;

    private final List<int[]> ghostCells = new ArrayList<>();

    protected AbstractBattleView(MainApp app, GameController controller) {
        this.app = app;
        this.controller = controller;
    }

    // ================= Template method =================

    /** Assembles the shared battle skeleton. Subclasses customize via hooks only. */
    public final StackPane build() {
        launcherBar = new HBox(10);
        launcherBar.setAlignment(Pos.CENTER);
        refreshLauncherBar();

        ownGrid = createOwnGrid();
        enemyGrid = createEnemyGrid();
        attachFireHandlers();

        Pane layout = assembleLayout();
        StackPane root = decorateRoot(layout);

        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> { if (e.getCode().toString().equals("R")) toggleOrientation(); });
        root.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.SECONDARY) toggleOrientation(); });
        root.requestFocus();

        SoundManager.getInstance().playBattleMusic();
        onViewShown();
        return root;
    }

    // ================= Hooks (subclass responsibilities) =================

    /** Builds the local player's grid, including any pre-rendered fleet/shots. */
    protected abstract BoardGridPane createOwnGrid();

    /** Builds the enemy grid (no ship layout is ever rendered here). */
    protected abstract BoardGridPane createEnemyGrid();

    /** Assembles the screen-specific chrome around the shared widgets. */
    protected abstract Pane assembleLayout();

    /** Optional root decoration (e.g. animated ocean background). */
    protected abstract StackPane decorateRoot(Pane layout);

    /** Called after the screen is visible: turn kick-off, network handlers, etc. */
    protected abstract void onViewShown();

    /** Resolves a validated shot — locally via the controller, or over the network. */
    protected abstract void resolveShot(Coordinate anchor);

    /** True when the local player may act right now (gates ghost + fire clicks). */
    protected abstract boolean canFireNow();

    /** Extra gating for weapon buttons beyond availability/ammo (network turn). */
    protected boolean extraWeaponGate() { return true; }

    /** The player whose launcher is currently aimed. */
    protected abstract Player firingPlayer();

    /** Board size the shot will land on — used for live-cell checks. */
    protected abstract int targetBoardSize();

    /** True when the cell has already been HIT/MISS/SUNK and cannot be re-shot. */
    protected abstract boolean isCellAlreadyResolved(Coordinate c);

    /** Ghost highlight style fragment appended to {@link BoardGridPane#BASE_STYLE}. */
    protected abstract String ghostStyle();

    /** Restores one cell after the ghost leaves it. */
    protected abstract void repaintGhostCell(int row, int col);

    /** Applies a launcher selection for the firing player. */
    protected abstract void selectLauncher(LauncherType type);

    /** UI reaction to a click on an already-shelled area. */
    protected abstract void reportBlockedShot();

    /** UI reaction to rejected nuclear launch codes (re-arm DEFAULT + repaint). */
    protected abstract void onNuclearRejected();

    /** Weapon-button styling per state. */
    protected abstract String launcherButtonStyle(LauncherButtonState state);

    /** Confirmation text shown by the shared exit dialog. */
    protected abstract String exitPrompt();

    /** Extra teardown when the player confirms exit (e.g. close socket). */
    protected void onExitConfirmed() { }

    /** Notified after the shared orientation toggle; subclasses refresh labels. */
    protected abstract void onOrientationChanged();

    // ================= Shared behavior =================

    /** Shared fire-click pipeline used by both battle screens. */
    private void handleFireClick(Coordinate anchor) {
        if (!canFireNow()) return;

        LauncherType type = firingPlayer().getSelectedLauncher();
        List<Coordinate> pattern = type.getTargetCells(anchor, firingOrientation());

        boolean anyLiveCell = pattern.stream().anyMatch(c ->
                c.isWithinBounds(targetBoardSize()) && !isCellAlreadyResolved(c));
        if (!anyLiveCell) {
            reportBlockedShot();
            return;
        }

        if (type == LauncherType.NUCLEAR) {
            boolean authorized = NuclearLaunchDialog.askAndAwaitAuthorization(enemyGrid.getScene().getWindow());
            if (!authorized) {
                onNuclearRejected();
                return;
            }
        }

        if (type == LauncherType.NUCLEAR) {
            SoundManager.getInstance().playNuclear();
        } else {
            SoundManager.getInstance().playFire();
        }
        clearGhost();
        resolveShot(anchor);
    }

    protected final Orientation firingOrientation() {
        return firingPlayer().getLauncherOrientation();
    }

    private void attachFireHandlers() {
        int size = enemyGrid.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                final int row = r, col = c;
                enemyGrid.getCell(r, c).setOnMouseClicked(e -> handleFireClick(new Coordinate(row, col)));
                enemyGrid.getCell(r, c).setOnMouseEntered(e -> showGhost(row, col));
                enemyGrid.getCell(r, c).setOnMouseExited(e -> clearGhost());
            }
        }
    }

    private void showGhost(int row, int col) {
        if (!canFireNow()) return;
        clearGhost();
        LauncherType type = firingPlayer().getSelectedLauncher();
        List<Coordinate> cells = type.getTargetCells(new Coordinate(row, col), firingOrientation());
        int size = enemyGrid.getSize();
        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            enemyGrid.getCell(c).setStyle(BoardGridPane.BASE_STYLE + ghostStyle());
            ghostCells.add(new int[]{c.getRow(), c.getCol()});
        }
    }

    private void clearGhost() {
        for (int[] rc : ghostCells) {
            repaintGhostCell(rc[0], rc[1]);
        }
        ghostCells.clear();
    }

    protected final void refreshLauncherBar() {
        launcherBar.getChildren().clear();
        for (LauncherType type : LauncherType.values()) {
            launcherBar.getChildren().add(buildLauncherButton(type));
        }
    }

    private Button buildLauncherButton(LauncherType type) {
        Player player = firingPlayer();
        int size = controller.getSelectedTheater().getBoardSize();
        boolean available = type.isAvailableFor(size);
        int ammo = controller.getAmmoRemaining(player, type);
        boolean hasAmmo = type == LauncherType.DEFAULT || ammo > 0;
        boolean enabled = available && hasAmmo && extraWeaponGate();

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

        LauncherButtonState state = !enabled ? LauncherButtonState.DISABLED
                : player.getSelectedLauncher() == type ? LauncherButtonState.SELECTED
                : LauncherButtonState.ENABLED;
        b.setStyle("-fx-background-radius:8; -fx-border-radius:8; -fx-border-width:1.5; "
                + "-fx-font-size:12px; -fx-font-weight:bold; " + launcherButtonStyle(state));
        b.setDisable(!enabled);

        b.setOnAction(e -> {
            selectLauncher(type);
            refreshLauncherBar();
        });
        return b;
    }

    protected final void toggleOrientation() {
        firingPlayer().toggleLauncherOrientation();
        onOrientationChanged();
    }

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText(null);
        alert.setContentText(exitPrompt());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            onExitConfirmed();
            SoundManager.getInstance().stopBgm();
            SoundManager.getInstance().playMenuMusic();
            app.showMainMenu();
        }
    }

    /** Shared EXIT button wired to the common confirm dialog. */
    protected final Button buildExitButton() {
        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> { SoundManager.getInstance().playClick(); confirmExit(); });
        return exit;
    }
}