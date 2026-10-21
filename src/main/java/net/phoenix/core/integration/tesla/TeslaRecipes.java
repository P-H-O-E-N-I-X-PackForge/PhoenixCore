package net.phoenix.core.integration.tesla;

import net.minecraft.data.recipes.RecipeOutput;

public final class TeslaRecipes {

    private TeslaRecipes() {}

    public static void init(RecipeOutput provider) {
        TeslaHatchRecipes.init(provider);
        TeslaMultiAmpHatchRecipes.init(provider);
        TeslaStabilizerRecipes.init(provider);
        WirelessChargerRecipes.init(provider);
    }
}
