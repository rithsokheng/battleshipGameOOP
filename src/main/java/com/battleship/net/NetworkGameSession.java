package com.battleship.net;

import com.battleship.model.Player;
import com.battleship.model.Theater;

/**
 * Shared mutable state for one "Play With a Friend" match. Passed by reference
 * between the lobby, placement, and battle screens so they all see the same
 * connection, local player, and opponent tracker. Ammo lives on {@code me} itself
 * (Player.initLauncherAmmo) — no need to duplicate it here.
 */
public class NetworkGameSession {

    private final NetworkSession session;
    private final Theater theater;
    private final boolean isHost;
    private final Player me;
    private final EnemyTracker enemyTracker;
    private volatile boolean myTurn;

    public NetworkGameSession(NetworkSession session, Theater theater, boolean isHost,
                              Player me, EnemyTracker enemyTracker) {
        this.session = session;
        this.theater = theater;
        this.isHost = isHost;
        this.me = me;
        this.enemyTracker = enemyTracker;
    }

    public NetworkSession getSession() { return session; }
    public Theater getTheater() { return theater; }
    public boolean isHost() { return isHost; }
    public Player getMe() { return me; }
    public EnemyTracker getEnemyTracker() { return enemyTracker; }
    public boolean isMyTurn() { return myTurn; }

    /** Grants the local player the turn (after a START or an answered FIRE). */
    public void beginMyTurn() { this.myTurn = true; }

    /** Hands the turn to the remote opponent (after firing or when START says so). */
    public void beginOpponentTurn() { this.myTurn = false; }

    /** Applies the host's START decision: hostMovesFirst determines whose turn it is. */
    public void beginMatch(boolean hostMovesFirst) {
        this.myTurn = (isHost == hostMovesFirst);
    }
}
