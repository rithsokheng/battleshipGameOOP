package com.battleship.view;

import com.battleship.controller.GameController;
import com.battleship.model.Board;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Player;
import com.battleship.model.Theater;
import com.battleship.net.EnemyTracker;
import com.battleship.net.NetMessage;
import com.battleship.net.NetUtil;
import com.battleship.net.NetworkGameSession;
import com.battleship.net.NetworkSession;
import com.battleship.net.Role;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.security.SecureRandom;

/**
 * Hosts a LAN match: opens a socket, shows a scannable QR code (and the raw
 * text) that encodes this machine's IP, port, and a short join code, then
 * waits for a friend on the same network to connect.
 */
public class HostLobbyView {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ViewNavigator nav;
    private final GameController controller;
    private final Theater theater;
    private final String code;
    private final int port;
    private NetworkSession pendingSession;

    public HostLobbyView(ViewNavigator nav, GameController controller, Theater theater) {
        this.nav = nav;
        this.controller = controller;
        this.theater = theater;
        this.code = String.format("%04d", RANDOM.nextInt(10000));
        this.port = NetUtil.findFreePort();
        controller.setTheater(theater); // sets up selectedTheater so placement helpers work below
    }

    public StackPane build() {
        Label title = new Label("HOSTING GAME");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 28));
        title.getStyleClass().add("app-title");

        String ip = NetUtil.getLocalIp();
        String invite = "BATTLESHIP:" + ip + ":" + port + ":" + code;

        ImageView qrView = new ImageView(QrCodeUtil.generate(invite, 220));
        qrView.setFitWidth(220);
        qrView.setFitHeight(220);
        StackPane qrFrame = new StackPane(qrView);
        qrFrame.setPadding(new Insets(14));
        qrFrame.getStyleClass().add("qr-frame");

        Label howTo = new Label("Have your friend open Battleship, tap JOIN A GAME, then scan\n" +
                "this code with their phone camera to read the text below — or\n" +
                "just tell them the details out loud.");
        howTo.setWrapText(true);
        howTo.setMaxWidth(360);
        howTo.getStyleClass().add("dim-text");
        howTo.setAlignment(Pos.CENTER);
        howTo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label details = new Label("IP: " + ip + "    PORT: " + port + "    CODE: " + code);
        details.getStyleClass().add("accent-text");
        details.setStyle("-fx-font-size:14px;");

        Label status = new Label("Waiting for opponent to join\u2026");
        status.getStyleClass().add("accent-text");
        status.setStyle("-fx-font-size:13px;");

        Button cancel = new Button("CANCEL");
        cancel.getStyleClass().add("ghost-button");
        cancel.setPrefWidth(120);
        cancel.setOnAction(e -> {
            if (pendingSession != null) pendingSession.close();
            nav.showMultiplayerLobby();
        });

        VBox layout = new VBox(14, title, qrFrame, details, howTo, status, cancel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));

        StackPane root = new StackPane(layout);

        startHosting(status);
        return root;
    }

    private void startHosting(Label status) {
        pendingSession = NetworkSession.host(port,
                session -> {
                    status.setText("Opponent connecting\u2026 verifying code");
                    session.setOnMessage(msg -> handleHandshakeMessage(session, msg, status));
                },
                error -> {
                    status.setText("Could not host — try again.");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Hosting failed");
                    alert.setHeaderText(null);
                    alert.setContentText("Couldn't start hosting: " + error.getMessage());
                    alert.showAndWait();
                });
                },
                Platform::runLater);
    }

    private void handleHandshakeMessage(NetworkSession session, NetMessage msg, Label status) {
        if (!(msg instanceof NetMessage.Hello hello)) return;

        if (!code.equals(hello.code())) {
            session.send(new NetMessage.Reject("Wrong join code"));
            session.close();
            status.setText("A connection used the wrong code. Still waiting\u2026");
            startHosting(status);
            return;
        }

        NetMessage welcome = new NetMessage.Welcome(theater.name());
        session.send(welcome);

        int size = theater.getBoardSize();
        Player me = new Player("You (Host)", true, new Board(size));
        me.initLauncherAmmo(size);
        Player me = new HumanPlayer("You (Host)", theater);
        NetworkGameSession netSession = new NetworkGameSession(
                session, theater, Role.HOST, me, new EnemyTracker(size));
                session, theater, Role.HOST, me);

        nav.showNetworkShipPlacement(netSession);
    }
}
