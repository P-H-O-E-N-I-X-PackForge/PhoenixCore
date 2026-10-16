package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@net.neoforged.fml.common.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public class GatedRecipeRegistry extends SimpleJsonResourceReloadListener {

    public static final GatedRecipeRegistry INSTANCE = new GatedRecipeRegistry();

    private List<GatedRecipeRule> rules = List.of();
    private final Map<ResourceLocation, String> raw = new java.util.LinkedHashMap<>();

    private GatedRecipeRegistry() {
        super(new Gson(), "conflux/gated_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<GatedRecipeRule> loaded = new ArrayList<>();
        raw.clear();
        data.forEach((id, json) -> GatedRecipeRule.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(
                        error -> PhoenixCore.LOGGER.error("Failed to load gated recipe rule {}: {}", id, error))
                .ifPresent(rule -> {
                    loaded.add(rule);
                    raw.put(id, json.toString());
                }));
        rules = List.copyOf(loaded);
        PhoenixCore.LOGGER.info("Loaded {} gated recipe rule(s)", rules.size());

        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) RecipeGateApplier.applyGates(server.getRecipeManager().getRecipes());
    }

    public Map<ResourceLocation, String> rawJson() {
        return java.util.Collections.unmodifiableMap(raw);
    }

    public void clientReset() {
        rules = List.of();
        raw.clear();
    }

    public void clientAdd(ResourceLocation id, String json) {
        GatedRecipeRule.CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json))
                .resultOrPartial(error -> PhoenixCore.LOGGER.error("Bad synced gated recipe rule {}: {}", id, error))
                .ifPresent(rule -> {
                    List<GatedRecipeRule> next = new ArrayList<>(rules);
                    next.add(rule);
                    rules = List.copyOf(next);
                    raw.put(id, json);
                });
    }

    public List<GatedRecipeRule> getRules() {
        return rules;
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        RecipeGateApplier.applyGates(event.getServer().getRecipeManager().getRecipes());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();
        RecipeGateApplier.applyGates(server.getRecipeManager().getRecipes());
    }
}
