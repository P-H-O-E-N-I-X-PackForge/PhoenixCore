package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftSteelAndBronzeRecipes {

    private PftSteelAndBronzeRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "compressor", "rubber_sheet1")
                .itemInputs("1x gtceu:sticky_resin")
                .itemOutputs("1x gtceu:rubber_plate")
                .duration(250)
                .EUt(12)
                .save();

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes", "gtceu:steel_dust",
                "1x gtceu:coal_dust", "gtceu:iron_dust");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_2", "gtceu:steel_dust",
                "1x gtceu:coal_dust", "gtceu:wrought_iron_dust");

        PftCrafting.shapeless(provider, "paper_shapeless_pftsteelandbronzerecipes", "minecraft:paper",
                "3x minecraft:sugarcane");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_3", "gtceu:steel_dust",
                "1x minecraft:coal", "gtceu:wrought_iron_dust");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_4", "gtceu:steel_dust",
                "1x minecraft:coal", "gtceu:iron_dust");

        PftCrafting.shapeless(provider, "glass_tube_shapeless_pftsteelandbronzerecipes", "gtceu:glass_tube",
                "6x minecraft:glass_pane");

        PftCrafting.shapeless(provider, "crying_obsidian_shapeless_pftsteelandbronzerecipes",
                "minecraft:crying_obsidian", "1x minecraft:obsidian");

        PftCrafting.shapeless(provider, "dormant_ember_dust_shapeless_pftsteelandbronzerecipes",
                "phoenixcore:dormant_ember_dust", "#c:tools/mortar", "phoenixcore:dormant_ember_ingot");

        PftCrafting.shapeless(provider, "permafrost_dust_shapeless_pftsteelandbronzerecipes",
                "phoenixcore:permafrost_dust", "#c:tools/mortar", "phoenixcore:permafrost_ingot");

        PftCrafting.shapeless(provider, "fiery_bronze_dust_shapeless_pftsteelandbronzerecipes",
                "4x phoenixcore:fiery_bronze_dust", "3x gtceu:copper_dust", "1x gtceu:tin_dust",
                "1x phoenixcore:dormant_ember_dust");

        PftCrafting.shapeless(provider, "programmed_circuit_shapeless_pftsteelandbronzerecipes",
                "1x gtceu:programmed_circuit", "#gtceu:circuits");

        PftCrafting.shapeless(provider, "tin_alloy_dust_shapeless_pftsteelandbronzerecipes", "4x gtceu:tin_alloy_dust",
                "3x gtceu:iron_dust", "gtceu:tin_dust");

        PftCrafting.shapeless(provider, "red_alloy_dust_shapeless_pftsteelandbronzerecipes", "1x gtceu:red_alloy_dust",
                "4x minecraft:redstone", "1x gtceu:copper_dust");

        PftCrafting.shapeless(provider, "aurum_steel_dust_shapeless_pftsteelandbronzerecipes",
                "5x phoenixcore:aurum_steel_dust", "2x gtceu:steel_dust", "1x gtceu:gold_dust",
                "2x phoenixcore:dormant_ember_dust", "1x phoenixcore:aurum_wood_dust");

        PftCrafting.shaped(provider, "wood_plate_pftsteelandbronzerecipes", "gtceu:wood_plate",
                new String[] { " L ", " S ", " S " }, "S", "#minecraft:planks", "L", "#c:tools/hammer");

        PftCrafting.shaped(provider, "iron_furnace_pftsteelandbronzerecipes", "ironfurnaces:iron_furnace",
                new String[] { "SSS", "SLS", "SSS" }, "S", "gtceu:wrought_iron_ingot", "L", "minecraft:furnace");

        PftCrafting.shaped(provider, "iron_furnace_pftsteelandbronzerecipes_2", "ironfurnaces:iron_furnace",
                new String[] { "SSS", "GLG", "SSS" }, "S", "gtceu:wrought_iron_ingot", "L",
                "ironfurnaces:copper_furnace", "G", "minecraft:glass");

        PftCrafting.shaped(provider, "large_steam_compressor_pftsteelandbronzerecipes", "gtceu:large_steam_compressor",
                new String[] { "SPS", "PCP", "SLS" }, "S", "gtceu:steam_machine_casing", "L",
                "phoenixcore:fiery_bronze_gear", "P", "gtceu:dense_steel_plate", "C", "gtceu:hp_steam_compressor");

        PftCrafting.shaped(provider, "rubber_plate_pftsteelandbronzerecipes", "gtceu:rubber_plate",
                new String[] { " L ", " S ", " S " }, "S", "gtceu:sticky_resin", "L", "#c:tools/hammer");

        PftCrafting.shaped(provider, "lp_steam_liquid_boiler_pftsteelandbronzerecipes", "gtceu:lp_steam_liquid_boiler",
                new String[] { "PPP", "PBP", "PWP" }, "P", "phoenixcore:fiery_bronze_plate", "W",
                "gtceu:bronze_brick_casing", "B", "minecraft:glass");

        PftCrafting.shaped(provider, "lp_steam_solid_boiler_pftsteelandbronzerecipes", "gtceu:lp_steam_solid_boiler",
                new String[] { "PPP", "PTP", "GBG" }, "P", "phoenixcore:fiery_bronze_plate", "B", "minecraft:furnace",
                "G", "minecraft:bricks", "T", "#c:tools/wrench");

        PftCrafting.shaped(provider, "lp_steam_solar_boiler_pftsteelandbronzerecipes", "gtceu:lp_steam_solar_boiler",
                new String[] { "BBB", "PPP", "FWF" }, "P", "gtceu:silver_plate", "W", "gtceu:bronze_brick_casing", "B",
                "minecraft:glass", "F", "phoenixcore:fiery_bronze_small_fluid_pipe");

        PftCrafting.shaped(provider, "lp_steam_extractor_pftsteelandbronzerecipes", "gtceu:lp_steam_extractor",
                new String[] { "FFF", "ZWB", "FFF" }, "W", "gtceu:bronze_brick_casing", "B", "minecraft:glass", "F",
                "phoenixcore:fiery_bronze_small_fluid_pipe", "Z", "#c:pistons");

        PftCrafting.shaped(provider, "lp_steam_macerator_pftsteelandbronzerecipes", "gtceu:lp_steam_macerator",
                new String[] { "PFP", "FWF", "FZF" }, "P", "minecraft:diamond", "W", "gtceu:bronze_machine_casing", "F",
                "phoenixcore:fiery_bronze_small_fluid_pipe", "Z", "#c:pistons");

        PftCrafting.shaped(provider, "lp_steam_compressor_pftsteelandbronzerecipes", "gtceu:lp_steam_compressor",
                new String[] { "FFF", "ZWZ", "FFF" }, "W", "gtceu:bronze_machine_casing", "F",
                "phoenixcore:fiery_bronze_small_fluid_pipe", "Z", "#c:pistons");

        PftCrafting.shaped(provider, "lp_steam_forge_hammer_pftsteelandbronzerecipes", "gtceu:lp_steam_forge_hammer",
                new String[] { "FZF", "FWF", "FAF" }, "W", "gtceu:bronze_machine_casing", "F",
                "phoenixcore:fiery_bronze_small_fluid_pipe", "Z", "#c:pistons", "A", "minecraft:anvil");

        PftCrafting.shaped(provider, "lp_steam_furnace_pftsteelandbronzerecipes", "gtceu:lp_steam_furnace",
                new String[] { "FFF", "FWF", "FAF" }, "W", "gtceu:bronze_brick_casing", "F",
                "phoenixcore:fiery_bronze_small_fluid_pipe", "A", "minecraft:furnace");

        PftCrafting.shaped(provider, "lp_steam_alloy_smelter_pftsteelandbronzerecipes", "gtceu:lp_steam_alloy_smelter",
                new String[] { "FFF", "AWA", "FFF" }, "W", "gtceu:bronze_brick_casing", "F",
                "phoenixcore:fiery_bronze_small_fluid_pipe", "A", "minecraft:furnace");

        PftCrafting.shaped(provider, "lp_steam_rock_crusher_pftsteelandbronzerecipes", "gtceu:lp_steam_rock_crusher",
                new String[] { "ZFZ", "FWF", "PFP" }, "P", "minecraft:diamond", "W", "gtceu:bronze_machine_casing", "F",
                "phoenixcore:fiery_bronze_small_fluid_pipe", "Z", "#c:pistons");

        PftCrafting.shaped(provider, "lp_steam_miner_pftsteelandbronzerecipes", "gtceu:lp_steam_miner",
                new String[] { "PFP", "FWF", "ZFZ" }, "P", "minecraft:diamond", "W", "gtceu:bronze_brick_casing", "F",
                "phoenixcore:fiery_bronze_normal_fluid_pipe", "Z", "phoenixcore:small_fiery_bronze_gear");

        PftCrafting.shaped(provider, "steam_machine_casing_pftsteelandbronzerecipes", "2x gtceu:steam_machine_casing",
                new String[] { "PFP", "PZP", "PWP" }, "P", "phoenixcore:fiery_bronze_plate", "W", "#c:tools/wrench",
                "F", "#c:tools/hammer", "Z", "minecraft:bricks");

        PftCrafting.shaped(provider, "industrial_steam_casing_pftsteelandbronzerecipes",
                "2x gtceu:industrial_steam_casing", new String[] { "PFP", "PZP", "PWP" }, "P",
                "phoenixcore:fiery_bronze_plate", "W", "#c:tools/wrench", "F", "#c:tools/hammer", "Z",
                "gtceu:steel_frame");

        PftCrafting.shaped(provider, "bronze_firebox_casing_pftsteelandbronzerecipes", "2x gtceu:bronze_firebox_casing",
                new String[] { "PRP", "RFR", "PRP" }, "P", "phoenixcore:fiery_bronze_plate", "F",
                "phoenixcore:fiery_bronze_frame", "R", "phoenixcore:fiery_bronze_rod");

        PftCrafting.shaped(provider, "tantalite_ore_pftsteelandbronzerecipes", "17x gtceu:tantalite_ore",
                new String[] { "PPP", "PPP", "PPP" }, "P", "phoenixcore:phoenix_feather");

        PftCrafting.shaped(provider, "rubber_sapling_pftsteelandbronzerecipes", "16x gtceu:rubber_sapling",
                new String[] { "   ", "   ", "  P" }, "P", "phoenixcore:phoenix_feather");

        PftCrafting.shaped(provider, "aurum_sapling_pftsteelandbronzerecipes", "1x forbidden_arcanus:aurum_sapling",
                new String[] { "   ", "   ", " P " }, "P", "phoenixcore:phoenix_feather");

        PftCrafting.shaped(provider, "apatite_ore_pftsteelandbronzerecipes", "17x gtceu:apatite_ore",
                new String[] { "PPP", "PPP", "PP " }, "P", "phoenixcore:phoenix_feather");

        PftCrafting.shaped(provider, "clay_ball_pftsteelandbronzerecipes", "64x minecraft:clay_ball",
                new String[] { "P  ", "   ", "   " }, "P", "phoenixcore:phoenix_feather");

        PftCrafting.shaped(provider, "lv_machine_casing_pftsteelandbronzerecipes", "gtceu:lv_machine_casing",
                new String[] { "PPP", "PWP", "PPP" }, "P", "phoenixcore:aurum_steel_plate", "W", "#c:tools/wrench");

        PftCrafting.shaped(provider, "mv_machine_casing_pftsteelandbronzerecipes", "gtceu:mv_machine_casing",
                new String[] { "PPP", "PWP", "PPP" }, "P", "phoenixcore:aluminfrost_plate", "W", "#c:tools/wrench");

        PftCrafting.shaped(provider, "hv_machine_casing_pftsteelandbronzerecipes", "gtceu:hv_machine_casing",
                new String[] { "PPP", "PWP", "PPP" }, "P", "phoenixcore:frost_reinforced_stained_steel_plate", "W",
                "#c:tools/wrench");

        PftCrafting.shaped(provider, "luv_machine_casing_pftsteelandbronzerecipes", "gtceu:luv_machine_casing",
                new String[] { "PPP", "PWP", "PPP" }, "P", "phoenixcore:resonant_rhodium_alloy_plate", "W",
                "#c:tools/wrench");

        PftCrafting.shaped(provider, "iv_machine_casing_pftsteelandbronzerecipes", "gtceu:iv_machine_casing",
                new String[] { "PPP", "PWP", "PPP" }, "P", "phoenixcore:void_touched_tungsten_steel_plate", "W",
                "#c:tools/wrench");

        PftCrafting.shaped(provider, "ev_machine_casing_pftsteelandbronzerecipes", "gtceu:ev_machine_casing",
                new String[] { "PPP", "PWP", "PPP" }, "P", "phoenixcore:source_imbued_titanium_plate", "W",
                "#c:tools/wrench");

        PftCrafting.shaped(provider, "bronze_machine_casing_pftsteelandbronzerecipes", "gtceu:bronze_machine_casing",
                new String[] { "PPP", "PWP", "PPP" }, "P", "phoenixcore:fiery_bronze_plate", "W", "#c:tools/wrench");

        PftCrafting.shaped(provider, "lv_machine_hull_pftsteelandbronzerecipes", "gtceu:lv_machine_hull",
                new String[] { "SPS", "WZW", "   " }, "P", "phoenixcore:aurum_steel_plate", "W",
                "gtceu:tin_single_cable", "Z", "gtceu:lv_machine_casing", "S", "gtceu:wrought_iron_plate");

        PftCrafting.shaped(provider, "bronze_brick_casing_pftsteelandbronzerecipes", "gtceu:bronze_brick_casing",
                new String[] { "FFF", "FHF", "BBB" }, "F", "phoenixcore:fiery_bronze_plate", "H", "#c:tools/hammer",
                "B", "minecraft:bricks");

        PftCrafting.shaped(provider, "steam_input_hatch_pftsteelandbronzerecipes", "gtceu:steam_input_hatch",
                new String[] { "FWF", "FZF", "FWF" }, "F", "phoenixcore:fiery_bronze_plate", "W",
                "phoenixcore:fiery_bronze_normal_fluid_pipe", "Z", "gtceu:bronze_drum");

        PftCrafting.shaped(provider, "nasa_workbench_pftsteelandbronzerecipes", "ad_astra:nasa_workbench",
                new String[] { "APW", "SZS", "WPA" }, "P", "phoenixcore:dense_frost_reinforced_stained_steel_plate",
                "W", "#gtceu:circuits/ev", "Z", "gtceu:ev_machine_hull", "S", "phoenixcore:aluminfrost_gear", "A",
                "gtceu:ev_robot_arm");

        PftCrafting.shaped(provider, "iv_machine_hull_pftsteelandbronzerecipes", "gtceu:iv_machine_hull",
                new String[] { "SPS", "WZW", "   " }, "P", "phoenixcore:void_touched_tungsten_steel_plate", "W",
                "gtceu:tin_single_cable", "Z", "gtceu:iv_machine_casing", "S", "gtceu:polytetrafluoroethylene_plate");

        PftCrafting.shaped(provider, "mv_machine_hull_pftsteelandbronzerecipes", "gtceu:mv_machine_hull",
                new String[] { "SPS", "WZW", "   " }, "P", "phoenixcore:aluminfrost_plate", "W",
                "gtceu:copper_single_cable", "Z", "gtceu:mv_machine_casing", "S", "gtceu:wrought_iron_plate");

        PftCrafting.shaped(provider, "hv_machine_hull_pftsteelandbronzerecipes", "gtceu:hv_machine_hull",
                new String[] { "SPS", "WZW", "   " }, "P", "phoenixcore:frost_reinforced_stained_steel_plate", "W",
                "gtceu:gold_single_cable", "Z", "gtceu:hv_machine_casing", "S", "gtceu:polyethylene_plate");

        PftCrafting.shaped(provider, "ev_machine_hull_pftsteelandbronzerecipes", "gtceu:ev_machine_hull",
                new String[] { "SPS", "WZW", "   " }, "P", "phoenixcore:source_imbued_titanium_plate", "W",
                "gtceu:aluminium_single_cable", "Z", "gtceu:ev_machine_casing", "S", "gtceu:polyethylene_plate");

        PftCrafting.shaped(provider, "distillation_tower_pftsteelandbronzerecipes", "gtceu:distillation_tower",
                new String[] { "PWP", "ZSZ", "PWP" }, "P", "#gtceu:circuits/hv", "W",
                "gtceu:stainless_steel_large_fluid_pipe", "Z", "gtceu:hv_electric_pump", "S", "gtceu:hv_machine_hull");

        PftCrafting.shaped(provider, "pyrolyse_oven_pftsteelandbronzerecipes", "gtceu:pyrolyse_oven",
                new String[] { "APW", "PSP", "AZW" }, "P", "#gtceu:circuits/lv", "W", "gtceu:tin_quadruple_wire", "Z",
                "gtceu:lv_electric_pump", "A", "gtceu:lv_electric_piston", "S", "gtceu:lv_machine_hull");

        PftCrafting.shaped(provider, "storage_bus_pftsteelandbronzerecipes", "2x ae2:storage_bus",
                new String[] { "   ", "IPC", "   " }, "I", "ae2:interface", "P", "gtceu:mv_electric_piston", "C",
                "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "storage_bus_pftsteelandbronzerecipes_2", "4x ae2:storage_bus",
                new String[] { "   ", "IPC", "   " }, "I", "ae2:interface", "P", "gtceu:hv_electric_piston", "C",
                "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "storage_bus_pftsteelandbronzerecipes_3", "8x ae2:storage_bus",
                new String[] { "   ", "IPC", "   " }, "I", "ae2:interface", "P", "gtceu:ev_electric_piston", "C",
                "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "storage_bus_pftsteelandbronzerecipes_4", "16x ae2:storage_bus",
                new String[] { "   ", "IPC", "   " }, "I", "ae2:interface", "P", "gtceu:iv_electric_piston", "C",
                "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "glass_bottle_pftsteelandbronzerecipes", "minecraft:glass_bottle",
                new String[] { "   ", " P ", "P P" }, "P", "minecraft:glass");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_5", "gtceu:steel_dust",
                "1x gtceu:coke_dust", "gtceu:iron_dust");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_6", "gtceu:steel_dust",
                "1x gtceu:coal_dust", "minecraft:iron_ingot");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_7", "gtceu:steel_dust",
                "1x gtceu:coke_dust", "minecraft:iron_ingot");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_8", "gtceu:steel_dust",
                "1x gtceu:coke", "gtceu:iron_dust");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_9", "gtceu:steel_dust",
                "1x gtceu:coke", "gtceu:wrought_iron_dust");

        PftCrafting.shapeless(provider, "aluminfrost_drum_shapeless_pftsteelandbronzerecipes",
                "phoenixcore:aluminfrost_drum", "1x phoenixcore:aluminfrost_drum");

        PftCrafting.shapeless(provider, "void_touched_tungsten_steel_drum_shapeless_pftsteelandbronzerecipes",
                "phoenixcore:void_touched_tungsten_steel_drum", "1x phoenixcore:void_touched_tungsten_steel_drum");

        PftCrafting.shapeless(provider, "frost_reinforced_stained_steel_drum_shapeless_pftsteelandbronzerecipes",
                "phoenixcore:frost_reinforced_stained_steel_drum",
                "1x phoenixcore:frost_reinforced_stained_steel_drum");

        PftCrafting.shapeless(provider, "source_imbued_titanium_drum_shapeless_pftsteelandbronzerecipes",
                "phoenixcore:source_imbued_titanium_drum", "1x phoenixcore:source_imbued_titanium_drum");

        PftCrafting.shapeless(provider, "resonant_rhodium_alloy_drum_shapeless_pftsteelandbronzerecipes",
                "phoenixcore:resonant_rhodium_alloy_drum", "1x phoenixcore:resonant_rhodium_alloy_drum");

        PftCrafting.shapeless(provider, "magma_dust_shapeless_pftsteelandbronzerecipes", "phoenixcore:magma_dust",
                "1x minecraft:magma_block", "#c:tools/hammer");

        PftCrafting.shapeless(provider, "aurum_wood_dust_shapeless_pftsteelandbronzerecipes",
                "4x phoenixcore:aurum_wood_dust", "1x forbidden_arcanus:aurum_log", "#c:tools/hammer");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_10", "gtceu:steel_dust",
                "1x minecraft:charcoal", "gtceu:wrought_iron_dust");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_11", "gtceu:steel_dust",
                "1x gtceu:charcoal_dust", "gtceu:iron_dust");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_12", "gtceu:steel_dust",
                "1x gtceu:charcoal_dust", "minecraft:iron_ingot");

        PftCrafting.shapeless(provider, "steel_dust_shapeless_pftsteelandbronzerecipes_13", "gtceu:steel_dust",
                "1x minecraft:charcoal", "minecraft:iron_ingot");
    }
}
