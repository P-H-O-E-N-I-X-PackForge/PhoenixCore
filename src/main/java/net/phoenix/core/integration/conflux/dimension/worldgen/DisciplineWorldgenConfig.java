package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DisciplineWorldgenConfig {

    public final String disciplineId;
    public final OreConfig[] ores;
    public final BiomeBlockPalette blockPalette;
    public final SurfaceRuleSet[] surfaceRules;
    public final StructureSet structures;
    public final DecorationConfig decorations;
    public final CaveConfig caves;

    public DisciplineWorldgenConfig(
                                    String disciplineId,
                                    OreConfig[] ores,
                                    BiomeBlockPalette blockPalette,
                                    SurfaceRuleSet[] surfaceRules,
                                    StructureSet structures,
                                    DecorationConfig decorations,
                                    CaveConfig caves) {
        this.disciplineId = disciplineId;
        this.ores = ores;
        this.blockPalette = blockPalette;
        this.surfaceRules = surfaceRules;
        this.structures = structures;
        this.decorations = decorations;
        this.caves = caves;
    }

    public static DisciplineWorldgenConfig fromJson(JsonObject json, String disciplineId) {
        OreConfig[] ores = parseOres(json);
        BiomeBlockPalette blockPalette = BiomeBlockPalette.fromJson(json);
        SurfaceRuleSet[] surfaceRules = parseSurfaceRules(json);
        StructureSet structures = StructureSet.fromJson(json.getAsJsonObject("structures"));
        DecorationConfig decorations = DecorationConfig.fromJson(json.getAsJsonObject("decorations"));
        CaveConfig caves = CaveConfig.fromJson(json.getAsJsonObject("caves"));

        return new DisciplineWorldgenConfig(
                disciplineId, ores, blockPalette, surfaceRules, structures, decorations, caves);
    }

    private static OreConfig[] parseOres(JsonObject json) {
        if (!json.has("ores")) return new OreConfig[0];

        JsonArray oresArray = json.getAsJsonArray("ores");
        OreConfig[] ores = new OreConfig[oresArray.size()];

        for (int i = 0; i < oresArray.size(); i++) {
            ores[i] = OreConfig.fromJson(oresArray.get(i).getAsJsonObject());
        }

        return ores;
    }

    private static SurfaceRuleSet[] parseSurfaceRules(JsonObject json) {
        if (!json.has("surface_rules")) return new SurfaceRuleSet[0];

        JsonObject rulesObj = json.getAsJsonObject("surface_rules");
        SurfaceRuleSet[] rules = new SurfaceRuleSet[rulesObj.size()];

        int idx = 0;
        for (String stage : rulesObj.keySet()) {
            rules[idx++] = new SurfaceRuleSet(stage, rulesObj.get(stage).getAsString());
        }

        return rules;
    }

    public static class OreConfig {

        public final ResourceLocation oreBlock;
        public final int veinSize;
        public final int veinsPerChunk;
        public final int minY;
        public final int maxY;
        public final String targetBlock;

        public OreConfig(ResourceLocation oreBlock, int veinSize, int veinsPerChunk, int minY, int maxY,
                         String targetBlock) {
            this.oreBlock = oreBlock;
            this.veinSize = veinSize;
            this.veinsPerChunk = veinsPerChunk;
            this.minY = minY;
            this.maxY = maxY;
            this.targetBlock = targetBlock;
        }

        public static OreConfig fromJson(JsonObject json) {
            ResourceLocation ore = new ResourceLocation(json.get("ore").getAsString());
            int veinSize = json.get("vein_size").getAsInt();
            int veinsPerChunk = json.get("veins_per_chunk").getAsInt();
            int minY = json.has("min_y") ? json.get("min_y").getAsInt() : 0;
            int maxY = json.has("max_y") ? json.get("max_y").getAsInt() : 256;
            String targetBlock = json.has("target_block") ? json.get("target_block").getAsString() : "minecraft:stone";

            return new OreConfig(ore, veinSize, veinsPerChunk, minY, maxY, targetBlock);
        }
    }

    public static class BiomeBlockPalette {

        public final Map<String, BlockTypeSet> blocksByStage;

        public BiomeBlockPalette(Map<String, BlockTypeSet> blocksByStage) {
            this.blocksByStage = blocksByStage;
        }

        public static BiomeBlockPalette fromJson(JsonObject json) {
            Map<String, BlockTypeSet> blocksByStage = new HashMap<>();

            if (json.has("block_palette")) {
                JsonObject paletteObj = json.getAsJsonObject("block_palette");
                for (String stage : paletteObj.keySet()) {
                    blocksByStage.put(stage, BlockTypeSet.fromJson(paletteObj.getAsJsonObject(stage)));
                }
            }

            return new BiomeBlockPalette(blocksByStage);
        }

        public static class BlockTypeSet {

            public final String stone;
            public final String dirt;
            public final String grass;
            public final String deepslate;

            public BlockTypeSet(String stone, String dirt, String grass, String deepslate) {
                this.stone = stone;
                this.dirt = dirt;
                this.grass = grass;
                this.deepslate = deepslate;
            }

            public static BlockTypeSet fromJson(JsonObject json) {
                String stone = json.has("stone") ? json.get("stone").getAsString() : "minecraft:stone";
                String dirt = json.has("dirt") ? json.get("dirt").getAsString() : "minecraft:dirt";
                String grass = json.has("grass") ? json.get("grass").getAsString() : "minecraft:grass_block";
                String deepslate = json.has("deepslate") ? json.get("deepslate").getAsString() : "minecraft:deepslate";

                return new BlockTypeSet(stone, dirt, grass, deepslate);
            }
        }
    }

    public static class SurfaceRuleSet {

        public final String stage;
        public final String ruleSetName;

        public SurfaceRuleSet(String stage, String ruleSetName) {
            this.stage = stage;
            this.ruleSetName = ruleSetName;
        }
    }

    public static class StructureSet {

        public final List<StructureConfig> structures;
        public final float density;

        public StructureSet(List<StructureConfig> structures, float density) {
            this.structures = structures;
            this.density = density;
        }

        public static StructureSet fromJson(JsonObject json) {
            List<StructureConfig> structures = new ArrayList<>();
            float density = 1.0f;

            if (json.has("structures")) {
                JsonArray structArray = json.getAsJsonArray("structures");
                for (JsonElement elem : structArray) {
                    structures.add(StructureConfig.fromJson(elem.getAsJsonObject()));
                }
            }

            if (json.has("density")) {
                density = json.get("density").getAsFloat();
            }

            return new StructureSet(structures, density);
        }

        public static class StructureConfig {

            public final String name;
            public final String type;
            public final float rarity;
            public final int minY;
            public final int maxY;

            public StructureConfig(String name, String type, float rarity, int minY, int maxY) {
                this.name = name;
                this.type = type;
                this.rarity = rarity;
                this.minY = minY;
                this.maxY = maxY;
            }

            public static StructureConfig fromJson(JsonObject json) {
                String name = json.get("name").getAsString();
                String type = json.get("type").getAsString();
                float rarity = json.has("rarity") ? json.get("rarity").getAsFloat() : 1.0f;
                int minY = json.has("min_y") ? json.get("min_y").getAsInt() : 0;
                int maxY = json.has("max_y") ? json.get("max_y").getAsInt() : 256;

                return new StructureConfig(name, type, rarity, minY, maxY);
            }
        }
    }

    public static class DecorationConfig {

        public final float grassDensity;
        public final float treeDensity;
        public final float flowerDensity;
        public final Map<String, Integer> customDecorations;

        public DecorationConfig(float grassDensity, float treeDensity, float flowerDensity,
                                Map<String, Integer> customDecorations) {
            this.grassDensity = grassDensity;
            this.treeDensity = treeDensity;
            this.flowerDensity = flowerDensity;
            this.customDecorations = customDecorations;
        }

        public static DecorationConfig fromJson(JsonObject json) {
            float grassDensity = json.has("grass_density") ? json.get("grass_density").getAsFloat() : 1.0f;
            float treeDensity = json.has("tree_density") ? json.get("tree_density").getAsFloat() : 1.0f;
            float flowerDensity = json.has("flower_density") ? json.get("flower_density").getAsFloat() : 1.0f;

            Map<String, Integer> customDecorations = new HashMap<>();
            if (json.has("custom")) {
                JsonObject customObj = json.getAsJsonObject("custom");
                for (String key : customObj.keySet()) {
                    customDecorations.put(key, customObj.get(key).getAsInt());
                }
            }

            return new DecorationConfig(grassDensity, treeDensity, flowerDensity, customDecorations);
        }
    }

    public static class CaveConfig {

        public final float frequency;
        public final float density;
        public final boolean enabled;

        public CaveConfig(float frequency, float density, boolean enabled) {
            this.frequency = frequency;
            this.density = density;
            this.enabled = enabled;
        }

        public static CaveConfig fromJson(JsonObject json) {
            float frequency = json.has("frequency") ? json.get("frequency").getAsFloat() : 1.0f;
            float density = json.has("density") ? json.get("density").getAsFloat() : 1.0f;
            boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();

            return new CaveConfig(frequency, density, enabled);
        }
    }

    @Nullable
    public SurfaceRuleSet getSurfaceRuleForStage(String stage) {
        for (SurfaceRuleSet rule : surfaceRules) {
            if (stage.equals(rule.stage)) {
                return rule;
            }
        }
        return null;
    }

    @Nullable
    public BiomeBlockPalette.BlockTypeSet getBlocksForStage(String stage) {
        return blockPalette.blocksByStage.get(stage);
    }
}
