package com.battleship.controller;

import com.battleship.ai.AiShotPlan;
import com.battleship.ai.AIStrategy;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Player;
import com.battleship.model.ShotResult;
import com.battleship.model.Turn;

import java.security.SecureRandom;
import java.util.List;

/**
 * Encapsulates turn management, launcher selection and the firing pipeline.
 * Extracted from GameController so the controller can stay a thin mediator (SRP).
 */
public class BattleService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Player player1;
    private Player player2;
    private AIStrategy aiStrategy;   // null in hotseat mode
    private Turn currentTurn;

    /** True only between a fire() that ended the match and the controller reacting to it. */
    private boolean battleOver;

    public void init(Player player1, Player player2, AIStrategy aiStrategy) {
        this.player1 = player1;
        this.player2 = player2;
        this.aiStrategy = aiStrategy;
        this.currentTurn = Turn.PLAYER_1;
        this.battleOver = false;
    }

    /** Cryptographically fair coin flip determines who fires first. */
    public Player rollInitiative() {
        currentTurn = RANDOM.nextBoolean() ? Turn.PLAYER_1 : Turn.PLAYER_2;
        return getCurrentPlayer();
    }

    public Player getCurrentPlayer() { return currentTurn == Turn.PLAYER_1 ? player1 : player2; }
    public Player getOpponent() { return currentTurn == Turn.PLAYER_1 ? player2 : player1; }

    public boolean isAiTurn() {
        return aiStrategy != null && getCurrentPlayer() == player2;
    }

    public boolean selectLauncher(Player player, LauncherType type, int boardSize) {
        return player.selectLauncher(type, boardSize);
    }

    public void toggleOrientation(Player player) {
        player.toggleLauncherOrientation();
    }

    public int getAmmoRemaining(Player player, LauncherType type) {
        return player.getAmmoCount(type);
    }

    /**
     * Fires the current player's selected launcher, anchored at the given cell.
     * The pattern resolution is delegated to the shared {@link ShotResolver};
     * this method owns only the turn-level concerns: ammo consumption, launcher
     * reset, AI feedback and turn advancement (unless the defender just lost).
     */
    public LauncherFireResult fire(Coordinate anchor) {
        battleOver = false;
        Player attacker = getCurrentPlayer();
        Player defender = getOpponent();
        LauncherType type = attacker.getSelectedLauncher();

        LauncherFireResult result = ShotResolver.resolve(
                defender.getOwnBoard(),
                type,
                anchor,
                attacker.getLauncherOrientation());

        attacker.consumeAmmo(type);          // DEFAULT is infinite -> no-op
        attacker.resetLauncherAfterShot();   // must actively re-select each turn (rule 1)

        // The AI brain learns from its own shots only (polymorphic notification).
        if (aiStrategy != null && attacker == player2) {
            for (ShotResult r : result.results()) aiStrategy.notifyResult(r);
        }

        if (defender.hasLost()) {
            battleOver = true; // turn stays with the winner for game-over reporting
        } else {
            currentTurn = currentTurn.next();
        }
        return result;
    }

    /** Has the AI choose a weapon + target, applies the selection, and fires it. */
    public LauncherFireResult fireAiLauncher() {
        AiShotPlan plan = aiStrategy.chooseShotPlan(player1.getOwnBoard(), player2);
        player2.prepareShot(plan.type(), plan.orientation());
        return fire(plan.anchor());
    }

    public boolean isBattleOver() { return battleOver; }
}
