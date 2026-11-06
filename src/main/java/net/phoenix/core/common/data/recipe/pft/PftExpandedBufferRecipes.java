package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftExpandedBufferRecipes {

    private PftExpandedBufferRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembly_line", "expanded_me_pattern_buffer")
                .itemInputs("gtceu:zpm_dual_input_hatch", "2x gtceu:zpm_emitter", "4x #gtceu:circuits/zpm",
                        "4x ae2:pattern_provider", "4x ae2:interface", "8x ae2:speed_card", "4x ae2:capacity_card",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire")
                .inputFluids("gtceu:soldering_alloy 1152", "gtceu:lubricant 4000")
                .itemOutputs("phoenixcore:expanded_me_pattern_buffer")
                .stationResearch("gtceu:me_pattern_buffer", 30, 16, 32000, null)
                .duration(4000)
                .EUt(GTValues.VA[GTValues.ZPM])
                .save();

        PftRecipe.gt(provider, "assembly_line", "expanded_me_pattern_buffer_proxy")
                .itemInputs("gtceu:zpm_machine_hull", "4x gtceu:zpm_sensor", "2x #gtceu:circuits/zpm",
                        "2x ae2:quantum_link", "4x ae2:quantum_ring",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire",
                        "48x gtceu:fine_uranium_rhodium_dinaquadide_wire")
                .inputFluids("gtceu:soldering_alloy 1152", "gtceu:lubricant 2000")
                .itemOutputs("phoenixcore:expanded_me_pattern_buffer_proxy")
                .stationResearch("gtceu:me_pattern_buffer_proxy", 30, 32, 32000, null)
                .duration(4000)
                .EUt(GTValues.VA[GTValues.ZPM])
                .save();
    }
}
