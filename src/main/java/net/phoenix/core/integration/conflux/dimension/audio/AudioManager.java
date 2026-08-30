package net.phoenix.core.integration.conflux.dimension.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.*;

public class AudioManager {

    private static final AudioManager INSTANCE = new AudioManager();

    private final Map<String, AmbientTrack> ambientTracks = new HashMap<>();
    private final Map<String, SoundEffect> soundEffects = new HashMap<>();
    private final Map<String, AmbientTrack> activeTracks = new HashMap<>();

    private String currentDimension = "";
    private float globalVolume = 1.0f;

    public static AudioManager getInstance() {
        return INSTANCE;
    }

    public void registerAmbientTrack(String trackId, AmbientTrack track) {
        ambientTracks.put(trackId, track);
        System.out.println("[PhoenixCore Audio] Registered ambient track: " + trackId);
    }

    public void registerSoundEffect(String effectId, SoundEffect effect) {
        soundEffects.put(effectId, effect);
        System.out.println("[PhoenixCore Audio] Registered sound effect: " + effectId);
    }

    public void playAmbientTrack(String trackId) {
        AmbientTrack track = ambientTracks.get(trackId);
        if (track != null) {
            track.play();
            activeTracks.put(trackId, track);
        }
    }

    public void stopAmbientTrack(String trackId) {
        AmbientTrack track = activeTracks.get(trackId);
        if (track != null) {
            track.stop();
        }
    }

    public void stopAllAmbientTracks() {
        for (AmbientTrack track : activeTracks.values()) {
            track.stop();
        }
    }

    public void onDimensionChange(String newDimension) {
        if (newDimension.equals(currentDimension)) {
            return;
        }

        stopAllAmbientTracks();

        currentDimension = newDimension;

        switch (newDimension) {
            case "phoenix" -> playAmbientTrack("phoenix_main");
            case "sculk" -> playAmbientTrack("sculk_main");
            case "void" -> playAmbientTrack("void_main");
            case "sealed_a" -> playAmbientTrack("sealed_a_main");
            case "sealed_b" -> playAmbientTrack("sealed_b_main");
        }

        System.out.println("[PhoenixCore Audio] Switched to dimension: " + newDimension);
    }

    public void playSoundEffect(String effectId, Player player) {
        if (player == null || player.level().isClientSide) return;

        SoundEffect effect = soundEffects.get(effectId);
        if (effect != null) {
            playSoundEffectAt(effect, player.position());
        }
    }

    public void playSoundEffectAt(SoundEffect effect, Vec3 position) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Player player = mc.player;
            if (player == null) return;

            float attentuatedVolume = effect.getVolumeAtDistance(player.position(), position);
            if (attentuatedVolume <= 0.01f) {
                return; 
            }

            SoundManager soundManager = mc.getSoundManager();
            mc.level.playLocalSound(
                    position.x,
                    position.y,
                    position.z,
                    effect.getSoundEvent(),
                    SoundSource.AMBIENT,
                    attentuatedVolume * globalVolume,
                    effect.getPitch(),
                    false 
            );
        });
    }

    public void updateAmbience(float deltaTime) {
        for (AmbientTrack track : activeTracks.values()) {
            track.updateVolume(deltaTime);
        }

        activeTracks.entrySet().removeIf(entry -> !entry.getValue().isFading() && !entry.getValue().isPlaying());
    }

    public void setGlobalVolume(float volume) {
        this.globalVolume = Math.max(0.0f, Math.min(1.0f, volume));
        System.out.println("[PhoenixCore Audio] Global volume: " + (globalVolume * 100) + "%");
    }

    public float getGlobalVolume() {
        return globalVolume;
    }

    public String getCurrentDimension() {
        return currentDimension;
    }

    public Collection<AmbientTrack> getActiveTracks() {
        return activeTracks.values();
    }

    public void logStatistics() {
        System.out.println("[PhoenixCore Audio] Statistics:");
        System.out.println("  Ambient Tracks: " + ambientTracks.size());
        System.out.println("  Sound Effects: " + soundEffects.size());
        System.out.println("  Active Tracks: " + activeTracks.size());
        System.out.println("  Global Volume: " + (globalVolume * 100) + "%");
        System.out.println("  Current Dimension: " + (currentDimension.isEmpty() ? "None" : currentDimension));
    }

    public void cleanup() {
        stopAllAmbientTracks();
        activeTracks.clear();
        ambientTracks.clear();
        soundEffects.clear();
    }
}
