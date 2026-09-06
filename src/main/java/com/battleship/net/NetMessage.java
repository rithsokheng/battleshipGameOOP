package com.battleship.net;

import java.util.List;

/**
 * Wire format for all host&lt;-&gt;client messages: one JSON object per line,
 * serialized/deserialized with Gson. Only the fields relevant to `type` are
 * populated for any given message; the rest stay null/default.
 *
 * Message types:
 *  HELLO        client -> host    : {code}                 first message after TCP connect
 *  WELCOME      host -> client    : {theater}               accepted; battlefield size to use
 *  REJECT       host -> client    : {reason}                bad code / host busy; connection will close
 *  READY        either direction  : (no fields)              sender has finished ship placement
 *  START        host -> client    : {firstPlayer}            "HOST" or "CLIENT" goes first
 *  FIRE         attacker -> defender : {launcherType, anchorRow, anchorCol, horizontal}
 *  FIRE_RESULT  defender -> attacker : {results, sunkShips, defenderLost}
 */
public class NetMessage {

    public String type;

    // HELLO
    public String code;

    // WELCOME
    public String theater;

    // REJECT
    public String reason;

    // START
    public String firstPlayer;

    // FIRE
    public String launcherType;
    public int anchorRow;
    public int anchorCol;
    public boolean horizontal;

    // FIRE_RESULT
    public List<CellResult> results;
    public List<SunkInfo> sunkShips;
    public boolean defenderLost;

    public static NetMessage of(String type) {
        NetMessage m = new NetMessage();
        m.type = type;
        return m;
    }

    /** One resolved cell from a FIRE_RESULT: outcome is HIT, MISS, or SUNK. */
    public static class CellResult {
        public int row;
        public int col;
        public String outcome;
    }

    /** A ship that was sunk by a FIRE_RESULT, with every cell it occupied. */
    public static class SunkInfo {
        public String shipType;
        public List<int[]> cells; // each entry is {row, col}
    }
}
