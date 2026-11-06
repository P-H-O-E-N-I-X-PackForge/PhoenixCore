package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.plate;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;
import static com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.SOURCE_GEM;

/**
 * Recipes for the Source pipes and relays. Each pipe tier is made from the one below it, so the way up is clear.
 */
public final class SourceLogisticsRecipes {

    private SourceLogisticsRecipes() {}

    public static void init(RecipeOutput provider) {
        ASSEMBLER_RECIPES.recipeBuilder("source_pipe")
                .inputItems(SOURCE_GEM, 2)
                .inputItems(plate, Bronze, 2)
                .circuitMeta(1)
                .outputItems(new ItemStack(SourceLogistics.PIPES.get(SourcePipeTier.BASIC).get(), 4))
                .duration(100)
                .EUt(VA[LV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("gilded_source_pipe")
                .inputItems(SourceLogistics.PIPES.get(SourcePipeTier.BASIC).get(), 4)
                .inputItems(plate, Gold, 2)
                .inputItems(SOURCE_GEM, 2)
                .circuitMeta(2)
                .outputItems(new ItemStack(SourceLogistics.PIPES.get(SourcePipeTier.GILDED).get(), 4))
                .duration(120)
                .EUt(VA[MV])
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("arcane_source_pipe")
                .inputItems(SourceLogistics.PIPES.get(SourcePipeTier.GILDED).get(), 4)
                .inputItems(plate, Platinum, 2)
                .inputItems(SOURCE_GEM, 4)
                .circuitMeta(3)
                .outputItems(new ItemStack(SourceLogistics.PIPES.get(SourcePipeTier.ARCANE).get(), 4))
                .duration(150)
                .EUt(VA[HV])
                .save(provider);

        relay(provider, HV, 8, CustomTags.HV_CIRCUITS);
        relay(provider, EV, 16, CustomTags.EV_CIRCUITS);
        relay(provider, IV, 32, CustomTags.IV_CIRCUITS);
    }

    private static void relay(RecipeOutput provider, int tier, int gems,
                              net.minecraft.tags.TagKey<net.minecraft.world.item.Item> circuits) {
        var machine = SourceLogistics.RELAY[tier];
        if (machine == null) return;
        ASSEMBLER_RECIPES.recipeBuilder("source_relay_" + VN[tier].toLowerCase())
                .inputItems(GTMachines.HULL[tier].asStack())
                .inputItems(SOURCE_GEM, gems)
                .inputItems(circuits, 2)
                .inputItems(plate, Gold, 4)
                .outputItems(machine.asStack())
                .duration(300)
                .EUt(VA[tier])
                .save(provider);
    }
}
