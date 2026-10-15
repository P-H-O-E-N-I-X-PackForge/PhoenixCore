package net.phoenix.core.integration.conflux.dimension.worldgen;

import java.util.*;

public class WorldgenProfileBuilder {

    private final String disciplineId;
    private WorldgenProfile.TerrainProfile terrain;
    private WorldgenProfile.BiomeProfile biomes;
    private WorldgenProfile.DecorationProfile decorations;
    private WorldgenProfile.CaveProfile caves;
    private WorldgenProfile.LiquidProfile liquids;
    private WorldgenProfile.StructureProfile structures;
    private WorldgenProfile.ColorProfile colors;
    private WorldgenProfile.ProgressionProfile progression;

    public WorldgenProfileBuilder(String disciplineId) {
        this.disciplineId = disciplineId;
    }

    public WorldgenProfileBuilder terrain(
                                          float minHeight, float maxHeight, float avgHeight,
                                          float verticalScale, float horizontalScale) {
        this.terrain = new WorldgenProfile.TerrainProfile(
                minHeight, maxHeight, avgHeight,
                verticalScale, horizontalScale,
                0.5f, false, false, false);
        return this;
    }

    public WorldgenProfileBuilder terrainFull(
                                              float minHeight, float maxHeight, float avgHeight,
                                              float verticalScale, float horizontalScale,
                                              float roughness, boolean flat, boolean mountainous, boolean cavernous) {
        this.terrain = new WorldgenProfile.TerrainProfile(
                minHeight, maxHeight, avgHeight,
                verticalScale, horizontalScale,
                roughness, flat, mountainous, cavernous);
        return this;
    }

    public WorldgenProfileBuilder biomes(
                                         List<WorldgenProfile.BiomeDefinition> biomeList,
                                         String primaryBiome,
                                         int grassColor, int foliageColor, int waterColor, int skyColor, int fogColor) {
        this.biomes = new WorldgenProfile.BiomeProfile(
                biomeList, primaryBiome,
                grassColor, foliageColor, waterColor, skyColor, fogColor);
        return this;
    }

    public WorldgenProfileBuilder decorations(
                                              List<WorldgenProfile.TreeConfig> trees,
                                              List<String> flowers,
                                              List<String> shrubs,
                                              List<String> specialDecorations,
                                              float treeFrequency, float flowerFrequency, float vegetationDensity) {
        this.decorations = new WorldgenProfile.DecorationProfile(
                trees, flowers, shrubs, specialDecorations,
                treeFrequency, flowerFrequency, vegetationDensity);
        return this;
    }

    public WorldgenProfileBuilder caves(
                                        boolean enabled, float caveFrequency, float caveDensity,
                                        int minCaveSize, int maxCaveSize) {
        this.caves = new WorldgenProfile.CaveProfile(
                enabled, caveFrequency, caveDensity,
                minCaveSize, maxCaveSize,
                true, true, true,
                0.1f);
        return this;
    }

    public WorldgenProfileBuilder cavesFull(
                                            boolean enabled, float caveFrequency, float caveDensity,
                                            int minCaveSize, int maxCaveSize,
                                            boolean largeOHCaves, boolean noodleCaves, boolean cheeseCaves,
                                            float lavaChance) {
        this.caves = new WorldgenProfile.CaveProfile(
                enabled, caveFrequency, caveDensity,
                minCaveSize, maxCaveSize,
                largeOHCaves, noodleCaves, cheeseCaves,
                lavaChance);
        return this;
    }

    public WorldgenProfileBuilder liquids(
                                          float waterLakeFrequency, float lavaLakeFrequency,
                                          int waterLevel, int lavaLevel) {
        this.liquids = new WorldgenProfile.LiquidProfile(
                waterLakeFrequency, lavaLakeFrequency,
                waterLevel, lavaLevel,
                false, false,
                0.1f, 0.1f);
        return this;
    }

    public WorldgenProfileBuilder liquidsFull(
                                              float waterLakeFrequency, float lavaLakeFrequency,
                                              int waterLevel, int lavaLevel,
                                              boolean underwaterCaves, boolean lavaFeatures,
                                              float surfaceWaterChance, float surfaceLavaChance) {
        this.liquids = new WorldgenProfile.LiquidProfile(
                waterLakeFrequency, lavaLakeFrequency,
                waterLevel, lavaLevel,
                underwaterCaves, lavaFeatures,
                surfaceWaterChance, surfaceLavaChance);
        return this;
    }

    public WorldgenProfileBuilder structures(
                                             List<WorldgenProfile.StructureProfile.StructureConfig> structureList,
                                             float structureDensity) {
        this.structures = new WorldgenProfile.StructureProfile(
                structureList, structureDensity, !structureList.isEmpty());
        return this;
    }

    public WorldgenProfileBuilder colors(
                                         int grassColor, int foliageColor, int waterColor, int skyColor, int fogColor) {
        this.colors = new WorldgenProfile.ColorProfile(
                grassColor, foliageColor, waterColor, skyColor, fogColor,
                new HashMap<>());
        return this;
    }

    public WorldgenProfileBuilder progression(Map<String, WorldgenProfile.ProgressionProfile.WorldgenStage> stages) {
        this.progression = new WorldgenProfile.ProgressionProfile(stages);
        return this;
    }

    public WorldgenProfile build() {
        if (terrain == null) {
            terrain = new WorldgenProfile.TerrainProfile(
                    0, 256, 64, 1.0f, 1.0f, 0.5f, false, false, false);
        }
        if (biomes == null) {
            biomes = new WorldgenProfile.BiomeProfile(
                    new ArrayList<>(), "plains",
                    0x92BD59, 0x77AB2F, 0x3F76E4, 0x87CEEB, 0xB3D9FF);
        }
        if (decorations == null) {
            decorations = new WorldgenProfile.DecorationProfile(
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    0.5f, 0.5f, 0.5f);
        }
        if (caves == null) {
            caves = new WorldgenProfile.CaveProfile(
                    true, 0.7f, 0.6f, 5, 15,
                    true, true, true, 0.1f);
        }
        if (liquids == null) {
            liquids = new WorldgenProfile.LiquidProfile(
                    0.1f, 0.05f, 64, 32,
                    false, false, 0.1f, 0.05f);
        }
        if (structures == null) {
            structures = new WorldgenProfile.StructureProfile(
                    new ArrayList<>(), 1.0f, false);
        }
        if (colors == null) {
            colors = new WorldgenProfile.ColorProfile(
                    0x92BD59, 0x77AB2F, 0x3F76E4, 0x87CEEB, 0xB3D9FF,
                    new HashMap<>());
        }
        if (progression == null) {
            progression = new WorldgenProfile.ProgressionProfile(new HashMap<>());
        }

        return new WorldgenProfile(
                disciplineId, terrain, biomes, decorations, caves,
                liquids, structures, colors, progression);
    }
}
