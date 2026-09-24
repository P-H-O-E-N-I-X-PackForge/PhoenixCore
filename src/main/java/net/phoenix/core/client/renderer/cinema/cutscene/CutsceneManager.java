package net.phoenix.core.client.renderer.cinema.cutscene;

import net.phoenix.core.PhoenixCore;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads every {@code assets/<namespace>/cutscenes/*.json} into a {@link CutsceneDefinition}.
 * Reloads with resource packs (F3+T), so cutscenes can be edited without restarting.
 */
public final class CutsceneManager extends SimpleJsonResourceReloadListener {

    public static final CutsceneManager INSTANCE = new CutsceneManager();

    private Map<ResourceLocation, CutsceneDefinition> cutscenes = Map.of();

    private CutsceneManager() {
        super(new Gson(), "cutscenes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, CutsceneDefinition> loaded = new HashMap<>();
        jsons.forEach((id, json) -> CutsceneDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> PhoenixCore.LOGGER.error("Failed to load cutscene {}: {}", id, error))
                .ifPresent(definition -> loaded.put(id, definition)));
        cutscenes = Map.copyOf(loaded);
        PhoenixCore.LOGGER.info("Loaded {} cutscene(s)", cutscenes.size());
    }

    public Set<ResourceLocation> ids() {
        return cutscenes.keySet();
    }

    public Optional<CutsceneDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(cutscenes.get(id));
    }

    /** Opens the cutscene screen for the given id. Returns false if no such cutscene is loaded. */
    public static boolean play(ResourceLocation id) {
        Optional<CutsceneDefinition> definition = INSTANCE.get(id);
        if (definition.isEmpty() || definition.get().pages().isEmpty()) {
            PhoenixCore.LOGGER.warn("Tried to play unknown or empty cutscene {}", id);
            return false;
        }
        Minecraft.getInstance().setScreen(new CutsceneScreen(definition.get()));
        return true;
    }
}
