package com.battleship.view;

/**
 * Background-music control. Split out of {@link GameAudio} so a client that only
 * starts/stops BGM can depend on just this slice (ISP).
 */
public interface MusicAudio {

    void playMenuMusic();
    void playBattleMusic();
    void stopBgm();
}