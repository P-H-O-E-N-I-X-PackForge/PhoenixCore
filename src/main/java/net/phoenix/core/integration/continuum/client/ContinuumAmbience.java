package net.phoenix.core.integration.continuum.client;

import net.minecraft.sounds.SoundEvent;
import net.phoenix.core.api.PhoenixSounds;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ContinuumAmbience {

    private final Map<SoundEvent, ContinuumSounds.Loop> loops = new HashMap<>();

    public void update(@Nullable ContinuumSystem system, @Nullable ContinuumBody body, float duck) {
        Map<SoundEvent, Float> targets = new HashMap<>();
        float master = (float) PhoenixConfigs.INSTANCE.continuum.ambienceVolume * duck;

        add(targets, PhoenixSounds.VOID_COSMIC_DRONE.getMainEvent(),
                body != null ? 0.16f : system != null ? 0.26f : 0.32f);

        if (body != null) {
            DiscoveryStage stage = ContinuumClientState.stage(body.id());
            float known = stage == DiscoveryStage.SURVEYED ? 1.0f : 0.45f;
            PlanetParams params = body.params();
            switch (body.type()) {
                case STAR -> add(targets, PhoenixSounds.PHOENIX_VOLCANIC_RUMBLE.getMainEvent(), 0.4f);
                case BLACK_HOLE -> {
                    add(targets, PhoenixSounds.SEALED_B_INVERTED_HUM.getMainEvent(), 0.5f);
                    add(targets, PhoenixSounds.VOID_COSMIC_DRONE.getMainEvent(), 0.3f);
                }
                case MOON -> add(targets, PhoenixSounds.VOID_ETHEREAL_WIND.getMainEvent(), 0.12f * known);
                default -> {
                    add(targets, PhoenixSounds.PHOENIX_VOLCANIC_WIND.getMainEvent(),
                            Math.min(1.0f, params.atmoDensity()) * (params.gasGiant() ? 0.6f : 0.42f) * known);
                    if (params.oceanLevel() >= 0.5f) {
                        add(targets, PhoenixSounds.SCULK_DEEP_CAVE.getMainEvent(), 0.34f * known);
                    }
                    if (params.emissiveAmount() >= 0.3f) {
                        add(targets, PhoenixSounds.SEALED_A_NEON_HUM.getMainEvent(), 0.28f * known);
                    }
                }
            }
        } else if (system != null) {
            if (system.hasHole()) {
                add(targets, PhoenixSounds.SEALED_B_INVERTED_HUM.getMainEvent(), system.isQuasar() ? 0.42f : 0.3f);
                add(targets, PhoenixSounds.VOID_ETHEREAL_WIND.getMainEvent(), 0.14f);
            } else {
                add(targets, PhoenixSounds.PHOENIX_VOLCANIC_RUMBLE.getMainEvent(), 0.14f);
            }
        }

        Iterator<Map.Entry<SoundEvent, ContinuumSounds.Loop>> it = loops.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Float want = targets.get(entry.getKey());
            if (want == null || want * master < 0.005f) {
                entry.getValue().fadeOut();
                it.remove();
            }
        }
        targets.forEach((event, volume) -> {
            if (volume * master < 0.005f) return;
            ContinuumSounds.Loop loop = loops.computeIfAbsent(event, ContinuumSounds::ambientLoop);
            loop.setTarget(Math.min(1.0f, volume * master));
        });
    }

    private static void add(Map<SoundEvent, Float> targets, SoundEvent event, float volume) {
        targets.merge(event, volume, Float::sum);
    }

    public void stop() {
        loops.values().forEach(ContinuumSounds.Loop::fadeOut);
        loops.clear();
    }
}
