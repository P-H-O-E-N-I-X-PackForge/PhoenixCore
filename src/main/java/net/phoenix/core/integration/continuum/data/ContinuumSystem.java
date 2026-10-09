package net.phoenix.core.integration.continuum.data;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

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

    public boolean isQuasar() {
        return "quasar".equals(starKind);
    }

    public boolean hasHole() {
        return isBlackHole() || isQuasar();
    }
}
