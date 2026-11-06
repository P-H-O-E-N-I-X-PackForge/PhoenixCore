package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.PftRecipe;

public final class PftGtRecipes {

    private PftGtRecipes() {}

    public static void init(RecipeOutput provider) {
        PftEarthSieveRecipes.init(provider);
        PftHvEvRecipes.init(provider);
        PftIvRecipes.init(provider);
        PftMinerRecipes.init(provider);
        PftSteamAgeRecipes.init(provider);
        PftSterileHatchRecipes.init(provider);
        PftGreenhouseRecipes.init(provider);
        PftComponentsRecipes.init(provider);
        PftMachineRecipes.init(provider);
        PftRecipeLoops.init(provider);

        PftAe2CableRecipes.init(provider);
        PftAe2CardRecipes.init(provider);
        PftAe2InscriberRecipes.init(provider);
        PftAe2LoopRecipes.init(provider);
        PftExpandedBufferRecipes.init(provider);
        PftLapidaryRecipes.init(provider);
        PftDroneRecipes.init(provider);
        PftAe2PatternRecipes.init(provider);
        PftAe2PortableCellRecipes.init(provider);
        PftAe2PressRecipes.init(provider);
        PftAe2Recipes.init(provider);
        PftAe2TerminalRecipes.init(provider);
        PftAe2WtLibRecipes.init(provider);
        PftArchitectsPaletteRecipes.init(provider);
        PftBuildingBlockRecipes.init(provider);
        PftBuildingGadgetRecipes.init(provider);
        PftCcTweakedRecipes.init(provider);
        PftElevatorRecipes.init(provider);
        PftEnderBagRecipes.init(provider);
        PftExpandedAeRecipes.init(provider);
        PftExtendedAeRecipes.init(provider);
        PftFeatherRecipes.init(provider);
        PftFunctionalStorageRecipes.init(provider);
        PftHangGliderRecipes.init(provider);
        PftHnnRecipes.init(provider);
        PftIntegratedDynamicsRecipes.init(provider);
        PftMiscRecipes.init(provider);
        PftSmeltingRecipes.init(provider);
        PftSophisticatedStorageRecipes.init(provider);
        PftSteelAndBronzeRecipes.init(provider);
        PftTorchmasterRecipes.init(provider);
        PftVanillaRecipes.init(provider);
        PftXtonesRecipes.init(provider);
        PftAe2LoopRecipes.init(provider);

        var problems = PftRecipe.problems();
        if (!problems.isEmpty()) {
            PhoenixCore.LOGGER.warn("[PFT] {} script recipe(s) were skipped because something they use does not exist:",
                    problems.size());
            problems.forEach(p -> PhoenixCore.LOGGER.warn("[PFT]   {}", p));
        }
    }
}
