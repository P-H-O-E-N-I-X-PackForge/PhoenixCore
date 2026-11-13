package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.phoenix.conflux.ConfluxConfigs;
import net.phoenix.conflux.api.ConfluxApi;
import net.phoenix.core.common.data.recipeConditions.DisciplineCondition;

import dev.emi.emi.api.recipe.EmiRecipe;

public final class DisciplineRecipeVisibility {

    private DisciplineRecipeVisibility() {}

    public static boolean hide(EmiRecipe recipe) {
        var backing = recipe.getBackingRecipe();
        if (backing == null || !(backing.value() instanceof GTRecipe gt)) return false;
        ConfluxConfigs cfg = ConfluxConfigs.INSTANCE;
        if (cfg != null && cfg.showOtherDisciplineRecipes) return false;
        String mine = ConfluxApi.clientDiscipline();
        for (var condition : gt.conditions) {
            if (condition instanceof DisciplineCondition d && !d.getDiscipline().equals(mine)) return true;
        }
        return false;
    }
}
