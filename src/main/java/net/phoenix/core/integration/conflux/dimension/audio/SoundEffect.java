package net.phoenix.core.integration.conflux.dimension.audio;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

public class SoundEffect {

    public enum SoundType {
        GRAVITY_ZONE_ENTER,
        GRAVITY_ZONE_EXIT,
        PLATFORM_BOARDING,
        PLATFORM_LANDING,
        PLATFORM_MOVING,
        SHADER_ACTIVE,
        AMBIENT_LOOP,
        PARTICLE_BURST,
        PROGRESSION_UNLOCK
    }

    private final ResourceLocation soundId;
    private final SoundEvent soundEvent;
    private final SoundType type;
    private final float volume;
    private final float pitch;
    private final float range;
    private final String description;

    public SoundEffect(
        ResourceLocation soundId,
        SoundType type,
        float volume,
        float pitch,
        float range,
        String description
    ) {
        this.soundId = soundId;
        this.soundEvent = SoundEvent.createVariableRangeEvent(soundId);
        this.type = type;
        this.volume = volume;
        this.pitch = pitch;
        this.range = range;
        this.description = description;
    }

    public static SoundEffect withPitchVariation(
        ResourceLocation soundId,
        SoundType type,
        float volume,
        float basePitch,
        float pitchVariation,
        float range,
        String description
    ) {
        float randomPitch = basePitch + (float) (Math.random() - 0.5f) * pitchVariation;
        return new SoundEffect(soundId, type, volume, randomPitch, range, description);
    }

    public float getPitchWithVariation(float variation) {
        return pitch + (float) (Math.random() - 0.5f) * variation;
    }

    public float getVolumeAtDistance(Vec3 listenerPos, Vec3 soundPos) {
        double distance = listenerPos.distanceTo(soundPos);
        if (distance > range) {
            return 0.0f;
        }

        double normalized = distance / range;
        return (float) (volume * (1.0 - normalized * 0.7));
    }

    public ResourceLocation getSoundId() { return soundId; }
    public SoundEvent getSoundEvent() { return soundEvent; }
    public SoundType getType() { return type; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public float getRange() { return range; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("SoundEffect[%s type=%s vol=%.2f pitch=%.2f range=%.1f]",
            description, type, volume, pitch, range);
    }
}
