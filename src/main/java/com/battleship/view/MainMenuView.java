package com.battleship.view;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Main menu dashboard (ISSUE 1). Shows PLAY / HOW TO PLAY / OPTIONS / EXIT only —
 * no game-mode buttons here. Vertical layout, centered, naval-themed buttons.
 */
public class MainMenuView {

    private final ViewNavigator nav;

    public MainMenuView(ViewNavigator nav) {
        this.nav = nav;
    }

    private StackPane root;

    public StackPane build() {
        Image logo = ImageResources.ui("logo-battleship");
        ImageView logoView = null;
        if (logo != null) {
            logoView = new ImageView(logo);
            logoView.setFitWidth(180);
            logoView.setPreserveRatio(true);
        }

        Label title = new Label("BATTLESHIP");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 58));
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("\u2693  NAVAL COMMAND  \u2693");
        subtitle.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 15));
        subtitle.getStyleClass().add("app-subtitle");

        Button play = navButton("\u25B6  PLAY");
        play.getStyleClass().addAll("primary-button", "featured-button");
        play.setOnAction(e -> { nav.getAudio().playClick(); nav.showModeSelect(); });

        Button howToPlay = navButton("\u2753  HOW TO PLAY");
        howToPlay.getStyleClass().add("ghost-button");
        howToPlay.setOnAction(e -> { nav.getAudio().playClick(); showHowToPlay(); });

        Button options = navButton("\u2699  OPTIONS");
        options.getStyleClass().add("ghost-button");
        options.setOnAction(e -> { nav.getAudio().playClick(); showOptions(); });

        Button exit = navButton("\u2716  EXIT");
        exit.getStyleClass().add("ghost-button");
        exit.setOnAction(e -> { nav.getAudio().playClick(); nav.getStage().close(); });

        VBox buttonBox = new VBox(14, play, howToPlay, options, exit);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(320);
        buttonBox.getStyleClass().add("card-panel");
        buttonBox.setPadding(new Insets(28, 26, 28, 26));

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(18);
        if (logoView != null) layout.getChildren().add(logoView);
        layout.getChildren().add(title);
        layout.getChildren().add(subtitle);
        layout.getChildren().add(buttonBox);
        VBox.setMargin(buttonBox, new Insets(34, 0, 0, 0));

        root = new StackPane();
        javafx.scene.canvas.Canvas ocean = DecorUtil.animatedOceanScene(root);
        root.getChildren().add(ocean);
        ImageView compass = DecorUtil.compassWatermark(420);
        if (compass != null) {
            compass.setOpacity(0.06);
            root.getChildren().add(compass);
        }
        root.getChildren().add(layout);

        // Gentle entrance so the menu doesn't just pop into place.
        layout.setOpacity(0.0);
        layout.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(420), layout);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), layout);
        rise.setToY(0);
        fade.play();
        rise.play();

        return root;
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(280);
        b.setPrefHeight(54);
        b.setMaxWidth(280);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        b.setOnMouseEntered(e -> { b.setScaleX(1.035); b.setScaleY(1.035); b.setTranslateY(-1); });
        b.setOnMouseExited(e -> { b.setScaleX(1.0); b.setScaleY(1.0); b.setTranslateY(0); });
        return b;
    }

    private void showHowToPlay() {
        StackPane overlay = MenuOverlays.howToPlay(nav.getAudio(),
                () -> root.getChildren().remove(root.getChildren().size() - 1));
        root.getChildren().add(overlay);
    }

    private void showOptions() {
        StackPane overlay = MenuOverlays.options(nav.getAudio(),
                () -> root.getChildren().remove(root.getChildren().size() - 1));
        root.getChildren().add(overlay);
    }
}
