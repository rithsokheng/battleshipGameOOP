package com.battleship.persistence;

import com.battleship.model.GameState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Handles serialization/deserialization of full game state to/from JSON,
 * per the save format in the spec (version, timestamp, boards, turnHistory...).
 */
public class SaveGameService {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Serializes the current game state and writes it to [timestamp]_battleship_save.json */
    public Path save(GameSaveDTO saveData, Path directory) throws IOException {
        // TODO: build filename with Instant.now() timestamp, write JSON via gson
        throw new UnsupportedOperationException("TODO");
    }

    /** Parses a save file back into DTOs for GameController reconstruction. */
    public GameSaveDTO load(Path file) throws IOException {
        // TODO: read file, gson.fromJson(...)
        throw new UnsupportedOperationException("TODO");
    }
}
