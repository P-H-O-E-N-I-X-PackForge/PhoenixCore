package net.phoenix.core.integration.continuum.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
import net.phoenix.core.integration.continuum.common.ContinuumServerEvents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.*;

/**
 * The loaded systems and bodies. They are datapack JSON under {@code data/<ns>/continuum/systems|bodies/*.json},
 * loaded on the server and synced to clients as raw JSON ({@link #snapshot} / {@link #acceptSync}); both sides run the
 * same {@link #parseSystem} / {@link #parseBody}. In single-player the client and server share these maps, which is
 * harmless because they always hold identical content.
 */
public final class ContinuumData {

    private ContinuumData() {}

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SYSTEM_DIR = "continuum/systems";
    private static final String BODY_DIR = "continuum/bodies";

    private static Map<ResourceLocation, ContinuumSystem> systems = Map.of();
    private static Map<ResourceLocation, ContinuumBody> bodies = Map.of();
    private static Map<ResourceLocation, String> rawSystems = Map.of();
    private static Map<ResourceLocation, String> rawBodies = Map.of();

    /** Raw definitions as loaded, for sending to clients. */
    public record Snapshot(Map<ResourceLocation, String> systems, Map<ResourceLocation, String> bodies) {}

    public static void registerServerListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static Snapshot snapshot() {
        return new Snapshot(rawSystems, rawBodies);
    }

    /** Client side: replace the definitions with what the server sent. */
    public static void acceptSync(Snapshot snapshot) {
        Map<ResourceLocation, ContinuumSystem> newSystems = new LinkedHashMap<>();
        Map<ResourceLocation, ContinuumBody> newBodies = new LinkedHashMap<>();
        snapshot.systems().forEach((id, json) -> parse(id, json, o -> newSystems.put(id, parseSystem(id, o))));
        snapshot.bodies().forEach((id, json) -> parse(id, json, o -> newBodies.put(id, parseBody(id, o))));

        systems = newSystems;
        bodies = newBodies;
        rawSystems = snapshot.systems();
        rawBodies = snapshot.bodies();
    }

    private static void parse(ResourceLocation id, String json, java.util.function.Consumer<JsonObject> consumer) {
        try {
            JsonElement element = JsonParser.parseString(json);
            if (element.isJsonObject()) consumer.accept(element.getAsJsonObject());
        } catch (Exception e) {
            LOGGER.error("[PhoenixCore/Continuum] Bad definition {}: {}", id, e.getMessage());
        }
    }

    public static Collection<ContinuumSystem> systems() {
        return systems.values();
    }

    public static @Nullable ContinuumSystem system(ResourceLocation id) {
        return systems.get(id);
    }

    public static @Nullable ContinuumBody body(ResourceLocation id) {
        return bodies.get(id);
    }

    public static Collection<ContinuumBody> allBodies() {
        return bodies.values();
    }

    /** Every body in a system, planets first then moons, in a stable order. */
    public static List<ContinuumBody> bodiesOf(ResourceLocation system) {
        List<ContinuumBody> result = new ArrayList<>();
        for (ContinuumBody body : bodies.values()) {
            if (body.system().equals(system)) result.add(body);
        }
        result.sort(Comparator.comparing(ContinuumBody::isMoon).thenComparing(ContinuumBody::orbitAu)
                .thenComparing(b -> b.id().toString()));
        return result;
    }

    public static List<ContinuumBody> moonsOf(ResourceLocation parent) {
        List<ContinuumBody> result = new ArrayList<>();
        for (ContinuumBody body : bodies.values()) {
            if (parent.equals(body.parent())) result.add(body);
        }
        result.sort(Comparator.comparing(ContinuumBody::orbitAu));
        return result;
    }

    // ---------------- parsing (shared) ----------------

    public static ContinuumSystem parseSystem(ResourceLocation id, JsonObject json) {
        float[] pos = new float[] { 0, 0, 0 };
        if (json.has("galaxy")) {
            var array = GsonHelper.getAsJsonArray(json, "galaxy");
            for (int i = 0; i < 3 && i < array.size(); i++) pos[i] = array.get(i).getAsFloat();
        }
        JsonObject gate = GsonHelper.getAsJsonObject(json, "gate", new JsonObject());
        return new ContinuumSystem(
                id,
                GsonHelper.getAsString(json, "name", id.getPath()),
                GsonHelper.getAsString(json, "description", ""),
                pos[0], pos[1], pos[2],
                color(json, "star_color", 0xffd9a0),
                GsonHelper.getAsFloat(json, "star_radius", 1.0f),
                GsonHelper.getAsString(json, "star_kind", "star"),
                DiscoveryStage.parse(GsonHelper.getAsString(json, "initial_stage", "unknown"),
                        DiscoveryStage.UNKNOWN),
                optionalId(gate, "detected_research"),
                optionalId(gate, "surveyed_research"));
    }

    public static ContinuumBody parseBody(ResourceLocation id, JsonObject json) {
        JsonObject orbit = GsonHelper.getAsJsonObject(json, "orbit", new JsonObject());
        JsonObject surface = GsonHelper.getAsJsonObject(json, "surface", new JsonObject());
        JsonObject atmosphere = GsonHelper.getAsJsonObject(json, "atmosphere", new JsonObject());
        JsonObject gate = GsonHelper.getAsJsonObject(json, "gate", new JsonObject());

        String name = GsonHelper.getAsString(json, "name", id.getPath());
        String description = GsonHelper.getAsString(json, "description", "");

        ContinuumBody.Type type = ContinuumBody.Type.PLANET;
        try {
            type = ContinuumBody.Type.valueOf(GsonHelper.getAsString(json, "type", "planet").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        PlanetParams params = new PlanetParams(
                name, description,
                GsonHelper.getAsBoolean(json, "gas_giant", false),
                GsonHelper.getAsFloat(surface, "seed", 1.0f),
                GsonHelper.getAsFloat(surface, "scale", 2.5f),
                GsonHelper.getAsFloat(surface, "ocean_level", 0.0f),
                GsonHelper.getAsFloat(surface, "polar_ice", 0.0f),
                GsonHelper.getAsFloat(surface, "cloud_cover", 0.0f),
                GsonHelper.getAsFloat(surface, "bump", 0.7f),
                color(surface, "low", 0x444444), color(surface, "mid", 0x888888), color(surface, "high", 0xcccccc),
                color(surface, "ocean_deep", 0x03123a), color(surface, "ocean_shallow", 0x1a6f8f),
                color(surface, "emissive", 0xff5a14), GsonHelper.getAsFloat(surface, "emissive_amount", 0.0f),
                color(atmosphere, "color", 0x6aa8ff), GsonHelper.getAsFloat(atmosphere, "density", 0.0f),
                GsonHelper.getAsFloat(json, "axial_tilt", 0.0f),
                GsonHelper.getAsFloat(json, "spin_deg_per_sec", 4.0f),
                GsonHelper.getAsFloat(json, "cloud_spin_deg_per_sec", 6.0f));

        JsonObject lore = GsonHelper.getAsJsonObject(json, "lore", new JsonObject());
        return new ContinuumBody(
                id,
                new ResourceLocation(GsonHelper.getAsString(json, "system")),
                optionalId(json, "parent"),
                type, name, description,
                GsonHelper.getAsFloat(orbit, "radius_au", 1.0f),
                Math.max(GsonHelper.getAsFloat(orbit, "period_days", 100.0f), 0.01f),
                GsonHelper.getAsFloat(orbit, "phase", 0.0f),
                GsonHelper.getAsFloat(json, "radius", 1.0f),
                GsonHelper.getAsInt(json, "trip_minutes", 10),
                GsonHelper.getAsString(gate, "min_tier", "start"),
                optionalId(gate, "detected_research"),
                optionalId(gate, "surveyed_research"),
                params,
                parseYields(json),
                DiscoveryStage.parse(GsonHelper.getAsString(json, "initial_stage", "unknown"),
                        DiscoveryStage.UNKNOWN),
                GsonHelper.getAsString(lore, "detected", ""),
                GsonHelper.getAsString(lore, "surveyed", ""));
    }

    private static List<ContinuumBody.Yield> parseYields(JsonObject json) {
        List<ContinuumBody.Yield> yields = new ArrayList<>();
        if (!json.has("yields")) return yields;

        for (JsonElement element : GsonHelper.getAsJsonArray(json, "yields")) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            int min = Math.max(1, GsonHelper.getAsInt(entry, "min", 1));
            int max = Math.max(min, GsonHelper.getAsInt(entry, "max", min));
            yields.add(new ContinuumBody.Yield(new ResourceLocation(GsonHelper.getAsString(entry, "item")), min, max,
                    Math.max(0.0f, Math.min(1.0f, GsonHelper.getAsFloat(entry, "chance", 1.0f)))));
        }
        return yields;
    }

    private static @Nullable ResourceLocation optionalId(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        return new ResourceLocation(GsonHelper.getAsString(json, key));
    }

    private static int color(JsonObject json, String key, int fallback) {
        if (!json.has(key) || json.get(key).isJsonNull()) return fallback;
        String text = GsonHelper.getAsString(json, key);
        try {
            return Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---------------- server reload listener ----------------

    private record Parsed(Map<ResourceLocation, ContinuumSystem> systems, Map<ResourceLocation, ContinuumBody> bodies,
                          Map<ResourceLocation, String> rawSystems, Map<ResourceLocation, String> rawBodies) {}

    private static final class Loader extends SimplePreparableReloadListener<Parsed> {

        @Override
        protected Parsed prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, ContinuumSystem> loadedSystems = new LinkedHashMap<>();
            Map<ResourceLocation, ContinuumBody> loadedBodies = new LinkedHashMap<>();
            Map<ResourceLocation, String> loadedRawSystems = new LinkedHashMap<>();
            Map<ResourceLocation, String> loadedRawBodies = new LinkedHashMap<>();

            read(manager, SYSTEM_DIR, (id, json) -> {
                loadedSystems.put(id, parseSystem(id, json));
                loadedRawSystems.put(id, json.toString());
            });
            read(manager, BODY_DIR, (id, json) -> {
                loadedBodies.put(id, parseBody(id, json));
                loadedRawBodies.put(id, json.toString());
            });
            return new Parsed(loadedSystems, loadedBodies, loadedRawSystems, loadedRawBodies);
        }

        @Override
        protected void apply(Parsed parsed, ResourceManager manager, ProfilerFiller profiler) {
            systems = parsed.systems();
            bodies = parsed.bodies();
            rawSystems = parsed.rawSystems();
            rawBodies = parsed.rawBodies();
            LOGGER.info("[PhoenixCore/Continuum] Loaded {} systems and {} bodies", systems.size(), bodies.size());
            ContinuumServerEvents.onDataReloaded();
        }

        private static void read(ResourceManager manager, String dir,
                                 java.util.function.BiConsumer<ResourceLocation, JsonObject> consumer) {
            for (Map.Entry<ResourceLocation, Resource> entry : manager
                    .listResources(dir, path -> path.getPath().endsWith(".json")).entrySet()) {
                ResourceLocation file = entry.getKey();
                String path = file.getPath();
                ResourceLocation id = new ResourceLocation(file.getNamespace(),
                        path.substring(dir.length() + 1, path.length() - ".json".length()));
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (element.isJsonObject()) consumer.accept(id, element.getAsJsonObject());
                } catch (Exception e) {
                    LOGGER.error("[PhoenixCore/Continuum] Failed to load {}: {}", file, e.getMessage());
                }
            }
        }
    }
}
