package net.phoenix.core.integration.continuum.client.render;

import java.util.List;

/**
 * Everything the planet shader needs to draw one body. Colours are packed {@code 0xRRGGBB}. In the real data
 * model (see the design doc) these come from the body's datapack JSON; milestone 1 uses the hard-coded
 * {@link #PRESETS}.
 */
public record PlanetParams(
                           String name,
                           String description,
                           boolean gasGiant,
                           float seed,
                           float scale,
                           float oceanLevel,
                           float polarIce,
                           float cloudCover,
                           float bump,
                           int low, int mid, int high,
                           int oceanDeep, int oceanShallow,
                           int emissive, float emissiveAmount,
                           int atmoColor, float atmoDensity,
                           float axialTiltDeg,
                           float spinDegPerSec,
                           float cloudSpinDegPerSec) {

    /**
     * What a merely *detected* body looks like: the right silhouette and spin, but flat grey-blue, no air, no
     * clouds, no ocean - a scan return, not a place.
     */
    public PlanetParams ghost() {
        return new PlanetParams(name, description, false, seed, 3.0f, 0.0f, 0.0f, 0.0f, 0.25f,
                0x2a3140, 0x46546a, 0x6b7b92,
                0x000000, 0x000000,
                0x000000, 0.0f,
                0x88aaff, 0.0f,
                axialTiltDeg, spinDegPerSec, 0.0f);
    }

    public static final PlanetParams ANVIL = new PlanetParams(
            "Anvil", "Rocky, industrial haze", false,
            3.0f, 2.6f, 0.0f, 0.12f, 0.15f, 0.9f,
            0x3b332c, 0x7a6450, 0xb89b78,
            0x000000, 0x000000,
            0x000000, 0.0f,
            0xffa45c, 0.7f,
            8f, 4f, 6f);

    public static final PlanetParams RIME = new PlanetParams(
            "Rime", "Ice world", false,
            11.0f, 3.2f, 0.0f, 0.75f, 0.25f, 0.5f,
            0x9fb8d6, 0xd7e6f5, 0xffffff,
            0x000000, 0x000000,
            0x000000, 0.0f,
            0xa8d4ff, 0.55f,
            24f, 3f, 4f);

    public static final PlanetParams EMBER = new PlanetParams(
            "Ember", "Volcanic crust with lava cracks", false,
            7.0f, 3.0f, 0.0f, 0.0f, 0.1f, 1.2f,
            0x1a1210, 0x3a2a22, 0x6a5040,
            0x000000, 0x000000,
            0xff5a14, 1.6f,
            0xff7a3a, 0.5f,
            4f, 2.5f, 5f);

    public static final PlanetParams VERDANT = new PlanetParams(
            "Verdant", "Temperate ocean world", false,
            21.0f, 2.2f, 0.5f, 0.35f, 0.55f, 0.55f,
            0x2f5a2a, 0x6b8a3a, 0xb59b6a,
            0x03123a, 0x1a6f8f,
            0x000000, 0.0f,
            0x6aa8ff, 0.9f,
            19f, 5f, 8f);

    public static final PlanetParams TEMPEST = new PlanetParams(
            "Tempest", "Gas giant, banded storms", true,
            5.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0xb0743a, 0xe3c496, 0xf6ead2,
            0x000000, 0x000000,
            0x000000, 0.0f,
            0xffd9a0, 0.35f,
            3f, 9f, 0f);

    public static final List<PlanetParams> PRESETS = List.of(ANVIL, RIME, EMBER, VERDANT, TEMPEST);
}
