 package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.common.machine.PhoenixTeslaMachines;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

 public class TeslaHatchRecipes {
    private static final int[] VA = GTValues.VA;
     public static void init(@NotNull Consumer<FinishedRecipe> provider) {
run(provider);
     }

    public static void run(@NotNull Consumer<FinishedRecipe> provider) {
        for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {
            if (tier >= GTValues.LuV) {
                processAssemblyLineHatch(provider, tier);
            } else {
                processAssemblerHatch(provider, tier);
            }

        }
    }

    private static void processAssemblerHatch(@NotNull Consumer<FinishedRecipe> provider, int tier) {
        var fluidStack = getFluidForTier(tier);
        var voltageCoil = getCoilForTier(tier);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_hatch_2a_" + GTValues.VN[tier].toLowerCase())
                .inputItems(voltageCoil)
                .inputItems(GTMachines.HULL[tier])
                .inputFluids(fluidStack)
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .duration(200)
                .EUt(VA[tier])
                .save(provider);
    }

    private static void processAssemblyLineHatch(@NotNull Consumer<FinishedRecipe> provider, int tier) {
        var fluidStack = getFluidForTier(tier);

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_hatch_2a_" + GTValues.VN[tier].toLowerCase())
                .inputItems(GTMachines.HULL[tier])
                .inputItems(getCoilForTier(tier), 4)
                .inputItems(GTItems.ADVANCED_CIRCUIT_BOARD, 2)
                .inputFluids(fluidStack)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144))
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .duration(600)
                .EUt(VA[tier])
                .scannerResearch(GTMachines.HULL[tier - 1].asStack())
                .save(provider);
    }

    private static FluidStack getFluidForTier(int tier) {
        return switch (tier) {
            case GTValues.LV -> GTMaterials.Water.getFluid(1000);
            case GTValues.MV -> GTMaterials.Lubricant.getFluid(50);
            case GTValues.HV -> GTMaterials.Nitrobenzene.getFluid(250);
            case GTValues.LuV -> GTMaterials.Mutagen.getFluid(100);
            default -> GTMaterials.SolderingAlloy.getFluid(144);
        };
    }

    private static ItemStack getCoilForTier(int tier) {
        return switch (tier) {
            case GTValues.LV -> GTItems.VOLTAGE_COIL_LV.asStack();
            case GTValues.MV -> GTItems.VOLTAGE_COIL_MV.asStack();
            case GTValues.HV -> GTItems.VOLTAGE_COIL_HV.asStack();
            case GTValues.EV -> GTItems.VOLTAGE_COIL_EV.asStack();
            default -> GTItems.VOLTAGE_COIL_LV.asStack();
        };
    }
}