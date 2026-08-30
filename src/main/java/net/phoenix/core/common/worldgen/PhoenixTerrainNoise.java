package net.phoenix.core.common.worldgen;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class PhoenixTerrainNoise {

    private PhoenixTerrainNoise() {}

    // Broad, low-frequency mask that scales terrain amplitude region by region, so a single
    // discipline dimension mixes calm flats, rolling hills and full-intensity peaks instead of
    // uniform "mountains everywhere" - the same role vanilla's continentalness/erosion noises
    // play in the overworld. REGION_MIN_FACTOR is how weak the calmest regions get relative to
    // the discipline's authored amplitude, not literally flat, so terrain still has some relief.
    private static final double REGION_FREQUENCY = 0.0015;
    private static final double REGION_MIN_FACTOR = 0.2;
    private static final long REGION_SEED_OFFSET = 0x9E3779B97F4A7C15L;

    // Wherever a discipline's spawn platform sits (always world-local origin - see
    // DisciplineStartingArea.ANCHOR), terrain is forced calm within SPAWN_CALM_RADIUS and eases
    // back up to the region mask's normal variety by SPAWN_CALM_RADIUS + SPAWN_TRANSITION_RADIUS,
    // so nobody spawns on the edge of a cliff or ridge regardless of which region they land in.
    private static final double SPAWN_CALM_RADIUS = 48.0;
    private static final double SPAWN_TRANSITION_RADIUS = 160.0;
    private static final double SPAWN_MIN_FACTOR = 0.08;

    private static double regionFactor(SimplexNoise regionNoise, double x, double z) {
        double n = regionNoise.getValue(x * REGION_FREQUENCY, 0, z * REGION_FREQUENCY);
        double base = REGION_MIN_FACTOR + (1.0 - REGION_MIN_FACTOR) * (n * 0.5 + 0.5);

        double dist = Math.sqrt(x * x + z * z);
        double spawnBlend = Math.max(0.0, Math.min(1.0, (dist - SPAWN_CALM_RADIUS) / SPAWN_TRANSITION_RADIUS));
        return SPAWN_MIN_FACTOR + (base - SPAWN_MIN_FACTOR) * spawnBlend;
    }

    public static TerrainSampler heightmap(long seed, double baseY, double amplitude, double frequency, int octaves) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        return (x, y, z) -> {
            double amp = amplitude * regionFactor(regionNoise, x, z);
            double height = baseY + amp * fbm(noise, x, 0, z, octaves, frequency);
            return height - y;
        };
    }

    public static TerrainSampler volumetric(long seed, double baseY, double amplitude, double xzFreq, double yFreq,
                                            int octaves) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        return (x, y, z) -> {
            double amp = amplitude * regionFactor(regionNoise, x, z);
            double d = baseY + amp * fbm3D(noise, x, y, z, octaves, xzFreq, yFreq);
            return d - y;
        };
    }

    /**
     * Sharp ridged peaks instead of smooth rolling hills - each octave folds the noise
     * around zero and squares it, so values near a ridge line get pulled toward the peak
     * instead of smoothly blending, giving jagged mountains rather than round bumps.
     */
    public static TerrainSampler ridged(long seed, double baseY, double amplitude, double frequency, int octaves) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        return (x, y, z) -> {
            double amp = amplitude * regionFactor(regionNoise, x, z);
            double height = baseY + amp * ridgedFbm(noise, x, z, octaves, frequency);
            return height - y;
        };
    }

    /**
     * Heightmap terrain whose sample position is offset by a second, lower-frequency
     * noise field before being read - "bends" the terrain in swirling patterns instead
     * of following straight noise contours, for a distorted/unstable look.
     */
    public static TerrainSampler warped(long seed, double baseY, double amplitude, double frequency, int octaves,
                                        double warpStrength) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise warpNoise = makeNoise(seed ^ 0x5EED5EEDL);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        double warpFreq = frequency * 0.4;
        return (x, y, z) -> {
            double wx = x + warpNoise.getValue(x * warpFreq, 0, z * warpFreq) * warpStrength;
            double wz = z + warpNoise.getValue(x * warpFreq + 500, 0, z * warpFreq + 500) * warpStrength;
            double amp = amplitude * regionFactor(regionNoise, x, z);
            double height = baseY + amp * fbm(noise, wx, 0, wz, octaves, frequency);
            return height - y;
        };
    }

    /**
     * Heightmap terrain quantized into flat steps - reads as stacked plateaus/platforms
     * rather than a continuous slope, for an artificial/industrial feel.
     */
    public static TerrainSampler terraced(long seed, double baseY, double amplitude, double frequency, int octaves,
                                          double stepSize) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        return (x, y, z) -> {
            double amp = amplitude * regionFactor(regionNoise, x, z);
            double rawHeight = baseY + amp * fbm(noise, x, 0, z, octaves, frequency);
            double height = Math.floor(rawHeight / stepSize) * stepSize;
            return height - y;
        };
    }

    public static TerrainSampler caves(long seed, double frequency, double threshold) {
        SimplexNoise noise1 = makeNoise(seed);
        SimplexNoise noise2 = makeNoise(seed ^ 0xDEADBEEFL);
        return (x, y, z) -> {
            double n1 = noise1.getValue(x * frequency, y * frequency, z * frequency);
            double n2 = noise2.getValue(x * frequency + 100, y * frequency * 0.5, z * frequency + 100);

            double tube = Math.abs(n1) + Math.abs(n2);
            return tube - threshold;
        };
    }

    /**
     * Large open pockets rather than thin tunnels - a single 3D noise field carved wherever it
     * crosses a high threshold, so only a sparse fraction of space becomes big connected rooms.
     * Meant to be composed alongside {@link #caves} (tunnels) via two {@link #withCaves} calls
     * for real variety - vanilla-style "cheese" caverns plus "spaghetti" tunnels - never anything
     * water-filled, since neither this nor {@link #caves} ever place liquid, only remove blocks.
     */
    public static TerrainSampler cheeseCaves(long seed, double frequency, double threshold) {
        SimplexNoise noise = makeNoise(seed);
        return (x, y, z) -> {
            double n = noise.getValue(x * frequency, y * frequency, z * frequency);
            return threshold - n;
        };
    }

    /**
     * Carves wherever the cave noise says to, with no regard for how close that is to the
     * surface - fine deep underground, but caves generated this way can (and reliably do) punch
     * straight through to open air near the top, and will tear right through the shallow floor
     * of a river/ocean basin that's already been thinned down to a few blocks by
     * {@link #withWater}, turning what should be a contained body of water into a gaping pit.
     * Prefer {@link #withCaves(TerrainSampler, TerrainSampler, double, double)}, which protects a
     * near-surface band from this; this overload is kept only for cases with no such band to
     * protect.
     */
    public static TerrainSampler withCaves(TerrainSampler terrain, TerrainSampler caves) {
        return (x, y, z) -> {
            double t = terrain.sample(x, y, z);
            double c = caves.sample(x, y, z);

            if (t > 0 && c < 0) {
                return c;
            }
            return t;
        };
    }

    /**
     * Same carving as the two-argument overload, but suppresses it entirely at or above
     * {@code protectAboveY}, tapering smoothly down to full strength over {@code taperDistance}
     * blocks below that - so caves can freely honeycomb the deep underground but can't open a
     * pit at the surface or hollow out the ground under a lake/river/ocean into nothing.
     */
    public static TerrainSampler withCaves(TerrainSampler terrain, TerrainSampler caves,
                                            double protectAboveY, double taperDistance) {
        return (x, y, z) -> {
            double t = terrain.sample(x, y, z);
            if (t <= 0) return t;

            double c = caves.sample(x, y, z);
            double suppress = smoothstep(protectAboveY - taperDistance, protectAboveY, y);
            double protectedC = c + suppress * 50.0;

            if (protectedC < 0) {
                return protectedC;
            }
            return t;
        };
    }

    /**
     * Broad ocean mask plus a narrower, winding river mask, blended smoothly (not a hard on/off
     * threshold, which would read as a cliff at the water's edge). Deliberately split out from
     * the terrain sampler itself: {@link #withWater} uses it to lower terrain toward a basin
     * floor, but the actual decision of where to PLACE water blocks needs to come from this mask
     * directly (see {@link #isWaterColumn}), not from "is this column air below sea level" -
     * that would also flood any cave that happens to open up near sea level, which is exactly
     * the water-filled-cave problem this is meant to avoid.
     */
    public static final class WaterMask {
        private final SimplexNoise oceanNoise;
        private final SimplexNoise riverNoise;
        private final double oceanFrequency;
        private final double oceanCoverage;
        private final double oceanDepth;
        private final double riverFrequency;
        private final double riverWidth;
        private final double riverDepth;
        private final int seaLevel;
        private final TerrainSampler baseTerrain;
        private final double baseY;

        WaterMask(long seed, int seaLevel, double oceanFrequency, double oceanCoverage, double oceanDepth,
                  double riverFrequency, double riverWidth, double riverDepth,
                  TerrainSampler baseTerrain, double baseY) {
            this.oceanNoise = makeNoise(seed ^ 0x0CEA4000BEEFL);
            this.riverNoise = makeNoise(seed ^ 0x21112ADE0000L);
            this.seaLevel = seaLevel;
            this.oceanFrequency = oceanFrequency;
            this.oceanCoverage = oceanCoverage;
            this.oceanDepth = oceanDepth;
            this.riverFrequency = riverFrequency;
            this.riverWidth = riverWidth;
            this.riverDepth = riverDepth;
            this.baseTerrain = baseTerrain;
            this.baseY = baseY;
        }

        private double oceanT(double x, double z) {
            if (oceanCoverage <= 0) return 0.0;
            double n = oceanNoise.getValue(x * oceanFrequency, 0, z * oceanFrequency);
            double threshold = oceanCoverage * 2.0 - 1.0;
            return smoothstep(threshold + 0.12, threshold - 0.12, n);
        }

        // "Distance from this noise field's zero-crossing" is only a consistent-width river
        // where the field's slope is roughly constant. Near any local min/max the slope flattens
        // toward zero, so the same value threshold spans a much larger area there - a single
        // low-frequency field reliably produces one or two spots where the "river" balloons into
        // a big flat blob instead of a winding line, which reads exactly like a flood-fill. Adding
        // a smaller, faster, decorrelated second field breaks up those flat spots (it's very
        // unlikely both fields go flat at the same place) while barely perturbing the overall path.
        private double riverT(double x, double z) {
            if (riverWidth <= 0) return 0.0;
            double n1 = riverNoise.getValue(x * riverFrequency, 0, z * riverFrequency);
            double n2 = riverNoise.getValue(x * riverFrequency * 3.1 + 4000, 0, z * riverFrequency * 3.1 + 4000);
            double n = n1 + n2 * 0.25;
            return smoothstep(riverWidth, riverWidth * 0.5, Math.abs(n));
        }

        /** The natural land height at this column, ignoring any water carving. */
        private double localHeight(double x, double z) {
            return baseTerrain.sample((int) x, (int) baseY, (int) z) + baseY;
        }

        // Real terrain has fine bumps from the higher fbm octaves - sampling the exact per-column
        // height for a river's water level means the "surface" jumps up and down with every bump
        // instead of sitting flat, which is what read as a chaotic wall/wave of water rather than
        // a contained channel. A river's actual water level follows the general lay of the land,
        // not every pebble in the streambed, so this averages a handful of nearby samples (a
        // simple box blur) to get a level that only tracks the broad slope of the terrain.
        private static final double SMOOTH_RADIUS = 10.0;

        private double smoothedLocalHeight(double x, double z) {
            double sum = localHeight(x, z);
            sum += localHeight(x + SMOOTH_RADIUS, z);
            sum += localHeight(x - SMOOTH_RADIUS, z);
            sum += localHeight(x, z + SMOOTH_RADIUS);
            sum += localHeight(x, z - SMOOTH_RADIUS);
            return sum / 5.0;
        }

        /** 0 (dry) to 1 (fully underwater). */
        double waterT(double x, double z) {
            return Math.max(oceanT(x, z), riverT(x, z));
        }

        /**
         * The floor this column's terrain blends toward wherever waterT > 0. Oceans carve to a
         * fixed depth below the world's sea level, same as a real ocean basin - but rivers carve
         * a shallow trench a fixed depth below whatever the LOCAL land height already is. Using
         * the same "depth below sea level" logic for rivers as oceans was the bug here: on any
         * terrain sitting well above sea level (which is most of it - these disciplines use
         * amplitude-driven hills/peaks, not overworld-flat baseY), a river band would carve all
         * the way down to that fixed sea-level-relative floor regardless of how high the
         * surrounding ground was, turning every river-mountain crossing into a flooded slot
         * canyon - a solid wall of water many blocks tall instead of a shallow stream.
         */
        double floorY(double x, double z) {
            if (oceanT(x, z) >= riverT(x, z)) {
                return seaLevel - oceanDepth;
            }
            return smoothedLocalHeight(x, z) - riverDepth;
        }

        /**
         * The Y water should fill up to in this column - sea level for oceans (one continuous
         * flat plane, like a real ocean), but the local land height for rivers, since a river
         * running through a hillside sits at that hillside's height, not at global sea level.
         */
        public double waterSurfaceY(double x, double z) {
            if (oceanT(x, z) >= riverT(x, z)) {
                return seaLevel;
            }
            return smoothedLocalHeight(x, z);
        }

        // Where isWaterColumn switches on - see depthFraction for why the height blend needs
        // this exact same boundary rather than using raw waterT directly.
        private static final double WATER_COLUMN_THRESHOLD = 0.35;

        /**
         * True only for the clearly-underwater core of the ocean/river band, not the thin
         * blended-shoreline edge - used to decide where to actually place water blocks, so a
         * shallow coastline slope doesn't get flooded a block too far inland.
         */
        public boolean isWaterColumn(double x, double z) {
            return waterT(x, z) > WATER_COLUMN_THRESHOLD;
        }

        /**
         * How far this column sits past the isWaterColumn threshold, remapped to its own fresh
         * 0..1 range - exactly 0 right at the shoreline boundary, 1 at full open water. withWater
         * used to blend the floor by raw waterT, which meant a column right at the threshold
         * (waterT just over 0.35) already had its floor blended 35% of the way to the basin
         * floor - water didn't switch on at zero depth and taper up, it switched on already
         * partway deep, with only the remaining 65% of the range left to reach full depth. That
         * abrupt "already deep" onset, compressed into however narrow the noise's spatial
         * transition happens to be, is what read as a wall/cliff of water instead of a wading
         * -depth shore. Remapping so depth is exactly 0 at the same boundary isWaterColumn uses
         * fixes that at the source, independent of the mask noise's own spatial gradient.
         */
        double depthFraction(double x, double z) {
            double t = waterT(x, z);
            if (t <= WATER_COLUMN_THRESHOLD) return 0.0;
            return Math.min(1.0, (t - WATER_COLUMN_THRESHOLD) / (1.0 - WATER_COLUMN_THRESHOLD));
        }

        public int seaLevel() {
            return seaLevel;
        }
    }

    public static WaterMask waterMask(long seed, int seaLevel, double oceanFrequency, double oceanCoverage,
                                       double oceanDepth, double riverFrequency, double riverWidth, double riverDepth,
                                       TerrainSampler baseTerrain, double baseY) {
        if (oceanCoverage <= 0 && riverWidth <= 0) return null;
        return new WaterMask(seed, seaLevel, oceanFrequency, oceanCoverage, oceanDepth,
                riverFrequency, riverWidth, riverDepth, baseTerrain, baseY);
    }

    /**
     * Lowers terrain toward the mask's basin floor wherever it says this column is underwater,
     * blending smoothly into the surrounding land instead of clamping to a hard threshold.
     * Composed onto the base terrain BEFORE caves are added, so the basin is carved from solid
     * ground the same way caves are - this never places water itself, see {@link WaterMask}.
     */
    public static TerrainSampler withWater(TerrainSampler terrain, WaterMask mask, double baseY) {
        if (mask == null) return terrain;
        return (x, y, z) -> {
            // depthFraction, not raw waterT - see WaterMask#depthFraction for why blending by
            // waterT directly made every body of water switch on already partway toward full
            // depth instead of tapering up from a natural, wading-depth shoreline.
            double depthFraction = mask.depthFraction(x, z);
            if (depthFraction <= 0.0) return terrain.sample(x, y, z);

            double floorY = mask.floorY(x, z);
            double rawHeight = terrain.sample(x, (int) baseY, z) + baseY;
            double blendedHeight = rawHeight + (floorY - rawHeight) * depthFraction;
            return blendedHeight - y;
        };
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    public static TerrainSampler vein(long seed, double scale, double threshold) {
        SimplexNoise n1 = makeNoise(seed);
        SimplexNoise n2 = makeNoise(seed ^ 0xCAFEBABEL);
        return (x, y, z) -> {
            double a = Math.abs(n1.getValue(x * scale, y * scale * 0.5, z * scale));
            double b = Math.abs(n2.getValue(x * scale + 31.7, y * scale * 0.5 + 17.3, z * scale - 41.2));
            return (a + b) * 0.5 - threshold;
        };
    }

    private static SimplexNoise makeNoise(long seed) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed));
        return new SimplexNoise(random);
    }

    private static double fbm(SimplexNoise noise, double x, double y, double z, int octaves, double freq) {
        double value = 0;
        double amplitude = 1.0;
        double maxAmp = 0;
        for (int i = 0; i < octaves; i++) {
            value += noise.getValue(x * freq, y * freq, z * freq) * amplitude;
            maxAmp += amplitude;
            amplitude *= 0.5;
            freq *= 2.0;
        }
        return value / maxAmp;
    }

    private static double ridgedFbm(SimplexNoise noise, double x, double z, int octaves, double freq) {
        double value = 0;
        double amplitude = 1.0;
        double maxAmp = 0;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(noise.getValue(x * freq, 0, z * freq));
            value += n * n * amplitude;
            maxAmp += amplitude;
            amplitude *= 0.5;
            freq *= 2.0;
        }
        // n*n is always in [0,1], so remap back to roughly [-1,1] to match fbm()'s range
        return (value / maxAmp) * 2.0 - 1.0;
    }

    private static double fbm3D(SimplexNoise noise, double x, double y, double z, int octaves, double xzFreq,
                                double yFreq) {
        double value = 0;
        double amplitude = 1.0;
        double maxAmp = 0;
        for (int i = 0; i < octaves; i++) {
            value += noise.getValue(x * xzFreq, y * yFreq, z * xzFreq) * amplitude;
            maxAmp += amplitude;
            amplitude *= 0.5;
            xzFreq *= 2.0;
            yFreq *= 2.0;
        }
        return value / maxAmp;
    }
}
