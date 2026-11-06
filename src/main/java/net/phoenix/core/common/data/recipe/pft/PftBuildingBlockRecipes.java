package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftBuildingBlockRecipes {

    private PftBuildingBlockRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "mixer", "skystone_dust")
                .itemInputs("gtceu:stone_dust", "#c:dusts/certus_quartz")
                .itemOutputs("2x ae2:sky_dust")
                .circuit(1)
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "skystone_dust")
                .itemInputs("gtceu:stone_dust", "#c:dusts/certus_quartz")
                .itemOutputs("8x ae2:sky_dust")
                .inputFluids("gtceu:construction_foam 100")
                .circuit(1)
                .EUt(2)
                .duration(20)
                .save();
    }
}
