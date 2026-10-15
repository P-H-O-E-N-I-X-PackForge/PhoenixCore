package net.phoenix.core.integration.continuum.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.integration.continuum.data.ContinuumBody;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class Affinities extends SimpleJsonResourceReloadListener {

    public static final Affinities INSTANCE = new Affinities();

    public record Factors(float trip, float wear, float yield, String label) {

        public static final Factors NONE = new Factors(1.0f, 1.0f, 1.0f, "");

        public boolean none() {
            return trip == 1.0f && wear == 1.0f && yield == 1.0f;
        }
    }

    private record Entry(@Nullable ResourceLocation system, @Nullable String type, @Nullable ResourceLocation body,
                         float trip, float wear, float yield, String label) {

        boolean matches(ContinuumBody candidate) {
            if (system != null && !system.equals(candidate.system())) return false;
            if (body != null && !body.equals(candidate.id())) return false;
            return type == null || type.equals(candidate.type().name().toLowerCase(Locale.ROOT));
        }
    }

    private Map<String, List<Entry>> byDiscipline = Map.of();

    private Affinities() {
        super(new Gson(), "continuum/disciplines");
    }

    @SubscribeEvent
    public static void addListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, List<Entry>> loaded = new HashMap<>();
        files.forEach((id, element) -> {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
                String discipline = GsonHelper.getAsString(json, "discipline").toLowerCase(Locale.ROOT);
                for (JsonElement item : GsonHelper.getAsJsonArray(json, "affinities")) {
                    JsonObject e = GsonHelper.convertToJsonObject(item, "affinity");
                    loaded.computeIfAbsent(discipline, d -> new ArrayList<>()).add(new Entry(
                            e.has("system") ? new ResourceLocation(GsonHelper.getAsString(e, "system")) : null,
                            e.has("type") ? GsonHelper.getAsString(e, "type").toLowerCase(Locale.ROOT) : null,
                            e.has("body") ? new ResourceLocation(GsonHelper.getAsString(e, "body")) : null,
                            GsonHelper.getAsFloat(e, "trip", 1.0f), GsonHelper.getAsFloat(e, "wear", 1.0f),
                            GsonHelper.getAsFloat(e, "yield", 1.0f), GsonHelper.getAsString(e, "label", "")));
                }
            } catch (RuntimeException e) {
                PhoenixCore.LOGGER.error("[Continuum] Bad discipline affinities {}: {}", id, e.getMessage());
            }
        });
        byDiscipline = loaded;
    }

    public static Factors of(@Nullable String discipline, ContinuumBody body) {
        if (discipline == null) return Factors.NONE;
        List<Entry> entries = INSTANCE.byDiscipline.get(discipline.toLowerCase(Locale.ROOT));
        if (entries == null) return Factors.NONE;

        float trip = 1.0f;
        float wear = 1.0f;
        float yield = 1.0f;
        StringBuilder label = new StringBuilder();
        for (Entry entry : entries) {
            if (!entry.matches(body)) continue;
            trip *= entry.trip;
            wear *= entry.wear;
            yield *= entry.yield;
            if (!entry.label.isEmpty()) {
                if (label.length() > 0) label.append("  ");
                label.append(entry.label);
            }
        }
        return new Factors(trip, wear, yield, label.toString());
    }

    public static Factors of(MinecraftServer server, UUID team, ContinuumBody body) {
        return of(WorldResearchData.get(server.overworld()).getDiscipline(team), body);
    }

    public static int scaled(int base, float factor, net.minecraft.util.RandomSource random) {
        float exact = base * factor;
        int whole = (int) Math.floor(exact);
        return whole + (random.nextFloat() < exact - whole ? 1 : 0);
    }
}
