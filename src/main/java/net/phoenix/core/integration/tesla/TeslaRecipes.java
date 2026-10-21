package net.phoenix.core.integration.tesla;

import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.fml.ModList;

/** The recipes for the standalone Tesla Network mod's machines and items, kept here because they use core materials. */
public final class TeslaRecipes {

    public static final String TESLA_MOD_ID = "phoenix_tesla_network";

    private TeslaRecipes() {}

    public static void init(RecipeOutput provider) {
        if (!ModList.get().isLoaded(TESLA_MOD_ID)) return;
        TeslaHatchRecipes.init(provider);
        TeslaMultiAmpHatchRecipes.init(provider);
        TeslaStabilizerRecipes.init(provider);
        WirelessChargerRecipes.init(provider);
    }
}
