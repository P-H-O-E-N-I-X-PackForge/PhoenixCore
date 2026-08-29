package net.phoenix.core.common.worldgen;

import org.jetbrains.annotations.Nullable;

public record TerrainProfile(
                             String name,
                             long seed,
                             int minY,
                             int maxY,
                             int seaLevel,
                             double baseY,
                             double amplitude,
                             double frequency,
                             int octaves,
                             boolean caves,
                             boolean volumetric,
                             TerrainSampler sampler,
                             @Nullable PhoenixTerrainNoise.WaterMask waterMask) {

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public enum Style {
        HEIGHTMAP,
        VOLUMETRIC,
        RIDGED,
        WARPED,
        TERRACED
    }

    public static class Builder {

        private final String name;
        private long seed = 0L;
        private int minY = -64;
        private int maxY = 320;
        private int seaLevel = 63;
        private double baseY = 64;
        private double amplitude = 80;
        private double frequency = 0.004;
        private int octaves = 5;
        private boolean caves = true;
        private boolean volumetric = false;
        private Style style = Style.HEIGHTMAP;
        private double warpStrength = 12.0;
        private double terraceStep = 8.0;

        private double oceanFrequency = 0.0007;
        private double oceanCoverage = 0.0;
        private double oceanDepth = 24.0;
        private double riverFrequency = 0.0024;
        private double riverWidth = 0.0;
        private double riverDepth = 4.0;

        public Builder(String name) {
            this.name = name;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder minY(int y) {
            this.minY = y;
            return this;
        }

        public Builder maxY(int y) {
            this.maxY = y;
            return this;
        }

        public Builder seaLevel(int y) {
            this.seaLevel = y;
            return this;
        }

        public Builder baseY(double y) {
            this.baseY = y;
            return this;
        }

        public Builder amplitude(double amp) {
            this.amplitude = amp;
            return this;
        }

        public Builder frequency(double freq) {
            this.frequency = freq;
            return this;
        }

        public Builder octaves(int n) {
            this.octaves = n;
            return this;
        }

        public Builder caves(boolean b) {
            this.caves = b;
            return this;
        }

        public Builder volumetric(boolean b) {
            this.volumetric = b;
            if (b) this.style = Style.VOLUMETRIC;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            this.volumetric = style == Style.VOLUMETRIC;
            return this;
        }

        public Builder warpStrength(double strength) {
            this.warpStrength = strength;
            return this;
        }

        public Builder terraceStep(double step) {
            this.terraceStep = step;
            return this;
        }

        public Builder ocean(double frequency, double coverage, double depth) {
            this.oceanFrequency = frequency;
            this.oceanCoverage = coverage;
            this.oceanDepth = depth;
            return this;
        }

        public Builder river(double frequency, double width, double depth) {
            this.riverFrequency = frequency;
            this.riverWidth = width;
            this.riverDepth = depth;
            return this;
        }

        public TerrainProfile build() {
            TerrainSampler terrain = switch (style) {
                case VOLUMETRIC -> PhoenixTerrainNoise.volumetric(seed, baseY, amplitude, frequency, frequency * 2,
                        octaves);
                case RIDGED -> PhoenixTerrainNoise.ridged(seed, baseY, amplitude, frequency, octaves);
                case WARPED -> PhoenixTerrainNoise.warped(seed, baseY, amplitude, frequency, octaves, warpStrength);
                case TERRACED -> PhoenixTerrainNoise.terraced(seed, baseY, amplitude, frequency, octaves, terraceStep);
                case HEIGHTMAP -> PhoenixTerrainNoise.heightmap(seed, baseY, amplitude, frequency, octaves);
            };

            PhoenixTerrainNoise.WaterMask waterMask = PhoenixTerrainNoise.waterMask(seed + 7, seaLevel,
                    oceanFrequency, oceanCoverage, oceanDepth, riverFrequency, riverWidth, riverDepth,
                    terrain, baseY);
            if (waterMask != null) {
                terrain = PhoenixTerrainNoise.withWater(terrain, waterMask, baseY);
            }

            if (caves) {

                double waterFloor = Math.min(seaLevel - oceanDepth, baseY - riverDepth);
                double protectAboveY = Math.min(waterFloor, baseY) - 5.0;
                double taperDistance = 20.0;

                TerrainSampler tunnels = PhoenixTerrainNoise.caves(seed + 1, frequency * 4, 0.05);
                terrain = PhoenixTerrainNoise.withCaves(terrain, tunnels, protectAboveY, taperDistance);

                TerrainSampler caverns = PhoenixTerrainNoise.cheeseCaves(seed + 3, frequency * 1.5, 0.72);
                terrain = PhoenixTerrainNoise.withCaves(terrain, caverns, protectAboveY, taperDistance);
            }

            return new TerrainProfile(name, seed, minY, maxY, seaLevel, baseY, amplitude, frequency, octaves, caves,
                    volumetric, terrain, waterMask);
        }
    }
}
