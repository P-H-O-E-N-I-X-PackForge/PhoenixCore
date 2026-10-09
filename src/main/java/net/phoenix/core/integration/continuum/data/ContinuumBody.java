package net.phoenix.core.integration.continuum.data;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ContinuumBody(
                            ResourceLocation id,
                            ResourceLocation system,
                            @Nullable ResourceLocation parent,
                            Type type,
                            String name,
                            String description,
                            float orbitAu,
                            float periodDays,
                            float phase,
                            float radius,
                            int tripMinutes,
                            String minTier,
                            @Nullable ResourceLocation detectedResearch,
                            @Nullable ResourceLocation surveyedResearch,
                            PlanetParams params,
                            List<Yield> yields,
                            DiscoveryStage initialStage,
                            String loreDetected,
                            String loreSurveyed,
                            @Nullable String discipline,
                            @Nullable PdimCost pdimCost) {

    public record PdimCost(ResourceLocation item, int count) {}

    public record Yield(ResourceLocation item, int min, int max, float chance) {}

    public enum Type {

        PLANET,
        MOON,
        BLACK_HOLE,
        STAR;

        public String label() {
            return switch (this) {
                case PLANET -> "Planet";
                case MOON -> "Moon";
                case BLACK_HOLE -> "Black hole";
                case STAR -> "Star";
            };
        }
    }

    public boolean isCentral() {
        return orbitAu <= 0.0f && parent == null;
    }

    public String lore(DiscoveryStage stage) {
        return switch (stage) {
            case DETECTED -> loreDetected;
            case SURVEYED -> loreSurveyed;
            default -> "";
        };
    }

    public boolean isMoon() {
        return parent != null;
    }
}
