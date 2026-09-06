package com.battleship.persistence;

import java.util.List;

/**
 * Flat, Gson-friendly representation of a full game save, matching the
 * JSON schema in the spec (version, timestamp, boardSize, players, turnHistory...).
 */
public class GameSaveDTO {
    public String version;
    public String timestamp;
    public int boardSize;
    public String gameState;
    public PlayerDTO player1;
    public PlayerDTO player2;
    public List<TurnRecordDTO> turnHistory;
    public int currentPlayerIndex;

    public static class PlayerDTO {
        public String name;
        public boolean isHuman;
        public String[][] ownBoard;
        public List<ShipDTO> ships;
    }

    public static class ShipDTO {
        public String type;
        public int hits;
        public List<String> coordinates; // algebraic notation
    }

    public static class TurnRecordDTO {
        public String shooter;
        public String target;
        public String coordinate;
        public String result;
        public String shipSunk;
    }
}
