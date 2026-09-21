package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.weapon.Weapon;

import com.battleship.view.battle.WeaponConsole;
import com.battleship.view.quiz.NuclearLaunchDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Composite view base for both battle screens (local + network).
 * Composes autonomous components (such as {@link WeaponConsole}) and manages
 * the shared targeting ghost preview, fire-click pipeline, and navigation exit.
 */
public abstract class AbstractBattleView {

    protected final ViewNavigator nav;
    protected final GameController controller;
    /** Audio facade injected from the navigator — never the static singleton. */
    protected final GameAudio audio;

    protected BoardGridPane ownGrid;
    protected BoardGridPane enemyGrid;
    protected final WeaponConsole weaponConsole = new WeaponConsole();
    protected HBox launcherBar;

    private final List<Coordinate> ghostCells = new ArrayList<>();

    protected AbstractBattleView(ViewNavigator nav, GameController controller) {
        this.nav = nav;
        this.controller = controller;
        this.audio = nav.getAudio();
    }

    // ================= Template method =================

    /** Assembles the shared battle skeleton. Subclasses customize via hooks only. */
    public final StackPane build() {
        launcherBar = weaponConsole.node();
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

        audio.playBattleMusic();
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

    /** Ghost highlight style class, e.g. {@link BoardGridPane#GHOST_TARGET}. */
    protected abstract String ghostStyleClass();

    /** Restores one cell after the ghost leaves it. */
    protected abstract void repaintGhostCell(int row, int col);

    /** Applies a weapon selection for the firing player. */
    protected abstract void selectWeapon(Weapon weapon);

    /** UI reaction to a click on an already-shelled area. */
    protected abstract void reportBlockedShot();

    /** UI reaction to rejected nuclear launch codes (re-arm default + repaint). */
    protected abstract void onNuclearRejected();

    /** Default orientation label text shared across battle views. */
    protected final String orientationLabelText() {
        Orientation o = firingPlayer().weaponOrientation();
        return "Orientation: " + (o.isHorizontal() ? "HORIZONTAL" : "VERTICAL")
                + "  (R or Right-Click to rotate \u2014 affects Salvo / Nuclear)";
    }

    /** Shared shot outcome audio playback. */
    protected final void playResultAudio(boolean anyHit, boolean anySunk) {
        if (anySunk) {
            audio.playSunk();
        } else if (anyHit) {
            audio.playHit();
        } else {
            audio.playMiss();
        }
    }

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

        Weapon weapon = firingPlayer().selectedWeapon();
        List<Coordinate> pattern = weapon.calculateBlastArea(anchor, firingOrientation());

        boolean anyLiveCell = pattern.stream().anyMatch(c ->
                c.isWithinBounds(targetBoardSize()) && !isCellAlreadyResolved(c));
        if (!anyLiveCell) {
            reportBlockedShot();
            return;
        }

        if (weapon.requiresAuthorization()) {
            boolean authorized = NuclearLaunchDialog.askAndAwaitAuthorization(enemyGrid.getScene().getWindow());
            if (!authorized) {
                onNuclearRejected();
                return;
            }
        }

        weapon.playFiringSound(audio);
        clearGhost();
        resolveShot(anchor);
    }


    protected final Orientation firingOrientation() {
        return firingPlayer().weaponOrientation();
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
        Weapon weapon = firingPlayer().selectedWeapon();
        List<Coordinate> cells = weapon.calculateBlastArea(new Coordinate(row, col), firingOrientation());
        int size = enemyGrid.getSize();
        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            enemyGrid.setCellState(c, ghostStyleClass());
            ghostCells.add(c);
        }
    }

    private void clearGhost() {
        for (Coordinate gc : ghostCells) {
            repaintGhostCell(gc.getRow(), gc.getCol());
        }
        ghostCells.clear();
    }

    protected final void refreshLauncherBar() {
        weaponConsole.refresh(
                firingPlayer(),
                controller.getSelectedTheater().getBoardSize(),
                canFireNow() && extraWeaponGate(),
                weapon -> {
                    selectWeapon(weapon);
                    refreshLauncherBar();
                });
    }

    protected final void toggleOrientation() {
        firingPlayer().toggleWeaponOrientation();
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
            audio.stopBgm();
            audio.playMenuMusic();
            nav.showMainMenu();
        }
    }

    /** Shared EXIT button wired to the common confirm dialog. */
    protected final Button buildExitButton() {
        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> { audio.playClick(); confirmExit(); });
        return exit;
    }
}