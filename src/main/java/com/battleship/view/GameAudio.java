package com.battleship.view;

/**
 * Abstraction for game audio (fixes C2). Views depend on this interface and
 * receive it via {@link ViewNavigator#getAudio()} instead of calling the
 * static {@code SoundManager} singleton — so a silent stub can be injected in
 * tests and the sound implementation stays swappable.
 */
public interface GameAudio {

    // ---------- Sound effects ----------
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

    // ---------- Background music ----------
    void playMenuMusic();
    void playBattleMusic();
    void stopBgm();

    // ---------- Volume / mute (options screen) ----------
    void setMasterVolume(double v);
    double getMasterVolume();
    void setSfxVolume(double v);
    double getSfxVolume();
    void setMuted(boolean m);
    boolean isMuted();
    void toggleMute();
}
