package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftAe2LoopRecipes {

    private PftAe2LoopRecipes() {}

    private static void processor(RecipeOutput provider, String id, String[] items, String output, long eut) {
        PftRecipe.gt(provider, "circuit_assembler", id)
                .itemInputs((Object[]) items)
                .inputFluids("gtceu:soldering_alloy 74")
                .itemOutputs(output)
                .duration(600)
                .EUt(eut)
                .save();
    }

    private static String[] processorInputs(String printed, String circuit, String part, String wire) {
        return new String[] { "1x ae2:printed_" + printed + "_processor", "1x ae2:printed_silicon",
                "1x #gtceu:circuits/" + circuit,
                part, "2x gtceu:fine_" + wire + "_wire" };
    }

    public static void init(RecipeOutput provider) {
        processor(provider, "processor_logicmv", processorInputs("logic", "mv", "#gtceu:resistors", "aluminium"),
                "6x ae2:logic_processor", 64);
        processor(provider, "processor_logichv", processorInputs("logic", "hv", "4x #gtceu:resistors", "electrum"),
                "8x ae2:logic_processor", 256);
        processor(provider, "processor_logicev",
                processorInputs("logic", "ev", "1x gtceu:smd_resistor", "niobium_titanium"),
                "10x ae2:logic_processor", 512);
        processor(provider, "processor_logiciv",
                processorInputs("logic", "iv", "1x gtceu:advanced_smd_resistor", "hssg"),
                "12x ae2:logic_processor", 1028);
        processor(provider, "processor_calculation2",
                processorInputs("calculation", "mv", "gtceu:capacitor", "aluminium"),
                "6x ae2:calculation_processor", 64);
        processor(provider, "processor_calculation3",
                processorInputs("calculation", "hv", "4x gtceu:smd_capacitor", "electrum"),
                "8x ae2:calculation_processor", 256);
        processor(provider, "processor_calculation4",
                processorInputs("calculation", "ev", "1x gtceu:smd_capacitor", "niobium_titanium"),
                "10x ae2:calculation_processor", 512);
        processor(provider, "processor_calculation5",
                processorInputs("calculation", "iv", "1x gtceu:advanced_smd_capacitor", "hssg"),
                "12x ae2:calculation_processor", 1048);
        processor(provider, "processor_engineering2",
                processorInputs("engineering", "mv", "#gtceu:inductors", "aluminium"),
                "6x ae2:engineering_processor", 64);
        processor(provider, "processor_engineering3",
                processorInputs("engineering", "hv", "4x gtceu:smd_inductor", "electrum"),
                "8x ae2:engineering_processor", 256);
        processor(provider, "processor_engineering4",
                processorInputs("engineering", "ev", "1x gtceu:smd_inductor", "niobium_titanium"),
                "10x ae2:engineering_processor", 512);
        processor(provider, "processor_engineering5",
                processorInputs("engineering", "iv", "1x gtceu:advanced_smd_inductor", "hssg"),
                "12x ae2:engineering_processor", 1048);

        PftRecipe.gt(provider, "forming_press", "void_portal")
                .itemInputs("4x minecraft:diamond_block")
                .itemOutputs("1x javd:portal_block")
                .duration(600)
                .EUt(64)
                .save();

        String[][] providers = { { "lv", "steel", "1" }, { "mv", "aluminium", "2" }, { "hv", "stainless_steel", "3" },
                { "ev", "titanium", "4" } };
        for (String[] tier : providers) {
            core(provider, "interface_" + tier[0], tier, "#c:glass_blocks", "ae2:interface");
            core(provider, "pattern_provider_" + tier[0], tier, "minecraft:crafting_table", "ae2:pattern_provider");
        }

        String[][] assemblers = { { "hv", "stainless_steel", "1" }, { "ev", "titanium", "4" },
                { "iv", "tungsten_steel", "16" } };
        for (String[] tier : assemblers) {
            core(provider, "molecular_assembler_" + tier[0], tier, "ae2:quartz_glass", "ae2:molecular_assembler");
        }

        String[] sizes = { "1", "4", "16", "64", "256" };
        for (int i = 0; i < sizes.length; i++) {
            PftRecipe.gt(provider, "canner", sizes[i] + "k_crafting_storage")
                    .itemInputs("ae2:crafting_unit", "ae2:cell_component_" + sizes[i] + "k")
                    .itemOutputs("ae2:" + sizes[i] + "k_crafting_storage")
                    .duration(100)
                    .EUt((long) Math.pow(4, i))
                    .save();
        }
    }

    private static void core(RecipeOutput provider, String id, String[] tier, String glass, String output) {
        PftCrafting.shaped(provider, id + "_pft", tier[2] + "x " + output, new String[] { "PGP", "ACF", "PGP" },
                "P", "#c:plates/" + tier[1], "G", glass, "A", "ae2:annihilation_core", "F", "ae2:formation_core", "C",
                "gtceu:" + tier[0] + "_conveyor_module");
    }
}
