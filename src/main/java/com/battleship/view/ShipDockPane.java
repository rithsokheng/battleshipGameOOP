package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Map;

/**
 * ISSUE 5: ship dock whose blocks visually rotate (tall vs wide) to match
 * the current global orientation, and expose native drag-and-drop so
 * ShipPlaceView's board cells can accept them.
 */
public class ShipDockPane extends VBox {

    private static final int UNIT = 34;

    private final GameController controller;
    private final Player player;
    private Orientation orientation = Orientation.HORIZONTAL;
    private ShipType selectedShipType = null;
    private java.util.function.Consumer<ShipType> onSelectionChanged;

    public ShipDockPane(GameController controller, Player player) {
        this.controller = controller;
        this.player = player;
        setSpacing(16);
        setPadding(new Insets(8, 4, 8, 4));
        setAlignment(Pos.TOP_CENTER);
        refresh();
    }

    public void setOnSelectionChanged(java.util.function.Consumer<ShipType> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public ShipType getSelectedShip() {
        return selectedShipType;
    }

    public void selectShip(ShipType type) {
        this.selectedShipType = type;
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(selectedShipType);
        }
        refresh();
    }

    public void clearSelection() {
        if (this.selectedShipType != null) {
            this.selectedShipType = null;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(null);
            }
            refresh();
        }
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        refresh();
    }

    /** Rebuilds the dock contents from the controller's remaining-ship counts. */
    public void refresh() {
        getChildren().clear();
        Label header = new Label("SHIP DOCK");
        header.getStyleClass().add("side-card-title");
        getChildren().add(header);

        Map<ShipType, Integer> remaining = controller.getRemainingShipCounts(player);
        if (selectedShipType != null && remaining.getOrDefault(selectedShipType, 0) == 0) {
            selectedShipType = null;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(null);
            }
        }
        for (Map.Entry<ShipType, Integer> entry : remaining.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                getChildren().add(buildShipNode(entry.getKey()));
            }
        }
    }

    private Pane buildShipNode(ShipType type) {
        Image sprite = ImageResources.ship(type, orientation);
        int len = type.getSize();
        double w = orientation.isHorizontal() ? UNIT * len + (len - 1) : UNIT;
        double h = orientation.isHorizontal() ? UNIT : UNIT * len + (len - 1);

        StackPane block = new StackPane();
        block.setPrefSize(w, h);
        block.setMaxSize(w, h);

        boolean isSelected = type == selectedShipType;

        Rectangle frame = new Rectangle(w, h);
        frame.setArcWidth(10);
        frame.setArcHeight(10);
        frame.setFill(sprite == null ? Color.web("#1c4468") : Color.TRANSPARENT);
        frame.setStroke(isSelected ? Color.web("#ffd166") : Color.web("#63c4ff", 0.85));
        frame.setStrokeWidth(isSelected ? 2.2 : 1.3);

        if (sprite != null) {
            // The whole hull rendered as one uncut image, so the ship reads as
            // a single vessel in the dock rather than a row of bordered tiles.
            ImageView iv = new ImageView(sprite);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(w, h);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            iv.setClip(clip);
            block.getChildren().add(iv);
        }
        block.getChildren().add(frame);

        if (isSelected) {
            block.setEffect(new javafx.scene.effect.DropShadow(12, Color.web("#ffd166", 0.85)));
            block.getStyleClass().addAll("ship-block", "ship-block-selected");
        } else {
            block.setEffect(new javafx.scene.effect.DropShadow(8, Color.web("#44b8ff", 0.4)));
            block.getStyleClass().add("ship-block");
        }

        block.setUserData(type);

        block.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (selectedShipType == type) {
                    clearSelection();
                } else {
                    selectShip(type);
                }
                event.consume();
            }
        });

        block.setOnDragDetected(event -> {
            Dragboard db = block.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(type.name());
            db.setContent(content);
            event.consume();
        });

        return block;
    }
}
