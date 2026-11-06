package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftSteamAgeRecipes {

    private PftSteamAgeRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "ember_igniter", "flaming_dust")
                .itemInputs("8x phoenixcore:dormant_ember_dust", "4x phoenixcore:magma_dust")
                .inputFluids("minecraft:lava 144")
                .itemOutputs("4x phoenixcore:flaming_dust")
                .duration(150)
                .EUt(20)
                .save();

        PftRecipe.gt(provider, "alloy_smelter", "flamingmatrix")
                .itemInputs("1x phoenixcore:flaming_dust", "1x minecraft:clay_ball")
                .itemOutputs("4x hostilenetworks:prediction_matrix")
                .duration(100)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "alloy_smelter", "tin_alloy")
                .itemInputs("3x minecraft:iron_ingot", "1x gtceu:tin_ingot")
                .itemOutputs("4x gtceu:tin_alloy_ingot")
                .duration(100)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "lv_machine_casing")
                .circuit(8)
                .itemInputs("8x phoenixcore:aurum_steel_plate")
                .itemOutputs("1x gtceu:lv_machine_casing")
                .duration(70)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "luv_machine_casing")
                .circuit(8)
                .itemInputs("8x phoenixcore:resonant_rhodium_alloy_plate")
                .itemOutputs("1x gtceu:luv_machine_casing")
                .duration(70)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "iv_machine_casing")
                .circuit(8)
                .itemInputs("8x phoenixcore:void_touched_tungsten_steel_plate")
                .itemOutputs("1x gtceu:iv_machine_casing")
                .duration(70)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "ev_machine_casing")
                .circuit(8)
                .itemInputs("8x phoenixcore:source_imbued_titanium_plate")
                .itemOutputs("1x gtceu:ev_machine_casing")
                .duration(70)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "macerator", "magma_dust")
                .itemInputs("1x minecraft:magma_block")
                .itemOutputs("4x phoenixcore:magma_dust")
                .duration(50)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "forge_hammer", "dust")
                .itemInputs("1x minecraft:sand")
                .itemOutputs("1x kubejs:dust")
                .duration(40)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "dirttty")
                .itemInputs("1x kubejs:dust")
                .inputFluids("minecraft:water 100")
                .itemOutputs("1x minecraft:dirt")
                .duration(40)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "extractor", "frost")
                .itemInputs("1x phoenixcore:permafrost_dust")
                .outputFluids("phoenixcore:frost 144")
                .duration(80)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "extractor", "magma")
                .itemInputs("1x phoenixcore:magma_dust")
                .outputFluids("phoenixcore:magma 500")
                .duration(150)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "steam_machine_casing")
                .circuit(6)
                .itemInputs("6x phoenixcore:fiery_bronze_plate", "1x minecraft:bricks")
                .itemOutputs("2x gtceu:steam_machine_casing")
                .duration(50)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "forge_hammer", "sophisticatedstorage:packing_tap")
                .itemInputs("3x minecraft:paper")
                .itemOutputs("1x sophisticatedstorage:packing_tape")
                .duration(1000)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "mv_machine_casing")
                .circuit(6)
                .itemInputs("8x phoenixcore:aluminfrost_plate")
                .itemOutputs("1x gtceu:mv_machine_casing")
                .duration(100)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "hv_machine_casing")
                .circuit(6)
                .itemInputs("8x phoenixcore:frost_reinforced_stained_steel_plate")
                .itemOutputs("1x gtceu:hv_machine_casing")
                .duration(100)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "assembler", "industrial_steam_casing")
                .circuit(6)
                .itemInputs("6x phoenixcore:fiery_bronze_plate", "1x gtceu:steel_frame")
                .itemOutputs("2x gtceu:industrial_steam_casing")
                .duration(100)
                .EUt(32)
                .save();

        PftRecipe.gt(provider, "electric_blast_furnace", "aluminfrost")
                .circuit(2)
                .itemInputs("32x phoenixcore:unrefined_aluminfrost_dust", "1x gtceu:cobalt_dust")
                .inputFluids("phoenixcore:frost 6000")
                .blastFurnaceTemp(1700)
                .itemOutputs("32x phoenixcore:aluminfrost_ingot")
                .duration(4200)
                .EUt(100)
                .save();

        PftRecipe.gt(provider, "compressor", "dense_steel_plate")
                .itemInputs("14x gtceu:steel_plate")
                .itemOutputs("1x gtceu:dense_steel_plate")
                .duration(100)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "electric_blast_furnace", "frost_reinforced_stained_steelv2")
                .circuit(2)
                .itemInputs("1x phoenixcore:frost_reinforced_stained_steel_dust")
                .inputFluids("phoenixcore:cryo_ember_fluid 150")
                .itemOutputs("1x phoenixcore:frost_reinforced_stained_steel_ingot")
                .blastFurnaceTemp(1700)
                .duration(740)
                .EUt(GTValues.VA[GTValues.HV] / 2.5)
                .blastFurnaceTemp(1700)
                .save();

        PftRecipe.gt(provider, "electric_blast_furnace", "frost_reinforced_stained_steelv1")
                .circuit(1)
                .itemInputs("1x phoenixcore:frost_reinforced_stained_steel_dust")
                .itemOutputs("1x phoenixcore:frost_reinforced_stained_steel_ingot")
                .blastFurnaceTemp(1700)
                .duration(1100)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();

        PftRecipe.gt(provider, "electric_blast_furnace", "aluminfrost2")
                .circuit(1)
                .itemInputs("32x phoenixcore:unrefined_aluminfrost_dust", "1x gtceu:cobalt_dust")
                .itemOutputs("32x phoenixcore:aluminfrost_ingot")
                .blastFurnaceTemp(1700)
                .duration(5000)
                .EUt(100)
                .save();

        PftRecipe.gt(provider, "electric_blast_furnace", "aluminfrost_return")
                .circuit(2)
                .itemInputs("1x phoenixcore:aluminfrost_dust")
                .inputFluids("gtceu:nitrogen 100")
                .itemOutputs("1x phoenixcore:aluminfrost_ingot")
                .blastFurnaceTemp(1700)
                .duration(240)
                .EUt(100)
                .save();

        PftRecipe.gt(provider, "electric_blast_furnace", "aluminfrost_return2")
                .circuit(1)
                .itemInputs("1x phoenixcore:aluminfrost_dust")
                .itemOutputs("1x phoenixcore:aluminfrost_ingot")
                .blastFurnaceTemp(1700)
                .duration(360)
                .EUt(100)
                .save();

        PftRecipe.gt(provider, "mixer", "cryo_ember_fluid")
                .circuit(1)
                .itemInputs("12x phoenixcore:dormant_ember_dust", "3x phoenixcore:permafrost_dust")
                .inputFluids("minecraft:water 8000")
                .outputFluids("phoenixcore:cryo_ember_fluid 4000")
                .duration(882)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "mixer", "frost_reinforced_stained_steel_dust")
                .circuit(6)
                .itemInputs("6x gtceu:iron_dust", "1x gtceu:nickel_dust", "1x gtceu:manganese_dust",
                        "gtceu:chromium_dust", "1x gtceu:carbon_dust")
                .inputFluids("phoenixcore:frost 100")
                .itemOutputs("11x phoenixcore:frost_reinforced_stained_steel_dust")
                .EUt(GTValues.VA[GTValues.MV] / 2)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "water_source", "pump_water")
                .circuit(0)
                .outputFluids("minecraft:water 1000")
                .duration(20)
                .save();

        PftRecipe.gt(provider, "water_source", "pump_water_alt")
                .circuit(1)
                .outputFluids("minecraft:water 100")
                .duration(2)
                .save();

        PftCrafting.shapeless(provider, "clay_shapeless_pftsteamagerecipes", "minecraft:clay", "kubejs:dust",
                "minecraft:water_bucket");

        PftCrafting.shaped(provider, "lv_earth_sieve_pftsteamagerecipes", "gtceu:lv_earth_sieve",
                new String[] { "OFO", "DWD", "CFC" }, "O", "phoenixcore:aurum_steel_plate", "W",
                "gtceu:lv_machine_hull", "D", "gtceu:lv_electric_piston", "F", "gtceu:item_filter", "C",
                "#gtceu:circuits/lv");

        PftCrafting.shaped(provider, "mv_earth_sieve_pftsteamagerecipes", "gtceu:mv_earth_sieve",
                new String[] { "OFO", "DWD", "CFC" }, "O", "phoenixcore:aluminfrost_plate", "W",
                "gtceu:mv_machine_hull", "D", "gtceu:mv_electric_piston", "F", "gtceu:item_filter", "C",
                "#gtceu:circuits/mv");

        PftCrafting.shaped(provider, "large_steam_sieve_pftsteamagerecipes", "gtceu:large_steam_sieve",
                new String[] { "WFW", "WCW", "WFW" }, "W", "gtceu:steam_machine_casing", "F", "gtceu:item_filter", "C",
                "gtceu:lv_earth_sieve");

        PftCrafting.shaped(provider, "aurum_steel_drill_head_pftsteamagerecipes", "phoenixcore:aurum_steel_drill_head",
                new String[] { "FWF", "FWF", "WCW" }, "W", "gtceu:dense_steel_plate", "F",
                "phoenixcore:dense_aurum_steel_plate", "C", "#c:tools/hammer");

        PftCrafting.shaped(provider, "frost_reinforced_stained_steel_drill_head_pftsteamagerecipes",
                "phoenixcore:frost_reinforced_stained_steel_drill_head", new String[] { "FWF", "FWF", "WCW" }, "W",
                "gtceu:dense_steel_plate", "F", "phoenixcore:dense_frost_reinforced_stained_steel_plate", "C",
                "#c:tools/hammer");

        PftCrafting.shaped(provider, "source_imbued_titanium_drill_head_pftsteamagerecipes",
                "phoenixcore:source_imbued_titanium_drill_head", new String[] { "FWF", "FWF", "WCW" }, "W",
                "phoenixcore:dense_source_imbued_titanium_plate", "F",
                "phoenixcore:dense_frost_reinforced_stained_steel_plate", "C", "#c:tools/hammer");

        PftCrafting.shaped(provider, "aluminfrost_drill_head_pftsteamagerecipes", "phoenixcore:aluminfrost_drill_head",
                new String[] { "FWF", "FWF", "WCW" }, "W", "gtceu:dense_steel_plate", "F",
                "phoenixcore:dense_aluminfrost_plate", "C", "#c:tools/hammer");

        PftCrafting.shaped(provider, "space_grade_steel_drill_head_pftsteamagerecipes",
                "phoenixcore:space_grade_steel_drill_head", new String[] { "FWF", "FWF", "WCW" }, "W",
                "gtceu:dense_steel_plate", "F", "phoenixcore:dense_space_grade_steel_plate", "C", "#c:tools/hammer");

        PftCrafting.shaped(provider, "hv_earth_sieve_pftsteamagerecipes", "gtceu:hv_earth_sieve",
                new String[] { "OFO", "DWD", "CFC" }, "O", "phoenixcore:frost_reinforced_stained_steel_plate", "W",
                "gtceu:hv_machine_hull", "D", "gtceu:hv_electric_piston", "F", "gtceu:item_filter", "C",
                "#gtceu:circuits/hv");

        PftCrafting.shaped(provider, "lv_ember_igniter_pftsteamagerecipes", "gtceu:lv_ember_igniter",
                new String[] { "FPF", "WZW", "PFP" }, "P", "minecraft:lava_bucket", "W", "gtceu:tin_double_wire", "F",
                "phoenixcore:aurum_steel_plate", "Z", "gtceu:lv_machine_hull");

        PftCrafting.shaped(provider, "mv_ember_igniter_pftsteamagerecipes", "gtceu:mv_ember_igniter",
                new String[] { "FPF", "WZW", "PFP" }, "P", "minecraft:lava_bucket", "W",
                "gtceu:cupronickel_double_wire", "F", "phoenixcore:aluminfrost_plate", "Z", "gtceu:mv_machine_hull");

        PftCrafting.shaped(provider, "hv_ember_igniter_pftsteamagerecipes", "gtceu:hv_ember_igniter",
                new String[] { "FPF", "WZW", "PFP" }, "P", "minecraft:lava_bucket", "W", "gtceu:kanthal_double_wire",
                "F", "phoenixcore:frost_reinforced_stained_steel_plate", "Z", "gtceu:hv_machine_hull");
    }
}
