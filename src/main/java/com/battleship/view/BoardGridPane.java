package com.battleship.view;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.projection.ShipSnapshot;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.List;

/**
 * Reusable board grid used by the placement screens, both battle screens and the
 * game-over screens.
 *
 * <p>Ships are painted from immutable {@link ShipSnapshot}s (V1.2) — this widget
 * never receives a mutable domain entity, so a UI component cannot alter the
 * game state it renders.</p>
 */
public class BoardGridPane extends GridPane {

    /** Base style class carried by every cell; state classes are toggled on top. */
    public static final String CELL_CLASS = "board-cell";

    /** Ghost-preview classes, usable via {@link #setCellState(Coordinate, String)}. */
    public static final String GHOST_VALID = "board-cell-ghost-valid";
    public static final String GHOST_INVALID = "board-cell-ghost-invalid";
    public static final String GHOST_TARGET = "board-cell-ghost-target";

    private static final String CELL_SHIP = "board-cell-ship";
    private static final String CELL_MISS = "board-cell-miss";
    private static final String CELL_HIT = "board-cell-hit";
    private static final String CELL_SUNK = "board-cell-sunk";

    /** Every class this grid may toggle on a cell; cleared before a new state is applied. */
    private static final String[] CELL_STATE_CLASSES = {
            CELL_SHIP, CELL_MISS, CELL_HIT, CELL_SUNK,
            GHOST_VALID, GHOST_INVALID, GHOST_TARGET
    };

    private static final int CELL_PX = 42;

    private final int size;
    private final StackPane[][] cells;
    private final double cellPx;

    public BoardGridPane(int size) {
        this.size = size;
        this.cells = new StackPane[size][size];
        this.cellPx = size > 10 ? CELL_PX : Math.max(28, 420.0 / size);
        setHgap(1);
        setVgap(1);
        build();
    }

    private void build() {
        double cellPx = this.cellPx;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(cellPx, cellPx);
                cell.getStyleClass().add(CELL_CLASS);
                cells[r][c] = cell;
                add(cell, c, r);
            }
        }
    }

    public StackPane getCell(int row, int col) { return cells[row][col]; }
    public StackPane getCell(Coordinate c) { return cells[c.getRow()][c.getCol()]; }
    public int getSize() { return size; }

    public void resetCellStyle(int row, int col) {
        applyCellState(cells[row][col], null);
    }

    /** Applies a cell state (or {@code null} for the plain base cell) as a style class. */
    private void applyCellState(StackPane cell, String stateClass) {
        cell.getStyleClass().removeAll(CELL_STATE_CLASSES);
        if (stateClass != null) cell.getStyleClass().add(stateClass);
    }

    /** Applies one of the ghost/state classes to a cell; used by the ghost previews. */
    public void setCellState(Coordinate c, String stateClass) {
        applyCellState(cells[c.getRow()][c.getCol()], stateClass);
    }

    /** Renders a placed (not-yet-shot) ship, used during placement and on own-fleet boards. */
    public void renderShip(ShipSnapshot ship) {
        Orientation orientation = ship.orientation();
        Image sprite = ImageResources.ship(ship.type(), orientation);
        List<Coordinate> occupied = ship.cells();
        int len = occupied.size();

        for (int i = 0; i < len; i++) {
            Coordinate c = occupied.get(i);
            StackPane cell = cells[c.getRow()][c.getCol()];
            cell.getChildren().clear();
            applyCellState(cell, CELL_SHIP);

            if (sprite != null) {
                // The source art is a single square image per hull; slice out the
                // portion that belongs to this cell along the ship's long axis.
                boolean horizontal = orientation.isHorizontal();
                double sliceW = horizontal ? sprite.getWidth() / len : sprite.getWidth();
                double sliceH = horizontal ? sprite.getHeight() : sprite.getHeight() / len;
                double x = horizontal ? i * sliceW : 0;
                double y = horizontal ? 0 : i * sliceH;

                ImageView iv = new ImageView(sprite);
                iv.setViewport(new Rectangle2D(x, y, sliceW, sliceH));
                iv.setFitWidth(cellPx);
                iv.setFitHeight(cellPx);
                iv.setPreserveRatio(false);
                cell.getChildren().add(iv);
            }
        }
    }

    /**
     * Plays a quick fire-1 -> fire-2 -> fire-3 -> hit-explosion flipbook in the
     * given cell. Returns false (and adds nothing) if any frame is missing, so
     * the caller can fall back to the static explosion image or the plain "X".
     */
    private boolean playFireFlipbook(StackPane cell) {
        Image f1 = ImageResources.effect("fire-1");
        Image f2 = ImageResources.effect("fire-2");
        Image f3 = ImageResources.effect("fire-3");
        Image explosion = ImageResources.effect("hit-explosion");
        if (f1 == null || f2 == null || f3 == null || explosion == null) return false;

        ImageView iv = new ImageView(f1);
        iv.setFitWidth(cellPx * 0.85);
        iv.setFitHeight(cellPx * 0.85);
        iv.setPreserveRatio(true);
        cell.getChildren().add(iv);

        Timeline flipbook = new Timeline(
                new KeyFrame(Duration.millis(90), e -> iv.setImage(f2)),
                new KeyFrame(Duration.millis(180), e -> iv.setImage(f3)),
                new KeyFrame(Duration.millis(270), e -> iv.setImage(explosion))
        );
        flipbook.play();
        return true;
    }

    /** Renders a MISS or a non-fatal HIT. Sunk ships must go through renderSunkShip(). */
    public void renderShot(Coordinate coord, CellStatus status) {
        StackPane cell = cells[coord.getRow()][coord.getCol()];
        cell.getChildren().clear();
        switch (status) {
            case MISS -> {
                applyCellState(cell, CELL_MISS);
                Image splash = ImageResources.effect("miss-splash");
                if (splash != null) {
                    ImageView iv = new ImageView(splash);
                    iv.setFitWidth(cellPx * 0.7);
                    iv.setFitHeight(cellPx * 0.7);
                    iv.setPreserveRatio(true);
                    cell.getChildren().add(iv);
                } else {
                    StackPane dot = new StackPane();
                    dot.setMaxSize(8, 8);
                    dot.getStyleClass().add("board-miss-dot");
                    cell.getChildren().add(dot);
                }
            }
            case HIT -> {
                applyCellState(cell, CELL_HIT);
                if (!playFireFlipbook(cell)) {
                    Image fire = ImageResources.effect("hit-explosion");
                    if (fire != null) {
                        ImageView iv = new ImageView(fire);
                        iv.setFitWidth(cellPx * 0.85);
                        iv.setFitHeight(cellPx * 0.85);
                        iv.setPreserveRatio(true);
                        cell.getChildren().add(iv);
                    } else {
                        Label x = new Label("\u00D7");
                        x.getStyleClass().add("board-hit-marker");
                        cell.getChildren().add(x);
                    }
                }
                ScaleTransition st = new ScaleTransition(Duration.millis(100), cell);
                st.setFromX(1.0);
                st.setFromY(1.0);
                st.setToX(1.3);
                st.setToY(1.3);
                st.setAutoReverse(true);
                st.setCycleCount(2);
                st.play();
            }
            default -> { /* SHIP/EMPTY/SUNK handled elsewhere */ }
        }
    }

    /** Colours every cell of a sunk hull (not just the triggering hit) and crosses it out. */
    public void renderSunkShip(List<Coordinate> occupiedCells) {
        Image fire = ImageResources.effect("hit-explosion");
        for (Coordinate c : occupiedCells) {
            StackPane cell = cells[c.getRow()][c.getCol()];
            cell.getChildren().clear();
            applyCellState(cell, CELL_SUNK);
            if (fire != null) {
                ImageView iv = new ImageView(fire);
                iv.setFitWidth(cellPx * 0.85);
                iv.setFitHeight(cellPx * 0.85);
                iv.setPreserveRatio(true);
                cell.getChildren().add(iv);
            }
            Line diagonal = new Line(-14, -14, 14, 14);
            diagonal.getStyleClass().add("board-sunk-cross");
            cell.getChildren().add(diagonal);
            StackPane.setAlignment(diagonal, Pos.CENTER);
        }
    }

    public void clearAll() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                cells[r][c].getChildren().clear();
                applyCellState(cells[r][c], null);
            }
        }
    }
}
