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

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        // Use V for actual voltage (long), VA for Tier index (int)
        final long[] V = GTValues.V;

        for (int tier = LV; tier <= OpV; tier++) {
            // 1. Machine existence check
            if (PhoenixTeslaMachines.TESLA_INPUT_2A[tier] == null) continue;

            // 2. Hatch Array safety
            if (tier >= GTMachines.ENERGY_INPUT_HATCH.length || GTMachines.ENERGY_INPUT_HATCH[tier] == null) continue;

            // 3. Physical Item check (The fix for the IllegalArgumentException)
            ItemStack baseHatch = GTMachines.ENERGY_INPUT_HATCH[tier].asStack();
            if (baseHatch.isEmpty()) continue;

            if (tier >= ZPM) {
                processAssemblyLineHatchStation(provider, tier, V, baseHatch);
            } else if (tier == LuV) {
                processAssemblyLineHatchScanner(provider, tier, V, baseHatch);
            } else {
                processAssemblerHatch(provider, tier, V, baseHatch);
                if (tier <= EV) processMultiAmpAssemblerHatch(provider, tier, V);
            }
        }
    }

    private static void processAssemblerHatch(Consumer<FinishedRecipe> provider, int tier, long[] V,
                                              ItemStack baseHatch) {
        FluidStack fluid = getFluidForTier(tier);
        ItemStack stabilizer = getTeslaStabilizerForTier(tier);
        if (fluid.isEmpty() || stabilizer.isEmpty()) return;

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_2a_" + VN[tier].toLowerCase())
                .inputItems(baseHatch)
                .inputItems(getQuadCableForTier(tier), 2)
                .inputItems(getDensePlateForTier(tier))
                .inputItems(GTCraftingComponents.EMITTER.get(tier))
                .inputItems(stabilizer)
                .inputFluids(fluid)
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .duration(600).EUt(V[tier]).save(provider);
    }

    private static void processAssemblyLineHatchScanner(Consumer<FinishedRecipe> provider, int tier, long[] V,
                                                        ItemStack baseHatch) {
        ItemStack researchStack = PhoenixTeslaMachines.TESLA_INPUT_2A[tier - 1].asStack();
        if (researchStack.isEmpty()) return; // Research must have a valid item

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_input_hatch_2a_scanner_" + VN[tier].toLowerCase())
                .inputItems(baseHatch)
                .inputItems(getQuadCableForTier(tier), 4)
                .inputItems(getCrystalBoards(tier), getCrystalBoardAmount(tier))
                .inputItems(GTCraftingComponents.EMITTER.get(tier), 2)
                .inputItems(getTeslaStabilizerForTier(tier), 2)
                .inputFluids(getFluidForTier(tier))
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144))
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .duration(600).EUt(V[tier])
                .scannerResearch(b -> b.researchStack(researchStack).duration(2400).EUt(V[IV]))
                .save(provider);
    }

    private static void processAssemblyLineHatchStation(Consumer<FinishedRecipe> provider, int tier, long[] V,
                                                        ItemStack baseHatch) {
        ItemStack researchStack = PhoenixTeslaMachines.TESLA_INPUT_2A[tier - 1].asStack();
        if (researchStack.isEmpty()) return;

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("tesla_input_hatch_2a_station_" + VN[tier].toLowerCase())
                .inputItems(baseHatch)
                .inputItems(getQuadCableForTier(tier), 4)
                .inputItems(getCrystalBoards(tier), getCrystalBoardAmount(tier))
                .inputItems(GTCraftingComponents.EMITTER.get(tier), 2)
                .inputItems(getTeslaStabilizerForTier(tier), 2)
                .inputFluids(getFluidForTier(tier))
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144))
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .duration(600).EUt(V[tier])
                .stationResearch(b -> b.researchStack(researchStack).CWUt(8))
                .save(provider);
    }

    private static void processMultiAmpAssemblerHatch(Consumer<FinishedRecipe> provider, int tier, long[] V) {
        ItemStack wire4 = getQuadWireForTier(tier);
        if (wire4.isEmpty()) return;

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("tesla_input_hatch_4a_" + VN[tier].toLowerCase())
                .inputItems(PhoenixTeslaMachines.TESLA_INPUT_2A[tier])
                .inputItems(wire4, 2)
                .inputItems(getPlateForTier(tier), 2)
                .outputItems(PhoenixTeslaMachines.TESLA_INPUT_4A[tier])
                .EUt(V[tier]).duration(100).save(provider);
    }

    public static ItemStack getCoilForTier(int tier) {
        return switch (tier) {
            case LV -> GTItems.VOLTAGE_COIL_LV.asStack();
            case MV -> GTItems.VOLTAGE_COIL_MV.asStack();
            case HV -> GTItems.VOLTAGE_COIL_HV.asStack();
            case EV -> GTItems.VOLTAGE_COIL_EV.asStack();
            case IV -> GTItems.VOLTAGE_COIL_IV.asStack();
            case LuV -> GTItems.VOLTAGE_COIL_LuV.asStack();
            case ZPM -> GTItems.VOLTAGE_COIL_ZPM.asStack();
            case UV -> GTItems.VOLTAGE_COIL_UV.asStack();
            default -> GTItems.VOLTAGE_COIL_LV.asStack();
        };
    }

    private static ItemStack getCrystalBoards(int tier) {
        return switch (tier) {
            case LuV, ZPM -> GTItems.ENGRAVED_LAPOTRON_CHIP.asStack();
            case UV, UHV -> GTItems.ENGRAVED_CRYSTAL_CHIP.asStack();
            default -> getCoilForTier(tier);
        };
    }

    private static int getCrystalBoardAmount(int tier) {
        return (tier >= UV) ? 8 : 2;
    }

    public static ItemStack getTeslaStabilizerForTier(int tier) {
        return switch (tier) {
            case LV -> PhoenixItems.LV_TESLA_STABILIZER.asStack();
            case MV -> PhoenixItems.MV_TESLA_STABILIZER.asStack();
            case HV -> PhoenixItems.HV_TESLA_STABILIZER.asStack();
            case EV -> PhoenixItems.EV_TESLA_STABILIZER.asStack();
            case IV -> PhoenixItems.IV_TESLA_STABILIZER.asStack();
            case LuV -> PhoenixItems.LuV_TESLA_STABILIZER.asStack();
            case ZPM -> PhoenixItems.ZPM_TESLA_STABILIZER.asStack();
            case UV -> PhoenixItems.UV_TESLA_STABILIZER.asStack();
            case UHV -> PhoenixItems.UHV_TESLA_STABILIZER.asStack();
            default -> PhoenixItems.LV_TESLA_STABILIZER.asStack();
        };
    }

    private static FluidStack getFluidForTier(int tier) {
        return switch (tier) {
            case LV -> PhoenixProgressionMaterials.AURUM_STEEL.getFluid(256);
            case MV -> PhoenixProgressionMaterials.ALUMINFROST.getFluid(288);
            case HV -> PhoenixProgressionMaterials.FROST_REINFORCED_STAINED_STEEL.getFluid(288);
            case EV -> PhoenixProgressionMaterials.SOURCE_IMBUED_TITANIUM.getFluid(288);
            case IV -> PhoenixProgressionMaterials.VOID_TOUCHED_TUNGSTEN_STEEL.getFluid(288);
            case LuV -> PhoenixProgressionMaterials.RESONANT_RHODIUM_ALLOY.getFluid(288);
            case ZPM -> PhoenixProgressionMaterials.ADVANCED_QUIN_NAQUADIAN_ALLOY.getFluid(288);
            default -> GTMaterials.SolderingAlloy.getFluid(288);
        };
    }
}
