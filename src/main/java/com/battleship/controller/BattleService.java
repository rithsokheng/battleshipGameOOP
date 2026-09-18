package com.battleship.controller;

import com.battleship.ai.AiShotPlan;
import com.battleship.ai.AIStrategy;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Player;
import com.battleship.model.Ship;
import com.battleship.model.ShotResult;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private int currentPlayerIndex;

    /** True only between a fire() that ended the match and the controller reacting to it. */
    private boolean battleOver;

    public void init(Player player1, Player player2, AIStrategy aiStrategy) {
        this.player1 = player1;
        this.player2 = player2;
        this.aiStrategy = aiStrategy;
        this.currentPlayerIndex = 0;
        this.battleOver = false;
    }

    /** Cryptographically fair coin flip determines who fires first. */
    public Player rollInitiative() {
        currentPlayerIndex = RANDOM.nextBoolean() ? 0 : 1;
        return getCurrentPlayer();
    }

    public Player getCurrentPlayer() { return currentPlayerIndex == 0 ? player1 : player2; }
    public Player getOpponent() { return currentPlayerIndex == 0 ? player2 : player1; }

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
        return player.getAmmo().getAmmo(type);
    }

    /**
     * Fires the current player's selected launcher, anchored at the given cell.
     * Already-shot and out-of-bounds cells within the pattern are skipped, but
     * ammo is still consumed once for the whole shot. Advances the turn afterward
     * (unless the defender just lost).
     */
    public LauncherFireResult fire(Coordinate anchor) {
        battleOver = false;
        Player attacker = getCurrentPlayer();
        Player defender = getOpponent();
        LauncherType type = attacker.getSelectedLauncher();
        int size = defender.getOwnBoard().getSize();

        List<Coordinate> cells = type.getTargetCells(anchor, attacker.getLauncherOrientation());
        List<ShotResult> results = new ArrayList<>();
        LinkedHashSet<Ship> sunk = new LinkedHashSet<>();

        for (Coordinate c : cells) {
            if (!c.isWithinBounds(size)) continue;
            CellStatus existing = defender.getOwnBoard().getCellStatus(c);
            if (existing == CellStatus.HIT || existing == CellStatus.MISS || existing == CellStatus.SUNK) continue;
            ShotResult r = defender.getOwnBoard().receiveShot(c);
            results.add(r);
            if (r.outcome() == CellStatus.SUNK) sunk.add(r.shipSunk());
        }

        attacker.getAmmo().consume(type);
        attacker.resetLauncherAfterShot(); // must actively re-select each turn (rule 1)

        // The AI brain learns from its own shots only (polymorphic notification).
        if (aiStrategy != null && attacker == player2) {
            for (ShotResult r : results) aiStrategy.notifyResult(r);
        }

        if (defender.hasLost()) {
            battleOver = true; // turn stays with the winner for game-over reporting
        } else {
            currentPlayerIndex = 1 - currentPlayerIndex;
        }
        return new LauncherFireResult(results, new ArrayList<>(sunk));
    }

    /** Has the AI choose a weapon + target, applies the selection, and fires it. */
    public LauncherFireResult fireAiLauncher() {
        AiShotPlan plan = aiStrategy.chooseShotPlan(player1.getOwnBoard(), player2.getAmmo());
        player2.prepareShot(plan.type(), plan.orientation());
        return fire(plan.anchor());
    }

    public boolean isBattleOver() { return battleOver; }
}