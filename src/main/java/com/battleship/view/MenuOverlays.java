package com.battleship.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Builds the two modal overlays reachable from the Main Menu: the
 * "Tactical Manual" (How To Play) and the "Operational Controls" (Options)
 * panels. Both are self-contained StackPanes meant to be stacked on top of
 * whatever screen is currently showing, dismissed by removing them again.
 */
final class MenuOverlays {

    private MenuOverlays() { }

    // ---------------------------------------------------------------
    // HOW TO PLAY
    // ---------------------------------------------------------------

    static StackPane howToPlay(GameAudio audio, Runnable onClose) {
        Label title = new Label("HOW TO PLAY: TACTICAL MANUAL");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 22));
        title.getStyleClass().add("overlay-title");

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(22);
        grid.add(manualBlock("1. SETUP: PLACE YOUR FLEET",
                "Drag ships from the Dock onto your grid.\nRight-click or press 'R' to rotate."), 0, 0);
        grid.add(manualBlock("2. SELECT TARGET GRID",
                "Switch views between YOUR FLEET\nand ENEMY WATERS."), 1, 0);
        grid.add(manualBlock("3. FIRE & SPECIAL AMMO",
                "Click a cell in Enemy Waters to fire.\nCheck special ammo limits before use."), 0, 1);
        grid.add(manualBlock("4. TURN & VICTORY",
                "Turns alternate between admirals.\nDestroy all enemy ships to win."), 1, 1);

        Button close = new Button("CLOSE");
        close.getStyleClass().add("primary-button");
        close.setPrefWidth(160);
        close.setPrefHeight(42);
        close.setOnAction(e -> { audio.playClick(); onClose.run(); });

        VBox content = new VBox(22, title, divider(), grid, close);
        content.setAlignment(Pos.CENTER);
        VBox.setMargin(close, new Insets(6, 0, 0, 0));

        VBox card = wrapCard(content, 620);
        return backdrop(card);
    }

    private static VBox manualBlock(String heading, String body) {
        Label h = new Label(heading);
        h.getStyleClass().add("overlay-block-title");
        Label b = new Label(body);
        b.getStyleClass().add("info-text");
        b.setWrapText(true);
        VBox box = new VBox(8, h, b);
        box.setMaxWidth(240);
        return box;
    }

    // ---------------------------------------------------------------
    // OPTIONS
    // ---------------------------------------------------------------

    static StackPane options(GameAudio audio, Runnable onClose) {
        Label title = new Label("SETTINGS: OPERATIONAL CONTROLS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 22));
        title.getStyleClass().add("overlay-title");

        // --- Audio & Communications ---
        Label audioHeading = sectionHeading("1. AUDIO & COMMUNICATIONS");
        Slider master = themedSlider(audio.getMasterVolume() * 100);
        Slider sfx = themedSlider(audio.getSfxVolume() * 100);
        ToggleButton muteAll = new ToggleButton(audio.isMuted() ? "MUTED" : "MUTE ALL");
        muteAll.getStyleClass().add("switch-toggle");
        muteAll.setSelected(audio.isMuted());

        master.valueProperty().addListener((obs, old, val) ->
                audio.setMasterVolume(val.doubleValue() / 100));
        sfx.valueProperty().addListener((obs, old, val) ->
                audio.setSfxVolume(val.doubleValue() / 100));
        muteAll.setOnAction(e -> {
            boolean m = muteAll.isSelected();
            audio.setMuted(m);
            muteAll.setText(m ? "MUTED" : "MUTE ALL");
        });

        GridPane audioGrid = new GridPane();
        audioGrid.setHgap(16);
        audioGrid.setVgap(10);
        audioGrid.add(rowLabel("\uD83D\uDD0A  MASTER VOLUME"), 0, 0);
        audioGrid.add(master, 1, 0);
        audioGrid.add(rowLabel("\uD83C\uDFA7  SFX VOLUME"), 0, 1);
        audioGrid.add(sfx, 1, 1);
        GridPane.setHgrow(master, Priority.ALWAYS);
        GridPane.setHgrow(sfx, Priority.ALWAYS);
        master.setMaxWidth(Double.MAX_VALUE);
        sfx.setMaxWidth(Double.MAX_VALUE);

        HBox audioRow = new HBox(24, audioGrid, muteAll);
        audioRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(audioGrid, Priority.ALWAYS);

        // --- Graphics & Interface ---
        Label gfxHeading = sectionHeading("2. GRAPHICS & INTERFACE");
        ToggleGroup resGroup = new ToggleGroup();
        HBox resRow = new HBox(10, rowLabel("\uD83D\uDDA5  RESOLUTION"),
                segmentedGroupWithToggle(resGroup, "1160x740", "1360x820", "Fullscreen"));
        ToggleGroup scaleGroup = new ToggleGroup();
        HBox scaleRow = new HBox(10, rowLabel("\uD83C\uDFA8  UI SCALE"),
                segmentedGroupWithToggle(scaleGroup, "Standard", "Large"));
        ToggleGroup animGroup = new ToggleGroup();
        HBox animRow = new HBox(10, rowLabel("\u2728  ANIMATIONS"),
                segmentedGroupWithToggle(animGroup, "On", "Off"));
        VBox gfxBox = new VBox(10, resRow, scaleRow, animRow);

        // --- Controls & Shortcuts ---
        Label ctrlHeading = sectionHeading("3. CONTROLS & SHORTCUTS");
        HBox ctrlRow = new HBox(28,
                keybindPair("\u2328  FIRE", "CLICK"),
                keybindPair("\uD83D\uDDB1  ROTATE", "R / R-CLICK"),
                keybindPair("\uD83D\uDEE5  REMOVE SHIP", "CLICK SHIP"));

        Button save = new Button("SAVE & APPLY");
        save.getStyleClass().addAll("primary-button", "featured-button");
        save.setPrefWidth(180);
        save.setPrefHeight(42);

        Button close = new Button("CLOSE");
        close.getStyleClass().add("ghost-button");
        close.setPrefWidth(140);
        close.setPrefHeight(42);
        close.setOnAction(e -> { audio.playClick(); onClose.run(); });

        HBox buttons = new HBox(16, save, close);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(16,
                title, divider(),
                audioHeading, audioRow, divider(),
                gfxHeading, gfxBox, divider(),
                ctrlHeading, ctrlRow,
                buttons);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setFillWidth(true);
        VBox.setMargin(buttons, new Insets(8, 0, 0, 0));
        buttons.setAlignment(Pos.CENTER);
        title.setAlignment(Pos.CENTER);
        VBox.setMargin(title, new Insets(0, 0, 0, 0));

        VBox card = wrapCard(content, 620);

        save.setOnAction(e -> {
            audio.playClick();
            if (card.getScene() != null && card.getScene().getWindow() instanceof javafx.stage.Stage stage) {
                if (resGroup.getSelectedToggle() != null) {
                    String chosenRes = (String) resGroup.getSelectedToggle().getUserData();
                    if ("1360x820".equals(chosenRes)) {
                        stage.setFullScreen(false);
                        stage.setWidth(1360);
                        stage.setHeight(820);
                        stage.centerOnScreen();
                    } else if ("Fullscreen".equals(chosenRes)) {
                        stage.setFullScreen(true);
                    } else {
                        stage.setFullScreen(false);
                        stage.setWidth(1160);
                        stage.setHeight(740);
                        stage.centerOnScreen();
                    }
                }
            }
            onClose.run();
        });

        return backdrop(card);
    }

    private static HBox segmentedGroupWithToggle(ToggleGroup group, String... options) {
        HBox box = new HBox(6);
        for (int i = 0; i < options.length; i++) {
            ToggleButton tb = new ToggleButton(options[i]);
            tb.getStyleClass().add("segmented-toggle");
            tb.setToggleGroup(group);
            tb.setUserData(options[i]);
            if (i == 0) tb.setSelected(true);
            box.getChildren().add(tb);
        }
        return box;
    }

    private static Label sectionHeading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("overlay-block-title");
        return l;
    }

    private static Label rowLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("info-text");
        l.setPrefWidth(150);
        return l;
    }

    private static Slider themedSlider(double value) {
        Slider s = new Slider(0, 100, value);
        s.getStyleClass().add("themed-slider");
        return s;
    }

    private static HBox segmentedGroup(String... options) {
        ToggleGroup group = new ToggleGroup();
        HBox box = new HBox(6);
        for (int i = 0; i < options.length; i++) {
            ToggleButton tb = new ToggleButton(options[i]);
            tb.getStyleClass().add("segmented-toggle");
            tb.setToggleGroup(group);
            if (i == options.length - 1 && options.length > 2) tb.setSelected(true);
            else if (options.length == 2 && i == 0) tb.setSelected(true);
            box.getChildren().add(tb);
        }
        if (group.getSelectedToggle() == null && !box.getChildren().isEmpty()) {
            ((ToggleButton) box.getChildren().get(0)).setSelected(true);
        }
        return box;
    }

    private static VBox keybindPair(String label, String key) {
        Label l = new Label(label);
        l.getStyleClass().add("dim-text");
        Label k = new Label(key);
        k.getStyleClass().add("keybind-box");
        k.setPrefWidth(70);
        VBox box = new VBox(6, l, k);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ---------------------------------------------------------------
    // Shared chrome
    // ---------------------------------------------------------------

    private static Region divider() {
        Region r = new Region();
        r.getStyleClass().add("overlay-divider");
        r.setMaxWidth(Double.MAX_VALUE);
        return r;
    }

    private static VBox wrapCard(javafx.scene.Node content, double width) {
        VBox card = new VBox(content);
        card.getStyleClass().add("overlay-card");
        card.setMaxWidth(width);
        card.setAlignment(Pos.TOP_CENTER);
        return card;
    }

    private static StackPane backdrop(javafx.scene.Node card) {
        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("overlay-backdrop");
        StackPane.setAlignment(card, Pos.CENTER);
        return overlay;
    }
}
