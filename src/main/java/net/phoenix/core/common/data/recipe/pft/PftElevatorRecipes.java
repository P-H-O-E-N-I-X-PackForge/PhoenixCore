package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftElevatorRecipes {

    private PftElevatorRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "chemical_bath", "elevator_washing")
                .itemInputs("#elevatorid:elevators")
                .inputFluids("gtceu:chlorine 50")
                .itemOutputs("elevatorid:elevator_gray")
                .duration(400)
                .EUt(2)
                .save();

        PftCrafting.shaped(provider, "elevator_gray_pftelevatorrecipes", "elevatorid:elevator_gray",
                new String[] { "PPP", "GFG", "PPP" }, "P", "#c:plates/steel", "G", "#c:small_gears/steel", "F",
                "#c:frames/steel");
    }
}
