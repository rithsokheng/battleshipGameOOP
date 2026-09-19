package com.battleship.net;

import com.battleship.controller.LauncherFireResult;
import com.battleship.controller.ShotResolution;
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
    /** Shot-resolution strategy, injectable for tests (fixes F5). */
    private final ShotResolution shotResolution;

    /** Production constructor — uses the standard shot resolver. */
    public NetworkBattleMediator(NetworkGameSession session) {
        this(session, ShotResolver.STANDARD);
    }

    /** Testable constructor — inject the shot-resolution strategy (DIP, fixes F5). */
    public NetworkBattleMediator(NetworkGameSession session, ShotResolution shotResolution) {
        this.session = session;
        this.me = session.getMe();
        this.shotResolution = shotResolution;
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
        LauncherFireResult resolution = shotResolution.resolve(
                me.getMutableBoard(), fire.launcherType(), fire.anchor(), fire.orientation());

        boolean lost = me.getMutableBoard().isAllShipsSunk();
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

