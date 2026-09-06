package com.battleship.view;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Ship;
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
 * Reusable board grid used by ShipPlaceView, BattleView, and GameOverView.
 * ISSUE 6: renderSunkShip() colors EVERY cell belonging to a sunk ship
 * (not just the last hit) and adds a cross-out line + darker border.
 */
public class BoardGridPane extends GridPane {

    public static final String BASE_STYLE =
            "-fx-background-color:#102e4a; -fx-border-color:#2e5d87; -fx-border-width:0.5;";
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
                cell.setStyle(BASE_STYLE);
                cells[r][c] = cell;
                add(cell, c, r);
            }
        }
    }

    public StackPane getCell(int row, int col) { return cells[row][col]; }
    public StackPane getCell(Coordinate c) { return cells[c.getRow()][c.getCol()]; }
    public int getSize() { return size; }

    public void resetCellStyle(int row, int col) {
        cells[row][col].setStyle(BASE_STYLE);
    }

    /** Renders a placed (not-yet-shot) ship, used during placement. */
    public void renderShip(Ship ship) {
        boolean horizontal = ship.isHorizontal();
        Image sprite = ImageResources.ship(ship.getType(), horizontal);
        List<Coordinate> occupied = ship.getOccupiedCells();
        int len = occupied.size();

        for (int i = 0; i < len; i++) {
            Coordinate c = occupied.get(i);
            StackPane cell = cells[c.getRow()][c.getCol()];
            cell.getChildren().clear();
            cell.setStyle(BASE_STYLE + "-fx-background-color:#2e6690;");

            if (sprite != null) {
                // The source art is a single square image per hull; slice out the
                // portion that belongs to this cell along the ship's long axis.
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
                cell.setStyle(BASE_STYLE + "-fx-background-color:#081a2d;");
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
                    dot.setStyle("-fx-background-color:#f5f7fa; -fx-background-radius:50%;");
                    cell.getChildren().add(dot);
                }
            }
            case HIT -> {
                cell.setStyle(BASE_STYLE + "-fx-background-color:#3a1a1a;");
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
                        x.setStyle("-fx-text-fill:#ff5c5c; -fx-font-size:20px; -fx-font-weight:bold;");
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

    /** ISSUE 6: colors every cell of the sunk ship, not just the triggering hit. */
    public void renderSunkShip(Ship ship) {
        Image fire = ImageResources.effect("hit-explosion");
        for (Coordinate c : ship.getOccupiedCells()) {
            StackPane cell = cells[c.getRow()][c.getCol()];
            cell.getChildren().clear();
            cell.setStyle("-fx-background-color:#5a2a2a; -fx-border-color:#ff5c5c; " +
                          "-fx-border-width:2; -fx-border-radius:2;");
            if (fire != null) {
                ImageView iv = new ImageView(fire);
                iv.setFitWidth(cellPx * 0.85);
                iv.setFitHeight(cellPx * 0.85);
                iv.setPreserveRatio(true);
                cell.getChildren().add(iv);
            }
            Line diagonal = new Line(-14, -14, 14, 14);
            diagonal.setStroke(javafx.scene.paint.Color.web("#ffd166"));
            diagonal.setStrokeWidth(2);
            cell.getChildren().add(diagonal);
            StackPane.setAlignment(diagonal, Pos.CENTER);
        }
    }

    public void clearAll() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                cells[r][c].getChildren().clear();
                cells[r][c].setStyle(BASE_STYLE);
            }
        }
    }
}
