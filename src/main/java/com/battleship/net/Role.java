package com.battleship.net;

/**
 * Which side of a network match this session belongs to — replaces the raw
 * {@code boolean isHost} flag (fixes P3, primitive obsession). An enum makes
 * the two roles explicit and extensible (e.g. a future SPECTATOR role).
 */
public enum Role {
    /** Created the match and owns the listening socket. */
    HOST,
    /** Connected to an existing match via an invite code. */
    CLIENT
}
