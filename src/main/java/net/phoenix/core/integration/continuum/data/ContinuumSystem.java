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

    /**
     * How a black hole's accretion disk looks, from the system's optional "hole" block. Every field has a neutral
     * default, so a system with no block looks exactly as it always did.
     *
     * @param diskSize      multiplier on the disk's outer radius (1 = normal, 2 = twice as wide)
     * @param diskIntensity brightness and density of the disk (1 = normal, 2+ = a furnace)
     * @param feeding       0 - 1: infalling debris streams and bright clumps that reach past the disk
     * @param tilt          0 = disk edge-on, 1 = face-on (a tilt of t is t * 90 degrees; the default 0.075 is the usual nearly edge-on look)
     * @param hotColor      inner disk colour as 0xRRGGBB, or -1 for the default
     * @param coolColor     outer disk colour as 0xRRGGBB, or -1 for the default
     * @param separation    binary holes only: distance between the pair, in shadow radii
     * @param massRatio     binary holes only: companion mass / primary mass (0 - 1); sets its size and orbit
     * @param periodSeconds binary holes only: seconds per orbit of the pair
     */
    public record HoleStyle(float diskSize, float diskIntensity, float feeding, float tilt, int hotColor, int coolColor,
                            float separation, float massRatio, float periodSeconds) {

        public static final HoleStyle DEFAULT = new HoleStyle(1.0f, 1.0f, 0.0f, 0.075f, -1, -1, 3.2f, 0.6f, 24.0f);

        /** A copy with a different disk size and intensity: used to give each hole of a pair its own smaller disk. */
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

    /** A Wolf-Rayet star: a hot, massive star shedding its outer layers (set "star_kind": "wolf_rayet"). */
    public boolean isWolfRayet() {
        return "wolf_rayet".equals(starKind);
    }

    /**
     * How an ordinary star is drawn, chosen by "star_kind": red_giant, brown_dwarf, rapid_rotator; anything else is
     * a plain star. Matches the STYLE_* constants in SceneRenderer.
     */
    public int starStyle() {
        return switch (starKind) {
            case "red_giant" -> 1;
            case "brown_dwarf" -> 2;
            case "rapid_rotator" -> 3;
            default -> 0;
        };
    }

    /** A pulsar: a tiny star with two sweeping beams (set "star_kind": "pulsar"). */
    public boolean isPulsar() {
        return "pulsar".equals(starKind);
    }

    /** A young star in its protoplanetary disk (set "star_kind": "protostar"). */
    public boolean isProtostar() {
        return "protostar".equals(starKind);
    }

    /** A white dwarf inside a planetary nebula (set "star_kind": "planetary_nebula"). */
    public boolean isPlanetaryNebula() {
        return "planetary_nebula".equals(starKind);
    }

    /** Two black holes orbiting each other (set "star_kind": "binary_black_hole"). */
    public boolean isBinaryHole() {
        return "binary_black_hole".equals(starKind);
    }

    public boolean hasHole() {
        return isBlackHole() || isQuasar() || isBinaryHole();
    }
}
