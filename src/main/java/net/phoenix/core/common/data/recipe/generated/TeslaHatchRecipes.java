 package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.GTCraftingComponents;
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

 public class TeslaHatchRecipes {
    private static final int[] VA = GTValues.VA;
     public static void init(@NotNull Consumer<FinishedRecipe> provider) {
run(provider);
     }

     public static void run(@NotNull Consumer<FinishedRecipe> provider) {
         for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {

             if (tier >= GTValues.ZPM) {
                 processAssemblyLineHatchStation(provider, tier);
             }
             else if (tier == GTValues.LuV) {
                 processAssemblyLineHatchScanner(provider, tier);
             }
             else if (tier >= GTValues.IV) {
                 processAssemblerHatch(provider, tier);
             }else if (tier >= GTValues.LV && tier <= GTValues.EV) {
                 processAssemblerHatch(provider, tier);
                 processMultiAmpAssemblerHatch(provider, tier);
                 
             }
         }
     }

     private static void processMultiAmpAssemblerHatch(@NotNull Consumer<FinishedRecipe> provider, int tier) {
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_4a" + VN[tier].toLowerCase())
                 .inputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                 .inputItems(getQuadWireForTier(tier), 2)
                 .inputItems(getPlateForTier(tier), 2)
                 .outputItems(PhoenixTeslaMachines.TESLA_INPUT_4A[tier])
                 .EUt(GTValues.VA[tier])
                 .duration(100)
                 .save(provider);
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_output_hatch_4a" + VN[tier].toLowerCase())
                 .inputItems(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier])
                 .inputItems(getQuadWireForTier(tier), 2)
                 .inputItems(getPlateForTier(tier), 2)
                 .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_4A[tier])
                 .EUt(GTValues.VA[tier])
                 .duration(100)
                 .save(provider);
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_16a" + VN[tier].toLowerCase())
                 .inputItems(GTMachines.TRANSFORMER[tier])
                 .inputItems(PhoenixTeslaMachines.TESLA_INPUT_4A[tier])
                 .inputItems(getOctalWireForTier(tier), 2)
                 .inputItems(getPlateForTier(tier), 4)
                 .outputItems(PhoenixTeslaMachines.TESLA_INPUT_16A[tier])
                 .EUt(GTValues.VA[tier])
                 .duration(200)
                 .save(provider);
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_output_hatch_16a" + VN[tier].toLowerCase())
                 .inputItems(GTMachines.POWER_TRANSFORMER[tier])
                 .inputItems(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier])
                 .inputItems(getOctalWireForTier(tier), 2)
                 .inputItems(getPlateForTier(tier), 6)
                 .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_16A[tier])
                 .EUt(GTValues.VA[tier])
                 .duration(200)
                 .save(provider);
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_64a" + VN[tier].toLowerCase())
                 .inputItems(GTMachines.POWER_TRANSFORMER[tier])
                 .inputItems(PhoenixTeslaMachines.TESLA_INPUT_16A[tier])
                 .inputItems(getHexWireForTier(tier), 2)
                 .inputItems(getPlateForTier(tier), 6)
                 .outputItems(PhoenixTeslaMachines.TESLA_INPUT_64A[tier])
                 .EUt(GTValues.VA[tier])
                 .duration(100)
                 .save(provider);
         GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_output_hatch_64a" + VN[tier].toLowerCase())
                 .inputItems(PhoenixTeslaMachines.TESLA_OUTPUT_16A[tier])
                 .inputItems(getHexWireForTier(tier), 2)
                 .inputItems(getPlateForTier(tier), 2)
                 .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_64A[tier])
                 .EUt(GTValues.VA[tier])
                 .duration(100)
                 .save(provider);
     }

    private static void processAssemblerHatch(@NotNull Consumer<FinishedRecipe> provider, int tier) {
        var fluidStack = getFluidForTier(tier);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_2a" + GTValues.VN[tier].toLowerCase())
                .inputItems(GTMachines.ENERGY_INPUT_HATCH[tier])
                .inputItems(getQuadCableForTier(tier), 2)
                .inputItems(getDensePlateForTier(tier))
                .inputItems(GTCraftingComponents.EMITTER.get(tier), 1)
                .inputItems(getTeslaStabilizerForTier(tier), 1)
                .inputFluids(fluidStack)
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .duration(600)
                .EUt(VA[tier])
                .save(provider);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_output_hatch_2a" + GTValues.VN[tier].toLowerCase())
                .inputItems(GTMachines.ENERGY_OUTPUT_HATCH[tier])
                .inputItems(getQuadCableForTier(tier), 2)
                .inputItems(getDensePlateForTier(tier))
                .inputItems(GTCraftingComponents.SENSOR.get(tier), 1)
                .inputItems(getTeslaStabilizerForTier(tier), 1)
                .inputFluids(fluidStack)
                .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier])
                .duration(600)
                .EUt(VA[tier])
                .save(provider);
    }

    private static void processAssemblyLineHatchScanner(@NotNull Consumer<FinishedRecipe> provider, int tier) {
        var fluidStack = getFluidForTier(tier);

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_input_hatch_2a_scanner" + GTValues.VN[tier].toLowerCase())
                .inputItems(GTMachines.ENERGY_INPUT_HATCH[tier])
                .inputItems(getQuadCableForTier(tier), 4)
                .inputItems(getCrystalBoards(tier), getCrystalBoardAmount(tier))
                .inputItems(GTCraftingComponents.EMITTER.get(tier), 2)
                .inputItems(getTeslaStabilizerForTier(tier), 2)
                .inputFluids(fluidStack)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144 * tier/6))
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .duration(600)
                .EUt(VA[tier])
                .scannerResearch(b -> b
                        .researchStack(PhoenixTeslaMachines.TESLA_INPUT_2A[tier - 1].asStack())
                        .duration(2400)
                        .EUt(VA[IV]))
                .save(provider);

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_output_hatch_2a_scanner" + GTValues.VN[tier].toLowerCase())
                .inputItems(GTMachines.ENERGY_OUTPUT_HATCH[tier])
                .inputItems(getQuadCableForTier(tier), 4)
                .inputItems(getCrystalBoards(tier), getCrystalBoardAmount(tier))
                .inputItems(GTCraftingComponents.SENSOR.get(tier), 2)
                .inputItems(getTeslaStabilizerForTier(tier), 2)
                .inputFluids(fluidStack)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144 * tier/6))
                .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier])
                .duration(600)
                .EUt(VA[tier])
                .scannerResearch(b -> b
                        .researchStack(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier - 1].asStack())
                        .duration(2400)
                        .EUt(VA[IV]))
                .save(provider);


    }

     private static void processAssemblyLineHatchStation(@NotNull Consumer<FinishedRecipe> provider, int tier) {
         var fluidStack = getFluidForTier(tier);

         GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_input_hatch_2a_station" + GTValues.VN[tier].toLowerCase())
                 .inputItems(GTMachines.ENERGY_INPUT_HATCH[tier])
                 .inputItems(getQuadCableForTier(tier), 4)
                 .inputItems(getCrystalBoards(tier), getCrystalBoardAmount(tier))
                 .inputItems(GTCraftingComponents.EMITTER.get(tier), 2)
                 .inputItems(getTeslaStabilizerForTier(tier), 2)
                 .inputFluids(fluidStack)
                 .inputFluids(GTMaterials.SolderingAlloy.getFluid(144 * tier/6))
                 .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                 .duration(600)
                 .EUt(VA[tier])
                 .stationResearch(b -> b
                         .researchStack(PhoenixTeslaMachines.TESLA_INPUT_2A[tier - 1].asStack())
                         .CWUt(8))
                 .save(provider);

         GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_output_hatch_2a_station" + GTValues.VN[tier].toLowerCase())
                 .inputItems(GTMachines.ENERGY_OUTPUT_HATCH[tier])
                 .inputItems(getQuadCableForTier(tier), 4)
                 .inputItems(getCrystalBoards(tier), getCrystalBoardAmount(tier))
                 .inputItems(GTCraftingComponents.SENSOR.get(tier), 2)
                 .inputItems(getTeslaStabilizerForTier(tier), 2)
                 .inputFluids(fluidStack)
                 .inputFluids(GTMaterials.SolderingAlloy.getFluid(144 * tier/6))
                 .outputItems(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier])
                 .duration(600)
                 .EUt(VA[tier])
                 .stationResearch(b -> b
                         .researchStack(PhoenixTeslaMachines.TESLA_OUTPUT_2A[tier - 1].asStack())
                         .CWUt(8))
                 .save(provider);



     }
    private static ItemStack getCrystalBoards(int tier) {
        return switch (tier) {
            case GTValues.LuV, ZPM -> GTItems.ENGRAVED_LAPOTRON_CHIP.asStack();
            case GTValues.UV, UHV -> GTItems.ENGRAVED_CRYSTAL_CHIP.asStack();
            default -> GTItems.VOLTAGE_COIL_LV.asStack();
        };
    }

    private static int getCrystalBoardAmount(int amount) {
         return switch (amount){
             case GTValues.LuV -> 2;
             case GTValues.ZPM -> 4;
             case GTValues.UV -> 8;
             case GTValues.UHV -> 16;
             default -> 1;
         };
    }
    public static ItemStack getTeslaStabilizerForTier(int tier){
        return switch (tier) {
            case GTValues.LV -> PhoenixItems.LV_TESLA_STABILIZER.asStack();
            case GTValues.MV -> PhoenixItems.MV_TESLA_STABILIZER.asStack();
            case GTValues.HV -> PhoenixItems.HV_TESLA_STABILIZER.asStack();
            case GTValues.EV -> PhoenixItems.EV_TESLA_STABILIZER.asStack();
            case GTValues.IV -> PhoenixItems.IV_TESLA_STABILIZER.asStack();
            case GTValues.LuV -> PhoenixItems.LuV_TESLA_STABILIZER.asStack();
            case GTValues.ZPM -> PhoenixItems.ZPM_TESLA_STABILIZER.asStack();
            case GTValues.UV -> PhoenixItems.UV_TESLA_STABILIZER.asStack();
            case GTValues.UHV -> PhoenixItems.UHV_TESLA_STABILIZER.asStack();
            default -> PhoenixItems.MV_TESLA_STABILIZER.asStack();
        };
    }

    private static FluidStack getFluidForTier(int tier) {
        return switch (tier) {
            case GTValues.LV -> PhoenixProgressionMaterials.AURUM_STEEL.getFluid(256);
            case GTValues.MV -> PhoenixProgressionMaterials.ALUMINFROST.getFluid(288);
            case GTValues.HV -> PhoenixProgressionMaterials.FROST_REINFORCED_STAINED_STEEL.getFluid(288);
            case GTValues.EV -> PhoenixProgressionMaterials.SOURCE_IMBUED_TITANIUM.getFluid(288);
            case GTValues.IV -> PhoenixProgressionMaterials.VOID_TOUCHED_TUNGSTEN_STEEL.getFluid(288);
            case GTValues.LuV -> PhoenixProgressionMaterials.RESONANT_RHODIUM_ALLOY.getFluid(288);
            case GTValues.ZPM -> PhoenixProgressionMaterials.ADVANCED_QUIN_NAQUADIAN_ALLOY.getFluid(288);
            default -> GTMaterials.SolderingAlloy.getFluid(288);
        };
    }

    public static ItemStack getCoilForTier(int tier) {
        return switch (tier) {
            case GTValues.LV -> GTItems.VOLTAGE_COIL_LV.asStack();
            case GTValues.MV -> GTItems.VOLTAGE_COIL_MV.asStack();
            case GTValues.HV -> GTItems.VOLTAGE_COIL_HV.asStack();
            case GTValues.EV -> GTItems.VOLTAGE_COIL_EV.asStack();
            case GTValues.IV -> GTItems.VOLTAGE_COIL_IV.asStack();
            case GTValues.LuV -> GTItems.VOLTAGE_COIL_LuV.asStack();
            case GTValues.ZPM -> GTItems.VOLTAGE_COIL_ZPM.asStack();
            default -> GTItems.VOLTAGE_COIL_LV.asStack();
        };
    }
}