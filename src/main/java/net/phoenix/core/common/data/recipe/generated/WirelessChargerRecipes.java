 package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.GTCraftingComponents;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.data.materials.PhoenixProgressionMaterials;
import net.phoenix.core.common.machine.PhoenixTeslaMachines;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static net.phoenix.core.common.data.recipe.generated.CustomComponetRecipes.*;
import static net.phoenix.core.common.data.recipe.generated.TeslaHatchRecipes.*;

 public class WirelessChargerRecipes {
    private static final int[] VA = GTValues.VA;
     public static void init(@NotNull Consumer<FinishedRecipe> provider) {
run(provider);
     }

     public static void run(@NotNull Consumer<FinishedRecipe> provider) {
         for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {

              if (tier >= GTValues.LV && tier <= EV) {
                 processWirelessCharger(provider, tier);
             } else if (tier >= IV) {
                 processWirelessChargerWithQuantumChest(provider, tier);
             }
         }
     }

     private static void processWirelessCharger(@NotNull Consumer<FinishedRecipe> provider, int tier) {
         VanillaRecipeHelper.addShapedRecipe(provider, true,
                 "wireless_turbo_charger_" + GTValues.VN[tier].toLowerCase(),
                 PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier].asStack(),
                 "WCW", "WTW", "RXR",
                 'C', (GTMachines.SUPER_CHEST[tier].asStack()),
                 'T', (GTMachines.CHARGER_4[tier].asStack()),
                 'R', (getOctalWireForTier(tier)),
                 'X', (getTeslaStabilizerForTier(tier)),
                 'W', (getQuadCableForTier(tier)));


     }
     private static void processWirelessChargerWithQuantumChest(@NotNull Consumer<FinishedRecipe> provider, int tier) {
         VanillaRecipeHelper.addShapedRecipe(provider, true,
                 "wireless_turbo_charger_" + GTValues.VN[tier].toLowerCase(),
                 PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier].asStack(),
                 "WCW", "WTW", "RXR",
                 'C', (GTMachines.QUANTUM_CHEST[tier].asStack()),
                 'T', (GTMachines.CHARGER_4[tier].asStack()),
                 'R', (getOctalWireForTier(tier)),
                 'X', (getTeslaStabilizerForTier(tier)),
                 'W', (getQuadCableForTier(tier)));


     }

}