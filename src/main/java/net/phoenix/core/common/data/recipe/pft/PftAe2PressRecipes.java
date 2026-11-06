package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftAe2PressRecipes {

    private PftAe2PressRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "laser_engraver", "gtceu:inscriber_silicon_press")
                .itemInputs("#c:plates/steel")
                .notConsumable("gtceu:sapphire_lens")
                .itemOutputs("ae2:silicon_press")
                .duration(500)
                .EUt(120)
                .save();

        PftRecipe.gt(provider, "laser_engraver", "gtceu:inscriber_logic_press")
                .itemInputs("#c:plates/steel")
                .notConsumable("gtceu:emerald_lens")
                .itemOutputs("ae2:logic_processor_press")
                .duration(500)
                .EUt(120)
                .save();

        PftRecipe.gt(provider, "laser_engraver", "gtceu:inscriber_engineering_press")
                .itemInputs("#c:plates/steel")
                .notConsumable("gtceu:ruby_lens")
                .itemOutputs("ae2:engineering_processor_press")
                .duration(500)
                .EUt(120)
                .save();

        PftRecipe.gt(provider, "laser_engraver", "gtceu:inscriber_calculation_press")
                .itemInputs("#c:plates/steel")
                .notConsumable("gtceu:glass_lens")
                .itemOutputs("ae2:calculation_processor_press")
                .duration(500)
                .EUt(120)
                .save();
    }
}
