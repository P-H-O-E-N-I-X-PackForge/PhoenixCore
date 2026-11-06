package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCooking;

public final class PftSmeltingRecipes {

    private PftSmeltingRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCooking.smelting(provider, "charcoal_from_oak_log_smelting", "minecraft:charcoal", "minecraft:oak_log");

        PftCooking.smelting(provider, "charcoal_from_spruce_log_smelting", "minecraft:charcoal",
                "minecraft:spruce_log");

        PftCooking.smelting(provider, "charcoal_from_birch_log_smelting", "minecraft:charcoal", "minecraft:birch_log");

        PftCooking.smelting(provider, "charcoal_from_jungle_log_smelting", "minecraft:charcoal",
                "minecraft:jungle_log");

        PftCooking.smelting(provider, "charcoal_from_acacia_log_smelting", "minecraft:charcoal",
                "minecraft:acacia_log");

        PftCooking.smelting(provider, "charcoal_from_dark_oak_log_smelting", "minecraft:charcoal",
                "minecraft:dark_oak_log");

        PftCooking.smelting(provider, "charcoal_from_mangrove_log_smelting", "minecraft:charcoal",
                "minecraft:mangrove_log");

        PftCooking.smelting(provider, "charcoal_from_crimson_stem_smelting", "minecraft:charcoal",
                "minecraft:crimson_stem");

        PftCooking.smelting(provider, "charcoal_from_warped_stem_smelting", "minecraft:charcoal",
                "minecraft:warped_stem");

        PftCooking.smelting(provider, "charcoal_from_rubber_log_smelting", "minecraft:charcoal", "gtceu:rubber_log");

        PftCooking.smelting(provider, "glass_from_sand_smelting", "minecraft:glass", "minecraft:sand");

        PftCooking.smelting(provider, "wrought_iron_ingot_from_iron_ingot_smelting", "gtceu:wrought_iron_ingot",
                "minecraft:iron_ingot");

        PftCooking.smelting(provider, "dormant_ember_dust_from_raw_dormant_ember_smelting",
                "phoenixcore:dormant_ember_dust", "phoenixcore:raw_dormant_ember");

        PftCooking.smelting(provider, "dormant_ember_dust_from_deepslate_dormant_ember_ore_smelting",
                "phoenixcore:dormant_ember_dust", "phoenixcore:deepslate_dormant_ember_ore");

        PftCooking.smelting(provider, "dormant_ember_dust_from_dormant_ember_ore_smelting",
                "phoenixcore:dormant_ember_dust", "phoenixcore:dormant_ember_ore");

        PftCooking.smelting(provider, "permafrost_dust_from_raw_permafrost_smelting", "phoenixcore:permafrost_dust",
                "phoenixcore:raw_permafrost");

        PftCooking.smelting(provider, "permafrost_dust_from_permafrost_ore_smelting", "phoenixcore:permafrost_dust",
                "phoenixcore:permafrost_ore");

        PftCooking.smelting(provider, "permafrost_dust_from_deepslate_permafrost_ore_smelting",
                "phoenixcore:permafrost_dust", "phoenixcore:deepslate_permafrost_ore");

        PftCooking.smelting(provider, "charcoal_from_oak_wood_smelting", "minecraft:charcoal", "minecraft:oak_wood");

        PftCooking.smelting(provider, "charcoal_from_spruce_wood_smelting", "minecraft:charcoal",
                "minecraft:spruce_wood");

        PftCooking.smelting(provider, "charcoal_from_birch_wood_smelting", "minecraft:charcoal",
                "minecraft:birch_wood");

        PftCooking.smelting(provider, "charcoal_from_jungle_wood_smelting", "minecraft:charcoal",
                "minecraft:jungle_wood");

        PftCooking.smelting(provider, "charcoal_from_acacia_wood_smelting", "minecraft:charcoal",
                "minecraft:acacia_wood");

        PftCooking.smelting(provider, "charcoal_from_dark_oak_wood_smelting", "minecraft:charcoal",
                "minecraft:dark_oak_wood");

        PftCooking.smelting(provider, "charcoal_from_mangrove_wood_smelting", "minecraft:charcoal",
                "minecraft:mangrove_wood");

        PftCooking.smelting(provider, "charcoal_from_crimson_hyphae_smelting", "minecraft:charcoal",
                "minecraft:crimson_hyphae");

        PftCooking.smelting(provider, "charcoal_from_aurum_log_smelting", "minecraft:charcoal",
                "forbidden_arcanus:aurum_log");

        PftCooking.smelting(provider, "charcoal_from_warped_hyphae_smelting", "minecraft:charcoal",
                "minecraft:warped_hyphae");

        PftCooking.smelting(provider, "charcoal_from_blue_archwood_log_smelting", "minecraft:charcoal",
                "ars_nouveau:blue_archwood_log");

        PftCooking.smelting(provider, "charcoal_from_red_archwood_log_smelting", "minecraft:charcoal",
                "ars_nouveau:red_archwood_log");

        PftCooking.smelting(provider, "charcoal_from_purple_archwood_log_smelting", "minecraft:charcoal",
                "ars_nouveau:purple_archwood_log");

        PftCooking.smelting(provider, "charcoal_from_green_archwood_log_smelting", "minecraft:charcoal",
                "ars_nouveau:green_archwood_log");

        PftCooking.smelting(provider, "steel_ingot_from_steel_dust_smelting", "gtceu:steel_ingot", "gtceu:steel_dust");

        PftCooking.smelting(provider, "roasted_peanut_from_peanut", "phoenixcore:roasted_peanut", "phoenixcore:peanut");
        PftCooking.smoking(provider, "roasted_peanut_from_peanut_smoking", "phoenixcore:roasted_peanut",
                "phoenixcore:peanut");
    }
}
