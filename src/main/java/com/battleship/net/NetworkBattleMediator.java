package com.battleship.net;

import com.battleship.controller.LauncherFireResult;
import com.battleship.controller.ShotResolver;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.model.ShotResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the controller-level logic for a network battle (fixes V7 / SRP),
 * keeping the View free from DTO construction and network I/O.
 */
public class NetworkBattleMediator {

    private final NetworkGameSession session;
    private final Player me;

    public NetworkBattleMediator(NetworkGameSession session) {
        this.session = session;
        this.me = session.getMe();
    }

    public record IncomingFireOutcome(
            LauncherFireResult resolution,
            boolean lost,
            boolean anyHit,
            boolean anySunk) {}

    /**
     * Resolves an incoming shot against the local board, formats and dispatches
     * the FireResult reply message across the network session, and returns the outcome.
     */
    public IncomingFireOutcome resolveAndReply(NetMessage.Fire fire) {
        LauncherFireResult resolution = ShotResolver.resolve(
                me.getOwnBoard(), fire.launcherType(), fire.anchor(), fire.orientation());

        boolean lost = me.getOwnBoard().isAllShipsSunk();
        boolean anyHit = resolution.results().stream().anyMatch(ShotResult::isHit);
        boolean anySunk = !resolution.sunkShips().isEmpty();

        List<NetMessage.CellResult> cellResults = new ArrayList<>();
        for (ShotResult r : resolution.results()) {
            cellResults.add(new NetMessage.CellResult(r.coordinate(), r.outcome()));
        }
        List<NetMessage.SunkShipInfo> sunkInfos = new ArrayList<>();
        for (Ship s : resolution.sunkShips()) {
            sunkInfos.add(new NetMessage.SunkShipInfo(s.getType(), s.getOccupiedCells()));
        }
        if (session.getSession() != null) {
            session.getSession().send(new NetMessage.FireResult(cellResults, sunkInfos, lost));
        }

        return new IncomingFireOutcome(resolution, lost, anyHit, anySunk);
    }
}

