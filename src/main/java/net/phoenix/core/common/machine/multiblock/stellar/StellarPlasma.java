package net.phoenix.core.common.machine.multiblock.stellar;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class StellarPlasma {

    private StellarPlasma() {}

    public record Recipe(ResourceLocation id, ResourceLocation fluid, String name, StellarForgeState.Stage body,
                         float heatMin, float heatMax, double minMass, double rate, float damageStability,
                         double damageMass, float damageHeat) {

        public boolean eligible(StellarForgeState s) {
            return s.stage == body && s.heat >= heatMin && s.heat <= heatMax && s.mass >= minMass;
        }

        public @Nullable String whyNot(StellarForgeState s) {
            if (s.stage != body)
                return "needs a " + (body == StellarForgeState.Stage.BLACK_HOLE ? "black hole" : "star");
            if (s.mass < minMass) return String.format(Locale.ROOT, "needs %,.0f M (it has %,.0f)", minMass, s.mass);
            if (s.heat < heatMin || s.heat > heatMax) {
                return String.format(Locale.ROOT, "needs heat %.0f to %.0f (it is %.0f)", heatMin, heatMax, s.heat);
            }
            return null;
        }
    }

    private static final Map<ResourceLocation, Recipe> FROM_DATA = new TreeMap<>();
    private static final Map<ResourceLocation, Recipe> FROM_CODE = new TreeMap<>();

    private static volatile @Nullable List<Recipe> clientCopy;

    public static void setClientCopy(List<Recipe> recipes) {
        clientCopy = List.copyOf(recipes);
    }

    public static List<Recipe> forDisplay() {
        var copy = clientCopy;
        return copy != null ? copy : all();
    }

    public static synchronized void register(Recipe recipe) {
        FROM_CODE.put(recipe.id(), recipe);
    }

    public static synchronized List<Recipe> all() {
        Map<ResourceLocation, Recipe> merged = new TreeMap<>(FROM_DATA);
        merged.putAll(FROM_CODE);
        return new ArrayList<>(merged.values());
    }

    public static @Nullable Recipe get(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation key = ResourceLocation.tryParse(id);
        for (Recipe r : all()) {
            if (r.id().equals(key)) return r;
        }
        return null;
    }

    public static String next(String id) {
        List<Recipe> list = all();
        if (list.isEmpty()) return "";
        if (id == null || id.isEmpty()) return list.get(0).id().toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().toString().equals(id))
                return i + 1 < list.size() ? list.get(i + 1).id().toString() : "";
        }
        return "";
    }

    @SubscribeEvent
    public static void onSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        var packet = new net.phoenix.core.network.packet.S2CStellarDataPacket(all(), StellarTint.forDisplay());
        for (var player : event.getRelevantPlayers().toList()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        }
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {

        Loader() {
            super(new Gson(), "phoenixcore/stellar_plasmas");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> found, ResourceManager manager,
                             ProfilerFiller profiler) {
            synchronized (StellarPlasma.class) {
                FROM_DATA.clear();
                for (var entry : found.entrySet()) {
                    try {
                        FROM_DATA.put(entry.getKey(), parse(entry.getKey(),
                                GsonHelper.convertToJsonObject(entry.getValue(), "plasma")));
                    } catch (RuntimeException e) {
                        PhoenixCore.LOGGER.warn("Stellar plasma {} is not valid: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
        }
    }

    static Recipe parse(ResourceLocation id, JsonObject json) {
        String bodyText = GsonHelper.getAsString(json, "body", "star");
        StellarForgeState.Stage body = switch (bodyText) {
            case "star" -> StellarForgeState.Stage.STAR;
            case "black_hole" -> StellarForgeState.Stage.BLACK_HOLE;
            default -> throw new IllegalArgumentException("body is \"star\" or \"black_hole\", not " + bodyText);
        };
        float heatMin = 0.0f;
        float heatMax = 100.0f;
        if (json.has("heat")) {
            JsonObject heat = GsonHelper.getAsJsonObject(json, "heat");
            heatMin = GsonHelper.getAsFloat(heat, "min", 0.0f);
            heatMax = GsonHelper.getAsFloat(heat, "max", 100.0f);
        }
        float damageStability = 0.0f;
        double damageMass = 0.0;
        float damageHeat = 0.0f;
        if (json.has("damage")) {
            JsonObject damage = GsonHelper.getAsJsonObject(json, "damage");
            damageStability = GsonHelper.getAsFloat(damage, "stability", 0.0f);
            damageMass = GsonHelper.getAsFloat(damage, "mass", 0.0f);
            damageHeat = GsonHelper.getAsFloat(damage, "heat", 0.0f);
        }
        return new Recipe(id, ResourceLocation.parse(GsonHelper.getAsString(json, "fluid")),
                GsonHelper.getAsString(json, "name", id.getPath()), body, heatMin, heatMax,
                GsonHelper.getAsFloat(json, "min_mass", 0.0f), Math.max(0.0, GsonHelper.getAsFloat(json, "rate", 4.0f)),
                damageStability, damageMass, damageHeat);
    }
}
