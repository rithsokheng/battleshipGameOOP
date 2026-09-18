package com.battleship.view;

/**
 * Abstraction for game audio (fixes C2). Views depend on this interface and
 * receive it via {@link ViewNavigator#getAudio()} instead of calling the
 * static {@code SoundManager} singleton — so a silent stub can be injected in
 * tests (see {@link SilentAudio}) and the sound implementation stays swappable.
 *
 * <p>Fixes the ISP concern noted in review: instead of one 17-method blob, the
 * contract is the composition of three narrow roles — {@link SfxAudio},
 * {@link MusicAudio} and {@link AudioSettings} — which can also be injected
 * individually by clients that need only one of them.</p>
 */
public interface GameAudio extends SfxAudio, MusicAudio, AudioSettings {
}
