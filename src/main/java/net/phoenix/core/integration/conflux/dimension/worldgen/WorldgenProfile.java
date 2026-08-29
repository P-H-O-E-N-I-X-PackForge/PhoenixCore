package net.phoenix.core.integration.conflux.dimension.worldgen;

import java.util.*;

public class WorldgenProfile {

    public final String disciplineId;
    public final TerrainProfile terrain;
    public final BiomeProfile biomes;
    public final DecorationProfile decorations;
    public final CaveProfile caves;
    public final LiquidProfile liquids;
    public final StructureProfile structures;
    public final ColorProfile colors;
    public final ProgressionProfile progression;

    public WorldgenProfile(
                           String disciplineId,
                           TerrainProfile terrain,
                           BiomeProfile biomes,
                           DecorationProfile decorations,
                           CaveProfile caves,
                           LiquidProfile liquids,
                           StructureProfile structures,
                           ColorProfile colors,
                           ProgressionProfile progression) {
        this.disciplineId = disciplineId;
        this.terrain = terrain;
        this.biomes = biomes;
        this.decorations = decorations;
        this.caves = caves;
        this.liquids = liquids;
        this.structures = structures;
        this.colors = colors;
        this.progression = progression;
    }

    public static class TerrainProfile {

        public final float minHeight;
        public final float maxHeight;
        public final float avgHeight;
        public final float verticalScale;
        public final float horizontalScale;
        public final float roughness;
        public final boolean flat;
        public final boolean mountainous;
        public final boolean cavernous;

        public TerrainProfile(
                              float minHeight, float maxHeight, float avgHeight,
                              float verticalScale, float horizontalScale,
                              float roughness, boolean flat, boolean mountainous, boolean cavernous) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.avgHeight = avgHeight;
            this.verticalScale = verticalScale;
            this.horizontalScale = horizontalScale;
            this.roughness = roughness;
            this.flat = flat;
            this.mountainous = mountainous;
            this.cavernous = cavernous;
        }
    }

    public static class BiomeProfile {

        public final List<BiomeDefinition> biomes;
        public final String primaryBiome;
        public final float grassColor;
        public final float foliageColor;
        public final float waterColor;
        public final float skyColor;
        public final float fogColor;

        public BiomeProfile(
                            List<BiomeDefinition> biomes,
                            String primaryBiome,
                            float grassColor, float foliageColor, float waterColor,
                            float skyColor, float fogColor) {
            this.biomes = biomes;
            this.primaryBiome = primaryBiome;
            this.grassColor = grassColor;
            this.foliageColor = foliageColor;
            this.waterColor = waterColor;
            this.skyColor = skyColor;
            this.fogColor = fogColor;
        }
    }

    public static class BiomeDefinition {

        public final String biomeId;
        public final String displayName;
        public final float temperature;
        public final float humidity;
        public final float grassColor;
        public final float foliageColor;
        public final float waterColor;
        public final String surfaceBlock;
        public final String subSurfaceBlock;
        public final float rainfall;

        public BiomeDefinition(
                               String biomeId, String displayName,
                               float temperature, float humidity,
                               float grassColor, float foliageColor, float waterColor,
                               String surfaceBlock, String subSurfaceBlock,
                               float rainfall) {
            this.biomeId = biomeId;
            this.displayName = displayName;
            this.temperature = temperature;
            this.humidity = humidity;
            this.grassColor = grassColor;
            this.foliageColor = foliageColor;
            this.waterColor = waterColor;
            this.surfaceBlock = surfaceBlock;
            this.subSurfaceBlock = subSurfaceBlock;
            this.rainfall = rainfall;
        }
    }

    public static class DecorationProfile {

        public final List<TreeConfig> trees;
        public final List<String> flowers;
        public final List<String> shrubs;
        public final List<String> specialDecorations;
        public final float treeFrequency;
        public final float flowerFrequency;
        public final float vegetationDensity;

        public DecorationProfile(
                                 List<TreeConfig> trees, List<String> flowers, List<String> shrubs,
                                 List<String> specialDecorations,
                                 float treeFrequency, float flowerFrequency, float vegetationDensity) {
            this.trees = trees;
            this.flowers = flowers;
            this.shrubs = shrubs;
            this.specialDecorations = specialDecorations;
            this.treeFrequency = treeFrequency;
            this.flowerFrequency = flowerFrequency;
            this.vegetationDensity = vegetationDensity;
        }
    }

    public static class TreeConfig {

        public final String treeType;
        public final int minHeight;
        public final int maxHeight;
        public final float frequency;

        public TreeConfig(String treeType, int minHeight, int maxHeight, float frequency) {
            this.treeType = treeType;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.frequency = frequency;
        }
    }

    public static class CaveProfile {

        public final boolean enabled;
        public final float caveFrequency;
        public final float caveDensity;
        public final int minCaveSize;
        public final int maxCaveSize;
        public final boolean largeOHCaves;
        public final boolean noodleCaves;
        public final boolean cheeseCaves;
        public final float lavaChance;

        public CaveProfile(
                           boolean enabled, float caveFrequency, float caveDensity,
                           int minCaveSize, int maxCaveSize,
                           boolean largeOHCaves, boolean noodleCaves, boolean cheeseCaves,
                           float lavaChance) {
            this.enabled = enabled;
            this.caveFrequency = caveFrequency;
            this.caveDensity = caveDensity;
            this.minCaveSize = minCaveSize;
            this.maxCaveSize = maxCaveSize;
            this.largeOHCaves = largeOHCaves;
            this.noodleCaves = noodleCaves;
            this.cheeseCaves = cheeseCaves;
            this.lavaChance = lavaChance;
        }
    }

    public static class LiquidProfile {

        public final float waterLakeFrequency;
        public final float lavaLakeFrequency;
        public final int waterLevel;
        public final int lavaLevel;
        public final boolean underwaterCaves;
        public final boolean lavaFeatures;
        public final float surfaceWaterChance;
        public final float surfaceLavaChance;

        public LiquidProfile(
                             float waterLakeFrequency, float lavaLakeFrequency,
                             int waterLevel, int lavaLevel,
                             boolean underwaterCaves, boolean lavaFeatures,
                             float surfaceWaterChance, float surfaceLavaChance) {
            this.waterLakeFrequency = waterLakeFrequency;
            this.lavaLakeFrequency = lavaLakeFrequency;
            this.waterLevel = waterLevel;
            this.lavaLevel = lavaLevel;
            this.underwaterCaves = underwaterCaves;
            this.lavaFeatures = lavaFeatures;
            this.surfaceWaterChance = surfaceWaterChance;
            this.surfaceLavaChance = surfaceLavaChance;
        }
    }

    public static class StructureProfile {

        public final List<StructureConfig> structures;
        public final float structureDensity;
        public final boolean enableStructures;

        public StructureProfile(List<StructureConfig> structures, float structureDensity, boolean enableStructures) {
            this.structures = structures;
            this.structureDensity = structureDensity;
            this.enableStructures = enableStructures;
        }

        public static class StructureConfig {

            public final String structureId;
            public final float rarity;
            public final int minY;
            public final int maxY;

            public StructureConfig(String structureId, float rarity, int minY, int maxY) {
                this.structureId = structureId;
                this.rarity = rarity;
                this.minY = minY;
                this.maxY = maxY;
            }
        }
    }

    public static class ColorProfile {

        public final int grassColor;
        public final int foliageColor;
        public final int waterColor;
        public final int skyColor;
        public final int fogColor;
        public final Map<String, Integer> biomeColors;

        public ColorProfile(
                            int grassColor, int foliageColor, int waterColor,
                            int skyColor, int fogColor,
                            Map<String, Integer> biomeColors) {
            this.grassColor = grassColor;
            this.foliageColor = foliageColor;
            this.waterColor = waterColor;
            this.skyColor = skyColor;
            this.fogColor = fogColor;
            this.biomeColors = biomeColors;
        }
    }

    public static class ProgressionProfile {

        public final Map<String, WorldgenStage> stages;

        public ProgressionProfile(Map<String, WorldgenStage> stages) {
            this.stages = stages;
        }

        public static class WorldgenStage {

            public final String stageName;
            public final boolean unlocksBiomes;
            public final List<String> newBiomes;
            public final boolean unlocksStructures;
            public final List<String> newStructures;
            public final boolean changesColors;
            public final ColorProfile stageColors;
            public final List<String> newDecorations;

            public WorldgenStage(
                                 String stageName,
                                 boolean unlocksBiomes, List<String> newBiomes,
                                 boolean unlocksStructures, List<String> newStructures,
                                 boolean changesColors, ColorProfile stageColors,
                                 List<String> newDecorations) {
                this.stageName = stageName;
                this.unlocksBiomes = unlocksBiomes;
                this.newBiomes = newBiomes;
                this.unlocksStructures = unlocksStructures;
                this.newStructures = newStructures;
                this.changesColors = changesColors;
                this.stageColors = stageColors;
                this.newDecorations = newDecorations;
            }
        }
    }
}
