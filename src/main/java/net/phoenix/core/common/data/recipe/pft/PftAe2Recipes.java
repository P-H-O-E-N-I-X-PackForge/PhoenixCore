package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftAe2Recipes {

    private PftAe2Recipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "alloy_smelter", "gtceu:quartz_glass")
                .itemInputs("4x #c:glass_blocks", "5x #c:dusts/certus_quartz")
                .itemOutputs("4x ae2:quartz_glass")
                .duration(100)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "alloy_smelter", "gtceu:vibrant_quartz_glass")
                .itemInputs("ae2:quartz_glass", "2x minecraft:glowstone_dust")
                .itemOutputs("2x ae2:quartz_vibrant_glass")
                .duration(100)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "cutter", "gtceu:tiny_tnt")
                .inputFluids("minecraft:water 9000")
                .itemInputs("minecraft:tnt")
                .itemOutputs("4x ae2:tiny_tnt")
                .duration(200)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "compressor", "gtceu:dense_energy_cell")
                .itemInputs("8x ae2:energy_cell")
                .itemOutputs("ae2:dense_energy_cell")
                .duration(100)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "canner", "gtceu:ae2_accelerator")
                .itemInputs("ae2:crafting_unit", "ae2:engineering_processor")
                .itemOutputs("ae2:crafting_accelerator")
                .duration(100)
                .EUt(64)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:4x_crafting_accelerator")
                .itemInputs("3x ae2:crafting_accelerator", "ae2:cell_component_4k", "#gtceu:circuits/hv")
                .itemOutputs("mae2:4x_crafting_accelerator")
                .duration(100)
                .EUt(130)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:16x_crafting_accelerator")
                .itemInputs("3x mae2:4x_crafting_accelerator", "ae2:cell_component_16k", "#gtceu:circuits/ev")
                .itemOutputs("mae2:16x_crafting_accelerator")
                .duration(100)
                .EUt(520)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:64x_crafting_accelerator")
                .itemInputs("3x mae2:16x_crafting_accelerator", "ae2:cell_component_64k", "#gtceu:circuits/iv")
                .itemOutputs("mae2:64x_crafting_accelerator")
                .duration(100)
                .EUt(2080)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:256x_crafting_accelerator")
                .itemInputs("3x mae2:64x_crafting_accelerator", "ae2:cell_component_256k", "#gtceu:circuits/luv")
                .itemOutputs("mae2:256x_crafting_accelerator")
                .duration(100)
                .EUt(8320)
                .save();

        PftRecipe.gt(provider, "canner", "gtceu:crafting_monitor")
                .itemInputs("ae2:crafting_unit", "ae2:storage_monitor")
                .itemOutputs("ae2:crafting_monitor")
                .duration(100)
                .EUt(4)
                .save();

        PftRecipe.gt(provider, "mixer", "gtceu:fluix_dust")
                .itemInputs("#c:dusts/nether_quartz",
                        "#c:dusts/certus_quartz",
                        "#c:dusts/redstone")
                .itemOutputs("3x gtceu:fluix_dust")
                .duration(200)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "polarizer", "gtceu:charged_certus_quartz")
                .itemInputs("gtceu:certus_quartz_gem")
                .itemOutputs("ae2:charged_certus_quartz_crystal")
                .duration(100)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "gtceu:fluix_pearl")
                .itemInputs("#c:dusts/fluix", "ender_eye")
                .itemOutputs("ae2:fluix_pearl")
                .duration(200)
                .EUt(480)
                .save();

        PftRecipe.gt(provider, "extruder", "gtceu:cable_anchor")
                .itemInputs("#c:plates/steel")
                .notConsumable("gtceu:bolt_extruder_mold")
                .itemOutputs("8x ae2:cable_anchor")
                .duration(20)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "alloy_smelter", "gtceu:quartz_fibers")
                .itemInputs("#c:dusts/certus_quartz", "ae2:quartz_glass")
                .itemOutputs("2x ae2:quartz_fiber")
                .duration(60)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "autoclave", "gtceu:fluix_crystal")
                .itemInputs("gtceu:fluix_dust")
                .inputFluids("water 100")
                .itemOutputs("ae2:fluix_crystal")
                .duration(200)
                .EUt(24)
                .save();

        PftRecipe.gt(provider, "autoclave", "gtceu:fluix_crystal_faster")
                .itemInputs("gtceu:fluix_dust")
                .inputFluids("gtceu:distilled_water 20")
                .itemOutputs("ae2:fluix_crystal")
                .duration(100)
                .EUt(24)
                .save();

        PftRecipe.gt(provider, "lathe", "fluix_lathing")
                .itemInputs("ae2:fluix_crystal")
                .itemOutputs("gtceu:fluix_rod")
                .duration(200)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "circuit_assembler", "gtceu:logic_processor")
                .itemInputs("ae2:printed_logic_processor",
                        "ae2:printed_silicon",
                        "#gtceu:circuits/lv",
                        "#gtceu:resistors",
                        "2x #c:fine_wires/tin")
                .itemOutputs("4x ae2:logic_processor")
                .duration(200)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "circuit_assembler", "gtceu:engineering_processor")
                .itemInputs("ae2:printed_engineering_processor",
                        "ae2:printed_silicon",
                        "#gtceu:circuits/lv",
                        "#gtceu:resistors",
                        "2x #c:fine_wires/tin")
                .itemOutputs("4x ae2:engineering_processor")
                .duration(200)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "circuit_assembler", "gtceu:calculation_processor")
                .itemInputs("ae2:printed_calculation_processor",
                        "ae2:printed_silicon",
                        "#gtceu:circuits/lv",
                        "#gtceu:resistors",
                        "2x #c:fine_wires/tin")
                .itemOutputs("4x ae2:calculation_processor")
                .duration(200)
                .EUt(30)
                .save();

        PftCrafting.shaped(provider, "wireless_access_point_pftae2recipes", "ae2:wireless_access_point",
                new String[] { "PWP", "RCR", "PFP" }, "P", "phoenixcore:frost_reinforced_stained_steel_plate", "W",
                "ae2:wireless_receiver", "R", "phoenixcore:frost_reinforced_stained_steel_rod", "C",
                "ae2:calculation_processor", "F", "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "quantum_ring_pftae2recipes", "2x ae2:quantum_ring",
                new String[] { "PLP", "EHE", "PLP" }, "P", "gtceu:titanium_plate", "L", "ae2:logic_processor", "E",
                "ae2:engineering_processor", "H", "gtceu:hv_field_generator");

        PftCrafting.shaped(provider, "quantum_link_pftae2recipes", "2x ae2:quantum_link",
                new String[] { "GPG", "PHP", "GPG" }, "G", "ae2:quartz_glass", "P", "ae2:fluix_pearl", "H",
                "gtceu:hv_field_generator");

        PftCrafting.shaped(provider, "drive_pftae2recipes", "ae2:drive", new String[] { "PEP", "KCK", "PEP" }, "P",
                "#c:plates/steel", "E", "ae2:engineering_processor", "K", "ae2:fluix_glass_cable", "C",
                "#gtceu:circuits/lv");

        PftCrafting.shaped(provider, "chest_pftae2recipes", "ae2:chest", new String[] { "GTG", "KCK", "PFP" }, "G",
                "ae2:quartz_glass", "T", "ae2:terminal", "K", "ae2:fluix_glass_cable", "C", "#gtceu:circuits/ulv", "P",
                "#c:plates/steel", "F", "#c:gems/fluix");

        PftCrafting.shaped(provider, "cell_workbench_pftae2recipes", "ae2:cell_workbench",
                new String[] { "FKL", "PCP", "PPP" }, "F", "gtceu:item_filter", "K", "ae2:fluix_glass_cable", "L",
                "gtceu:fluid_filter", "P", "#c:plates/steel", "C", "#c:chests/wooden");

        PftCrafting.shaped(provider, "io_port_pftae2recipes", "ae2:io_port", new String[] { "UGR", "DCD", "PLP" }, "U",
                "gtceu:lv_pump", "G", "#c:glass_blocks", "R", "gtceu:lv_robot_arm", "D", "ae2:drive", "C",
                "ae2:fluix_glass_cable", "P", "#c:plates/steel", "L", "ae2:logic_processor");

        PftCrafting.shaped(provider, "condenser_pftae2recipes", "ae2:condenser", new String[] { "PCP", "CHC", "PCP" },
                "P", "#c:plates/stainless_steel", "C", "gtceu:hv_electric_piston", "H", "#gtceu:circuits/hv");

        PftCrafting.shaped(provider, "energy_acceptor_pftae2recipes", "ae2:energy_acceptor",
                new String[] { "PFP", "FVF", "PFP" }, "P", "#c:plates/steel", "F", "#c:plates/fluix", "V",
                "gtceu:lv_voltage_coil");

        PftCrafting.shaped(provider, "energy_cell_pftae2recipes", "ae2:energy_cell",
                new String[] { "SPS", "CGC", "SPS" }, "S", "gtceu:certus_quartz_gem", "P", "#c:plates/certus_quartz",
                "C", "gtceu:lv_voltage_coil", "G", "ae2:quartz_vibrant_glass");

        PftCrafting.shaped(provider, "crafting_unit_pftae2recipes", "ae2:crafting_unit",
                new String[] { "PCP", "KMK", "PLP" }, "P", "#c:plates/aluminium", "C", "ae2:calculation_processor", "K",
                "ae2:fluix_glass_cable", "M", "#gtceu:circuits/mv", "L", "ae2:logic_processor");

        PftCrafting.shaped(provider, "creative_energy_cell_pftae2recipes", "ae2:creative_energy_cell",
                new String[] { "CCC", "CUC", "CCC" }, "C", "ae2:dense_energy_cell", "U", "gtceu:uv_field_generator");

        PftCrafting.shaped(provider, "memory_card_pftae2recipes", "ae2:memory_card",
                new String[] { "EC ", "WP ", "   " }, "E", "ae2:calculation_processor", "C", "#gtceu:circuits/lv", "W",
                "#c:fine_wires/gold", "P", "#c:plates/iron");

        PftCrafting.shaped(provider, "network_tool_pftae2recipes", "ae2:network_tool",
                new String[] { "wC ", "EP ", "   " }, "w", "#c:tools/wrench", "E", "ae2:calculation_processor", "C",
                "#gtceu:circuits/lv", "P", "#ae2:illuminated_panel");

        PftCrafting.shaped(provider, "wireless_receiver_pftae2recipes", "ae2:wireless_receiver",
                new String[] { "CPC", "RSR", "   " }, "C", "#gtceu:circuits/ev", "P", "ae2:fluix_pearl", "R",
                "phoenixcore:frost_reinforced_stained_steel", "S", "gtceu:hv_sensor");

        PftCrafting.shaped(provider, "annihilation_core_pftae2recipes", "ae2:annihilation_core", new String[] { "CPB" },
                "C", "#gtceu:circuits/ulv", "P", "ae2:logic_processor", "B", "#c:dusts/nether_quartz");

        PftCrafting.shaped(provider, "formation_core_pftae2recipes", "ae2:formation_core", new String[] { "CPB" }, "C",
                "#gtceu:circuits/ulv", "P", "ae2:logic_processor", "B", "#c:dusts/certus_quartz");

        PftCrafting.shaped(provider, "semi_dark_monitor_pftae2recipes", "ae2:semi_dark_monitor", new String[] { "WSG" },
                "W", "#c:fine_wires/tin", "S", "gtceu:computer_monitor_cover", "G", "ae2:quartz_vibrant_glass");

        PftCrafting.shaped(provider, "semi_dark_monitor_pftae2recipes_2", "ae2:semi_dark_monitor",
                new String[] { "mWG", "TCV", "xWG" }, "m", "#c:tools/mallet", "x", "#c:tools/wire_cutter", "W",
                "#c:fine_wires/copper", "G", "#c:plates/glass", "T", "#c:fine_wires/tin", "C", "#gtceu:circuits/lv",
                "V", "ae2:quartz_vibrant_glass");

        PftCrafting.shaped(provider, "wireless_booster_pftae2recipes", "ae2:wireless_booster",
                new String[] { "EC ", "PP " }, "E", "gtceu:lv_emitter", "C", "ae2:fluix_glass_cable", "P",
                "#c:plates/steel");
    }
}
