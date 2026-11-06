package net.phoenix.core.integration.conflux.signature;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class SignatureRegistry extends SimpleJsonResourceReloadListener {

    public static final SignatureRegistry INSTANCE = new SignatureRegistry();

    private Map<String, SignatureDefinition> byId = Map.of();

    private SignatureRegistry() {
        super(new Gson(), "conflux/signatures");
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, SignatureDefinition> loaded = new LinkedHashMap<>();
        jsons.forEach((rl, json) -> {
            try {

                String id = rl.getPath();
                loaded.put(id, SignatureDefinition.fromJson(id, json.getAsJsonObject()));
            } catch (RuntimeException e) {
                PhoenixCore.LOGGER.error("Bad signature definition {}: {}", rl, e.toString());
            }
        });
        byId = Map.copyOf(loaded);
        PhoenixCore.LOGGER.info("Loaded {} signature mechanic(s)", byId.size());
    }

    public Optional<SignatureDefinition> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<SignatureDefinition> all() {
        return new ArrayList<>(byId.values());
    }

    public List<SignatureDefinition> forDiscipline(String discipline) {
        List<SignatureDefinition> out = new ArrayList<>();
        for (SignatureDefinition def : byId.values()) if (def.discipline().equals(discipline)) out.add(def);
        return out;
    }
}
