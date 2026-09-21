package com.battleship.net;

import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.Orientation;
import com.battleship.model.ShipType;

import java.util.List;

/**
 * Type-safe wire protocol for host&lt;-&gt;client messages: each variant is its
 * own record carrying only the fields it needs (sealed hierarchy — the compiler
 * enforces exhaustiveness; stringly-typed dispatch like `case "BANANA"` is
 * impossible).
 *
 * <p>Wire format: one JSON object per line via {@link NetMessageCodec}, which adds
 * a "type" discriminator field for Gson transport.</p>
 *
 * <p>{@link Fire} carries a <em>weapon id</em> (a stable string owned by the weapon
 * strategy) rather than an enum constant, so a plugin weapon can travel the wire
 * without a new release of the protocol class (V3.1).</p>
 *
 * Message flow:
 *   HELLO        client -> host       first message after TCP connect (join code)
 *   WELCOME      host -> client       accepted; battlefield to use
 *   REJECT       host -> client       bad code / host busy; connection will close
 *   READY        either direction     sender has finished ship placement
 *   START        host -> client       "HOST" or "CLIENT" goes first
 *   FIRE         attacker -> defender weapon id, anchor cell and orientation
 *   FIRE_RESULT  defender -> attacker resolved cells, sunk ships, lost flag
 */
public sealed interface NetMessage {

    record Hello(String code) implements NetMessage { }

    record Welcome(String theater) implements NetMessage { }

    record Reject(String reason) implements NetMessage { }

    record Ready() implements NetMessage { }

    record Start(String firstPlayer) implements NetMessage { }

    record Fire(String weaponId, Coordinate anchor, Orientation orientation) implements NetMessage { }

    record FireResult(List<CellResult> results, List<SunkShipInfo> sunkShips, boolean defenderLost)
            implements NetMessage { }

    /** One resolved cell from a FIRE_RESULT. */
    record CellResult(Coordinate coordinate, CellStatus outcome) { }

    /** A ship that was sunk by a FIRE_RESULT, with every cell it occupied. */
    record SunkShipInfo(ShipType shipType, List<Coordinate> cells) { }
}
