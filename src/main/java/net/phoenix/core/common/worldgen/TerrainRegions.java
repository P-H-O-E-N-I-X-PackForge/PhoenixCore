package net.phoenix.core.common.worldgen;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;

public final class TerrainRegions {

    public record Spec(String biomeId, TerrainProfile.Style style, double baseY, double amplitude, double frequency,
                       int octaves, double extra, double bias) {}

    private static final double REGION_FREQUENCY = 0.0016;
    private static final double WARP_FREQUENCY = 0.004;
    private static final double WARP_STRENGTH = 70.0;
    private static final double BLEND_BAND = 0.16;

    private static final double MIN_RELIEF = 0.55;

    private final List<Spec> specs;
    private final TerrainSampler[] samplers;
    private final SimplexNoise noise;
    private final ThreadLocal<double[]> column = ThreadLocal
            .withInitial(() -> new double[] { Double.NaN, Double.NaN, 0, 0, 0 });

    public TerrainRegions(long seed, List<Spec> specs) {
        this.specs = List.copyOf(specs);
        this.noise = PhoenixTerrainNoise.makeNoisePublic(seed ^ 0xB10E5B10E5L);
        this.samplers = new TerrainSampler[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            Spec s = specs.get(i);
            long sub = seed + 101L * (i + 1);
            final Spec spec = s;
            samplers[i] = PhoenixTerrainNoise.withMinRelief(MIN_RELIEF, () -> switch (spec.style()) {
                case RIDGED -> PhoenixTerrainNoise.ridged(sub, spec.baseY(), spec.amplitude(), spec.frequency(),
                        spec.octaves());
                case WARPED -> PhoenixTerrainNoise.warped(sub, spec.baseY(), spec.amplitude(), spec.frequency(),
                        spec.octaves(),
                        spec.extra());
                case TERRACED -> PhoenixTerrainNoise.terraced(sub, spec.baseY(), spec.amplitude(), spec.frequency(),
                        spec.octaves(),
                        spec.extra());
                default -> PhoenixTerrainNoise.heightmap(sub, spec.baseY(), spec.amplitude(), spec.frequency(),
                        spec.octaves());
            });
        }
    }

    public List<Spec> specs() {
        return specs;
    }

    private double[] pick(int x, int z) {
        double[] c = column.get();
        if (c[0] == x && c[1] == z) return c;
        double wx = x + noise.getValue(x * WARP_FREQUENCY + 5000.0, 0, z * WARP_FREQUENCY) * WARP_STRENGTH;
        double wz = z + noise.getValue(x * WARP_FREQUENCY, 0, z * WARP_FREQUENCY + 9000.0) * WARP_STRENGTH;
        int best = -1;
        int second = -1;
        double bestScore = -Double.MAX_VALUE;
        double secondScore = -Double.MAX_VALUE;
        for (int i = 0; i < specs.size(); i++) {
            double score = noise.getValue((wx + i * 10037.29) * REGION_FREQUENCY, 0,
                    (wz + i * 7349.13) * REGION_FREQUENCY) + specs.get(i).bias();
            if (score > bestScore) {
                secondScore = bestScore;
                second = best;
                bestScore = score;
                best = i;
            } else if (score > secondScore) {
                secondScore = score;
                second = i;
            }
        }
        double margin = bestScore - secondScore;
        c[0] = x;
        c[1] = z;
        c[2] = best;
        c[3] = second;
        c[4] = second < 0 || margin >= BLEND_BAND ? 0.0 : 0.5 * (1.0 - margin / BLEND_BAND);
        return c;
    }

    public String biomeAt(int x, int z) {
        return specs.get((int) pick(x, z)[2]).biomeId();
    }

    public TerrainSampler sampler() {
        return (x, y, z) -> {
            double[] c = pick(x, z);
            double d = samplers[(int) c[2]].sample(x, y, z);
            double w = c[4];
            if (w <= 0.0) return d;
            double other = samplers[(int) c[3]].sample(x, y, z);
            return d + (other - d) * w;
        };
    }
}
