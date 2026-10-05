package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loads {@link GatedRecipeRule}s from {@code data/<namespace>/conflux/gated_recipes/*.json}. Applied
 *  to already-loaded recipes by {@link RecipeGateApplier}, called from the same mixin hook
 *  ({@code RecipeManagerMixin}) that already filters recipes via {@code RecipeBlacklist}. */
public class GatedRecipeRegistry extends SimpleJsonResourceReloadListener {

    public static final GatedRecipeRegistry INSTANCE = new GatedRecipeRegistry();

    private List<GatedRecipeRule> rules = List.of();

    private GatedRecipeRegistry() {
        super(new Gson(), "conflux/gated_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<GatedRecipeRule> loaded = new ArrayList<>();
        data.forEach((id, json) -> GatedRecipeRule.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(
                        error -> PhoenixCore.LOGGER.error("Failed to load gated recipe rule {}: {}", id, error))
                .ifPresent(loaded::add));
        rules = List.copyOf(loaded);
        PhoenixCore.LOGGER.info("Loaded {} gated recipe rule(s)", rules.size());
    }

    public List<GatedRecipeRule> getRules() {
        return rules;
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
