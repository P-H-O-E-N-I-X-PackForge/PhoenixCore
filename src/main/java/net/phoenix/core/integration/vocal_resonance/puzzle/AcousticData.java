package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * The server tables for the acoustic puzzle, loaded from data packs:
 * {@code data/<ns>/acoustic_sounds/*.json} (what each library sound is) and
 * {@code data/<ns>/acoustic_puzzles/*.json} (the sequences to play) and
 * {@code data/<ns>/acoustic_charts/*.json} (the rhythm charts that attune a lock).
 */
@Mod.EventBusSubscriber(modid = "phoenixcore")
public final class AcousticData {

    private static final Gson GSON = new Gson();

    private static volatile Map<ResourceLocation, AcousticSoundEntry> sounds = Map.of();
    private static volatile Map<ResourceLocation, AcousticPuzzle> puzzles = new TreeMap<>();
    private static volatile Map<ResourceLocation, AcousticChart> charts = new TreeMap<>();

    private AcousticData() {}

    public static @Nullable AcousticSoundEntry sound(ResourceLocation id) {
        return sounds.get(id);
    }

    public static @Nullable AcousticPuzzle puzzle(@Nullable ResourceLocation id) {
        return id == null ? null : puzzles.get(id);
    }

    /** The puzzle a jukebox falls back on when none is chosen: the first one by id, or null when there are none. */
    public static @Nullable AcousticPuzzle defaultPuzzle() {
        return puzzles.values().stream().findFirst().orElse(null);
    }

    /** The chart a lock plays to attune: the first one by id, or null when there are none. */
    public static @Nullable AcousticChart defaultChart() {
        return charts.values().stream().findFirst().orElse(null);
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(GSON, "acoustic_sounds") {

            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
                                 ProfilerFiller profiler) {
                Map<ResourceLocation, AcousticSoundEntry> loaded = new TreeMap<>();
                files.forEach((file, json) -> {
                    try {
                        AcousticSoundEntry entry = AcousticSoundEntry.fromJson((JsonObject) json);
                        loaded.put(entry.sound(), entry);
                    } catch (RuntimeException e) {
                        System.err.println("Bad acoustic sound " + file + ": " + e);
                    }
                });
                sounds = Map.copyOf(loaded);
            }
        });
        event.addListener(new SimpleJsonResourceReloadListener(GSON, "acoustic_charts") {

            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
                                 ProfilerFiller profiler) {
                Map<ResourceLocation, AcousticChart> loaded = new TreeMap<>();
                files.forEach((file, json) -> {
                    try {
                        loaded.put(file, AcousticChart.fromJson(file, (JsonObject) json));
                    } catch (RuntimeException e) {
                        System.err.println("Bad acoustic chart " + file + ": " + e);
                    }
                });
                charts = loaded;
            }
        });
        event.addListener(new SimpleJsonResourceReloadListener(GSON, "acoustic_puzzles") {

            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
                                 ProfilerFiller profiler) {
                Map<ResourceLocation, AcousticPuzzle> loaded = new TreeMap<>();
                files.forEach((file, json) -> {
                    try {
                        loaded.put(file, AcousticPuzzle.fromJson(file, (JsonObject) json));
                    } catch (RuntimeException e) {
                        System.err.println("Bad acoustic puzzle " + file + ": " + e);
                    }
                });
                puzzles = loaded;
            }
        });
    }
}
