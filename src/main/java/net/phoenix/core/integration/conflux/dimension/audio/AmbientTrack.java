package net.phoenix.core.integration.conflux.dimension.audio;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class AmbientTrack {

    private final ResourceLocation soundId;
    private final SoundEvent soundEvent;
    private final float volume;
    private final float pitch;
    private final boolean loop;
    private final float fadeInTime;
    private final float fadeOutTime;
    private final String description;

    private float currentVolume;
    private boolean isPlaying;

    public AmbientTrack(
                        ResourceLocation soundId,
                        float volume,
                        float pitch,
                        boolean loop,
                        float fadeInTime,
                        float fadeOutTime,
                        String description) {
        this.soundId = soundId;
        this.soundEvent = SoundEvent.createVariableRangeEvent(soundId);
        this.volume = volume;
        this.pitch = pitch;
        this.loop = loop;
        this.fadeInTime = fadeInTime;
        this.fadeOutTime = fadeOutTime;
        this.description = description;
        this.currentVolume = 0.0f;
        this.isPlaying = false;
    }

    public void play() {
        isPlaying = true;
        currentVolume = 0.0f;
        System.out.println("[PhoenixCore Audio] Playing ambient track: " + description);
    }

    public void stop() {
        isPlaying = false;
        System.out.println("[PhoenixCore Audio] Stopping ambient track: " + description);
    }

    public void updateVolume(float deltaTime) {
        if (isPlaying) {

            if (currentVolume < volume && fadeInTime > 0) {
                currentVolume = Math.min(volume, currentVolume + (volume / fadeInTime) * deltaTime);
            } else if (currentVolume < volume) {
                currentVolume = volume;
            }
        } else {

            if (currentVolume > 0 && fadeOutTime > 0) {
                currentVolume = Math.max(0, currentVolume - (volume / fadeOutTime) * deltaTime);
            } else {
                currentVolume = 0;
            }
        }
    }

    public float getCurrentVolume() {
        return currentVolume;
    }

    public boolean isPlaying() {
        return isPlaying && currentVolume > 0.01f;
    }

    public boolean isFading() {
        return (isPlaying && currentVolume < volume) ||
                (!isPlaying && currentVolume > 0);
    }

    public ResourceLocation getSoundId() {
        return soundId;
    }

    public SoundEvent getSoundEvent() {
        return soundEvent;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isLooping() {
        return loop;
    }

    public float getFadeInTime() {
        return fadeInTime;
    }

    public float getFadeOutTime() {
        return fadeOutTime;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("AmbientTrack[%s vol=%.2f pitch=%.2f loop=%b]",
                description, volume, pitch, loop);
    }
}
