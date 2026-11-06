package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftEnderBagRecipes {

    private PftEnderBagRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembler", "ender_bag")
                .itemInputs("4x minecraft:leather", "2x minecraft:ender_eye")
                .inputFluids("gtceu:ender_air 2000")
                .itemOutputs("enderchests:ender_bag")
                .duration(100)
                .EUt(2048)
                .save();
    }
}
