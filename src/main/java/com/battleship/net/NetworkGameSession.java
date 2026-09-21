package com.battleship.net;

import com.battleship.model.Player;
import com.battleship.model.Theater;
import com.battleship.model.fog.TrackingGrid;

/**
 * Shared mutable state for one "Play With a Friend" match. Passed by reference
 * between the lobby, placement, and battle screens so they all see the same
 * connection, local player, and enemy knowledge.
 *
 * <p>The bespoke {@code EnemyTracker} is gone (Smell 5.2): a network admiral now
 * keeps their observations in the same {@link TrackingGrid} the local game and
 * the AI use, so there is one model of reality instead of two. Ammunition lives on
 * {@code me} (its {@code Arsenal}) — no need to duplicate it here.</p>
 */
public class NetworkGameSession {

    private final NetworkSession session;
    private final Theater theater;
    private final Role role;
    private final Player me;
    private volatile boolean myTurn;

    public NetworkGameSession(NetworkSession session, Theater theater, Role role, Player me) {
        this.session = session;
        this.theater = theater;
        this.role = role;
        this.me = me;
    }

    public NetworkSession getSession() { return session; }
    public Theater getTheater() { return theater; }
    public Role getRole() { return role; }
    public boolean isHost() { return role == Role.HOST; }
    public Player getMe() { return me; }

    /** What this admiral knows about the enemy — never the enemy's real grid. */
    public TrackingGrid getEnemyKnowledge() { return me.trackingGrid(); }

    public boolean isMyTurn() { return myTurn; }

    /** Grants the local player the turn (after a START or an answered FIRE). */
    public void beginMyTurn() { this.myTurn = true; }

    /** Hands the turn to the remote opponent (after firing or when START says so). */
    public void beginOpponentTurn() { this.myTurn = false; }

    /** Rich domain-action alias for {@link #beginMyTurn()}. */
    public void passTurnToMe() { beginMyTurn(); }

    /** Rich domain-action alias for {@link #beginOpponentTurn()}. */
    public void passTurnToOpponent() { beginOpponentTurn(); }

    /** Checks whether the local player is currently allowed to fire/act. */
    public boolean canAct() {
        return myTurn && !isGameOver();
    }

    /** Returns true if either fleet has been completely destroyed. */
    public boolean isGameOver() {
        return me.isFleetDestroyed() || getEnemyKnowledge().isFleetFullyAccountedFor();
    }

    /** Applies the host's START decision: hostMovesFirst determines whose turn it is. */
    public void beginMatch(boolean hostMovesFirst) {
        this.myTurn = (isHost() == hostMovesFirst);
    }
}
