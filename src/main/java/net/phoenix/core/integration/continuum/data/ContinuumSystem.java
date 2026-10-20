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
                              @Nullable ResourceLocation surveyedResearch,
                              HoleStyle hole) {

    public record HoleStyle(float diskSize, float diskIntensity, float feeding, float tilt, int hotColor, int coolColor,
                            float separation, float massRatio, float periodSeconds) {

        public static final HoleStyle DEFAULT = new HoleStyle(1.0f, 1.0f, 0.0f, 0.075f, -1, -1, 3.2f, 0.6f, 24.0f);

        public HoleStyle withDisk(float size, float intensity) {
            return new HoleStyle(size, intensity, feeding, tilt, hotColor, coolColor, separation, massRatio,
                    periodSeconds);
        }
    }

    public boolean isBlackHole() {
        return "black_hole".equals(starKind);
    }

    public boolean isQuasar() {
        return "quasar".equals(starKind);
    }

    public boolean isWolfRayet() {
        return "wolf_rayet".equals(starKind);
    }

    public int starStyle() {
        return switch (starKind) {
            case "red_giant" -> 1;
            case "brown_dwarf" -> 2;
            case "rapid_rotator" -> 3;
            default -> 0;
        };
    }

    public boolean isPulsar() {
        return "pulsar".equals(starKind);
    }

    public boolean isProtostar() {
        return "protostar".equals(starKind);
    }

    public boolean isPlanetaryNebula() {
        return "planetary_nebula".equals(starKind);
    }

    public boolean isBinaryHole() {
        return "binary_black_hole".equals(starKind);
    }

    public boolean hasHole() {
        return isBlackHole() || isQuasar() || isBinaryHole();
    }
}
