package net.phoenix.core.common.data.bees;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

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
    public static final String HONEY_FLUID = "gtceu:honey";

    public static final int DEFAULT_IV_EUT = GTValues.VA[GTValues.IV];
    public static final int DEFAULT_LUV_EUT = GTValues.VA[GTValues.LuV];
    public static final int DEFAULT_ZPM_EUT = GTValues.VA[GTValues.ZPM];
    public static final int DEFAULT_UV_EUT = GTValues.VA[GTValues.UV];

    /**
     * Map of beeId -> display name.
     */
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

    /**
     * Full configs used by recipe generation. Includes tier selection for TagPrefix bees.
     */
    private static final List<String> TIER_THREE_BEES = List.of(
            // "thorium", "scheelite", "tungstate", "bauxite", "ilmenite", "pitchblende",
            // "graphite", "sphalerite", "chromite", "pyrolusite", "platinum", "bismuth",
            // "bastnasite", "tetrahedrite", "sulfur", "oilsands", "tantalite", "barite",
            // "vanadium_magnetite", "draconic", "pyrochlore", "voidglass_shard",
            "crystallized_fluxstone", "ignisium", "sky_steel");

    private static final List<String> TIER_TWO_BEES = List.of(
            // "diamond", "emerald", "netherite", "infinity", "arcane", "arcane_crystal",
            "spacial", "fluix", "obsidian", "withered", "ghostly", "prismarine");

    public static int tierFor(String beeId) {
        if (TIER_THREE_BEES.contains(beeId)) return 3;
        if (TIER_TWO_BEES.contains(beeId)) return 2;
        return 1;
    }

    public static final Map<String, FullBeeConfig> ALL_BEE_CONFIGS = createAllBeeConfigs();

    public static final List<ApisProgenitorConfig> UNIQUE_APIS_PROGENITOR_CONFIGS = List.of(
            new ApisProgenitorConfig(
                    "diamond_progenitor",
                    "ender", tierFor("ender"),
                    "diamond", tierFor("diamond"),
                    List.of("4x minecraft:lapis_block"),
                    List.of(),
                    100, DEFAULT_IV_EUT / 2)

    /*
     * ,
     * new ApisProgenitorConfig(
     * "lumber_bee",
     * "green_carpenter_bee", tierFor("green_carpenter_bee"),
     * "lumber", tierFor("lumber"),
     * List.of("128x #minecraft:logs"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "quarry_bee",
     * "digger_bee", tierFor("digger_bee"),
     * "quarry", tierFor("quarry"),
     * List.of("128x #forge:stone"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "lumber_from_green_carpenter",
     * "green_carpenter_bee", tierFor("green_carpenter_bee"),
     * "lumber", tierFor("lumber"),
     * List.of("128x #minecraft:logs"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "quarry_from_digger",
     * "digger_bee", tierFor("digger_bee"),
     * "quarry", tierFor("quarry"),
     * List.of("128x #forge:stone"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "rancher_from_lumber",
     * "lumber_bee", tierFor("lumber_bee"),
     * "rancher", tierFor("rancher"),
     * List.of("4x gtceu:skim_milk_bucket"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * 
     * // NOTE: your original file had a duplicated "diamond_progenitor" entry; keeping a single copy here.
     * new ApisProgenitorConfig(
     * "emerald_progenitor",
     * "diamond", tierFor("diamond"),
     * "emerald", tierFor("emerald"),
     * List.of("4x minecraft:emerald_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "pitchblende_progenitor",
     * "diamond", tierFor("diamond"),
     * "pitchblende", tierFor("pitchblende"),
     * List.of("4x gtceu:raw_pitchblende_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "copper_progenitor",
     * "crystalline", tierFor("crystalline"),
     * "copper", tierFor("copper"),
     * List.of("4x minecraft:copper_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "experience_progenitor",
     * "emerald", tierFor("emerald"),
     * "experience", tierFor("experience"),
     * List.of("64x crazyae2addons:xp_shard"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "arcane_progenitor",
     * "diamond", tierFor("diamond"),
     * "arcane", tierFor("arcane"),
     * List.of("32x ars_nouveau:source_gem_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "cinnabar_progenitor",
     * "diamond", tierFor("diamond"),
     * "cinnabar", tierFor("cinnabar"),
     * List.of("4x gtceu:raw_cinnabar_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "crimson_progenitor",
     * "brown_shroom", tierFor("brown_shroom"),
     * "crimson", tierFor("crimson"),
     * List.of("4x minecraft:crimson_fungus"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "silver_progenitor",
     * "iron", tierFor("iron"),
     * "silver", tierFor("silver"),
     * List.of("4x gtceu:raw_silver_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "infinity_progenitor",
     * "blazing", tierFor("blazing"),
     * "infinity", tierFor("infinity"),
     * List.of("32x minecraft:obsidian"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "tungstate_progenitor",
     * "silver", tierFor("silver"),
     * "tungstate", tierFor("tungstate"),
     * List.of("32x gtceu:raw_tungstate_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "tricalcium_phosphate_progenitor",
     * "apatite", tierFor("apatite"),
     * "tricalcium_phosphate", tierFor("tricalcium_phosphate"),
     * List.of("4x gtceu:raw_tricalcium_phosphate_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "apatite_progenitor",
     * "diamond", tierFor("diamond"),
     * "apatite", tierFor("apatite"),
     * List.of("4x gtceu:raw_apatite_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "spacial_progenitor",
     * "crystalline", tierFor("crystalline"),
     * "spacial", tierFor("spacial"),
     * List.of("4x gtceu:raw_certus_quartz_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "arcane_crystal_progenitor",
     * "diamond", tierFor("diamond"),
     * "arcane_crystal", tierFor("arcane_crystal"),
     * List.of("4x forbidden_arcanus:arcane_crystal_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "fluix_progenitor",
     * "spacial", tierFor("spacial"),
     * "fluix", tierFor("fluix"),
     * List.of("32x ae2:fluix_pearl"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "malachite_progenitor",
     * "diamond", tierFor("diamond"),
     * "malachite", tierFor("malachite"),
     * List.of("4x gtceu:raw_malachite_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "invar_progenitor",
     * "iron", tierFor("iron"),
     * "invar", tierFor("invar"),
     * List.of("4x gtceu:raw_nickel_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * 
     * new ApisProgenitorConfig(
     * "lepidolite_from_mason",
     * "mason", tierFor("mason"),
     * "lepidolite", tierFor("lepidolite"),
     * List.of("4x gtceu:raw_lepidolite_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "blazing_from_nomad",
     * "nomad", tierFor("nomad"),
     * "blazing", tierFor("blazing"),
     * List.of("16x minecraft:blaze_rod"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "sculk_from_digger",
     * "digger", tierFor("digger"),
     * "sculk", tierFor("sculk"),
     * List.of("64x minecraft:sculk"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "zinc_from_sweat",
     * "sweat", tierFor("sweat"),
     * "zinc", tierFor("zinc"),
     * List.of("4x minecraft:iron_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "menril_from_neon",
     * "neon_cuckoo", tierFor("neon_cuckoo"),
     * "menril", tierFor("menril"),
     * List.of("4x integrateddynamics:menril_log"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "niter_from_creeper",
     * "creeper", tierFor("creeper"),
     * "niter", tierFor("niter"),
     * List.of("4x minecraft:coal_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "redstone_from_chocolate",
     * "chocolate_mining", tierFor("chocolate_mining"),
     * "redstone", tierFor("redstone"),
     * List.of("4x minecraft:glowstone"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "silky_from_reed",
     * "reed", tierFor("reed"),
     * "silky", tierFor("silky"),
     * List.of("64x minecraft:string"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "coal_from_leafcutter",
     * "leafcutter", tierFor("leafcutter"),
     * "coal", tierFor("coal"),
     * List.of("4x minecraft:lava_bucket"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "silicon_from_nomad",
     * "nomad", tierFor("nomad"),
     * "silicon", tierFor("silicon"),
     * List.of("16x gtceu:silicon_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "obsidian_from_sweat",
     * "sweat", tierFor("sweat"),
     * "obsidian", tierFor("obsidian"),
     * List.of("4x minecraft:magma_cream"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "amber_from_resin",
     * "resin", tierFor("resin"),
     * "amber", tierFor("amber"),
     * List.of("168x gtceu:styrene_butadiene_rubber_dust"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "nickel_from_sweat",
     * "sweat", tierFor("sweat"),
     * "nickel", tierFor("nickel"),
     * List.of("4x gtceu:nickel_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "lead_from_blue",
     * "blue_banded", tierFor("blue_banded"),
     * "lead", tierFor("lead"),
     * List.of("4x minecraft:iron_block"),
     * List.of(),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * new ApisProgenitorConfig(
     * "sticky_resin_from_resin",
     * "resin", tierFor("resin"),
     * "sticky_resin", tierFor("sticky_resin"),
     * List.of(),
     * List.of("productivebees:honey 4000"),
     * 360, DEFAULT_IV_EUT / 2
     * ),
     * 
     * new ApisProgenitorConfig(
     * "thorium_progenitor",
     * "diamond", tierFor("diamond"),
     * "thorium", tierFor("thorium"),
     * List.of("4x gtceu:thorium_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "graphite_progenitor",
     * "diamond", tierFor("diamond"),
     * "graphite", tierFor("graphite"),
     * List.of("4x gtceu:raw_graphite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "sphalerite_progenitor",
     * "diamond", tierFor("diamond"),
     * "sphalerite", tierFor("sphalerite"),
     * List.of("4x gtceu:raw_sphalerite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "netherite_progenitor",
     * "diamond", tierFor("diamond"),
     * "netherite", tierFor("netherite"),
     * List.of("4x minecraft:ancient_debris"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "ender_progenitor",
     * "diamond", tierFor("diamond"),
     * "ender", tierFor("ender"),
     * List.of("4x minecraft:end_stone"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "acidic_progenitor",
     * "diamond", tierFor("diamond"),
     * "acidic", tierFor("acidic"),
     * List.of("4x gtceu:sulfuric_acid_bucket"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "chromite_progenitor",
     * "diamond", tierFor("diamond"),
     * "chromite", tierFor("chromite"),
     * List.of("4x gtceu:raw_chromite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "pyrolusite_progenitor",
     * "diamond", tierFor("diamond"),
     * "pyrolusite", tierFor("pyrolusite"),
     * List.of("4x gtceu:raw_pyrolusite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "platinum_progenitor",
     * "diamond", tierFor("diamond"),
     * "platinum", tierFor("platinum"),
     * List.of("4x gtceu:raw_platinum_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "bismuth_progenitor",
     * "diamond", tierFor("diamond"),
     * "bismuth", tierFor("bismuth"),
     * List.of("4x gtceu:bismuth_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "glowing_progenitor",
     * "diamond", tierFor("diamond"),
     * "glowing", tierFor("glowing"),
     * List.of("4x minecraft:glowstone"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "bastnasite_progenitor",
     * "diamond", tierFor("diamond"),
     * "bastnasite", tierFor("bastnasite"),
     * List.of("4x gtceu:raw_bastnasite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "tetrahedrite_progenitor",
     * "diamond", tierFor("diamond"),
     * "tetrahedrite", tierFor("tetrahedrite"),
     * List.of("4x gtceu:raw_tetrahedrite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "sulfur_progenitor",
     * "diamond", tierFor("diamond"),
     * "sulfur", tierFor("sulfur"),
     * List.of("4x gtceu:raw_sulfur_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "oilsands_progenitor",
     * "diamond", tierFor("diamond"),
     * "oilsands", tierFor("oilsands"),
     * List.of("4x gtceu:raw_oilsands_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "cobalt_progenitor",
     * "diamond", tierFor("diamond"),
     * "cobalt", tierFor("cobalt"),
     * List.of("4x gtceu:raw_cobaltite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "tantalite_progenitor",
     * "diamond", tierFor("diamond"),
     * "tantalite", tierFor("tantalite"),
     * List.of("4x gtceu:raw_tantalite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "barite_progenitor",
     * "diamond", tierFor("diamond"),
     * "barite", tierFor("barite"),
     * List.of("4x gtceu:raw_barite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "vanadium_magnetite_progenitor",
     * "diamond", tierFor("diamond"),
     * "vanadium_magnetite", tierFor("vanadium_magnetite"),
     * List.of("4x gtceu:raw_vanadium_magnetite_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "draconic_progenitor",
     * "diamond", tierFor("diamond"),
     * "draconic", tierFor("draconic"),
     * List.of("1x minecraft:dragon_egg"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "pyrochlore_progenitor",
     * "diamond", tierFor("diamond"),
     * "pyrochlore", tierFor("pyrochlore"),
     * List.of("4x gtceu:raw_pyrochlore_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "voidglass_shard_progenitor",
     * "diamond", tierFor("diamond"),
     * "voidglass_shard", tierFor("voidglass_shard"),
     * List.of("32x phoenixcore:raw_voidglass_shard_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "crystallized_fluxstone_progenitor",
     * "diamond", tierFor("diamond"),
     * "crystallized_fluxstone", tierFor("crystallized_fluxstone"),
     * List.of("32x gtce:raw_crystalized_fluxstone_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "ignisium_progenitor",
     * "diamond", tierFor("diamond"),
     * "ignisium", tierFor("ignisium"),
     * List.of("32x phoenixcore:raw_ignisium_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "sky_steel_progenitor",
     * "diamond", tierFor("diamond"),
     * "sky_steel", tierFor("sky_steel"),
     * List.of("32x megacells:sky_steel_block"),
     * List.of(),
     * 360, DEFAULT_LUV_EUT
     * ),
     * 
     * new ApisProgenitorConfig(
     * "infinity_special",
     * "draconic", tierFor("draconic"),
     * "infinity", tierFor("infinity"),
     * List.of("gtceu:neutronium_ingot"),
     * List.of("gtceu:infinity_fluid 200"),
     * 600, DEFAULT_UV_EUT
     * ),
     * new ApisProgenitorConfig(
     * "draconic_special",
     * "infinity", tierFor("infinity"),
     * "draconic", tierFor("draconic"),
     * List.of("gtceu:neutronium_ingot"),
     * List.of("gtceu:draconium_fluid 200"),
     * 600, DEFAULT_UV_EUT
     * )
     */
    );

    /**
     * Tier policy:
     * - Tier 3: late-game bees (LuV+ style list)
     * - Tier 2: mid-game "special" bees (rare/exotic)
     * - Tier 1: everything els
     * Adjust these lists to change tiering without touching recipe code.
     */
    private static Map<String, FullBeeConfig> createAllBeeConfigs() {
        Map<String, FullBeeConfig> configs = new HashMap<>();

        // Keeps your existing EUt adjustments (LuV tier). Tier selection is independent and uses tierFor().
        List<String> luvBees = List.of("thorium", "scheelite", "tungstate", "bauxite", "ilmenite", "pitchblende");

        for (String id : BEE_MATERIAL_TYPES) {
            String blockId, outputId;

            int currentEut = DEFAULT_IV_EUT;
            int d = 200; // Normal duration
            int wd = 300; // Wax duration
            int fd = 400; // Fluid duration
            int bd = 100; // Boosted duration (Faster)

            if (luvBees.contains(id)) {
                currentEut = DEFAULT_LUV_EUT;
                d = 400;
            }

            int outputCount = 1;

            switch (id) {
                case "iron":
                    blockId = "minecraft:iron_block";
                    outputId = "minecraft:raw_iron";
                    outputCount = 5;
                    break;
                case "gold":
                    blockId = "minecraft:gold_block";
                    outputId = "minecraft:raw_gold";
                    outputCount = 5;
                    break;
                case "copper":
                    blockId = "minecraft:copper_block";
                    outputId = "minecraft:raw_copper";
                    outputCount = 5;
                    break;
                case "diamond":
                    blockId = "minecraft:diamond_block";
                    outputId = "gtceu:raw_diamond";
                    outputCount = 5;
                    break;
                case "emerald":
                    blockId = "minecraft:emerald_block";
                    outputId = "gtceu:raw_emerald";
                    outputCount = 5;
                    break;
                case "lapis":
                    blockId = "minecraft:lapis_block";
                    outputId = "gtceu:raw_lapis";
                    outputCount = 5;
                    break;
                /*
                 * case "redstone":
                 * blockId = "minecraft:redstone_block";
                 * outputId = "minecraft:redstone";
                 * outputCount = 5;
                 * break;
                 * case "coal":
                 * blockId = "minecraft:coal_block";
                 * outputId = "gtceu:coal_ore";
                 * outputCount = 5;
                 * break;
                 * case "netherite":
                 * blockId = "minecraft:ancient_debris";
                 * outputId = "minecraft:ancient_debris";
                 * outputCount = 5;
                 * break;
                 * case "ghostly":
                 * blockId = "minecraft:phantom_membrane";
                 * outputId = "minecraft:ghast_tear";
                 * break;
                 * case "sponge":
                 * blockId = "minecraft:sponge";
                 * outputId = "minecraft:sponge";
                 * break;
                 * case "sculk":
                 * blockId = "minecraft:sculk";
                 * outputId = "minecraft:sculk";
                 * outputCount = 5;
                 * break;
                 * case "withered":
                 * blockId = "minecraft:wither_rose";
                 * outputId = "minecraft:wither_rose";
                 * outputCount = 5;
                 * break;
                 * case "skeletal":
                 * blockId = "minecraft:bone_block";
                 * outputId = "minecraft:bone";
                 * outputCount = 5;
                 * break;
                 * case "blazing":
                 * blockId = "minecraft:magma_block";
                 * outputId = "minecraft:blaze_powder";
                 * break;
                 * case "infinity":
                 * blockId = "enderio:grains_of_infinity";
                 * outputId = "enderio:grains_of_infinity";
                 * outputCount = 5;
                 * break;
                 * 
                 * case "apatite":
                 * blockId = "gtceu:raw_apatite_block";
                 * outputId = "gtceu:raw_apatite";
                 * outputCount = 5;
                 * break;
                 * case "tin":
                 * blockId = "gtceu:tin_block";
                 * outputId = "gtceu:raw_tin";
                 * outputCount = 5;
                 * break;
                 * case "silver":
                 * blockId = "gtceu:raw_silver_block";
                 * outputId = "gtceu:raw_silver";
                 * outputCount = 5;
                 * break;
                 * case "nickel":
                 * blockId = "gtceu:nickel_block";
                 * outputId = "gtceu:raw_nickel";
                 * outputCount = 5;
                 * break;
                 * case "lead":
                 * blockId = "gtceu:raw_lead_block";
                 * outputId = "gtceu:raw_lead";
                 * outputCount = 5;
                 * break;
                 * case "zinc":
                 * blockId = "gtceu:zinc_block";
                 * outputId = "gtceu:zinc_ingot";
                 * outputCount = 5;
                 * break;
                 * case "pitchblende":
                 * blockId = "gtceu:raw_pitchblende_block";
                 * outputId = "gtceu:raw_pitchblende";
                 * outputCount = 5;
                 * break;
                 * case "cinnabar":
                 * blockId = "gtceu:raw_cinnabar_block";
                 * outputId = "gtceu:raw_cinnabar";
                 * outputCount = 5;
                 * break;
                 * case "sapphire":
                 * blockId = "gtceu:raw_sapphire_block";
                 * outputId = "gtceu:raw_sapphire";
                 * outputCount = 5;
                 * break;
                 * case "ruby":
                 * blockId = "gtceu:raw_ruby_block";
                 * outputId = "gtceu:raw_ruby";
                 * outputCount = 5;
                 * break;
                 * case "topaz":
                 * blockId = "gtceu:raw_topaz_block";
                 * outputId = "gtceu:raw_topaz";
                 * outputCount = 5;
                 * break;
                 * case "amethyst":
                 * blockId = "minecraft:amethyst_block";
                 * outputId = "gtceu:raw_amethyst";
                 * outputCount = 5;
                 * break;
                 * case "realgar":
                 * blockId = "gtceu:raw_realgar_block";
                 * outputId = "gtceu:raw_realgar";
                 * outputCount = 5;
                 * break;
                 * case "stibnite":
                 * blockId = "gtceu:raw_stibnite_block";
                 * outputId = "gtceu:raw_stibnite";
                 * outputCount = 5;
                 * break;
                 * case "opal":
                 * blockId = "gtceu:raw_opal_block";
                 * outputId = "gtceu:raw_opal";
                 * outputCount = 5;
                 * break;
                 * case "pyrope":
                 * blockId = "gtceu:raw_pyrope_block";
                 * outputId = "gtceu:raw_pyrope";
                 * outputCount = 5;
                 * break;
                 * case "scheelite":
                 * blockId = "gtceu:raw_scheelite_block";
                 * outputId = "gtceu:raw_scheelite";
                 * outputCount = 5;
                 * break;
                 * case "cobaltite":
                 * blockId = "gtceu:raw_cobaltite_block";
                 * outputId = "gtceu:raw_cobaltite";
                 * outputCount = 5;
                 * break;
                 * case "cobalt":
                 * blockId = "gtceu:raw_cobalt_block";
                 * outputId = "gtceu:cobalt_dust";
                 * outputCount = 5;
                 * break;
                 * case "bauxite":
                 * blockId = "gtceu:raw_bauxite_block";
                 * outputId = "gtceu:raw_bauxite";
                 * outputCount = 5;
                 * break;
                 * case "tungstate":
                 * blockId = "gtceu:raw_tungstate_block";
                 * outputId = "gtceu:raw_tungstate";
                 * outputCount = 5;
                 * break;
                 * case "tricalcium_phosphate":
                 * blockId = "gtceu:raw_tricalcium_phosphate_block";
                 * outputId = "gtceu:raw_tricalcium_phosphate";
                 * outputCount = 5;
                 * break;
                 * case "galena":
                 * blockId = "gtceu:raw_galena_block";
                 * outputId = "gtceu:raw_galena";
                 * outputCount = 5;
                 * break;
                 * case "ilmenite":
                 * blockId = "gtceu:raw_ilmenite_block";
                 * outputId = "gtceu:raw_ilmenite";
                 * outputCount = 5;
                 * break;
                 * case "malachite":
                 * blockId = "gtceu:raw_malachite_block";
                 * outputId = "gtceu:raw_malachite";
                 * outputCount = 5;
                 * break;
                 * case "lepidolite":
                 * blockId = "gtceu:raw_lepidolite_block";
                 * outputId = "gtceu:raw_lepidolite";
                 * outputCount = 5;
                 * break;
                 * case "electrotine":
                 * blockId = "gtceu:raw_electrotine_block";
                 * outputId = "gtceu:raw_electrotine";
                 * outputCount = 5;
                 * break;
                 * 
                 * case "thorium":
                 * blockId = "gtceu:raw_thorium_block";
                 * outputId = "gtceu:raw_thorium";
                 * outputCount = 5;
                 * break;
                 * case "graphite":
                 * blockId = "gtceu:raw_graphite_block";
                 * outputId = "gtceu:raw_graphite";
                 * outputCount = 5;
                 * break;
                 * case "sphalerite":
                 * blockId = "gtceu:raw_sphalerite_block";
                 * outputId = "gtceu:raw_sphalerite";
                 * outputCount = 5;
                 * break;
                 * case "chromite":
                 * blockId = "gtceu:raw_chromite_block";
                 * outputId = "gtceu:raw_chromite_dust";
                 * outputCount = 5;
                 * break;
                 * case "pyrolusite":
                 * blockId = "gtceu:raw_pyrolusite_block";
                 * outputId = "gtceu:raw_pyrolusite";
                 * outputCount = 5;
                 * break;
                 * case "platinum":
                 * blockId = "gtceu:raw_platinum_block";
                 * outputId = "gtceu:raw_platinum";
                 * outputCount = 5;
                 * break;
                 * case "bastnasite":
                 * blockId = "gtceu:raw_bastnasite_block";
                 * outputId = "gtceu:raw_bastnasite";
                 * outputCount = 5;
                 * break;
                 * case "tetrahedrite":
                 * blockId = "gtceu:raw_tetrahedrite_block";
                 * outputId = "gtceu:raw_tetrahedrite";
                 * outputCount = 5;
                 * break;
                 * case "sulfur":
                 * blockId = "gtceu:raw_sulfur_block";
                 * outputId = "gtceu:raw_sulfur";
                 * outputCount = 5;
                 * break;
                 * case "oilsands":
                 * blockId = "gtceu:raw_oilsands_block";
                 * outputId = "gtceu:raw_oilsands";
                 * outputCount = 5;
                 * break;
                 * case "tantalite":
                 * blockId = "gtceu:raw_tantalite_block";
                 * outputId = "gtceu:raw_tantalite";
                 * outputCount = 5;
                 * break;
                 * case "barite":
                 * blockId = "gtceu:raw_barite_block";
                 * outputId = "gtceu:raw_barite";
                 * outputCount = 5;
                 * break;
                 * case "vanadium_magnetite":
                 * blockId = "gtceu:raw_vanadium_magnetite_block";
                 * outputId = "gtceu:raw_vanadium_magnetite";
                 * outputCount = 5;
                 * break;
                 * case "pyrochlore":
                 * blockId = "gtceu:raw_pyrochlore_block";
                 * outputId = "gtceu:raw_pyrochlore";
                 * outputCount = 5;
                 * break;
                 * case "draconic":
                 * blockId = "minecraft:dragon_egg";
                 * outputId = "productivebees:draconic_dust";
                 * outputCount = 5;
                 * break;
                 * 
                 * case "desh":
                 * blockId = "ad_astra:desh_block";
                 * outputId = "ad_astra:raw_desh";
                 * outputCount = 9;
                 * break;
                 * case "steel":
                 * blockId = "gtceu:steel_block";
                 * outputId = "gtceu:steel_ingot";
                 * outputCount = 3;
                 * break;
                 * case "sky_steel":
                 * blockId = "megacells:sky_steel_block";
                 * outputId = "megacells:sky_steel_ingot";
                 * outputCount = 3;
                 * break;
                 * case "voidglass_shard":
                 * blockId = "phoenixcore:raw_voidglass_shard_block";
                 * outputId = "phoenixcore:raw_voidglass_shard";
                 * outputCount = 5;
                 * break;
                 * case "ignisium":
                 * blockId = "phoenixcore:raw_ignisium_block";
                 * outputId = "gtceu:raw_ignisium";
                 * outputCount = 5;
                 * break;
                 * case "salty":
                 * blockId = "gtceu:salt_block";
                 * outputId = "gtceu:raw_salt";
                 * outputCount = 5;
                 * break;
                 * 
                 * case "water":
                 * blockId = "minecraft:water";
                 * outputId = "minecraft:salmon";
                 * break;
                 * case "rancher":
                 * blockId = "minecraft:milk";
                 * outputId = "productivebees:honeycomb_milky";
                 * break;
                 * case "steamy":
                 * blockId = "gtceu:steam_turbine_rotor";
                 * outputId = "gtceu:steam";
                 * break;
                 * case "acidic":
                 * blockId = "gtceu:sulfuric_acid_bucket";
                 * outputId = "gtceu:sulfuric_acid_bucket";
                 * break;
                 * case "fluorite":
                 * blockId = "phoenixcore:raw_fluorite_block";
                 * outputId = "phoenixcore:raw_fluorite";
                 * outputCount = 5;
                 * break;
                 * case "brown_shroom":
                 * blockId = "minecraft:brown_mushroom_block";
                 * outputId = "minecraft:brown_mushroom";
                 * outputCount = 5;
                 * break;
                 * case "red_shroom":
                 * blockId = "minecraft:red_mushroom_block";
                 * outputId = "minecraft:red_mushroom";
                 * outputCount = 5;
                 * break;
                 * case "crimson":
                 * blockId = "minecraft:crimson_stem";
                 * outputId = "minecraft:crimson_fungus";
                 * outputCount = 5;
                 * break;
                 * case "warped":
                 * blockId = "minecraft:warped_stem";
                 * outputId = "minecraft:warped_fungus";
                 * outputCount = 5;
                 * break;
                 * 
                 */

                default:
                    blockId = "gtceu:" + id + "_block";
                    outputId = "gtceu:" + id + "_ingot";
                    break;
            }

            int tier = tierFor(id);

            configs.put(id, new FullBeeConfig(
                    id,                // 1. beeId
                    tier,              // 2. tier (1/2/3) used for TagPrefix bee selection
                    blockId,           // 3. pollinationInputId
                    outputId,          // 4. finalOutputId
                    outputCount,       // 5. outputCount

                    // Standard Processing
                    currentEut,        // 6. decantingEut
                    d,                 // 7. decantingDuration
                    currentEut,        // 8. waxEut
                    wd,                // 9. waxDuration
                    currentEut,        // 10. fluidEut
                    fd,                // 11. fluidDuration

                    // Boosted Processing (Crystal Rose)
                    currentEut,        // 12. boostedDecantingEut
                    bd,                // 13. boostedDecantingDuration
                    currentEut,        // 14. boostedWaxEut
                    wd,                // 15. boostedWaxDuration
                    currentEut,        // 16. boostedFluidEut
                    fd                 // 17. boostedFluidDuration
            ));
        }
        return configs;
    }
}
