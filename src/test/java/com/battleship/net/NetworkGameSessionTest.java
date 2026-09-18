package com.battleship.net;

import com.battleship.model.Board;
import com.battleship.model.Player;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Role enum replaces the raw boolean isHost flag (P3). */
class NetworkGameSessionTest {

    private NetworkGameSession sessionFor(Role role) {
        Player me = new Player("Me", true, new Board(7));
        return new NetworkGameSession(null, Theater.SKIRMISH, role, me, new EnemyTracker(7));
    }

    @Test
    void hostRoleBehaviour() {
        NetworkGameSession host = sessionFor(Role.HOST);
        assertTrue(host.isHost());
        assertTrue(host.getRole() == Role.HOST);
        host.beginMatch(true);
        assertTrue(host.isMyTurn());
        host.beginMatch(false);
        assertFalse(host.isMyTurn());
    }

    @Test
    void clientRoleBehaviour() {
        NetworkGameSession client = sessionFor(Role.CLIENT);
        assertFalse(client.isHost());
        client.beginMatch(true);
        assertFalse(client.isMyTurn());
        client.beginMatch(false);
        assertTrue(client.isMyTurn());
    }
}
