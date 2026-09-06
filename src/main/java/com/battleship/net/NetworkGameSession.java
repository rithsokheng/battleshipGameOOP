package com.battleship.net;

import com.battleship.model.Player;
import com.battleship.model.Theater;

/**
 * Shared mutable state for one "Play With a Friend" match. Passed by reference
 * between the lobby, placement, and battle screens so they all see the same
 * connection, local player, and opponent tracker. Ammo lives on `me` itself
 * (Player.initLauncherAmmo) — no need to duplicate it here.
 */
public class NetworkGameSession {

    public NetworkSession session;
    public Theater theater;
    public boolean isHost;
    public Player me;
    public EnemyTracker enemyTracker;
    public boolean myTurn;
}
