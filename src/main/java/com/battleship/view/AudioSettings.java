package com.battleship.view;

/**
 * Volume and mute control (options screen). Split out of {@link GameAudio} so a
 * settings screen can depend on just this slice (ISP).
 */
public interface AudioSettings {

    void setMasterVolume(double v);
    double getMasterVolume();
    void setSfxVolume(double v);
    double getSfxVolume();
    void setMuted(boolean m);
    boolean isMuted();
    void toggleMute();
}