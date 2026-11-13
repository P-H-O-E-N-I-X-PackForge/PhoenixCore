package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftMinerRecipes {

    private PftMinerRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "large_chemical_reactor", "blazing_drilling_fluid_recipe_base")
                .inputFluids("gtceu:drilling_fluid 500",
                        "minecraft:lava 150",
                        "phoenixcore:cryo_ember_fluid 10000")
                .itemInputs("10x phoenixcore:dormant_ember_dust",
                        "2x gtceu:sulfur_dust")
                .outputFluids("phoenixcore:blazing_drilling_fluid 750")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "blazing_drilling_fluid_recipe_ignisium")
                .inputFluids("gtceu:drilling_fluid 500")
                .itemInputs("2x phoenixcore:ignisium_dust")
                .outputFluids("phoenixcore:blazing_drilling_fluid 900")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "calcite_generator")
                .notConsumable("minecraft:calcite")
                .inputFluids("minecraft:water 1000", "minecraft:lava 20")
                .itemOutputs("10x minecraft:calcite")
                .duration(360)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();
    }
}
