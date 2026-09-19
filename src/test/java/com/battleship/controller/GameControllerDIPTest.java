package com.battleship.controller;

import com.battleship.model.Coordinate;
import com.battleship.model.GameMode;
import com.battleship.model.Orientation;
import com.battleship.model.Player;
import com.battleship.model.ShipType;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerDIPTest {

    @Test
    void supportsConstructorInjection() {
        PlacementService customPlacement = new PlacementService();
        BattleService customBattle = new BattleService();

        GameController controller = new GameController(customPlacement, customBattle);
        assertNotNull(controller);
        assertEquals(com.battleship.model.GameState.MAIN_MENU, controller.getState());
    }

    @Test
    void removeShipAtEncapsulation() {
        GameController controller = new GameController();
        controller.setMode(GameMode.AI_EASY);
        controller.setTheater(Theater.SKIRMISH);

        Player player = controller.getPlayer1();
        Coordinate start = new Coordinate(0, 0);

        boolean placed = controller.placeShip(player, ShipType.PATROL_BOAT, start, Orientation.HORIZONTAL);
        assertTrue(placed);

        boolean removed = controller.removeShipAt(player, start);
        assertTrue(removed);

        // Verify removing at empty coordinate returns false cleanly without error
        boolean removedAgain = controller.removeShipAt(player, start);
        assertFalse(removedAgain);
    }
}

