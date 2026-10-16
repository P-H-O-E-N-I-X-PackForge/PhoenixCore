package net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.PhoenixTeslaMachines;

import org.jetbrains.annotations.NotNull;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static net.phoenix.core.common.data.recipe.generated.CustomComponetRecipes.*;
import static net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe.TeslaHatchRecipes.*;

public class WirelessChargerRecipes {

    private static final int[] VA = GTValues.VA;

    public static void init(@NotNull RecipeOutput provider) {
        run(provider);
    }

    public static void run(@NotNull RecipeOutput provider) {
        for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {
            if (PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier] == null) continue;

            if (tier <= EV) {
                processWirelessCharger(provider, tier);
            } else {
                processWirelessChargerWithQuantumChest(provider, tier);
            }
        }
    }

    private static void processWirelessCharger(@NotNull RecipeOutput provider, int tier) {
        if (GTMachines.SUPER_CHEST[tier] == null || GTMachines.CHARGER_4[tier] == null) return;

        ItemStack stabilizer = getTeslaStabilizerForTier(tier);
        Object octalWire = getOctalWireForTier(tier);
        Object quadCable = getQuadCableForTier(tier);

        String recipeId = "wireless_turbo_charger_" + GTValues.VN[tier].toLowerCase();
        if (isMissing(recipeId, "stabilizer", stabilizer) ||
                isMissing(recipeId, "octal wire", octalWire) ||
                isMissing(recipeId, "quad cable", quadCable)) {
            return;
        }

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                recipeId,
                PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier].asStack(),
                "WCW", "WTW", "RXR",
                'C', GTMachines.SUPER_CHEST[tier].asStack(),
                'T', GTMachines.CHARGER_4[tier].asStack(),
                'R', octalWire,
                'X', stabilizer,
                'W', quadCable);
    }

    private static void processWirelessChargerWithQuantumChest(@NotNull RecipeOutput provider, int tier) {
        if (tier >= GTMachines.QUANTUM_CHEST.length || GTMachines.QUANTUM_CHEST[tier] == null ||
                tier >= GTMachines.CHARGER_4.length || GTMachines.CHARGER_4[tier] == null)
            return;

        ItemStack stabilizer = getTeslaStabilizerForTier(tier);
        Object octalWire = getOctalWireForTier(tier);
        Object quadCable = getQuadCableForTier(tier);

        String recipeId = "wireless_turbo_charger_" + GTValues.VN[tier].toLowerCase();
        if (isMissing(recipeId, "stabilizer", stabilizer) ||
                isMissing(recipeId, "octal wire", octalWire) ||
                isMissing(recipeId, "quad cable", quadCable)) {
            return;
        }

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                recipeId,
                PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier].asStack(),
                "WCW", "WTW", "RXR",
                'C', GTMachines.QUANTUM_CHEST[tier].asStack(),
                'T', GTMachines.CHARGER_4[tier].asStack(),
                'R', octalWire,
                'X', stabilizer,
                'W', quadCable);
    }

    private static boolean isMissing(String recipeId, String what, Object ingredient) {
        boolean missing = ingredient == null || (ingredient instanceof ItemStack s && s.isEmpty());
        if (missing) {
            PhoenixAPI.LOGGER.warn("Skipping recipe '{}': {} is empty/missing for this tier", recipeId, what);
        }
        return missing;
    }
}
