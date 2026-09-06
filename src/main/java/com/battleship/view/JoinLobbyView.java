package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Board;
import com.battleship.model.Player;
import com.battleship.model.Theater;
import com.battleship.net.EnemyTracker;
import com.battleship.net.NetMessage;
import com.battleship.net.NetworkGameSession;
import com.battleship.net.NetworkSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Joins a LAN match by pasting/typing the invite text a host's QR code
 * decodes to (format: {@code BATTLESHIP:<ip>:<port>:<code>}).
 */
public class JoinLobbyView {

    private final MainApp app;
    private final GameController controller;
    private Label status;

    public JoinLobbyView(MainApp app, GameController controller) {
        this.app = app;
        this.controller = controller;
    }

    public StackPane build() {
        Label title = new Label("JOIN A GAME");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 28));
        title.getStyleClass().add("app-title");

        Label hint = new Label("Scan your friend's QR with your phone camera app — most phones\n" +
                "show the decoded text you can copy. Paste it below, or type it in manually.");
        hint.setWrapText(true);
        hint.setMaxWidth(380);
        hint.getStyleClass().add("dim-text");
        hint.setAlignment(Pos.CENTER);
        hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        TextField inviteField = new TextField();
        inviteField.setPromptText("BATTLESHIP:192.168.1.23:55123:4821");
        inviteField.setPrefWidth(360);
        inviteField.getStyleClass().add("dark-field");

        status = new Label("");
        status.getStyleClass().add("accent-text");
        status.setStyle("-fx-font-size:12px;");

        Button connect = new Button("CONNECT");
        connect.setPrefWidth(200);
        connect.setPrefHeight(46);
        connect.getStyleClass().add("primary-button");
        connect.setOnAction(e -> attemptConnect(inviteField.getText().trim()));

        Button back = new Button("BACK");
        back.getStyleClass().add("ghost-button");
        back.setPrefWidth(120);
        back.setOnAction(e -> app.showMultiplayerLobby());

        VBox layout = new VBox(14, title, hint, inviteField, connect, status, back);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));
        VBox.setMargin(back, new Insets(10, 0, 0, 0));

        return new StackPane(layout);
    }

    private void attemptConnect(String invite) {
        String[] parts = invite.split(":");
        if (parts.length != 4 || !"BATTLESHIP".equalsIgnoreCase(parts[0])) {
            showError("That doesn't look like a valid invite code. Expected format:\nBATTLESHIP:<ip>:<port>:<code>");
            return;
        }
        String ip = parts[1];
        String code = parts[3];
        int port;
        try {
            port = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            showError("Invalid port in invite code.");
            return;
        }

        status.setText("Connecting to " + ip + ":" + port + "\u2026");
        NetworkSession.connect(ip, port,
                session -> {
                    status.setText("Connected — sending join code\u2026");
                    session.setOnMessage(msg -> handleWelcome(session, msg));
                    NetMessage hello = NetMessage.of("HELLO");
                    hello.code = code;
                    session.send(hello);
                },
                error -> showError("Couldn't connect: " + error.getMessage()));
    }

    private void handleWelcome(NetworkSession session, NetMessage msg) {
        if (msg == null) return;
        if ("REJECT".equals(msg.type)) {
            session.close();
            showError("Host rejected the connection (wrong code?).");
            return;
        }
        if (!"WELCOME".equals(msg.type)) return;

        Theater theater = Theater.valueOf(msg.theater);
        int size = theater.getBoardSize();
        controller.setTheater(theater); // sets up selectedTheater so placement helpers work below

        NetworkGameSession netSession = new NetworkGameSession();
        netSession.session = session;
        netSession.theater = theater;
        netSession.isHost = false;
        netSession.me = new Player("You", true, new Board(size));
        netSession.me.initLauncherAmmo(size);
        netSession.enemyTracker = new EnemyTracker(size);

        app.setScreen(new NetworkShipPlaceView(app, controller, netSession).build());
    }

    private void showError(String message) {
        status.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection problem");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
