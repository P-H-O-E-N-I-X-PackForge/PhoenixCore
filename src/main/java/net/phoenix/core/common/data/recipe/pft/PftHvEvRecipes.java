package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftHvEvRecipes {

    private PftHvEvRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "macerator", "certus_quartz_dust_2")
                .itemInputs("ae2:certus_quartz_crystal")
                .itemOutputs("gtceu:certus_quartz_dust")
                .duration(80)
                .EUt(GTValues.VA[GTValues.LV] / 2)
                .save();

        PftRecipe.gt(provider, "assembler", "good_circuit_universal")
                .save();

        PftRecipe.gt(provider, "assembly_line", "kubejs:epp/assembler_matrix_frame")
                .itemInputs("2x phoenixcore:dense_eighty_five_percent_pure_nevonian_steel_plate",
                        "4x gtceu:blue_steel_frame", "gtceu:plascrete", "#gtceu:circuits/ev")
                .inputFluids("gtceu:polytetrafluoroethylene 288")
                .itemOutputs("4x extendedae:assembler_matrix_frame")
                .duration(150)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "assembly_line", "kubejs:epp/assembler_matrix_glass")
                .itemInputs("2x phoenixcore:dense_eighty_five_percent_pure_nevonian_steel_plate",
                        "4x gtceu:blue_steel_frame", "gtceu:cleanroom_glass", "#gtceu:circuits/ev")
                .inputFluids("gtceu:polytetrafluoroethylene 288")
                .itemOutputs("4x extendedae:assembler_matrix_glass")
                .duration(150)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "assembly_line", "kubejs:epp/assembler_matrix_wall")
                .itemInputs("extendedae:assembler_matrix_frame", "gtceu:hv_electric_motor",
                        "phoenixcore:small_eighty_five_percent_pure_nevonian_steel_gear")
                .itemOutputs("extendedae:assembler_matrix_wall")
                .duration(100)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "assembly_line", "kubejs:epp/assembler_matrix_pattern")
                .itemInputs("4x extendedae:assembler_matrix_frame", "4x ae2:engineering_processor",
                        "extendedae:ex_pattern_provider", "6x ae2:blue_lumen_paint_ball",
                        "2x phoenixcore:eighty_five_percent_pure_nevonian_steel_spring")
                .inputFluids("gtceu:polytetrafluoroethylene 288")
                .itemOutputs("extendedae:assembler_matrix_pattern")
                .duration(150)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "assembly_line", "kubejs:epp/assembler_matrix_crafter")
                .itemInputs("4x extendedae:assembler_matrix_frame", "4x ae2:engineering_processor",
                        "extendedae:ex_molecular_assembler", "6x ae2:purple_lumen_paint_ball",
                        "2x phoenixcore:eighty_five_percent_pure_nevonian_steel_screw")
                .inputFluids("gtceu:polytetrafluoroethylene 288")
                .itemOutputs("extendedae:assembler_matrix_crafter")
                .duration(150)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "assembly_line", "kubejs:epp/assembler_matrix_speed")
                .itemInputs("4x extendedae:assembler_matrix_frame", "4x megacells:accumulation_processor",
                        "mae2:4x_crafting_accelerator", "extendedae:ex_pattern_provider",
                        "extendedae:ex_molecular_assembler", "6x ae2:red_lumen_paint_ball",
                        "16x phoenixcore:eighty_five_percent_pure_nevonian_steel_frame")
                .inputFluids("gtceu:polytetrafluoroethylene 288")
                .itemOutputs("extendedae:assembler_matrix_speed")
                .duration(150)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "printed_accumulation_processor")
                .itemInputs("2x phoenixcore:eighty_five_percent_pure_nevonian_steel_gear",
                        "4x megacells:sky_steel_ingot", "gtceu:tempered_glass")
                .inputFluids("gtceu:hydrofluoric_acid 288")
                .itemOutputs("2x megacells:printed_accumulation_processor")
                .duration(150)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "circuit_assembler", "accumulation_processor")
                .itemInputs("4x phoenixcore:eighty_five_percent_pure_nevonian_steel_rod", "gtceu:tungsten_steel_frame",
                        "#gtceu:circuits/ev")
                .inputFluids("gtceu:soldering_alloy 288")
                .itemOutputs("1x megacells:accumulation_processor")
                .duration(150)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "centrifuge", "zircon_dust_from_blackstone_dust")
                .itemInputs("3x gtceu:blackstone_dust")
                .itemOutputs("1x phoenixcore:magma_dust", "1x phoenixcore:dormant_ember_dust", "gtceu:silicon_dust")
                .chancedOutput("1x phoenixcore:zircon_dust", 2500)
                .duration(650)
                .EUt(GTValues.VA[GTValues.MV] / 2)
                .save();

        PftRecipe.gt(provider, "centrifuge", "electrotine_dust")
                .itemInputs("3x gtceu:electrotine_dust")
                .itemOutputs("1x minecraft:redstone", "1x gtceu:electrum_dust")
                .duration(80)
                .EUt(GTValues.VA[GTValues.LV] / 2)
                .save();

        PftRecipe.gt(provider, "centrifuge", "oil_sands_light")
                .itemInputs("gtceu:oilsands_dust")
                .chancedOutput("minecraft:sand", 8500)
                .outputFluids("gtceu:oil_light 2000")
                .circuit(1)
                .duration(10 * 20)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "oil_sands_heavy")
                .itemInputs("gtceu:oilsands_dust")
                .chancedOutput("minecraft:sand", 8500)
                .outputFluids("gtceu:oil_heavy 2000")
                .circuit(2)
                .duration(10 * 20)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "oil_sands_normal")
                .itemInputs("gtceu:oilsands_dust")
                .chancedOutput("minecraft:sand", 8500)
                .outputFluids("gtceu:oil 2000")
                .circuit(3)
                .duration(10 * 20)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "oil_sands_raw")
                .itemInputs("gtceu:oilsands_dust")
                .chancedOutput("minecraft:sand", 8500)
                .outputFluids("gtceu:oil_medium 2000")
                .circuit(4)
                .duration(10 * 20)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "cutter", "quartz_plates")
                .itemInputs("ae2:quartz_block")
                .itemOutputs("4x gtceu:certus_quartz_plate")
                .inputFluids("gtceu:distilled_water 100")
                .duration(100)
                .EUt(GTValues.VA[GTValues.MV] / 2)
                .save();

        PftRecipe.gt(provider, "assembler", "tinted_glass")
                .itemInputs("4x gtceu:lead_plate", "12x gtceu:tempered_glass", "4x minecraft:amethyst_shard",
                        "2x phoenixcore:aluminfrost_plate")
                .circuit(6)
                .itemOutputs("12x minecraft:tinted_glass")
                .inputFluids("gtceu:steel 144")
                .duration(100)
                .EUt(GTValues.VA[GTValues.LV] / 2)
                .save();

        PftRecipe.gt(provider, "macerator", "aurum_wood_dust")
                .itemInputs("1x forbidden_arcanus:aurum_log")
                .itemOutputs("4x phoenixcore:aurum_wood_dust")
                .duration(100)
                .EUt(GTValues.VA[GTValues.LV] / 2)
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "uranium_oxide")
                .itemInputs("2x gtceu:uraninite_dust")
                .inputFluids("gtceu:oxygen 500")
                .outputFluids("phoenixcore:uranium_oxide 1000")
                .duration(100)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();

        PftRecipe.gt(provider, "assembler", "zirconium_rod")
                .itemInputs("4x gtceu:lead_plate", "gtceu:universal_fluid_cell", "2x gtceu:stainless_steel_plate",
                        "1x phoenixcore:dense_zircalloy_plate")
                .itemOutputs("12x phoenixcore:zirconium_rod")
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();

        PftRecipe.gt(provider, "canner", "zirconium_fuel_rod")
                .itemInputs("1x phoenixcore:zirconium_rod")
                .inputFluids("phoenixcore:uranium_oxide 250")
                .itemOutputs("phoenixcore:basic_fuel_rod")
                .duration(120)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();

        PftRecipe.gt(provider, "assembler", "dimensional_anchor")
                .itemInputs("4x phoenixcore:frost_reinforced_stained_steel_drilling_kit",
                        "2x phoenixcore:frost_reinforced_stained_steel_gear",
                        "8x phoenixcore:dense_frost_reinforced_stained_steel_plate", "3x #gtceu:circuits/ev",
                        "3x phoenixcore:dense_zircalloy_plate", "4x phoenixcore:icy_steel_matrix_single_wire")
                .inputFluids("gtceu:soldering_alloy 4200")
                .itemOutputs("1x phoenixcore:dimensional_anchor")
                .duration(150)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "assembler", "aetherial_fabricator")
                .itemInputs("4x phoenixcore:source_imbued_titanium_drill_head",
                        "2x phoenixcore:source_imbued_titanium_gear",
                        "8x phoenixcore:source_imbued_titanium_plate", "5x #gtceu:circuits/ev",
                        "3x phoenixcore:dense_zircalloy_plate", "4x phoenixcore:source_titanium_filament_single_wire")
                .inputFluids("gtceu:soldering_alloy 4200")
                .itemOutputs("1x phoenixcore:aetherical_fabricator")
                .duration(150)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "compressor", "budding_amethyst_assembly")
                .itemInputs("16x minecraft:amethyst_block")
                .itemOutputs("minecraft:budding_amethyst")
                .duration(1200)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "assembler", "flawless_budding_certus_quartz_assembly")
                .itemInputs("4x ae2:charged_certus_quartz_crystal",
                        "4x ae2:certus_quartz_crystal",
                        "ae2:growth_accelerator",
                        "ae2:fluix_crystal")
                .itemOutputs("ae2:flawless_budding_quartz")
                .duration(400)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "mixer", "void_touched_tungsten_steel")
                .itemInputs("4x gtceu:tungsten_dust", "4x phoenixcore:voidglass_shard_dust",
                        "2x gtceu:vanadium_steel_dust")
                .inputFluids("gtceu:liquid_ender_air 500")
                .itemOutputs("8x phoenixcore:void_touched_tungsten_steel_dust")
                .duration(160)
                .circuit(15)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "mixer", "space_grade_steel_dust")
                .itemInputs("1x phoenixcore:zircalloy_dust",
                        "2x gtceu:tungsten_dust",
                        "4x phoenixcore:icy_steel_matrix_dust",
                        "2x gtceu:molybdenum_dust")
                .inputFluids("gtceu:steel 750")
                .itemOutputs("4x phoenixcore:space_grade_steel_dust")
                .duration(200)
                .circuit(12)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "gas_collector", "void_air")
                .circuit(4)
                .outputFluids("gtceu:air 10000")
                .EUt(GTValues.VA[GTValues.LV] / 2)
                .duration(200)
                .save();

        PftRecipe.gt(provider, "centrifuge", "fissio")
                .itemInputs("5x phoenixcore:low_level_radioactive_waste_dust")
                .itemOutputs("5x gtceu:lead_dust", "3x gtceu:thorium_dust")
                .outputFluids("gtceu:helium 500")
                .duration(150)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "centrifuge", "medium_level_waste_processing")
                .itemInputs("5x phoenixcore:medium_level_radioactive_waste_dust")
                .itemOutputs("2x gtceu:neptunium_dust",
                        "2x gtceu:barium_dust",
                        "1x gtceu:plutonium_dust")
                .outputFluids("gtceu:helium 1000",
                        "gtceu:krypton 250")
                .duration(300)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "extractor", "lava_from_magma")
                .itemInputs("minecraft:magma_block")
                .outputFluids("minecraft:lava 250")
                .duration(280)
                .EUt(GTValues.VA[GTValues.LV] / 2)
                .save();

        PftRecipe.gt(provider, "extractor", "lava_from_magma_dust")
                .itemInputs("phoenixcore:magma_dust")
                .outputFluids("minecraft:lava 150")
                .duration(280)
                .EUt(GTValues.VA[GTValues.LV] / 2)
                .save();

        PftRecipe.gt(provider, "assembler", "space_grade_steel_drilling_kit")
                .itemInputs("16x phoenixcore:space_grade_steel_plate",
                        "32x phoenixcore:space_grade_steel_screw",
                        "4x gtceu:ev_field_generator",
                        "8x gtceu:ev_electric_motor",
                        "20x #gtceu:circuits/ev",
                        "16x phoenixcore:source_titanium_filament_single_wire",
                        "8x phoenixcore:space_grade_steel_drill_head",
                        "1x gtceu:ev_machine_hull")
                .inputFluids("gtceu:soldering_alloy 1200")
                .itemOutputs("phoenixcore:space_grade_steel_drilling_kit")
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "frost_reinforced_stained_steel_drilling_kit")
                .itemInputs("16x phoenixcore:frost_reinforced_stained_steel_plate",
                        "32x phoenixcore:frost_reinforced_stained_steel_screw",
                        "4x gtceu:hv_field_generator",
                        "8x gtceu:hv_electric_motor",
                        "24x #gtceu:circuits/hv",
                        "16x phoenixcore:icy_steel_matrix_single_wire",
                        "12x phoenixcore:frost_reinforced_stained_steel_drill_head",
                        "1x gtceu:hv_machine_hull")
                .inputFluids("gtceu:soldering_alloy 1200")
                .itemOutputs("phoenixcore:frost_reinforced_stained_steel_drilling_kit")
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "aluminfrost_drilling_kit")
                .itemInputs("16x phoenixcore:aluminfrost_plate",
                        "32x phoenixcore:aluminfrost_screw",
                        "4x gtceu:mv_field_generator",
                        "8x gtceu:mv_electric_motor",
                        "20x #gtceu:circuits/mv",
                        "16x phoenixcore:cryogenic_aluminum_strand_single_wire",
                        "10x phoenixcore:aluminfrost_drill_head",
                        "1x gtceu:mv_machine_hull")
                .inputFluids("gtceu:soldering_alloy 1200")
                .itemOutputs("phoenixcore:aluminfrost_drilling_kit")
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "aurum_steel_drilling_kit")
                .itemInputs("16x phoenixcore:aurum_steel_plate",
                        "32x phoenixcore:aurum_steel_screw",
                        "4x gtceu:lv_field_generator",
                        "8x gtceu:lv_electric_motor",
                        "20x #gtceu:circuits/lv",
                        "16x phoenixcore:magmatic_manganese_lead_single_wire",
                        "8x phoenixcore:aurum_steel_drill_head",
                        "1x gtceu:lv_machine_hull")
                .inputFluids("gtceu:soldering_alloy 1200")
                .itemOutputs("phoenixcore:aurum_steel_drilling_kit")
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "mixer", "magmatic_manganese_lead_alloy")
                .itemInputs("2x gtceu:manganese_dust",
                        "2x gtceu:lead_dust",
                        "8x phoenixcore:dormant_ember_dust")
                .inputFluids("minecraft:lava 288")
                .itemOutputs("12x phoenixcore:magmatic_manganese_lead_dust")
                .duration(200)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "mixer", "cryo_aluminum_superconductor_alloy")
                .itemInputs("4x gtceu:aluminium_dust",
                        "1x gtceu:silicon_dust",
                        "6x phoenixcore:permafrost_dust")
                .inputFluids("gtceu:nitrogen 250")
                .itemOutputs("5x phoenixcore:cryogenic_aluminum_strand_dust")
                .duration(300)
                .EUt(GTValues.VA[GTValues.MV])
                .save();

        PftRecipe.gt(provider, "mixer", "icy_steel_matrix_alloy")
                .itemInputs("4x gtceu:steel_dust",
                        "2x gtceu:stainless_steel_dust",
                        "4x phoenixcore:unrefined_aluminfrost_dust",
                        "12x phoenixcore:permafrost_dust")
                .inputFluids("gtceu:nitrogen 50")
                .itemOutputs("18x phoenixcore:icy_steel_matrix_dust")
                .duration(600)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "assembler", "diamond_ring")
                .itemInputs("4x gtceu:double_iridium_plate",
                        "32x gtceu:tungsten_steel_plate",
                        "2x gtceu:molybdenum_dust",
                        "1x gtceu:advanced_electric_jetpack")
                .inputFluids("phoenixcore:void_touched_tungsten_steel 670")
                .itemOutputs("1x angelring:diamond_ring")
                .duration(500)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "space_miner_module")
                .itemInputs("1x phoenixcore:space_grade_steel_drilling_kit",
                        "1x gtceu:ev_machine_hull",
                        "4x gtceu:ev_robot_arm",
                        "4x gtceu:ev_sensor",
                        "2x gtceu:ev_field_generator",
                        "8x #gtceu:circuits/ev",
                        "4x gtceu:ev_emitter",
                        "8x phoenixcore:space_grade_steel_plate",
                        "8x phoenixcore:source_titanium_filament_single_wire")
                .inputFluids("phoenixcore:cryo_ember_fluid 32000")
                .itemOutputs("phoenixcore:space_miner_module")
                .duration(600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "earthbound_miner_module")
                .itemInputs("2x phoenixcore:frost_reinforced_stained_steel_drilling_kit",
                        "4x phoenixcore:aurum_steel_drilling_kit",
                        "2x phoenixcore:aluminfrost_drilling_kit",
                        "1x gtceu:ev_machine_hull",
                        "4x gtceu:ev_robot_arm",
                        "8x gtceu:stable_machine_casing",
                        "4x gtceu:ev_electric_pump",
                        "32x #gtceu:circuits/ev",
                        "16x phoenixcore:icy_steel_matrix_single_wire")
                .inputFluids("phoenixcore:cryo_ember_fluid 12000")
                .itemOutputs("phoenixcore:earthbound_miner_module")
                .duration(600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "aluminfrost_drum")
                .itemInputs("2x phoenixcore:long_aluminfrost_rod", "4x phoenixcore:aluminfrost_plate")
                .circuit(2)
                .itemOutputs("phoenixcore:aluminfrost_drum")
                .duration(200)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "assembler", "aluminfrost_crate")
                .itemInputs("4x phoenixcore:long_aluminfrost_rod", "4x phoenixcore:aluminfrost_plate")
                .circuit(1)
                .itemOutputs("phoenixcore:aluminfrost_crate")
                .duration(200)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "assembler", "aurum_steel_drum")
                .itemInputs("2x phoenixcore:long_aurum_steel_rod", "4x phoenixcore:aurum_steel_plate")
                .circuit(2)
                .itemOutputs("phoenixcore:aurum_steel_drum")
                .duration(100)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "assembler", "aurum_crate")
                .itemInputs("4x phoenixcore:long_aurum_steel_rod", "4x phoenixcore:aurum_steel_plate")
                .circuit(1)
                .itemOutputs("phoenixcore:aurum_steel_crate")
                .duration(100)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "assembler", "void_touched_tungsten_steel_drum")
                .itemInputs("2x phoenixcore:long_void_touched_tungsten_steel_rod",
                        "4x phoenixcore:void_touched_tungsten_steel_plate")
                .circuit(2)
                .itemOutputs("phoenixcore:void_touched_tungsten_steel_drum")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "void_touched_tungsten_steel_crate")
                .circuit(1)
                .itemInputs("4x phoenixcore:long_void_touched_tungsten_steel_rod",
                        "4x phoenixcore:void_touched_tungsten_steel_plate")
                .itemOutputs("phoenixcore:void_touched_tungsten_steel_crate")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "frost_reinforced_stained_steel_drum")
                .itemInputs("2x phoenixcore:long_frost_reinforced_stained_steel_rod",
                        "4x phoenixcore:frost_reinforced_stained_steel_plate")
                .circuit(2)
                .itemOutputs("phoenixcore:frost_reinforced_stained_steel_drum")
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "assembler", "frost_reinforced_stained_steel_crate")
                .itemInputs("4x phoenixcore:long_frost_reinforced_stained_steel_rod",
                        "4x phoenixcore:frost_reinforced_stained_steel_plate")
                .circuit(1)
                .itemOutputs("phoenixcore:frost_reinforced_stained_steel_crate")
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "assembler", "source_imbued_titanium_drum")
                .itemInputs("2x phoenixcore:long_source_imbued_titanium_rod",
                        "4x phoenixcore:source_imbued_titanium_plate")
                .circuit(2)
                .itemOutputs("phoenixcore:source_imbued_titanium_drum")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "source_imbued_titanium_crate")
                .itemInputs("4x phoenixcore:long_source_imbued_titanium_rod",
                        "4x phoenixcore:source_imbued_titanium_plate")
                .circuit(1)
                .itemOutputs("phoenixcore:source_imbued_titanium_crate")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "resonant_rhodium_alloy_drum")
                .itemInputs("2x phoenixcore:long_resonant_rhodium_alloy_rod",
                        "4x phoenixcore:resonant_rhodium_alloy_plate")
                .circuit(2)
                .itemOutputs("phoenixcore:resonant_rhodium_alloy_drum")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "resonant_rhodium_alloy_crate")
                .itemInputs("4x phoenixcore:long_resonant_rhodium_alloy_rod",
                        "4x phoenixcore:resonant_rhodium_alloy_plate")
                .circuit(1)
                .itemOutputs("phoenixcore:resonant_rhodium_alloy_crate")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save();
    }
}
