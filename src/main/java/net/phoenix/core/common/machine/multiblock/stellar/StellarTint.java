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
import java.util.Map;

/**
 * Ways to colour what a Stellar Forge makes: a fluid put in the input hatch that is used up to dye the star, or the
 * black hole's disk, in the chamber. They come from data packs ({@code data/<namespace>/phoenixcore/stellar_tints/*.json})
 * and from code ({@link #register}), so other mods and pack makers can add their own:
 *
 * <pre>{@code
 * { "fluid": "gtceu:red_dye",   // a fluid id, or a tag such as "#mypack:glowing_things"
 *   "amount": 100,              // mB used up each time (default 100)
 *   "color": "#ff3030",         // the colour, or "reset" to put it back to the heat's own
 *   "applies_to": ["star", "black_hole"] }   // default both
 * }</pre>
 *
 * A star's dye carries over when it falls in as a black hole.
 */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class StellarTint {

    /** The colour (0xRRGGBB) a tint gives, or -1 to put it back to the heat's own. */
    public record Tint(@Nullable ResourceLocation fluid, @Nullable ResourceLocation tag, int amount, int color,
                       boolean star, boolean blackHole) {

        boolean matches(Fluid candidate) {
            if (fluid != null) return fluid.equals(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(candidate));
            return tag != null && candidate.builtInRegistryHolder().is(TagKey.create(Registries.FLUID, tag));
        }
    }

    private static final Map<ResourceLocation, Tint> FROM_DATA = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Tint> FROM_CODE = new LinkedHashMap<>();

    private StellarTint() {}

    /** Register a tint from code (kept across reloads). */
    public static synchronized void register(ResourceLocation id, Tint tint) {
        FROM_CODE.put(id, tint);
    }

    public static Tint of(ResourceLocation fluid, int amount, int color, boolean star, boolean blackHole) {
        return new Tint(fluid, null, amount, color, star, blackHole);
    }

    /** The tint a stack in a hatch would apply to a body in this stage, if it holds enough. */
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
        protected void apply(Map<ResourceLocation, JsonElement> found, ResourceManager manager, ProfilerFiller profiler) {
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
