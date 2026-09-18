package com.battleship.controller;

import com.battleship.model.Board;
import com.battleship.model.CellStatus;
import com.battleship.model.Coordinate;
import com.battleship.model.LauncherType;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the refactored turn/ammo flow: Turn enum, Player ammo delegates, ShotResolver. */
class BattleServiceTest {

    /** Builds a local battle; forces the given player to be on turn (via a harmless miss if not). */
    private BattleService initFor(Player first, Player p1, Player p2) {
        BattleService service = new BattleService();
        service.init(p1, p2, null);
        service.rollInitiative();
        if (service.getCurrentPlayer() != first) {
            service.getCurrentPlayer().prepareShot(LauncherType.DEFAULT, Orientation.HORIZONTAL);
            service.fire(new Coordinate(9, 9)); // harmless miss; hands the turn over
            assertFalse(service.isBattleOver());
        }
        return service;
    }

    @Test
    void defaultShotMissesConsumesInfiniteAmmoAndAdvancesTurn() {
        Player p1 = new Player("P1", true, new Board(10));
        Player p2 = new Player("P2", true, new Board(10));
        p1.initLauncherAmmo(10);
        p2.initLauncherAmmo(10);

        BattleService service = initFor(p1, p1, p2);
        p1.prepareShot(LauncherType.DEFAULT, Orientation.HORIZONTAL);
        Player starter = service.getCurrentPlayer();

        LauncherFireResult result = service.fire(new Coordinate(4, 4));

        assertEquals(1, result.results().size());
        assertEquals(CellStatus.MISS, result.results().get(0).outcome());
        // Infinite ammo must be untouched (V1: consume via Player delegate).
        assertEquals(Integer.MAX_VALUE, starter.getAmmoCount(LauncherType.DEFAULT));
        // Launcher resets after each shot (rule 1).
        assertEquals(LauncherType.DEFAULT, starter.getSelectedLauncher());
        // Turn advanced.
        assertNotEquals(starter, service.getCurrentPlayer());
        assertFalse(service.isBattleOver());
    }

    @Test
    void nuclearShotSinksFleetEndsBattleAndKeepsTurnWithWinner() {
        Player p1 = new Player("P1", true, new Board(10));
        Player p2 = new Player("P2", true, new Board(10));
        p1.initLauncherAmmo(10);
        p2.initLauncherAmmo(10);
        assertTrue(p2.getOwnBoard().placeShip(ShipType.PATROL_BOAT, new Coordinate(0, 0), Orientation.HORIZONTAL));

        BattleService service = initFor(p1, p1, p2);
        p1.prepareShot(LauncherType.NUCLEAR, Orientation.HORIZONTAL);

        LauncherFireResult result = service.fire(new Coordinate(0, 0));

        assertTrue(result.results().stream().anyMatch(r -> r.outcome() == CellStatus.SUNK));
        assertEquals(1, result.sunkShips().size());
        assertTrue(p2.hasLost());
        assertTrue(service.isBattleOver());
        // Ammo was consumed through the delegate; launcher reset.
        assertEquals(0, p1.getAmmoCount(LauncherType.NUCLEAR));
        assertEquals(LauncherType.DEFAULT, p1.getSelectedLauncher());
        // Turn stays with the winner so game-over reporting names the right player.
        assertEquals(p1, service.getCurrentPlayer());
    }

    @Test
    void playerAmmoDelegatesNeverExposeTheMutableInventory() {
        Player p = new Player("P", true, new Board(8));
        p.initLauncherAmmo(8);
        // Encapsulation (V1): only read/delegate access; consume & resupply go through the Player.
        assertEquals(1, p.getAmmoCount(LauncherType.NUCLEAR));
        p.consumeAmmo(LauncherType.NUCLEAR);
        assertFalse(p.hasAmmo(LauncherType.NUCLEAR));
        p.resupplyAmmo(LauncherType.NUCLEAR, 1);
        assertTrue(p.hasAmmo(LauncherType.NUCLEAR));
        assertTrue(p.isAmmoInfinite(LauncherType.DEFAULT));
    }

    @Test
    void aiPlanIsReadOnlyOverPlayerAmmo() {
        Player p = new Player("AI", false, new Board(10));
        p.initLauncherAmmo(10);
        com.battleship.ai.AIStrategy ai = new com.battleship.ai.HuntTargetAI();

        com.battleship.ai.AiShotPlan plan = ai.chooseShotPlan(new Board(10), p);

        // Planning must never mutate ammo — the AI only reads through delegates (V1).
        assertEquals(3, p.getAmmoCount(LauncherType.LEVEL_2));
        assertEquals(1, p.getAmmoCount(LauncherType.NUCLEAR));
        assertTrue(plan.type() == LauncherType.DEFAULT || plan.type() == LauncherType.LEVEL_2);
        assertTrue(plan.anchor().isWithinBounds(10));
    }
}
