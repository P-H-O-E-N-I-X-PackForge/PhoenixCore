package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftExtendedAeRecipes {

    private PftExtendedAeRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "canner", "infinite_water_cell")
                .itemInputs("ae2:fluid_cell_housing", "gtceu:infinite_water_cover")
                .itemOutputs("extendedae:infinity_water_cell")
                .duration(100)
                .EUt(600)
                .save();

        PftRecipe.gt(provider, "canner", "infinite_cobble_cell")
                .itemInputs("ae2:item_cell_housing", "gtceu:infinite_water_cover")
                .inputFluids("minecraft:lava 5000")
                .itemOutputs("extendedae:infinity_cobblestone_cell")
                .duration(100)
                .EUt(600)
                .save();
        PftRecipe.gt(provider, "assembler", "extended_pattern_provider")
                .itemInputs("ae2:pattern_provider",
                        "4x ae2:engineering_processor",
                        "gtceu:soc",
                        "#gtceu:circuits/hv")
                .itemOutputs("extendedae:ex_pattern_provider")
                .duration(200)
                .EUt(50)
                .save();

        PftRecipe.gt(provider, "assembler", "extended_interface")
                .itemInputs("ae2:interface",
                        "4x ae2:logic_processor",
                        "gtceu:soc",
                        "#gtceu:circuits/hv")
                .itemOutputs("extendedae:ex_interface")
                .duration(200)
                .EUt(50)
                .save();

        PftRecipe.gt(provider, "assembler", "extended_pattern_provider_upgrade")
                .itemInputs("4x ae2:engineering_processor",
                        "gtceu:soc",
                        "#gtceu:circuits/hv")
                .itemOutputs("extendedae:pattern_provider_upgrade")
                .duration(180)
                .EUt(50)
                .save();

        PftRecipe.gt(provider, "assembler", "extended_interface_upgrade")
                .itemInputs("4x ae2:logic_processor", "gtceu:soc", "#gtceu:circuits/hv")
                .itemOutputs("extendedae:interface_upgrade")
                .duration(180)
                .EUt(50)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:extended_card")
                .itemInputs("gtceu:hv_machine_casing",
                        "2x gtceu:hv_sensor",
                        "gtceu:hv_emitter")
                .inputFluids("gtceu:argon 1000")
                .itemOutputs("2x extendedae:wireless_connect")
                .duration(200)
                .EUt(8192)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:ebus_out")
                .itemInputs("2x ae2:export_bus", "2x ae2:speed_card", "#gtceu:circuits/hv")
                .itemOutputs("extendedae:ex_export_bus_part")
                .EUt(512)
                .duration(100)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:ebus_in")
                .itemInputs("2x ae2:import_bus", "2x ae2:speed_card", "#gtceu:circuits/hv")
                .itemOutputs("extendedae:ex_import_bus_part")
                .EUt(512)
                .duration(100)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:ebus_upgrade")
                .itemInputs("2x ae2:speed_card", "#gtceu:circuits/hv")
                .itemOutputs("extendedae:io_bus_upgrade")
                .circuit(1)
                .EUt(512)
                .duration(100)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:me_packing_tape")
                .itemInputs("gtceu:duct_tape", "#c:dusts/fluix")
                .itemOutputs("extendedae:me_packing_tape")
                .EUt(120)
                .duration(50)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:ex_drive")
                .itemInputs("2x ae2:drive", "2x ae2:capacity_card", "#gtceu:circuits/hv")
                .itemOutputs("extendedae:ex_drive")
                .EUt(120)
                .duration(200)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:drive_upgrade")
                .itemInputs("2x ae2:capacity_card", "#gtceu:circuits/hv")
                .circuit(1)
                .itemOutputs("extendedae:drive_upgrade")
                .EUt(120)
                .duration(200)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:ex_molecular_assembler")
                .itemInputs("3x ae2:molecular_assembler", "2x ae2:capacity_card", "#gtceu:circuits/hv")
                .itemOutputs("extendedae:ex_molecular_assembler")
                .EUt(120)
                .duration(200)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:ex_io_port")
                .itemInputs("2x ae2:io_port", "2x ae2:capacity_card", "ae2:speed_card")
                .itemOutputs("extendedae:ex_io_port")
                .EUt(120)
                .duration(200)
                .save();

        PftRecipe.gt(provider, "compressor", "fishbig")
                .itemInputs("64x minecraft:cookie")
                .itemOutputs("extendedae:fishbig")
                .EUt(1)
                .duration(431152000)
                .save();

        PftCrafting.shaped(provider, "pattern_modifier_pftextendedaerecipes", "extendedae:pattern_modifier",
                new String[] { "PDP", "AKB", "PCP" }, "P", "#c:plates/ultimet", "D", "gtceu:computer_monitor_cover",
                "A", "ae2:logic_processor", "K", "#gtceu:circuits/iv", "B", "ae2:engineering_processor", "C",
                "ae2:calculation_processor");

        PftCrafting.shaped(provider, "ex_pattern_access_part_pftextendedaerecipes", "extendedae:ex_pattern_access_part",
                new String[] { "PL", "Cs" }, "P", "ae2:pattern_access_terminal", "L", "ae2:logic_processor", "C",
                "#gtceu:circuits/hv", "s", "#c:tools/screwdriver");

        PftCrafting.shapeless(provider, "tag_storage_bus_shapeless_pftextendedaerecipes", "extendedae:tag_storage_bus",
                "ae2:storage_bus", "gtceu:item_tag_filter");

        PftCrafting.shapeless(provider, "tag_export_bus_shapeless_pftextendedaerecipes", "extendedae:tag_export_bus",
                "ae2:export_bus", "gtceu:item_tag_filter");

        PftCrafting.shapeless(provider, "precise_storage_bus_shapeless_pftextendedaerecipes",
                "extendedae:precise_storage_bus", "ae2:storage_bus", "gtceu:lv_robot_arm");

        PftCrafting.shapeless(provider, "precise_export_bus_shapeless_pftextendedaerecipes",
                "extendedae:precise_export_bus", "ae2:export_bus", "gtceu:lv_robot_arm");

        PftCrafting.shapeless(provider, "threshold_export_bus_shapeless_pftextendedaerecipes",
                "extendedae:threshold_export_bus", "ae2:export_bus", "gtceu:item_detector_cover");

        PftCrafting.shaped(provider, "oversize_interface_pftextendedaerecipes", "extendedae:oversize_interface",
                new String[] { "TFT", "EBE", "TAT" }, "T", "#c:plates/titanium", "F", "ae2:formation_core", "E",
                "extendedae:ex_interface", "B", "gtceu:hv_buffer", "A", "ae2:annihilation_core");
    }
}
