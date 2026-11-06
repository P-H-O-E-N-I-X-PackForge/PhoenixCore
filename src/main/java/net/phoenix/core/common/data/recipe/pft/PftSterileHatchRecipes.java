package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftSterileHatchRecipes {

    private PftSterileHatchRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembly_line", "sterile_cleaning_maintenance_hatch")
                .itemInputs("4x #gtceu:circuits/uhv",
                        "2x gtceu:uv_robot_arm",
                        "1x gtceu:uhv_machine_hull",
                        "1x gtceu:auto_maintenance_hatch",
                        "2x gtceu:ruthenium_trinium_americium_neutronate_single_wire")
                .inputFluids("gtceu:soldering_alloy 1152", "gtceu:tritanium 1152")
                .itemOutputs("gtmutils:sterile_cleaning_maintenance_hatch")
                .duration(600)
                .EUt(GTValues.VA[GTValues.UHV] / 2)
                .stationResearch("gtceu:cleaning_maintenance_hatch", 30720, 64, 32000, null)
                .save();

        PftRecipe.gt(provider, "fusion_reactor", "stabilized_antimatter")
                .inputFluids("kubejs:chaos 5000",
                        "gtceu:uranium_235 2500")
                .outputFluids("gtceu:stabilized_antimatter 1000")
                .duration(64)
                .EUt(1048576)
                .fusionStartEU(600000000)
                .save();

        PftRecipe.gt(provider, "extractor", "chaos")
                .itemInputs("4x draconicevolution:large_chaos_frag")
                .outputFluids("kubejs:chaos 6000")
                .duration(1000)
                .EUt(1048576)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_motor")
                .itemInputs("gtceu:long_magnetic_samarium_rod", "8x gtceu:long_stabilized_antimatter_rod",
                        "8x gtceu:stabilized_antimatter_ring", "16x gtceu:stabilized_antimatter_round",
                        "64x gtceu:fine_ruthenium_trinium_americium_neutronate_wire",
                        "64x gtceu:fine_ruthenium_trinium_americium_neutronate_wire",
                        "32x gtceu:fine_ruthenium_trinium_americium_neutronate_wire", "2x gtceu:europium_single_cable",
                        "4x gtceu:ruthenium_trinium_americium_neutronate_plate")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:lubricant 2000", "gtceu:tritanium 1152",
                        "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_electric_motor")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_electric_motor", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_piston")
                .itemInputs("gtceu:uhv_electric_motor", "4x gtceu:stabilized_antimatter_plate",
                        "4x gtceu:stabilized_antimatter_ring", "16x gtceu:stabilized_antimatter_round",
                        "4x gtceu:stabilized_antimatter_rod", "gtceu:stabilized_antimatter_gear",
                        "2x gtceu:small_stabilized_antimatter_gear", "2x gtceu:europium_single_cable",
                        "2x gtceu:ruthenium_trinium_americium_neutronate_plate")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:lubricant 2000", "gtceu:tritanium 1152",
                        "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_electric_piston")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_electric_piston", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_robot_arm")
                .itemInputs("4x gtceu:long_stabilized_antimatter_rod", "gtceu:stabilized_antimatter_gear",
                        "3x gtceu:small_stabilized_antimatter_gear", "3x gtceu:uhv_electric_motor",
                        "gtceu:uhv_electric_piston", "#gtceu:circuits/uhv", "2x #gtceu:circuits/uv",
                        "4x #gtceu:circuits/zpm", "4x gtceu:europium_single_cable",
                        "2x gtceu:ruthenium_trinium_americium_neutronate_plate")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:lubricant 2000", "gtceu:tritanium 1152",
                        "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_robot_arm")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_robot_arm", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_sensor")
                .itemInputs("gtceu:neutronium_frame", "gtceu:uhv_electric_motor",
                        "4x gtceu:stabilized_antimatter_plate", "2x gtceu:gravi_star", "#gtceu:circuits/uhv",
                        "64x gtceu:ruthenium_trinium_americium_neutronate_foil",
                        "32x gtceu:ruthenium_trinium_americium_neutronate_foil", "4x gtceu:europium_single_cable",
                        "2x gtceu:fine_ruthenium_trinium_americium_neutronate_wire")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:tritanium 1152", "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_sensor")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_sensor", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_emitter")
                .itemInputs("gtceu:neutronium_frame", "gtceu:uhv_electric_motor",
                        "4x gtceu:long_stabilized_antimatter_rod", "2x gtceu:gravi_star", "#gtceu:circuits/uhv",
                        "64x gtceu:ruthenium_trinium_americium_neutronate_foil",
                        "32x gtceu:ruthenium_trinium_americium_neutronate_foil", "4x gtceu:europium_single_cable",
                        "2x gtceu:fine_ruthenium_trinium_americium_neutronate_wire")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:tritanium 1152", "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_emitter")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_emitter", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_field_generator")
                .itemInputs("gtceu:neutronium_frame", "6x gtceu:stabilized_antimatter_plate", "2x gtceu:gravi_star",
                        "2x gtceu:uhv_emitter", "2x #gtceu:circuits/uhv",
                        "64x gtceu:fine_ruthenium_trinium_americium_neutronate_wire",
                        "64x gtceu:fine_ruthenium_trinium_americium_neutronate_wire", "4x gtceu:europium_single_cable",
                        "4x gtceu:ruthenium_trinium_americium_neutronate_plate")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:tritanium 1152", "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_field_generator")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_field_generator", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_conveyor")
                .itemInputs("2x gtceu:uhv_electric_motor", "2x gtceu:stabilized_antimatter_plate",
                        "4x gtceu:stabilized_antimatter_ring", "16x gtceu:stabilized_antimatter_round",
                        "4x gtceu:stabilized_antimatter_screw", "2x gtceu:europium_single_cable",
                        "2x gtceu:ruthenium_trinium_americium_neutronate_plate")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:lubricant 2000",
                        "gtceu:styrene_butadiene_rubber 5760", "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_conveyor_module")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_conveyor_module", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembly_line", "uhv_pump")
                .itemInputs("gtceu:uhv_electric_motor", "gtceu:neutronium_large_fluid_pipe",
                        "2x gtceu:stabilized_antimatter_plate", "8x gtceu:stabilized_antimatter_screw",
                        "16x gtceu:silicone_rubber_ring", "12x gtceu:stabilized_antimatter_ring",
                        "gtceu:tungsten_steel_rotor", "2x gtceu:europium_single_cable",
                        "2x gtceu:ruthenium_trinium_americium_neutronate_plate")
                .inputFluids("gtceu:soldering_alloy 5760", "gtceu:lubricant 2000", "gtceu:tritanium 1152",
                        "gtceu:naquadria 576")
                .itemOutputs("gtceu:uhv_electric_pump")
                .duration(600)
                .EUt(491520)
                .stationResearch("gtceu:uv_electric_pump", 368640, 96, 384000, null)
                .save();

        PftRecipe.gt(provider, "assembler", "uhv_fluid_regulator")
                .itemInputs("gtceu:uhv_electric_pump", "2x #gtceu:circuits/uhv",
                        "1x gtceu:ruthenium_trinium_americium_neutronate_plate")
                .circuit(1)
                .itemOutputs("gtceu:uhv_fluid_regulator")
                .duration(50)
                .EUt(1966080)
                .save();
    }
}
