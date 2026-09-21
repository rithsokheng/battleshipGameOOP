package com.battleship.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Thin TCP transport for a single host&lt;-&gt;client connection: one JSON
 * {@link NetMessage} per line. Connection setup and the blocking read loop run
 * on a background daemon thread; every callback is marshalled through the
 * caller-supplied {@link Executor} (fixes F6 + DIP).
 *
 * <p>Fixes the DIP audit finding: this class used to
 * {@code import javafx.application.Platform} and default to
 * {@code Platform::runLater}, which welded the transport layer to the JavaFX
 * toolkit — the class would not even link on a headless server, a CLI client or
 * a test without JavaFX on the classpath. The dispatcher is now a plain
 * {@link java.util.concurrent.Executor} injected by the caller: the JavaFX views
 * pass {@code Platform::runLater}, tests pass {@code Runnable::run}.</p>
 */
public class NetworkSession {

    private final NetMessageCodec codec = new NetMessageCodec();
    private final Executor dispatcher;

    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;

    private Consumer<NetMessage> onMessage;
    private Runnable onDisconnected;

    private NetworkSession(Executor dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "A thread dispatcher is required.");
    }

    private void dispatch(Runnable action) {
        dispatcher.execute(action);
    }

    /** Opens a listening socket on {@code port} and waits for exactly one peer to connect. */
    public static NetworkSession host(int port, Consumer<NetworkSession> onClientConnected,
                                      Consumer<Exception> onError, Executor dispatcher) {
        NetworkSession session = new NetworkSession(dispatcher);
        Thread t = new Thread(() -> {
            try {
                session.serverSocket = new ServerSocket(port);
                Socket client = session.serverSocket.accept();
                session.attach(client);
                session.dispatch(() -> onClientConnected.accept(session));
                session.listenLoop();
            } catch (Exception ex) {
                if (session.running) session.dispatch(() -> onError.accept(ex));
            }
        }, "battleship-net-host");
        t.setDaemon(true);
        t.start();
        return session;
    }

    /** Connects out to a host's IP/port, dispatching callbacks through the supplied executor. */
    public static void connect(String host, int port, Consumer<NetworkSession> onConnected,
                               Consumer<Exception> onError, Executor dispatcher) {
        Thread t = new Thread(() -> {
            NetworkSession session = new NetworkSession(dispatcher);
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 8000);
                session.attach(socket);
                session.dispatch(() -> onConnected.accept(session));
                session.listenLoop();
            } catch (Exception ex) {
                session.dispatch(() -> onError.accept(ex));
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
                    dispatch(() -> onMessage.accept(msg));
                }
            }
        } catch (IOException ignored) {
            // socket closed locally, or connection dropped by the peer
        } finally {
            running = false;
            if (onDisconnected != null) dispatch(onDisconnected);
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
