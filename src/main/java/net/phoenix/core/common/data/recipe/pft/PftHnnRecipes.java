package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftHnnRecipes {

    private PftHnnRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "electrolyzer", "gt_quartz")
                .itemInputs("4x minecraft:glass")
                .itemOutputs("minecraft:quartz")
                .duration(100)
                .EUt(20)
                .save();
    }
}
