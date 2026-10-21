package net.phoenix.core.integration.tesla;

import net.minecraft.data.recipes.RecipeOutput;

/** The recipes for the standalone Tesla Network mod's machines and items, kept here because they use core materials. */
public final class TeslaRecipes {

    private TeslaRecipes() {}

    public static void init(RecipeOutput provider) {
        TeslaHatchRecipes.init(provider);
        TeslaMultiAmpHatchRecipes.init(provider);
        TeslaStabilizerRecipes.init(provider);
        WirelessChargerRecipes.init(provider);
    }
}
