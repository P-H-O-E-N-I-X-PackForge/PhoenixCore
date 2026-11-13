package net.phoenix.core.client.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public final class FxThemeLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    private FxThemeLoader() {
        super(GSON, "fx_themes");
    }

    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new FxThemeLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        FxThemes.resetToBuiltIn();
        for (var entry : files.entrySet()) {
            String id = entry.getKey().getPath();
            try {
                FxThemes.register(parse(id, entry.getValue().getAsJsonObject()));
            } catch (RuntimeException e) {
                PhoenixCore.LOGGER.warn("[PFT] could not read the render theme {}: {}", entry.getKey(),
                        e.getMessage());
            }
        }
    }

    static FxTheme parse(String id, JsonObject json) {
        float[][] stops = null;
        if (json.has("stops")) {
            var list = json.getAsJsonArray("stops");
            if (list.size() < 2) throw new IllegalArgumentException("\"stops\" needs two colours or more");
            stops = new float[list.size()][];
            for (int i = 0; i < stops.length; i++) {
                stops[i] = FxTheme.parse(list.get(i).getAsString());
                if (stops[i] == null) throw new IllegalArgumentException("\"stops\" has a bad colour");
            }
        }
        float[] a = stops != null ? stops[0] : colour(json, "a");
        float[] b = stops != null ? stops[1] : colour(json, "b");
        float[] accent = json.has("accent") ? colour(json, "accent") : b;
        Map<String, Float> extras = new HashMap<>();
        if (json.has("extras")) {
            for (var e : json.getAsJsonObject("extras").entrySet()) extras.put(e.getKey(), e.getValue().getAsFloat());
        }
        return new FxTheme(id, json.has("name") ? json.get("name").getAsString() : id, a, b, accent,
                json.has("cycles") ? json.get("cycles").getAsFloat() : 1f,
                json.has("speed") ? json.get("speed").getAsFloat() : 0.1f, extras, stops);
    }

    private static float[] colour(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("no \"" + key + "\" colour");
        float[] c = FxTheme.parse(json.get(key).getAsString());
        if (c == null) throw new IllegalArgumentException("\"" + key + "\" is not a colour like FF00AA");
        return c;
    }
}
