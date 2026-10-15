package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NebulaData extends SimpleJsonResourceReloadListener {

    private static final NebulaData INSTANCE = new NebulaData();

    private List<Nebula> nebulas = List.of();

    private NebulaData() {
        super(new Gson(), "continuum/nebulas");
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    public static List<Nebula> all() {
        return INSTANCE.nebulas;
    }

    public record Inside(Nebula nebula, float depth) {}

    public static @org.jetbrains.annotations.Nullable Inside around(float x, float y, float z) {
        Inside best = null;
        for (Nebula nebula : INSTANCE.nebulas) {
            float distance = new Vector3f(x, y, z).sub(nebula.center()).length();
            float depth = 1.0f - distance / (nebula.radius() * 1.1f);
            if (depth > 0.0f && (best == null || depth > best.depth()))
                best = new Inside(nebula, Math.min(1.0f, depth));
        }
        return best;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<Nebula> loaded = new ArrayList<>();
        files.forEach((id, element) -> {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
                JsonArray c = GsonHelper.getAsJsonArray(json, "center");
                loaded.add(new Nebula(GsonHelper.getAsString(json, "name", id.getPath()),
                        new Vector3f(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()),
                        GsonHelper.getAsFloat(json, "radius", 5.0f), color(json, "color1", 0xff8040),
                        color(json, "color2", 0x8040ff), GsonHelper.getAsFloat(json, "density", 0.8f),
                        GsonHelper.getAsFloat(json, "seed", 1.0f), Math.max(1, GsonHelper.getAsInt(json, "puffs", 4))));
            } catch (RuntimeException e) {
                PhoenixCore.LOGGER.error("[Continuum] Bad nebula {}: {}", id, e.getMessage());
            }
        });
        nebulas = List.copyOf(loaded);
    }

    private static int color(JsonObject json, String key, int fallback) {
        String text = GsonHelper.getAsString(json, key, "");
        if (text.startsWith("#")) text = text.substring(1);
        try {
            return text.isEmpty() ? fallback : Integer.parseInt(text, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
