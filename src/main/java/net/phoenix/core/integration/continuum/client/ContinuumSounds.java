package net.phoenix.core.integration.continuum.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.phoenix.core.api.PhoenixSounds;

/**
 * Continuum's sound, kept in one place. One-shots reuse vanilla sounds at shifted pitches (so there are no audio
 * assets to ship yet); the engine rumble and the transit hum are Conflux's seamless ambience loops, faded in and out.
 * Swapping any of these for custom sounds later only touches this class.
 */
public final class ContinuumSounds {

    private ContinuumSounds() {}

    private static void play(SoundInstance sound) {
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void playLater(SoundInstance sound, int ticks) {
        Minecraft.getInstance().getSoundManager().playDelayed(sound, ticks);
    }

    /** A soft tick for any button, tab or selection. */
    public static void click() {
        play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.5f, 0.45f));
    }

    /** The launch itself: a firework whoosh under the engine rumble. */
    public static void liftoff() {
        play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.55f, 0.9f));
        play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 0.35f));
    }

    /** A small ping as a transit waypoint is passed. */
    public static void waypoint() {
        play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.get(), 1.7f, 0.35f));
    }

    /** A single soft chime: a new Archive entry. */
    public static void entry() {
        play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f, 0.6f));
        playLater(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.9f, 0.35f), 5);
    }

    /** A rising bell-like arpeggio on a successful arrival. */
    public static void chime() {
        float[] pitches = { 0.9f, 1.2f, 1.5f, 1.8f };
        int[] delays = { 0, 4, 8, 14 };
        for (int i = 0; i < pitches.length; i++) {
            SoundInstance note = SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitches[i], 0.8f);
            if (delays[i] == 0) play(note);
            else playLater(note, delays[i]);
        }
        playLater(SimpleSoundInstance.forUI(SoundEvents.BELL_BLOCK, 1.6f, 0.25f), 8);
    }

    /** Three low, flat pulses: a damaged rocket, or a mission that went wrong. */
    public static void warning() {
        for (int i = 0; i < 3; i++) {
            SoundInstance pulse = SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.get(), 0.5f, 0.9f);
            SoundInstance edge = SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.get(), 0.5f, 0.35f);
            if (i == 0) {
                play(pulse);
                play(edge);
            } else {
                playLater(pulse, i * 6);
                playLater(edge, i * 6);
            }
        }
    }

    /** The engine rumble: the Phoenix volcanic rumble loop. */
    public static Loop engineLoop() {
        return startLoop(PhoenixSounds.PHOENIX_VOLCANIC_RUMBLE.getMainEvent(), 0.0f);
    }

    /** The cruise hum: the void drone loop. */
    public static Loop humLoop() {
        return startLoop(PhoenixSounds.VOID_COSMIC_DRONE.getMainEvent(), 0.0f);
    }

    private static Loop startLoop(SoundEvent event, float volume) {
        Loop loop = new Loop(event, volume);
        play(loop);
        return loop;
    }

    /** A looping, non-positional sound whose volume eases toward a target. */
    public static final class Loop extends AbstractTickableSoundInstance {

        private float target;
        private boolean stopping;

        private Loop(SoundEvent event, float volume) {
            super(event, SoundSource.AMBIENT, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        public void setTarget(float target) {
            if (!stopping) this.target = target;
        }

        public void fadeOut() {
            stopping = true;
            target = 0.0f;
        }

        @Override
        public void tick() {
            volume += (target - volume) * 0.12f;
            if (stopping && volume < 0.01f) stop();
        }
    }
}
