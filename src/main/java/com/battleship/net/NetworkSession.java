package com.battleship.net;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Thin TCP transport for a single host&lt;-&gt;client connection: one JSON
 * {@link NetMessage} per line. Connection setup and the blocking read loop run
 * on a background daemon thread; every callback is marshalled back onto the
 * JavaFX application thread via {@link Platform#runLater}.
 */
public class NetworkSession {

    private final NetMessageCodec codec = new NetMessageCodec();
    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;

    private Consumer<NetMessage> onMessage;
    private Runnable onDisconnected;

    private NetworkSession() { }

    /** Opens a listening socket on {@code port} and waits for exactly one peer to connect. */
    public static NetworkSession host(int port, Consumer<NetworkSession> onClientConnected, Consumer<Exception> onError) {
        NetworkSession session = new NetworkSession();
        Thread t = new Thread(() -> {
            try {
                session.serverSocket = new ServerSocket(port);
                Socket client = session.serverSocket.accept();
                session.attach(client);
                Platform.runLater(() -> onClientConnected.accept(session));
                session.listenLoop();
            } catch (Exception ex) {
                if (session.running) Platform.runLater(() -> onError.accept(ex));
            }
        }, "battleship-net-host");
        t.setDaemon(true);
        t.start();
        return session;
    }

    /** Connects out to a host's IP/port. */
    public static void connect(String host, int port, Consumer<NetworkSession> onConnected, Consumer<Exception> onError) {
        Thread t = new Thread(() -> {
            NetworkSession session = new NetworkSession();
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 8000);
                session.attach(socket);
                Platform.runLater(() -> onConnected.accept(session));
                session.listenLoop();
            } catch (Exception ex) {
                Platform.runLater(() -> onError.accept(ex));
            }
        }, "battleship-net-client");
        t.setDaemon(true);
        t.start();
    }

    private void attach(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private void listenLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                NetMessage msg = codec.decode(line);
                if (onMessage != null) {
                    Platform.runLater(() -> onMessage.accept(msg));
                }
            }
        } catch (IOException ignored) {
            // socket closed locally, or connection dropped by the peer
        } finally {
            running = false;
            if (onDisconnected != null) Platform.runLater(onDisconnected);
        }
    }

    public void send(NetMessage msg) {
        if (out != null) out.println(codec.encode(msg));
    }

    public void setOnMessage(Consumer<NetMessage> onMessage) { this.onMessage = onMessage; }
    public void setOnDisconnected(Runnable onDisconnected) { this.onDisconnected = onDisconnected; }

    public void close() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
    }
}
