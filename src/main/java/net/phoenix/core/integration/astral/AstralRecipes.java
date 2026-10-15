package net.phoenix.core.integration.astral;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.materials.AstralMaterials;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

public final class AstralRecipes {

    private AstralRecipes() {}

    public static void init(RecipeOutput provider) {
        ASSEMBLER_RECIPES.recipeBuilder("astral_confluence_hatch_iv")
                .inputItems(GTMachines.HULL[IV].asStack())
                .inputItems(dust, AstralMaterials.ASTRAL_FILAMENT, 16)
                .inputItems(CustomTags.IV_CIRCUITS, 2)
                .inputItems(ELECTRIC_PUMP_IV)
                .inputItems(plate, Iridium, 4)
                .outputItems(AstralMachines.ASTRAL_CONFLUENCE_HATCH[IV].asStack())
                .duration(400).EUt(VA[IV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("astral_spinning_wheel")
                .inputItems(GTMachines.HULL[LuV].asStack())
                .inputItems(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY, 8)
                .inputItems(dust, AstralMaterials.ASTRAL_FILAMENT, 32)
                .inputItems(CustomTags.LuV_CIRCUITS, 4)
                .inputItems(ELECTRIC_MOTOR_LuV, 4)
                .inputItems(gear, NaquadahAlloy, 4)
                .inputFluids(SolderingAlloy, 1152)
                .outputItems(AstralMachines.ASTRAL_SPINNING_WHEEL.asStack())
                .duration(800).EUt(VA[LuV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("astral_loom")
                .inputItems(GTMachines.HULL[ZPM].asStack())
                .inputItems(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY, 16)
                .inputItems(CustomTags.ZPM_CIRCUITS, 4)
                .inputItems(ELECTRIC_MOTOR_ZPM, 4)
                .inputItems(plateDouble, NaquadahAlloy, 8)
                .inputFluids(AstralMaterials.SKEIN.getFluid(1152))
                .inputFluids(SolderingAlloy, 2304)
                .outputItems(AstralMachines.ASTRAL_LOOM.asStack())
                .duration(1200).EUt(VA[ZPM])
                .save(provider);

        for (int tier : GTValues.tiersBetween(IV, ZPM)) {
            var in = AstralMachines.ASTRAL_THREAD_HATCH_INPUT[tier];
            var out = AstralMachines.ASTRAL_THREAD_HATCH_OUTPUT[tier];
            if (in == null || out == null) continue;
            boolean top = tier == ZPM;
            var circuits = switch (tier) {
                case IV -> CustomTags.IV_CIRCUITS;
                case LuV -> CustomTags.LuV_CIRCUITS;
                default -> CustomTags.ZPM_CIRCUITS;
            };
            for (boolean input : new boolean[] { true, false }) {
                var builder = ASSEMBLER_RECIPES
                        .recipeBuilder("astral_thread_hatch_" + (input ? "input_" : "output_") + VN[tier].toLowerCase())
                        .inputItems(GTMachines.HULL[tier].asStack())
                        .inputItems(dust, AstralMaterials.ASTRAL_FILAMENT, 4 * (tier - IV + 1))
                        .inputItems(circuits, 2)
                        .inputItems(input ? ELECTRIC_PUMP_IV : ELECTRIC_PUMP_LuV, 1)
                        .inputFluids(AstralMaterials.SKEIN.getFluid(144 * (tier - IV + 1)));

                if (top) builder.inputFluids(AstralMaterials.ENSORCELLED_WEAVE.getFluid(288));
                builder.outputItems((input ? in : out).asStack())
                        .duration(300 + 100 * (tier - IV)).EUt(VA[tier])
                        .save(provider);
            }
        }
    }
}
