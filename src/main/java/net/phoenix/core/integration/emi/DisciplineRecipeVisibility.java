package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

public final class DisciplineRecipeVisibility {

    private DisciplineRecipeVisibility() {}

    /** For EMI pages that are not GT viewer pages but are backed by a GT recipe, such as the Aether Crucible's. */
    public static boolean hide(dev.emi.emi.api.recipe.EmiRecipe recipe) {
        var backing = recipe.getBackingRecipe();
        return backing != null && backing.value() instanceof GTRecipe gt && hidden(gt);
    }

    public static boolean hidden(GTRecipe gt) {
        for (var condition : gt.conditions) {
            if (condition.hiddenInViewer(gt)) return true;
        }
        return false;
    }
}
