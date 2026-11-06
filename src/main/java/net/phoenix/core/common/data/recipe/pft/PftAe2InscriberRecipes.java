package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftAe2InscriberRecipes {

    private PftAe2InscriberRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "forming_press", "printed_silicon_circuit")
                .itemInputs("#c:plates/silicon")
                .notConsumable("ae2:silicon_press")
                .itemOutputs("ae2:printed_silicon")
                .duration(200)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "forming_press", "printed_logic_processor")
                .itemInputs("#c:plates/gold")
                .notConsumable("ae2:logic_processor_press")
                .itemOutputs("ae2:printed_logic_processor")
                .duration(200)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "forming_press", "printed_engineering_processor")
                .itemInputs("#c:plates/diamond")
                .notConsumable("ae2:engineering_processor_press")
                .itemOutputs("ae2:printed_engineering_processor")
                .duration(200)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "forming_press", "printed_calculation_circuit")
                .itemInputs("#c:plates/certus_quartz")
                .notConsumable("ae2:calculation_processor_press")
                .itemOutputs("ae2:printed_calculation_processor")
                .duration(200)
                .EUt(30)
                .save();
    }
}
