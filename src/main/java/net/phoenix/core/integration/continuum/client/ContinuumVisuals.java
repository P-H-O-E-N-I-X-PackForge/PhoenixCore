package net.phoenix.core.integration.continuum.client;

import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.client.render.PlanetRenderer;

import org.jetbrains.annotations.Nullable;

/**
 * The visual settings every Continuum screen shares: the configured quality and planet style, with the map's Q and C
 * keys able to override them for the rest of the session.
 */
public final class ContinuumVisuals {

    private ContinuumVisuals() {}

    private static @Nullable PlanetRenderer.Quality qualityOverride;
    private static @Nullable Boolean cubeOverride;

    public static PlanetRenderer.Quality quality() {
        if (qualityOverride != null) return qualityOverride;
        return PlanetRenderer.Quality.valueOf(PhoenixConfigs.INSTANCE.continuum.renderQuality.name());
    }

    public static PlanetRenderer.Quality cycleQuality() {
        qualityOverride = quality().next();
        return qualityOverride;
    }

    public static boolean cube() {
        if (cubeOverride != null) return cubeOverride;
        return PhoenixConfigs.INSTANCE.continuum.planetStyle == PhoenixConfigs.ContinuumConfigs.PlanetStyle.CUBE;
    }

    public static boolean toggleCube() {
        cubeOverride = !cube();
        return cubeOverride;
    }

    /** Noise layers in the space backdrop. */
    public static int backdropOctaves() {
        return switch (quality()) {
            case LOW -> 3;
            case MEDIUM -> 4;
            case HIGH -> 5;
        };
    }

    /** Square cells along each face of a cube world. Chunkier for LOW (and for moons, which draw at LOW). */
    public static float cubeCells(PlanetRenderer.Quality quality) {
        return switch (quality) {
            case LOW -> 8.0f;
            case MEDIUM -> 12.0f;
            case HIGH -> 16.0f;
        };
    }
}
