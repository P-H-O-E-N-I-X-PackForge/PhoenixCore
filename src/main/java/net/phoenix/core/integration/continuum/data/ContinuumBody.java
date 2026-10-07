package net.phoenix.core.integration.continuum.data;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A planet, moon or black hole on the map.
 *
 * @param parent           the body a moon orbits, or null
 * @param orbitAu          orbit radius around the star (or around the parent, for moons), in the design doc's AU
 * @param radius           body radius in Earth-ish units; sets the drawn size
 * @param tripMinutes      base real-time mission length before upgrades
 * @param minTier          the progression tier a body is expected to be reachable at (informational for now)
 * @param detectedResearch Conflux research node that makes this body at least Detected, or null
 * @param surveyedResearch Conflux research node that makes this body Surveyed, or null
 * @param params           how it is drawn
 * @param yields           what one extraction probe can bring back from here
 * @param initialStage     the stage a team that has uncovered nothing yet starts at
 */
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
                            DiscoveryStage initialStage) {

    /**
     * One thing a probe may return: between {@code min} and {@code max} of {@code item}, with probability
     * {@code chance}, rolled per probe.
     */
    public record Yield(ResourceLocation item, int min, int max, float chance) {}

    public enum Type {

        PLANET,
        MOON,
        BLACK_HOLE;

        public String label() {
            return switch (this) {
                case PLANET -> "Planet";
                case MOON -> "Moon";
                case BLACK_HOLE -> "Black hole";
            };
        }
    }

    /** True for the object at the centre of its system (the black hole in a black-hole system). */
    public boolean isCentral() {
        return orbitAu <= 0.0f && parent == null;
    }

    public boolean isMoon() {
        return parent != null;
    }
}
