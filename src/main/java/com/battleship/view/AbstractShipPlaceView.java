package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.projection.ShipSnapshot;
import com.battleship.model.ShipType;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Template Method base for both ship-placement screens (local + network).
 *
 * <p>Owns every shared concern: the dock, the board grid, native drag-and-drop
 * (drag-over/enter/exit/drop plus click-to-remove), the orientation label and
 * its rotate wiring, the placement counter, the READY gating, and the exit
 * confirmation. Subclasses contribute only their screen chrome
 * ({@link #assembleLayout()}, {@link #decorateRoot(Pane)}) and their READY /
 * exit behaviour.</p>
 *
 * <p>This mirrors the {@link AbstractBattleView} design already proven on the
 * battle screens, eliminating the former near-verbatim duplication between
 * {@code ShipPlaceView} and {@code NetworkShipPlaceView}.</p>
 */
public abstract class AbstractShipPlaceView {

    protected final ViewNavigator nav;
    protected final GameController controller;
    /** The admiral deploying here (local: placing player; network: {@code netSession.getMe()}). */
    protected final Player player;
    /** Audio facade injected from the navigator — never the static singleton. */
    protected final GameAudio audio;

    protected BoardGridPane boardGridPane;
    protected ShipDockPane dockPane;
    protected Label orientationLabel;
    protected Label countLabel;
    protected Button readyButton;

    /** Current ship orientation, toggled by R / right-click / the ROTATE button. */
    protected Orientation orientation = Orientation.HORIZONTAL;

    private final List<Coordinate> ghostCells = new ArrayList<>();

    protected AbstractShipPlaceView(ViewNavigator nav, GameController controller, Player player) {
        this.nav = nav;
        this.controller = controller;
        this.player = player;
        this.audio = nav.getAudio();
    }

    // ================= Template method =================

    /** Assembles the shared placement skeleton. Subclasses customize via hooks only. */
    public final StackPane build() {
        dockPane = createDock();
        boardGridPane = new BoardGridPane(controller.getSelectedTheater().getBoardSize());
        setupDragTargets();

        orientationLabel = new Label();
        orientationLabel.getStyleClass().addAll("accent-text", "orientation-label");
        updateOrientationLabel();

        countLabel = new Label();

        readyButton = new Button(readyButtonLabel());
        readyButton.getStyleClass().add("primary-button");
        readyButton.setOnAction(e -> {
            audio.playClick();
            onReadyPressed();
        });

        Pane layout = assembleLayout();
        StackPane root = decorateRoot(layout);

        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("R")) toggleOrientation();
        });
        root.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) toggleOrientation();
        });
        root.requestFocus();

        onViewShown();
        refreshAll();
        return root;
    }

    // ================= Hooks (subclass responsibilities) =================

    /** Assembles the screen-specific chrome around the shared widgets. */
    protected abstract Pane assembleLayout();

    /** Optional root decoration (e.g. the animated ocean backdrop). */
    protected abstract StackPane decorateRoot(Pane layout);

    /** Handles the READY button (local: confirm + route; network: socket handshake). */
    protected abstract void onReadyPressed();

    /** Confirmation text shown by the shared exit dialog. */
    protected abstract String exitPrompt();

    /** Extra teardown once the player confirms exit, before returning to the menu. */
    protected abstract void onExitConfirmed();

    // ================= Overridable hooks (sensible defaults) =================

    /** Extra reason to keep the READY button disabled (e.g. READY already sent). */
    protected boolean isReadyLocked() { return false; }

    /** Called once the screen is fully assembled and wired, before the first refresh. */
    protected void onViewShown() { }

    /** Called after every orientation change. */
    protected void onOrientationToggled() { }

    /** Called at the end of {@link #refreshAll()} for extra per-screen labels. */
    protected void onRefreshed() { }

    /** READY button caption. */
    protected String readyButtonLabel() { return "READY"; }

    // ================= Shared behaviour =================

    /** Builds the dock tray; subclasses add style class / width in {@link #assembleLayout()}. */
    protected final ShipDockPane createDock() {
        ShipDockPane dock = new ShipDockPane(controller, player);
        dock.setOrientation(orientation);
        return dock;
    }

    /** Shared EXIT button wired to the common confirm dialog. */
    protected final Button buildExitButton() {
        Button exit = new Button("EXIT");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> {
            audio.playClick();
            confirmExit();
        });
        return exit;
    }

    /** Wires drag-and-drop and click-to-remove onto every board cell. */
    protected void setupDragTargets() {
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
                    Coordinate clicked = new Coordinate(row, col);
                    boolean removed = controller.removeShipAt(player, clicked);
                    if (removed) {
                        audio.playRemoveShip();
                        refreshAll();
                    }
                });

                cell.setOnDragDropped(event -> {
                    if (!event.getDragboard().hasString()) {
                        event.setDropCompleted(false);
                        event.consume();
                        return;
                    }
                    ShipType type = ShipType.valueOf(event.getDragboard().getString());
                    clearGhost();
                    boolean placed = controller.placeShip(player, type, new Coordinate(row, col), orientation);
                    if (placed) {
                        audio.playPlaceShip();
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
        String stateClass = valid ? BoardGridPane.GHOST_VALID : BoardGridPane.GHOST_INVALID;
        for (int i = 0; i < type.getSize(); i++) {
            int gr = orientation.isHorizontal() ? row : row + i;
            int gc = orientation.isHorizontal() ? col + i : col;
            if (gr < 0 || gr >= boardGridPane.getSize() || gc < 0 || gc >= boardGridPane.getSize()) continue;
            Coordinate ghostCoord = new Coordinate(gr, gc);
            boardGridPane.setCellState(ghostCoord, stateClass);
            ghostCells.add(ghostCoord);
        }
    }

    private void clearGhost() {
        for (Coordinate c : ghostCells) {
            boardGridPane.resetCellStyle(c.getRow(), c.getCol());
        }
        ghostCells.clear();
        // Re-render any already-placed ships that may have been under the ghost.
        for (com.battleship.model.projection.ShipSnapshot s : player.fleet()) {
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

    /** Re-renders the board, dock and counter, then re-evaluates the READY gating. */
    protected void refreshAll() {
        boardGridPane.clearAll();
        for (com.battleship.model.projection.ShipSnapshot s : player.fleet()) {
            boardGridPane.renderShip(s);
        }
        dockPane.refresh();
        int placed = player.deployedShipCount();
        int total = controller.getSelectedTheater().getTotalShipCount();
        countLabel.setText("Ships placed: " + placed + " / " + total);
        readyButton.setDisable(!controller.isPlacementComplete(player) || isReadyLocked());
        onRefreshed();
    }

    protected void toggleOrientation() {
        orientation = orientation.toggle();
        updateOrientationLabel();
        dockPane.setOrientation(orientation);
        onOrientationToggled();
    }

    protected void updateOrientationLabel() {
        orientationLabel.setText("Current orientation: " + (orientation.isHorizontal() ? "HORIZONTAL" : "VERTICAL"));
    }

    private void confirmExit() {
        if (AlertUtil.showConfirmation(nav.window(), "Exit Game", exitPrompt())) {
            onExitConfirmed();
            audio.stopBgm();
            audio.playMenuMusic();
            nav.showMainMenu();
        }
    }
}