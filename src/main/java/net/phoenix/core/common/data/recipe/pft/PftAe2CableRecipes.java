package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftAe2CableRecipes {

    private PftAe2CableRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembler", "gtceu:glass_fluix_cable")
                .itemInputs("2x ae2:quartz_fiber", "ae2:fluix_crystal")
                .itemOutputs("3x ae2:fluix_glass_cable")
                .duration(100)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:covered_fluix_worst")
                .itemInputs("ae2:fluix_glass_cable")
                .inputFluids("gtceu:rubber 144")
                .itemOutputs("ae2:fluix_covered_cable")
                .duration(150)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:covered_fluix_mid")
                .itemInputs("ae2:fluix_glass_cable")
                .inputFluids("gtceu:styrene_butadiene_rubber 36")
                .itemOutputs("ae2:fluix_covered_cable")
                .duration(150)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:covered_fluix_best")
                .itemInputs("ae2:fluix_glass_cable")
                .inputFluids("gtceu:silicone_rubber 72")
                .itemOutputs("ae2:fluix_covered_cable")
                .duration(150)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:smart_cable")
                .itemInputs("8x ae2:fluix_covered_cable", "#gtceu:circuits/lv")
                .itemOutputs("8x ae2:fluix_smart_cable")
                .duration(200)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "compressor", "gtceu:dense_cable")
                .itemInputs("4x ae2:fluix_covered_cable")
                .itemOutputs("ae2:fluix_covered_dense_cable")
                .duration(400)
                .EUt(2)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:smart_dense")
                .itemInputs("8x ae2:fluix_covered_dense_cable", "#gtceu:circuits/mv")
                .itemOutputs("8x ae2:fluix_smart_dense_cable")
                .duration(200)
                .EUt(8)
                .save();
    }
}
