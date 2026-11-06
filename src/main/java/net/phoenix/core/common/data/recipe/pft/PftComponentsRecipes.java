package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftComponentsRecipes {

    private PftComponentsRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembler", "fluid_voiding_cover")
                .itemInputs("2x #c:screws/steel",
                        "gtceu:fluid_detector_cover",
                        "gtceu:bronze_normal_fluid_pipe")
                .itemOutputs("gtceu:fluid_voiding_cover")
                .duration(100)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "assembler", "item_voiding_cover")
                .itemInputs("2x #c:screws/steel", "gtceu:item_detector_cover", "hopper")
                .itemOutputs("gtceu:item_voiding_cover")
                .duration(100)
                .EUt(30)
                .save();
    }
}
