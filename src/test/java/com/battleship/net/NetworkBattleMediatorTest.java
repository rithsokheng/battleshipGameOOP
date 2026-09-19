package com.battleship.net;

import com.battleship.model.Board;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkBattleMediatorTest {

    @Test
    void resolvesIncomingShotAndReportsOutcome() {
        Board board = new Board(7);
        board.placeShip(ShipType.PATROL_BOAT, new Coordinate(1, 1), Orientation.HORIZONTAL);
        Player me = new Player("Local Admiral", true, board);

        NetworkGameSession session = new NetworkGameSession(
                null, Theater.SKIRMISH, Role.CLIENT, me, new EnemyTracker(7));
        NetworkBattleMediator mediator = new NetworkBattleMediator(session);

        // Test miss
        NetMessage.Fire missFire = new NetMessage.Fire(LauncherType.DEFAULT, new Coordinate(0, 0), Orientation.HORIZONTAL);
        NetworkBattleMediator.IncomingFireOutcome missOutcome = mediator.resolveAndReply(missFire);
        assertFalse(missOutcome.anyHit());
        assertFalse(missOutcome.anySunk());
        assertFalse(missOutcome.lost());

        // Test hit 1
        NetMessage.Fire hitFire1 = new NetMessage.Fire(LauncherType.DEFAULT, new Coordinate(1, 1), Orientation.HORIZONTAL);
        NetworkBattleMediator.IncomingFireOutcome hitOutcome1 = mediator.resolveAndReply(hitFire1);
        assertTrue(hitOutcome1.anyHit());
        assertFalse(hitOutcome1.anySunk());
        assertFalse(hitOutcome1.lost());

        // Test hit 2 and sunk (Patrol boat is size 2: (1,1) and (1,2))
        NetMessage.Fire hitFire2 = new NetMessage.Fire(LauncherType.DEFAULT, new Coordinate(1, 2), Orientation.HORIZONTAL);
        NetworkBattleMediator.IncomingFireOutcome hitOutcome2 = mediator.resolveAndReply(hitFire2);
        assertTrue(hitOutcome2.anyHit());
        assertTrue(hitOutcome2.anySunk());
        assertTrue(hitOutcome2.lost()); // Only ship on board was sunk
    }
}

