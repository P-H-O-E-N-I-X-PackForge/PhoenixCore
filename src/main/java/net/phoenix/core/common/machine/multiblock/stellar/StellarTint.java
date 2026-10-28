package net.phoenix.core.common.machine.multiblock.stellar;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class StellarTint {

    public record Tint(@Nullable ResourceLocation fluid, @Nullable ResourceLocation tag, int amount, int color,
                       boolean star, boolean blackHole) {

        boolean matches(Fluid candidate) {
            if (fluid != null)
                return fluid.equals(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(candidate));
            return tag != null && candidate.builtInRegistryHolder().is(TagKey.create(Registries.FLUID, tag));
        }
    }

    private static final Map<ResourceLocation, Tint> FROM_DATA = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Tint> FROM_CODE = new LinkedHashMap<>();

    private StellarTint() {}

    private static volatile @Nullable List<Map.Entry<ResourceLocation, Tint>> clientCopy;

    public static void setClientCopy(List<Map.Entry<ResourceLocation, Tint>> tints) {
        clientCopy = List.copyOf(tints);
    }

    public static synchronized List<Map.Entry<ResourceLocation, Tint>> forDisplay() {
        var copy = clientCopy;
        if (copy != null) return copy;
        List<Map.Entry<ResourceLocation, Tint>> all = new java.util.ArrayList<>(FROM_DATA.entrySet());
        all.addAll(FROM_CODE.entrySet());
        return all;
    }

    public static synchronized void register(ResourceLocation id, Tint tint) {
        FROM_CODE.put(id, tint);
    }

    public static Tint of(ResourceLocation fluid, int amount, int color, boolean star, boolean blackHole) {
        return new Tint(fluid, null, amount, color, star, blackHole);
    }

    public static synchronized @Nullable Tint match(FluidStack stack, StellarForgeState.Stage stage) {
        if (stack.isEmpty()) return null;
        boolean star = stage == StellarForgeState.Stage.STAR;
        boolean hole = stage == StellarForgeState.Stage.BLACK_HOLE;
        if (!star && !hole) return null;
        for (var map : java.util.List.of(FROM_CODE, FROM_DATA)) {
            for (Tint tint : map.values()) {
                if (stack.getAmount() >= tint.amount() && tint.matches(stack.getFluid()) &&
                        (star ? tint.star() : tint.blackHole())) {
                    return tint;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {

        Loader() {
            super(new Gson(), "phoenixcore/stellar_tints");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> found, ResourceManager manager,
                             ProfilerFiller profiler) {
            synchronized (StellarTint.class) {
                FROM_DATA.clear();
                for (var entry : found.entrySet()) {
                    try {
                        FROM_DATA.put(entry.getKey(), parse(GsonHelper.convertToJsonObject(entry.getValue(), "tint")));
                    } catch (RuntimeException e) {
                        PhoenixCore.LOGGER.warn("Stellar tint {} is not valid: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
        }
    }

    static Tint parse(JsonObject json) {
        String fluidText = GsonHelper.getAsString(json, "fluid");
        ResourceLocation fluid = null;
        ResourceLocation tag = null;
        if (fluidText.startsWith("#")) tag = ResourceLocation.parse(fluidText.substring(1));
        else fluid = ResourceLocation.parse(fluidText);

        int color;
        String colorText = GsonHelper.getAsString(json, "color");
        if (colorText.equalsIgnoreCase("reset")) {
            color = -1;
        } else {
            color = Integer.parseInt(colorText.startsWith("#") ? colorText.substring(1) : colorText, 16) & 0xFFFFFF;
        }
        boolean star = true;
        boolean hole = true;
        if (json.has("applies_to")) {
            star = false;
            hole = false;
            for (JsonElement each : GsonHelper.getAsJsonArray(json, "applies_to")) {
                String what = each.getAsString();
                if (what.equals("star")) star = true;
                else if (what.equals("black_hole")) hole = true;
                else throw new IllegalArgumentException("applies_to is \"star\" or \"black_hole\", not " + what);
            }
        }
        return new Tint(fluid, tag, Math.max(1, GsonHelper.getAsInt(json, "amount", 100)), color, star, hole);
    }
}
