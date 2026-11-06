package net.phoenix.core.common.machine.multiblock.stellar;

import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.block.PhoenixBlocks;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTBlocks.FUSION_GLASS;
import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

public final class StellarForgeRecipes {

    private StellarForgeRecipes() {}

    public static void init(RecipeOutput provider) {
        ASSEMBLER_RECIPES.recipeBuilder("stellar_forge")
                .inputItems(GTMachines.HULL[UV].asStack())
                .inputItems(PhoenixBlocks.PHOENIX_HEART_CASING.get(), 16)
                .inputItems(FUSION_GLASS, 24)
                .inputItems(plate, Neutronium, 8)
                .inputItems(FIELD_GENERATOR_UV, 2)
                .inputItems(EMITTER_UV, 4)
                .inputItems(SENSOR_UV, 2)
                .inputItems(CustomTags.UV_CIRCUITS, 4)
                .inputFluids(SolderingAlloy, 1152)
                .outputItems(StellarForgeMachines.STELLAR_FORGE)
                .duration(1200).EUt(VA[UV])
                .save(provider);
    }
}
