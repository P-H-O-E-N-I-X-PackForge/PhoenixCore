package net.phoenix.core.client.renderer.cinema.cutscene;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class CutsceneMusic extends AbstractTickableSoundInstance {

    private static final float MIN_VOLUME = 0.001f;

    public CutsceneMusic(SoundEvent event, SoundSource source, float pitch, boolean loop) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.looping = loop;
        this.delay = 0;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.pitch = pitch;
        this.volume = MIN_VOLUME;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(MIN_VOLUME, volume);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {}
}
