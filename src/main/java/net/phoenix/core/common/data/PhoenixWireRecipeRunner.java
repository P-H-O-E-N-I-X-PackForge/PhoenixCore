package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.recipe.generated.PhoenixWireRecipeHandler;

import org.jetbrains.annotations.NotNull;

public class PhoenixWireRecipeRunner {

    public static void init(@NotNull RecipeOutput provider) {
        for (Material material : GTRegistries.MATERIALS) {
            PhoenixWireRecipeHandler.run(provider, material);
        }
    }
}
