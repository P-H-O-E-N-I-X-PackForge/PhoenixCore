package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftExpandedAeRecipes {

    private PftExpandedAeRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembler", "expanded_pattern_provider")
                .itemInputs("extendedae:ex_pattern_provider",
                        "16x ae2:engineering_processor",
                        "2x gtceu:soc",
                        "#gtceu:circuits/iv")
                .itemOutputs("expandedae:exp_pattern_provider")
                .duration(200)
                .EUt(50)
                .save();

        PftRecipe.gt(provider, "assembler", "expanded_pattern_provider_upgrade")
                .itemInputs("16x ae2:engineering_processor",
                        "2x gtceu:soc",
                        "#gtceu:circuits/iv")
                .itemOutputs("expandedae:exp_pattern_provider_upgrade")
                .duration(180)
                .EUt(50)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:exp_io_port")
                .itemInputs("2x extendedae:ex_io_port", "8x ae2:capacity_card", "4x ae2:speed_card")
                .itemOutputs("expandedae:exp_io_port")
                .EUt(120)
                .duration(200)
                .save();
    }
}
