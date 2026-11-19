package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.common.data.GTMedicalConditions;
import com.gregtechceu.gtceu.common.recipe.condition.AdjacentBlockCondition;
import com.gregtechceu.gtceu.common.recipe.condition.AdjacentFluidCondition;
import com.gregtechceu.gtceu.common.recipe.condition.BiomeCondition;
import com.gregtechceu.gtceu.common.recipe.condition.BiomeTagCondition;
import com.gregtechceu.gtceu.common.recipe.condition.CleanroomCondition;
import com.gregtechceu.gtceu.common.recipe.condition.DaytimeCondition;
import com.gregtechceu.gtceu.common.recipe.condition.DimensionCondition;
import com.gregtechceu.gtceu.common.recipe.condition.EnvironmentalHazardCondition;
import com.gregtechceu.gtceu.common.recipe.condition.PositionYCondition;
import com.gregtechceu.gtceu.common.recipe.condition.RainingCondition;
import com.gregtechceu.gtceu.common.recipe.condition.ThunderCondition;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.FMLEnvironment;
import net.phoenix.core.common.data.PftRecipe;
import net.phoenix.core.common.data.recipeConditions.DisciplineCondition;

/**
 * A recipe with a lot of conditions, to stress the recipe viewer pages: a tall panel, many text rows, an EMI pill and a
 * wide set of condition kinds at once. Only exists in a development environment.
 */
public final class PftViewerStressRecipes {

    private PftViewerStressRecipes() {}

    public static void init(RecipeOutput provider) {
        if (FMLEnvironment.production) return;

        PftRecipe.gt(provider, "cutter", "dev/viewer_stress_test")
                .itemInputs("1x minecraft:oak_log")
                .inputFluids("gtceu:lubricant 25")
                .itemOutputs("6x minecraft:stick", "2x gtceu:wood_dust")
                .duration(100)
                .EUt(30)
                .condition(new CleanroomCondition(CleanroomType.CLEANROOM))
                .condition(new DimensionCondition(Level.OVERWORLD))
                .condition(new DimensionCondition(true, Level.NETHER))
                .condition(new BiomeCondition(Biomes.PLAINS))
                .condition(new BiomeCondition(true, Biomes.DESERT))
                .condition(new BiomeTagCondition(BiomeTags.IS_FOREST))
                .condition(new RainingCondition(0.5f))
                .condition(new ThunderCondition(true, 0.2f))
                .condition(new PositionYCondition(0, 128))
                .condition(new EnvironmentalHazardCondition(true, GTMedicalConditions.CARBON_MONOXIDE_POISONING.get()))
                .condition(AdjacentBlockCondition.fromBlocks(Blocks.WATER, Blocks.MOSS_BLOCK))
                .condition(AdjacentFluidCondition.fromFluids(Fluids.WATER))
                .condition(new DaytimeCondition())
                .condition(new DisciplineCondition("void"))
                .save();
    }
}
