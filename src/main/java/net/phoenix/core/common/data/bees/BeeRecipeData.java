package net.phoenix.core.common.data.bees;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import net.minecraft.world.item.Tier;
import net.phoenix.core.common.data.recipe.records.ApisProgenitorConfig;
import net.phoenix.core.common.data.recipe.records.FullBeeConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.phoenix.core.common.data.materials.PhoenixBeeMaterials.SUGAR_WATER;

@SuppressWarnings("All")
public class BeeRecipeData {

    public static final String MOD_ID = "phoenixcore";
    public static final int FINAL_HONEY_OUTPUT_AMOUNT = 250;

    public static final Material SUGAR_WATER_MATERIAL = SUGAR_WATER;
    public static final int SUGAR_WATER_AMOUNT = 100;
    public static final String HONEY_FLUID = "phoenixcore:honey";

    public static final int DEFAULT_IV_EUT = GTValues.VA[GTValues.IV];
    public static final int DEFAULT_LUV_EUT = GTValues.VA[GTValues.LuV];
    public static final int DEFAULT_ZPM_EUT = GTValues.VA[GTValues.ZPM];
    public static final int DEFAULT_UV_EUT = GTValues.VA[GTValues.UV];



    public static final Map<String, String> ALL_BEE_NAMES = Map.<String, String>ofEntries(
            Map.entry("iron", "Iron Bee"),
            Map.entry("diamond", "Diamond Bee"),
            Map.entry("apatite", "Apatite Bee"),
            Map.entry("copper", "Copper Bee"),
            Map.entry("emerald", "Emerald Bee"),
            Map.entry("gold", "Gold Bee"),
            Map.entry("redstone", "Redstone Bee"),
            Map.entry("lapis", "Lapis Bee"),
            Map.entry("zinc", "Zinc Bee"),
            Map.entry("tin", "Tin Bee"),
            Map.entry("lead", "Lead Bee"),
            Map.entry("silver", "Silver Bee"),
            Map.entry("nickel", "Nickel Bee"),
            Map.entry("coal", "Coal Bee"),
            Map.entry("constantan", "Constantan Bee"),
            Map.entry("electrotine", "Electrotine Bee"),
            Map.entry("invar", "Invar Bee"),
            Map.entry("sapphire", "Sapphire Bee"),
            Map.entry("ruby", "RuBee"),
            Map.entry("amethyst", "Amethyst Bee"),
            Map.entry("topaz", "Topaz Bee"),
            Map.entry("fluorite", "Fluorite Bee"),
            Map.entry("cinnabar", "Cinnabar Bee"),
            Map.entry("realgar", "Realgar Bee"),
            Map.entry("stibnite", "Stibnite Bee"),
            Map.entry("opal", "Opal Bee"),
            Map.entry("pyrope", "Pyrope Bee"),
            Map.entry("scheelite", "Scheelite Bee"),
            Map.entry("cobaltite", "Cobaltite Bee"),
            Map.entry("cobalt", "Cobalt Bee"),
            Map.entry("bauxite", "Bauxite Bee"),
            Map.entry("tungstate", "Tungstate Bee"),
            Map.entry("desh", "Desh Bee"),
            Map.entry("steel", "Steel Bee"),
            Map.entry("tricalcium_phosphate", "Tricalcium Phosphate Bee"),
            Map.entry("pitchblende", "Pitchblende Bee"),
            Map.entry("galena", "Galena Bee"),
            Map.entry("ilmenite", "Ilmenite Bee"),
            Map.entry("niter", "Niter Bee"),
            Map.entry("malachite", "Malachite Bee"),
            Map.entry("obsidian", "Obsidian Bee"),
            Map.entry("blazing", "Blazing Bee"),
            Map.entry("prismarine", "Prismarine Bee"),
            Map.entry("sculk", "Sculk Bee"),
            Map.entry("sponge", "Sponge Bee"),
            Map.entry("frosty", "Frosty Bee"),
            Map.entry("slimy", "Slimy Bee"),
            Map.entry("menril", "Menril Bee"),
            Map.entry("salty", "Salty Bee"),
            Map.entry("steamy", "Steamy Bee"),
            Map.entry("warped", "Warped Shroombee"),
            Map.entry("brown_shroom", "Brown Shroombee"),
            Map.entry("red_shroom", "Red Shroombee"),
            Map.entry("crimson", "Crimson Shroombee"),
            Map.entry("arcane_crystal", "Arcanus Bee"),
            Map.entry("crystalline", "Crystalline Bee"),
            Map.entry("rune", "Rune Bee"),
            Map.entry("withered", "Withered Bee"),
            Map.entry("skeletal", "Skeletal Bee"),
            Map.entry("sticky_resin", "Sticky Resin Bee"),
            Map.entry("zombie", "ZomBee"),
            Map.entry("silicon", "Silicon Bee"),
            Map.entry("silky", "Silky Bee"),
            Map.entry("ghostly", "Ghostly Bee"),
            Map.entry("lepidolite", "Lepidolite Bee"),
            Map.entry("magmatic", "Magmatic Bee"),
            Map.entry("spacial", "Spatial Bee"),
            Map.entry("arcane", "Arcane Bee"),
            Map.entry("cheese", "CheesyB"),
            Map.entry("rocked", "Rocked Bee"),
            Map.entry("super_factory", "Super Factory Bee"),
            Map.entry("fluix", "Fluix Bee"),
            Map.entry("water", "Water Bee"),
            Map.entry("rancher", "Rancher Bee"));

    public static final List<String> LUMBER_LOG_TYPES = List.of(
            "minecraft:oak_log",
            "minecraft:spruce_log",
            "minecraft:birch_log",
            "minecraft:jungle_log",
            "minecraft:acacia_log",
            "minecraft:dark_oak_log",
            "minecraft:mangrove_log",
            "minecraft:cherry_log",
            "minecraft:crimson_stem",
            "gtceu:rubber_log",
            "forbidden_arcanus:edelwood_log",
            "forbidden_arcanus:aurum_log",
            "minecraft:warped_stem",
            "ars_nouveau:red_archwood_log",
            "ars_nouveau:blue_archwood_log",
            "ars_nouveau:green_archwood_log",
            "ars_nouveau:purple_archwood_log");

    public static final List<String> BEE_MATERIAL_TYPES = ALL_BEE_NAMES.keySet().stream().sorted()
            .collect(Collectors.toList());



    private static final List<String> TIER_THREE_BEES = List.of(
             "thorium", "scheelite", "tungstate", "bauxite", "ilmenite", "pitchblende",
             "graphite", "sphalerite", "chromite", "pyrolusite", "platinum", "bismuth",
             "bastnasite", "tetrahedrite", "sulfur", "oilsands", "tantalite", "barite",
             "vanadium_magnetite", "draconic", "pyrochlore", "voidglass_shard",
            "crystallized_fluxstone", "ignisium", "sky_steel");

    private static final List<String> TIER_TWO_BEES = List.of(
             "diamond", "emerald", "netherite", "infinity", "arcane", "arcane_crystal",
            "spacial", "fluix", "obsidian", "withered", "ghostly", "prismarine");

    public static int tierFor(String beeId) {
        if (TIER_THREE_BEES.contains(beeId)) return 3;
        if (TIER_TWO_BEES.contains(beeId)) return 2;
        return 1;
    }

    public static final Map<String, FullBeeConfig> ALL_BEE_CONFIGS = createAllBeeConfigs();

    public static final List<ApisProgenitorConfig> UNIQUE_APIS_PROGENITOR_CONFIGS = List.of(
            progenitor("diamond_progenitor", "ender", "diamond", "4x minecraft:lapis_block", 100, DEFAULT_IV_EUT / 2),
            progenitor("lumber_bee", "green_carpenter_bee", "lumber", "128x #minecraft:logs", 360, DEFAULT_IV_EUT / 2),
            progenitor("quarry_bee", "digger_bee", "quarry", "128x #forge:stone", 360, DEFAULT_IV_EUT / 2),
            progenitor("rancher_from_lumber", "lumber_bee", "rancher", "4x gtceu:skim_milk_bucket", 360, DEFAULT_IV_EUT / 2),

            diamondProg("emerald", "4x minecraft:emerald_block", DEFAULT_IV_EUT),
            diamondProg("pitchblende", "4x gtceu:raw_pitchblende_block", DEFAULT_IV_EUT),
            diamondProg("arcane", "32x ars_nouveau:source_gem_block", DEFAULT_IV_EUT / 2),
            diamondProg("cinnabar", "4x gtceu:raw_cinnabar_block", DEFAULT_IV_EUT / 2),
            diamondProg("apatite", "4x gtceu:raw_apatite_block", DEFAULT_IV_EUT / 2),
            diamondProg("malachite", "4x gtceu:raw_malachite_block", DEFAULT_IV_EUT / 2),

            progenitor("copper_progenitor", "crystalline", "copper", "4x minecraft:copper_block", 360, DEFAULT_IV_EUT / 2),
            progenitor("zinc_from_sweat", "sweat", "zinc", "4x minecraft:iron_block", 360, DEFAULT_IV_EUT / 2),
            progenitor("redstone_from_chocolate", "chocolate_mining", "redstone", "4x minecraft:glowstone", 360, DEFAULT_IV_EUT / 2),
            progenitor("coal_from_leafcutter", "leafcutter", "coal", "4x minecraft:lava_bucket", 360, DEFAULT_IV_EUT / 2),

            diamondProg("thorium", "4x gtceu:thorium_block", DEFAULT_LUV_EUT),
            diamondProg("netherite", "4x minecraft:ancient_debris", DEFAULT_LUV_EUT),
            diamondProg("ender", "4x minecraft:end_stone", DEFAULT_LUV_EUT),
            diamondProg("platinum", "4x gtceu:raw_platinum_block", DEFAULT_LUV_EUT),
            diamondProg("cobalt", "4x gtceu:raw_cobaltite_block", DEFAULT_LUV_EUT),
            diamondProg("draconic", "1x minecraft:dragon_egg", DEFAULT_LUV_EUT),
            diamondProg("sky_steel", "32x megacells:sky_steel_block", DEFAULT_LUV_EUT),
            diamondProg("acidic", "4x gtceu:sulfuric_acid_bucket", DEFAULT_LUV_EUT),

            fluidProg("sticky_resin_from_resin", "resin", "sticky_resin", "productivebees:honey 4000", 360, DEFAULT_IV_EUT / 2),

            new ApisProgenitorConfig("infinity_special", "draconic", tierFor("draconic"), "infinity", tierFor("infinity"),
                    List.of("gtceu:neutronium_ingot"), List.of("gtceu:infinity_fluid 200"), 600, DEFAULT_UV_EUT)
    );


    private static Map<String, FullBeeConfig> createAllBeeConfigs() {
        Map<String, FullBeeConfig> configs = new HashMap<>();

        configs.putAll(createOreBeeConfigs());
        configs.putAll(createMiscBeeConfigs());

        return configs;
    }

    private static Map<String, FullBeeConfig> createOreBeeConfigs() {
        Map<String, FullBeeConfig> configs = new HashMap<>();
        List<String> luvBees = List.of("thorium", "scheelite", "tungstate", "bauxite", "ilmenite", "pitchblende");

        List<String> oreIds = BEE_MATERIAL_TYPES.stream()
                .filter(id -> !List.of("water", "rancher", "steamy", "acidic", "brown_shroom", "red_shroom", "crimson", "warped", "ghostly", "sponge", "sculk", "withered", "skeletal", "blazing").contains(id))
                .collect(Collectors.toList());

        for (String id : oreIds) {
            String blockId, outputId;
            int currentEut = luvBees.contains(id) ? DEFAULT_LUV_EUT : DEFAULT_IV_EUT;
            int d = luvBees.contains(id) ? 400 : 200;
            int outputCount = 5;

            switch (id) {
                case "iron" -> { blockId = "minecraft:iron_block"; outputId = "minecraft:raw_iron"; }
                case "gold" -> { blockId = "minecraft:gold_block"; outputId = "minecraft:raw_gold"; }
                case "copper" -> { blockId = "minecraft:copper_block"; outputId = "minecraft:raw_copper"; }
                case "diamond" -> { blockId = "minecraft:diamond_block"; outputId = "gtceu:raw_diamond"; }
                case "emerald" -> { blockId = "minecraft:emerald_block"; outputId = "gtceu:raw_emerald"; }
                case "lapis" -> { blockId = "minecraft:lapis_block"; outputId = "gtceu:raw_lapis"; }
                case "ruby" -> { blockId = "gtceu:raw_ruby_block"; outputId = "gtceu:raw_ruby"; }
                case "redstone" -> { blockId = "minecraft:redstone_block"; outputId = "minecraft:redstone"; }
                case "coal" -> { blockId = "minecraft:coal_block"; outputId = "gtceu:coal_ore"; }
                case "netherite" -> { blockId = "minecraft:ancient_debris"; outputId = "minecraft:ancient_debris"; }
                case "amethyst" -> { blockId = "minecraft:amethyst_block"; outputId = "gtceu:raw_amethyst"; }
                case "zinc" -> { blockId = "gtceu:zinc_block"; outputId = "gtceu:zinc_ingot"; }
                case "cobalt" -> { blockId = "gtceu:raw_cobalt_block"; outputId = "gtceu:cobalt_dust"; }
                case "desh" -> { blockId = "ad_astra:desh_block"; outputId = "ad_astra:raw_desh"; outputCount = 9; }
                case "steel" -> { blockId = "gtceu:steel_block"; outputId = "gtceu:steel_ingot"; outputCount = 3; }
                case "sky_steel" -> { blockId = "megacells:sky_steel_block"; outputId = "megacells:sky_steel_ingot"; outputCount = 3; }
                case "infinity" -> { blockId = "enderio:grains_of_infinity"; outputId = "enderio:grains_of_infinity"; }
                case "salty" -> { blockId = "gtceu:salt_block"; outputId = "gtceu:raw_salt"; }
                case "fluorite" -> { blockId = "phoenixcore:raw_fluorite_block"; outputId = "phoenixcore:raw_fluorite"; }
                default -> {
                    blockId = "gtceu:" + id + "_block";
                    outputId = "gtceu:" + id + "_ingot";
                }
            }

            configs.put(id, buildConfig(id, blockId, outputId, outputCount, currentEut, d));
        }
        return configs;
    }

    private static Map<String, FullBeeConfig> createMiscBeeConfigs() {
        Map<String, FullBeeConfig> configs = new HashMap<>();
        List<String> miscIds = List.of("water", "rancher", "steamy", "acidic", "brown_shroom", "red_shroom", "crimson", "warped", "ghostly", "sponge", "sculk", "withered", "skeletal", "blazing");

        for (String id : miscIds) {
            String blockId, outputId;
            int outputCount = 1;

            switch (id) {
                case "ghostly" -> { blockId = "minecraft:phantom_membrane"; outputId = "minecraft:ghast_tear"; }
                case "sponge" -> { blockId = "minecraft:sponge"; outputId = "minecraft:sponge"; }
                case "sculk" -> { blockId = "minecraft:sculk"; outputId = "minecraft:sculk"; outputCount = 5; }
                case "withered" -> { blockId = "minecraft:wither_rose"; outputId = "minecraft:wither_rose"; outputCount = 5; }
                case "skeletal" -> { blockId = "minecraft:bone_block"; outputId = "minecraft:bone"; outputCount = 5; }
                case "blazing" -> { blockId = "minecraft:magma_block"; outputId = "minecraft:blaze_powder"; }
                case "water" -> { blockId = "minecraft:water"; outputId = "minecraft:salmon"; }
                case "rancher" -> { blockId = "minecraft:milk"; outputId = "productivebees:honeycomb_milky"; }
                case "steamy" -> { blockId = "gtceu:steam_turbine_rotor"; outputId = "gtceu:steam"; }
                case "acidic" -> { blockId = "gtceu:sulfuric_acid_bucket"; outputId = "gtceu:sulfuric_acid_bucket"; }
                case "brown_shroom" -> { blockId = "minecraft:brown_mushroom_block"; outputId = "minecraft:brown_mushroom"; outputCount = 5; }
                case "red_shroom" -> { blockId = "minecraft:red_mushroom_block"; outputId = "minecraft:red_mushroom"; outputCount = 5; }
                case "crimson" -> { blockId = "minecraft:crimson_stem"; outputId = "minecraft:crimson_fungus"; outputCount = 5; }
                case "warped" -> { blockId = "minecraft:warped_stem"; outputId = "minecraft:warped_fungus"; outputCount = 5; }
                default -> { blockId = "minecraft:air"; outputId = "minecraft:air"; }
            }

            configs.put(id, buildConfig(id, blockId, outputId, outputCount, DEFAULT_IV_EUT, 200));
        }
        return configs;
    }

    private static FullBeeConfig buildConfig(String id, String blockId, String outputId, int count, int eut, int dur) {
        return new FullBeeConfig(id, tierFor(id), blockId, outputId, count,
                eut, dur, eut, 300, eut, 400,
                eut, 100, eut, 300, eut, 400
        );
    }
    private static ApisProgenitorConfig progenitor(String name, String parent, String target, String item, int duration, int eut) {
        return new ApisProgenitorConfig(name, parent, tierFor(parent), target, tierFor(target),
                item.isEmpty() ? List.of() : List.of(item), List.of(), duration, eut);
    }

    private static ApisProgenitorConfig diamondProg(String target, String item, int eut) {
        return progenitor(target + "_progenitor", "diamond", target, item, 360, eut);
    }

    private static ApisProgenitorConfig fluidProg(String name, String parent, String target, String fluid, int duration, int eut) {
        return new ApisProgenitorConfig(name, parent, tierFor(parent), target, tierFor(target),
                List.of(), List.of(fluid), duration, eut);
    }
}
