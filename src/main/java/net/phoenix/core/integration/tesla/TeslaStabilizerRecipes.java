package net.phoenix.core.integration.tesla;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.GTCraftingComponents;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.RecipeOutput;

import org.jetbrains.annotations.NotNull;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static net.phoenix.core.common.data.recipe.generated.CustomComponetRecipes.*;
import static net.phoenix.core.integration.tesla.TeslaHatchRecipes.*;

public class TeslaStabilizerRecipes {

    private static final int[] VA = GTValues.VA;

    public static void init(@NotNull RecipeOutput provider) {
        run(provider);
    }

    private static void run(@NotNull RecipeOutput provider) {
        for (int tier = LV; tier <= GTValues.OpV; tier++) {
            if (tier == GTValues.LuV) {
                processScannerTeslaStabilizerRecipe(provider, tier);
            } else if (tier >= GTValues.ZPM) {
                processStationTeslaStabilizerRecipe(provider, tier);
            } else {
                processTeslaStabilizerRecipe(provider, tier);
            }
        }
    }

    private static void processTeslaStabilizerRecipe(@NotNull RecipeOutput provider, int tier) {
        var rod = TagPrefix.rod;

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                "tesla_stabilizer_" + GTValues.VN[tier].toLowerCase(),
                getTeslaStabilizerForTier(tier),
                " R ", "RCR", "XRX",
                'C', (getCoilForTier(tier)),
                'R', (getRodForTier(tier)),
                'X', getCircuitForTier(tier));
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_stabilizer_assembler_" + VN[tier].toLowerCase())
                .inputItems(getCoilForTier(tier))
                .inputItems(getRodForTier(tier), 4)
                .inputItems(getCircuitForTier(tier), 2)
                .outputItems(getTeslaStabilizerForTier(tier))
                .EUt(GTValues.VA[tier - 1])
                .duration(100)
                .save(provider);
    }

    private static void processScannerTeslaStabilizerRecipe(@NotNull RecipeOutput provider, int tier) {
        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_stabilizer_scanner_" + VN[tier].toLowerCase())
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144 * tier))
                .inputItems(getFrameForTier(tier))
                .inputItems(GTCraftingComponents.MOTOR.get(tier))
                .inputItems(getLongRodForTier(tier), 4)
                .inputItems(getPlateForTier(tier))
                .inputItems(getCircuitForTier(tier), 2)
                .inputItems(getCoilForTier(tier), 2)
                .inputItems(getFoilForTier(tier), 64)
                .inputItems(getQuadCableForTier(tier), 8)
                .outputItems(getTeslaStabilizerForTier(tier))
                .duration(800)
                .EUt(GTValues.VA[tier - 1])
                .scannerResearch(b -> b
                        .researchStack(getTeslaStabilizerForTier(tier - 1))
                        .duration(2400)
                        .EUt(VA[IV]))
                .save(provider);
    }

    private static void processStationTeslaStabilizerRecipe(@NotNull RecipeOutput provider, int tier) {
        if (getFrameForTier(tier).isEmpty() || getLongRodForTier(tier).isEmpty() || getPlateForTier(tier).isEmpty() ||
                getFoilForTier(tier).isEmpty() || getQuadCableForTier(tier).isEmpty()) {
            net.phoenix_tesla_network.tesla.PhoenixTeslaNetwork.LOGGER.warn(
                    "Skipping tesla stabilizer station recipe for tier {}: an ingredient is empty",
                    tier);
            return;
        }
        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_stabilizer_station_" + VN[tier].toLowerCase())
                .inputItems(getFrameForTier(tier))
                .inputItems(GTCraftingComponents.MOTOR.get(tier))
                .inputItems(getLongRodForTier(tier), 4)
                .inputItems(getPlateForTier(tier), 4)
                .inputItems(getCircuitForTier(tier), 2)
                .inputItems(getCoilForTier(tier), 2)
                .inputItems(getFoilForTier(tier), 64)
                .inputItems(getQuadCableForTier(tier), 8)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144 * tier))
                .outputItems(getTeslaStabilizerForTier(tier))
                .duration(800)
                .EUt(GTValues.VA[tier - 1])
                .stationResearch(b -> b
                        .researchStack(getTeslaStabilizerForTier(tier - 1))
                        .CWUt(8))
                .save(provider);
    }
}
