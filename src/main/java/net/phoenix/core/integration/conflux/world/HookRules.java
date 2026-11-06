package net.phoenix.core.integration.conflux.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class HookRules extends SimpleJsonResourceReloadListener {

    public static final HookRules INSTANCE = new HookRules();

    private volatile List<HookRule> rules = List.of();

    private HookRules() {
        super(new Gson(), "conflux_hooks");
    }

    @SubscribeEvent
    public static void onAddListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<HookRule> loaded = new ArrayList<>();
        jsons.forEach((id, json) -> {
            try {
                if (!json.isJsonObject()) return;
                HookRule rule = HookRule.fromJson(id.toString(), json.getAsJsonObject());
                if (rule.enabled()) loaded.add(rule);
            } catch (RuntimeException e) {
                PhoenixCore.LOGGER.error("[Conflux] Bad world hook {}: {}", id, e.toString());
            }
        });
        rules = List.copyOf(loaded);
        PhoenixCore.LOGGER.info("[Conflux] Loaded {} world hook rule(s)", rules.size());
    }

    public List<HookRule> all() {
        return rules;
    }
}
