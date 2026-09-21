package com.battleship.model.weapon;

/**
 * Six-cell area warhead (2x3): one round per battle, available on every
 * battlefield size, and gated behind launch-code authorization in the UI.
 */
public final class NuclearWarhead implements Weapon {

    public static final String ID = "NUCLEAR";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Nuclear";
    }

    @Override
    public int startingAmmo(int boardSize) {
        return 1;
    }

    @Override
    public boolean availableFor(int boardSize) {
        return true;
    }

    @Override
    public boolean hasInfiniteAmmo() {
        return false;
    }

    @Override
    public BlastPattern blastPattern() {
        return BlastPattern.of(2, 3); // 2x3 horizontally, 3x2 vertically
    }

    @Override
    public boolean requiresAuthorization() {
        return true;
    }

    @Override
    public void playFiringSound(com.battleship.view.SfxAudio audio) {
        audio.playNuclear();
    }
}

