package com.battleship.view;

/**
 * One-shot sound effects. Split out of {@link GameAudio} so a client that only
 * triggers SFX can depend on just this slice (ISP), instead of on music and
 * volume control it never touches.
 */
public interface SfxAudio {

    void playClick();
    void playFire();
    void playHit();
    void playMiss();
    void playSunk();
    void playNuclear();
    void playPlaceShip();
    void playRemoveShip();
    void playTurnStart();
    /** Plays the appropriate end-of-match sting (victory or defeat). */
    void playGameOver(boolean won);
}