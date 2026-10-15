package net.phoenix.core.integration.continuum.client.pdim;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class PdimClientState {

    private PdimClientState() {}

    private static boolean inside;
    private static float gravity = 1.0f;
    private static Map<ResourceLocation, Float> mine = Map.of();
    private static Map<ResourceLocation, net.phoenix.core.integration.continuum.pdim.PdimCosts.Cost> costs = Map.of();
    private static boolean everyVisit = true;

    public static void accept(boolean insideNow, float gravityNow, Map<ResourceLocation, Float> dimensions,
                              Map<ResourceLocation, net.phoenix.core.integration.continuum.pdim.PdimCosts.Cost> costMap,
                              boolean chargesEveryVisit) {
        inside = insideNow;
        gravity = gravityNow;
        mine = new HashMap<>(dimensions);
        costs = new HashMap<>(costMap);
        everyVisit = chargesEveryVisit;
    }

    public static @Nullable net.phoenix.core.integration.continuum.pdim.PdimCosts.Cost cost(ResourceLocation body) {
        return costs.get(body);
    }

    public static boolean chargesEveryVisit() {
        return everyVisit;
    }

    public static boolean inside() {
        return inside;
    }

    public static float gravity() {
        return gravity;
    }

    public static @Nullable Float dimension(ResourceLocation body) {
        return mine.get(body);
    }
}
