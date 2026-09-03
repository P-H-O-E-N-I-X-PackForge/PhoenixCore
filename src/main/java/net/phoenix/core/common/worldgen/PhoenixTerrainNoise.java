package net.phoenix.core.common.worldgen;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class PhoenixTerrainNoise {

    private PhoenixTerrainNoise() {}

    private static final double REGION_FREQUENCY = 0.0015;
    private static final double REGION_MIN_FACTOR = 0.2;
    private static final long REGION_SEED_OFFSET = 0x9E3779B97F4A7C15L;

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

    private static double[] columnCache() {
        return new double[] { Double.NaN, Double.NaN, 0.0 };
    }

    public static TerrainSampler heightmap(long seed, double baseY, double amplitude, double frequency, int octaves) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        ThreadLocal<double[]> cache = ThreadLocal.withInitial(PhoenixTerrainNoise::columnCache);
        return (x, y, z) -> {
            double[] c = cache.get();
            double height;
            if (c[0] == x && c[1] == z) {
                height = c[2];
            } else {
                double amp = amplitude * regionFactor(regionNoise, x, z);
                height = baseY + amp * fbm(noise, x, 0, z, octaves, frequency);
                c[0] = x; c[1] = z; c[2] = height;
            }
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

    public static TerrainSampler ridged(long seed, double baseY, double amplitude, double frequency, int octaves) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        ThreadLocal<double[]> cache = ThreadLocal.withInitial(PhoenixTerrainNoise::columnCache);
        return (x, y, z) -> {
            double[] c = cache.get();
            double height;
            if (c[0] == x && c[1] == z) {
                height = c[2];
            } else {
                double amp = amplitude * regionFactor(regionNoise, x, z);
                height = baseY + amp * ridgedFbm(noise, x, z, octaves, frequency);
                c[0] = x; c[1] = z; c[2] = height;
            }
            return height - y;
        };
    }

    public static TerrainSampler warped(long seed, double baseY, double amplitude, double frequency, int octaves,
                                        double warpStrength) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise warpNoise = makeNoise(seed ^ 0x5EED5EEDL);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        double warpFreq = frequency * 0.4;
        ThreadLocal<double[]> cache = ThreadLocal.withInitial(PhoenixTerrainNoise::columnCache);
        return (x, y, z) -> {
            double[] c = cache.get();
            double height;
            if (c[0] == x && c[1] == z) {
                height = c[2];
            } else {
                double wx = x + warpNoise.getValue(x * warpFreq, 0, z * warpFreq) * warpStrength;
                double wz = z + warpNoise.getValue(x * warpFreq + 500, 0, z * warpFreq + 500) * warpStrength;
                double amp = amplitude * regionFactor(regionNoise, x, z);
                height = baseY + amp * fbm(noise, wx, 0, wz, octaves, frequency);
                c[0] = x; c[1] = z; c[2] = height;
            }
            return height - y;
        };
    }

    public static TerrainSampler terraced(long seed, double baseY, double amplitude, double frequency, int octaves,
                                          double stepSize) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise regionNoise = makeNoise(seed ^ REGION_SEED_OFFSET);
        ThreadLocal<double[]> cache = ThreadLocal.withInitial(PhoenixTerrainNoise::columnCache);
        return (x, y, z) -> {
            double[] c = cache.get();
            double height;
            if (c[0] == x && c[1] == z) {
                height = c[2];
            } else {
                double amp = amplitude * regionFactor(regionNoise, x, z);
                double rawHeight = baseY + amp * fbm(noise, x, 0, z, octaves, frequency);
                height = Math.floor(rawHeight / stepSize) * stepSize;
                c[0] = x; c[1] = z; c[2] = height;
            }
            return height - y;
        };
    }

    private static final int CAVE_Y_STEP = 4;

    private interface DensityFn {
        double sample(double x, double y, double z);
    }

    private static final class CaveInterpCache {
        double x = Double.NaN, z = Double.NaN;
        int yLo = Integer.MIN_VALUE;
        double vLo, vHi;

        double sample(double x, double y, double z, DensityFn fn) {
            int newYLo = (int) Math.floor(y / CAVE_Y_STEP) * CAVE_Y_STEP;
            if (this.x != x || this.z != z) {
                this.x = x;
                this.z = z;
                this.yLo = newYLo;
                this.vLo = fn.sample(x, newYLo, z);
                this.vHi = fn.sample(x, newYLo + CAVE_Y_STEP, z);
            } else if (newYLo != this.yLo) {
                if (newYLo == this.yLo + CAVE_Y_STEP) {

                    this.vLo = this.vHi;
                    this.vHi = fn.sample(x, newYLo + CAVE_Y_STEP, z);
                } else if (newYLo == this.yLo - CAVE_Y_STEP) {

                    this.vHi = this.vLo;
                    this.vLo = fn.sample(x, newYLo, z);
                } else {
                    this.vLo = fn.sample(x, newYLo, z);
                    this.vHi = fn.sample(x, newYLo + CAVE_Y_STEP, z);
                }
                this.yLo = newYLo;
            }
            double t = (y - yLo) / (double) CAVE_Y_STEP;
            return vLo + (vHi - vLo) * t;
        }
    }

    public static TerrainSampler caves(long seed, double frequency, double threshold) {
        SimplexNoise noise1 = makeNoise(seed);
        SimplexNoise noise2 = makeNoise(seed ^ 0xDEADBEEFL);
        ThreadLocal<CaveInterpCache> cache = ThreadLocal.withInitial(CaveInterpCache::new);
        DensityFn fn = (x, y, z) -> {
            double n1 = noise1.getValue(x * frequency, y * frequency, z * frequency);
            double n2 = noise2.getValue(x * frequency + 100, y * frequency * 0.5, z * frequency + 100);

            double ridge1 = 1.0 - Math.abs(n1);
            double ridge2 = 1.0 - Math.abs(n2);
            double tunnel = ridge1 * ridge2;
            return threshold - tunnel;
        };
        return (x, y, z) -> cache.get().sample(x, y, z, fn);
    }

    public static TerrainSampler cheeseCaves(long seed, double frequency, double threshold) {
        SimplexNoise noise = makeNoise(seed);
        SimplexNoise warpNoise = makeNoise(seed ^ 0x6A5EBEE5L);
        ThreadLocal<CaveInterpCache> cache = ThreadLocal.withInitial(CaveInterpCache::new);
        DensityFn fn = (x, y, z) -> {
            double warp = warpNoise.getValue(x * frequency * 0.4, y * frequency * 0.4, z * frequency * 0.4);
            double n = noise.getValue(x * frequency + warp * 2.0, y * frequency, z * frequency + warp * 2.0);
            return threshold - n;
        };
        return (x, y, z) -> cache.get().sample(x, y, z, fn);
    }

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

        private double riverT(double x, double z) {
            if (riverWidth <= 0) return 0.0;
            double n1 = riverNoise.getValue(x * riverFrequency, 0, z * riverFrequency);
            double n2 = riverNoise.getValue(x * riverFrequency * 3.1 + 4000, 0, z * riverFrequency * 3.1 + 4000);
            double n = n1 + n2 * 0.25;
            return smoothstep(riverWidth, riverWidth * 0.5, Math.abs(n));
        }

        private double localHeight(double x, double z) {
            return baseTerrain.sample((int) x, (int) baseY, (int) z) + baseY;
        }

        private static final double SMOOTH_RADIUS = 10.0;

        private double smoothedLocalHeight(double x, double z) {
            double sum = localHeight(x, z);
            sum += localHeight(x + SMOOTH_RADIUS, z);
            sum += localHeight(x - SMOOTH_RADIUS, z);
            sum += localHeight(x, z + SMOOTH_RADIUS);
            sum += localHeight(x, z - SMOOTH_RADIUS);
            return sum / 5.0;
        }

        double waterT(double x, double z) {
            return Math.max(oceanT(x, z), riverT(x, z));
        }

        double floorY(double x, double z) {
            if (oceanT(x, z) >= riverT(x, z)) {
                return seaLevel - oceanDepth;
            }
            return smoothedLocalHeight(x, z) - riverDepth;
        }

        public double waterSurfaceY(double x, double z) {
            if (oceanT(x, z) >= riverT(x, z)) {
                return seaLevel;
            }
            return smoothedLocalHeight(x, z);
        }

        private static final double WATER_COLUMN_THRESHOLD = 0.35;

        public boolean isWaterColumn(double x, double z) {
            return waterT(x, z) > WATER_COLUMN_THRESHOLD;
        }

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

    public static TerrainSampler withWater(TerrainSampler terrain, WaterMask mask, double baseY) {
        if (mask == null) return terrain;
        return (x, y, z) -> {

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
