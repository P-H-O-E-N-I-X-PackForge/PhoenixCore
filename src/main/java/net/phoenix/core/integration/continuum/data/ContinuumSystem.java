package net.phoenix.core.integration.continuum.data;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * A star system on the galaxy map.
 *
 * @param galaxyX          position on the galaxy map, in map units (the home system sits at the origin)
 * @param starKind         {@code "star"}, {@code "black_hole"} or {@code "quasar"}
 * @param initialStage     the stage a team that has uncovered nothing yet starts at
 * @param detectedResearch optional Conflux research node that makes this system at least Detected
 * @param surveyedResearch optional Conflux research node that makes this system Surveyed
 */
public record ContinuumSystem(
                              ResourceLocation id,
                              String name,
                              String description,
                              float galaxyX, float galaxyY, float galaxyZ,
                              int starColor,
                              float starRadius,
                              String starKind,
                              DiscoveryStage initialStage,
                              @Nullable ResourceLocation detectedResearch,
                              @Nullable ResourceLocation surveyedResearch) {

    public boolean isBlackHole() {
        return "black_hole".equals(starKind);
    }

    /** A quasar: a black hole with a bright disk and jets. */
    public boolean isQuasar() {
        return "quasar".equals(starKind);
    }

    /** Either kind of black-hole system, drawn with the accretion disk. */
    public boolean hasHole() {
        return isBlackHole() || isQuasar();
    }
}
