package net.phoenix.core.integration.continuum;

import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.FinishedRecipe;
import net.phoenix.core.integration.continuum.machine.ContinuumMachines;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTBlocks.CASING_STEEL_SOLID;
import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

/**
 * Assembler recipes for Continuum's items and the Launch Complex. The ladder follows the bodies' pad tiers: probes and
 * kits are MV, the rocket and hull parts HV, the modules and the complex EV, so the first launches are within reach of
 * a mid-game base and the better upgrades are an investment.
 */
public final class ContinuumRecipes {

    private ContinuumRecipes() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        ASSEMBLER_RECIPES.recipeBuilder("continuum_probe")
                .inputItems(plate, Steel, 2)
                .inputItems(rod, Steel, 2)
                .inputItems(ELECTRIC_MOTOR_MV)
                .inputItems(CustomTags.MV_CIRCUITS, 1)
                .inputFluids(SolderingAlloy, 72)
                .outputItems(ContinuumRegistry.PROBE.asStack(2))
                .duration(200).EUt(VA[MV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("continuum_repair_kit")
                .inputItems(plate, Steel, 4)
                .inputItems(screw, Steel, 4)
                .inputItems(plate, Aluminium, 2)
                .inputItems(CustomTags.MV_CIRCUITS, 1)
                .inputFluids(SolderingAlloy, 72)
                .outputItems(ContinuumRegistry.REPAIR_KIT.asStack(2))
                .duration(160).EUt(VA[MV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("continuum_rocket")
                .inputItems(plate, Titanium, 16)
                .inputItems(frameGt, Titanium, 1)
                .inputItems(pipeLargeFluid, Titanium, 2)
                .inputItems(ELECTRIC_MOTOR_HV, 2)
                .inputItems(ELECTRIC_PUMP_HV, 1)
                .inputItems(CustomTags.HV_CIRCUITS, 4)
                .inputFluids(SolderingAlloy, 288)
                .outputItems(ContinuumRegistry.ROCKET)
                .duration(600).EUt(VA[HV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("continuum_module_hull")
                .inputItems(plate, Titanium, 6)
                .inputItems(plate, StainlessSteel, 4)
                .inputItems(screw, Titanium, 4)
                .inputFluids(SolderingAlloy, 144)
                .outputItems(ContinuumRegistry.MODULE_HULL)
                .duration(300).EUt(VA[HV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("continuum_module_overdrive")
                .inputItems(ELECTRIC_MOTOR_EV, 2)
                .inputItems(plate, Titanium, 2)
                .inputItems(CustomTags.EV_CIRCUITS, 2)
                .inputFluids(SolderingAlloy, 144)
                .outputItems(ContinuumRegistry.MODULE_OVERDRIVE)
                .duration(400).EUt(VA[EV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("continuum_module_interlock")
                .inputItems(SENSOR_EV, 1)
                .inputItems(EMITTER_EV, 1)
                .inputItems(plate, StainlessSteel, 2)
                .inputItems(CustomTags.EV_CIRCUITS, 4)
                .inputFluids(SolderingAlloy, 144)
                .outputItems(ContinuumRegistry.MODULE_INTERLOCK)
                .duration(400).EUt(VA[EV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("continuum_launch_complex")
                .inputItems(GTMachines.HULL[EV].asStack())
                .inputItems(CASING_STEEL_SOLID, 8)
                .inputItems(frameGt, Steel, 4)
                .inputItems(ELECTRIC_MOTOR_EV, 4)
                .inputItems(ROBOT_ARM_EV, 2)
                .inputItems(ELECTRIC_PUMP_HV, 2)
                .inputItems(CustomTags.EV_CIRCUITS, 4)
                .inputFluids(SolderingAlloy, 576)
                .outputItems(ContinuumMachines.LAUNCH_COMPLEX)
                .duration(600).EUt(VA[EV])
                .save(provider);
    }
}
