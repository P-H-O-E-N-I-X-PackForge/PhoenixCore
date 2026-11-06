package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.world.level.Level;

public class DimensionSignatureFeatures {

    public static class PhoenixSignature {

        public static final String NAME = "Volcanic Defiance";
        public static final String DESCRIPTION = "Gravity anomalies, floating lava islands, inverted sky terrain";

        public static final boolean GRAVITY_ANOMALIES = true;
        public static final boolean INVERTED_TERRAIN = true;
        public static final boolean LAVA_FALLS = true;
        public static final boolean FLOATING_ISLANDS = true;
        public static final boolean ASH_RAIN = true;
        public static final boolean SKY_VOLCANOES = true;

        public static final int SKY_COLOR = 0xFF6347;
        public static final int PARTICLE_COLOR = 0xFFA500;
        public static final float GRAVITY_SCALE = 0.5f;
        public static final float PARTICLE_DENSITY = 0.8f;

        public static final String VISUAL_EFFECT = """
                Sky Volcanoes: Massive volcanic formations visible in the sky,
                erupting with lava and ash. Players feel tiny.

                Inverted Terrain: Platforms float upside-down above you.
                Walk on both sides of the same block.

                Floating Islands: Islands slowly bob up and down with gentle motion.
                Creates sense of movement and life.

                Gravity Anomalies: Zones where gravity changes direction or strength.
                Walk on walls, float in air.
                """;
    }

    public static class SculkSignature {

        public static final String NAME = "Living Darkness";
        public static final String DESCRIPTION = "Bioluminescent ecosystem, sound creates ripples, entity awareness";

        public static final boolean BIOLUMINESCENCE = true;
        public static final boolean SOUND_RIPPLES = true;
        public static final boolean SCULK_EYES = true;
        public static final boolean TENDRILS = true;
        public static final boolean AMBIENT_WHISPERS = true;
        public static final boolean DARKNESS_ENTITIES = true;

        public static final int GLOW_COLOR = 0x00FF88;
        public static final float EYE_GLOW = 0.9f;
        public static final float RIPPLE_INTENSITY = 0.7f;
        public static final float BIOLUM_RANGE = 24.0f;

        public static final String VISUAL_EFFECT = """
                Living Bioluminescence: Everything glows in eerie cyan/blue.
                Plants illuminate as you pass. Darkness isn't empty—it's watching.

                Sound Ripples: Every sound creates visible ripples in the environment.
                Walk quietly or disturb reality itself.

                Sculk Tendrils: The world grows around you. Tendrils extend where
                you walk. Dimension is ALIVE and aware of your presence.

                The Eyes: Thousands of sculk "eyes" open and close in the darkness.
                They follow you. Genuinely unsettling.
                """;
    }

    public static class VoidSignature {

        public static final String NAME = "Cosmic Anomaly";
        public static final String DESCRIPTION = "Floating islands, planetary systems, gravity manipulation, rifts";

        public static final boolean GRAVITY_BRIDGES = true;
        public static final boolean ORBITING_PLANETS = true;
        public static final boolean TELEPORT_RIFTS = true;
        public static final boolean NEGATIVE_SPACE = true;
        public static final boolean COSMIC_PARTICLES = true;
        public static final boolean IMPOSSIBLE_GEOMETRY = true;

        public static final int SPACE_COLOR = 0x191970;
        public static final int PARTICLE_COLOR = 0x00FFFF;
        public static final float GRAVITY_VARIANCE = 2.0f;
        public static final float PARTICLE_DENSITY = 0.6f;

        public static final String VISUAL_EFFECT = """
                Orbiting Planets: Massive celestial bodies orbit overhead. Stars
                move across the sky. You're standing on a cosmic platform.

                Gravity Bridges: Invisible gravity connects floating islands.
                Walk across empty space on invisible paths. Mind-bending.

                Dimensional Rifts: Tears in reality show other dimensions
                swirling inside. Wormhole effects. Genuinely alien.

                Impossible Geometry: Stairs that go up but arrive lower.
                Doors that open onto the void. Escher-level disorientation.
                """;
    }

    public static class SealedASignature {

        public static final String NAME = "Neon Megacity";
        public static final String DESCRIPTION = "Towering structures, automated systems, neon lighting, mechanical drones";

        public static final boolean MEGA_STRUCTURES = true;
        public static final boolean MOVING_PLATFORMS = true;
        public static final boolean NEON_LIGHTING = true;
        public static final boolean FLYING_DRONES = true;
        public static final boolean MECHANICAL_SOUNDS = true;
        public static final boolean LIGHT_BEAMS = true;

        public static final int NEON_PINK = 0xFF1493;
        public static final int NEON_CYAN = 0x00FFFF;
        public static final int NEON_YELLOW = 0xFFFF00;
        public static final float STRUCTURE_HEIGHT = 150f;
        public static final float DRONE_SPEED = 0.5f;

        public static final String VISUAL_EFFECT = """
                Mega-Structures: Towers stretch 100+ blocks into the sky.
                Vertical city layout. Look up and see clouds touching distant towers.

                Neon Ambiance: Every surface glows with neon pink, cyan, yellow.
                Cyberpunk aesthetic. Feels like Blade Runner.

                Moving Platforms: Conveyors move cargo. Elevators descend from sky.
                You ride mechanical systems. World feels ALIVE and mechanical.

                Drone Traffic: Automated drones fly overhead on patrol routes.
                They scan, beep, move cargo. Distant mechanical sounds.
                """;
    }

    public static class SealedBSignature {

        public static final String NAME = "Reality Break";
        public static final String DESCRIPTION = "Dimensional rifts, inverted colors, distorted space, warped geometry";

        public static final boolean DIMENSIONAL_RIFTS = true;
        public static final boolean COLOR_INVERSION = true;
        public static final boolean REALITY_GLITCHES = true;
        public static final boolean WARPED_TERRAIN = true;
        public static final boolean FLOATING_DEBRIS = true;
        public static final boolean DISTORTION_WAVES = true;

        public static final int RIFT_COLOR = 0xFFFFFF;
        public static final int INVERSION_THRESHOLD = 0x7F7F7F;
        public static final float DISTORTION_STRENGTH = 0.8f;
        public static final float GLITCH_FREQUENCY = 0.3f;

        public static final String VISUAL_EFFECT = """
                Dimensional Rifts: Tears in the sky show inverted dimensions
                flickering in and out. Multiverse collision effect.

                Color Inversion: Zones where all colors flip. Red becomes cyan,
                blue becomes yellow. Genuinely disorienting.

                Reality Glitches: Blocks occasionally swap position, creating
                stuttering/teleportation effects. Feels like a corrupted world.

                Floating Debris: Chunks of terrain float independently. Blocks
                aren't connected—they drift slowly through space.
                """;
    }

    public static class FeatureApplier {

        public static void applyPhoenixFeatures(Level level) {}

        public static void applySculkFeatures(Level level) {}

        public static void applyVoidFeatures(Level level) {}

        public static void applySealedAFeatures(Level level) {}

        public static void applySealedBFeatures(Level level) {}
    }

    public static class SignatureConfig {

        public final String dimensionId;
        public final String featureName;
        public final String description;
        public final boolean enabled;
        public final float intensity;

        public SignatureConfig(String dimensionId, String featureName, String description, boolean enabled,
                               float intensity) {
            this.dimensionId = dimensionId;
            this.featureName = featureName;
            this.description = description;
            this.enabled = enabled;
            this.intensity = intensity;
        }
    }
}
