package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftDroneRecipes {

    private PftDroneRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembler", "drone_range_module_1")
                .itemInputs("2x gtceu:mv_sensor", "gtceu:mv_emitter", "4x #gtceu:circuits/mv",
                        "4x gtceu:aluminium_plate", "2x gtceu:red_alloy_single_cable")
                .inputFluids("gtceu:soldering_alloy 144")
                .itemOutputs("phoenixcore:drone_range_module_1")
                .duration(400)
                .EUt(GTValues.VA[GTValues.MV])
                .save();
        PftRecipe.gt(provider, "assembler", "drone_range_module_2")
                .itemInputs("phoenixcore:drone_range_module_1", "2x gtceu:hv_sensor", "gtceu:hv_emitter",
                        "4x #gtceu:circuits/hv", "4x gtceu:stainless_steel_plate")
                .inputFluids("gtceu:soldering_alloy 288")
                .itemOutputs("phoenixcore:drone_range_module_2")
                .duration(600)
                .EUt(GTValues.VA[GTValues.HV])
                .save();
        PftRecipe.gt(provider, "assembler", "drone_range_module_3")
                .itemInputs("phoenixcore:drone_range_module_2", "2x gtceu:ev_sensor", "gtceu:ev_emitter",
                        "4x #gtceu:circuits/ev", "4x gtceu:titanium_plate")
                .inputFluids("gtceu:soldering_alloy 576")
                .itemOutputs("phoenixcore:drone_range_module_3")
                .duration(800)
                .EUt(GTValues.VA[GTValues.EV])
                .save();
        PftRecipe.gt(provider, "assembler", "drone_range_module_4")
                .itemInputs("phoenixcore:drone_range_module_3", "2x gtceu:iv_sensor", "gtceu:iv_emitter",
                        "gtceu:iv_field_generator", "4x #gtceu:circuits/iv", "4x gtceu:tungsten_steel_plate")
                .inputFluids("gtceu:soldering_alloy 1152")
                .itemOutputs("phoenixcore:drone_range_module_4")
                .duration(1000)
                .EUt(GTValues.VA[GTValues.IV])
                .save();
    }
}
