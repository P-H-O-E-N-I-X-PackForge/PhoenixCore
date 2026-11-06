package net.phoenix.core.common.data;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.phoenix.core.PhoenixCore;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.CENTRIFUGE_RECIPES;

public final class CountDisplayExampleRecipes {

    private CountDisplayExampleRecipes() {}

    public static void init(RecipeOutput provider) {
        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("example_item_counts_in"))
                .inputItems(Items.COBBLESTONE, 64)
                .inputItems(Items.STONE, 999)
                .inputItems(Items.DIRT, 1_000)
                .inputItems(Items.GRAVEL, 1_500)
                .inputItems(Items.SAND, 9_999)
                .inputItems(Items.CLAY_BALL, 15_375)
                .inputItems(Items.COAL, 123_456)
                .inputItems(Items.IRON_NUGGET, 1_500_000)
                .outputItems(Items.NETHERRACK, 1)
                .EUt(1).duration(20)
                .save(provider);

        CENTRIFUGE_RECIPES.recipeBuilder(PhoenixCore.id("example_item_counts_out"))
                .inputItems(Items.COBBLESTONE, 2_048)
                .outputItems(Items.ANDESITE, 12)
                .outputItems(Items.DIORITE, 1_200)
                .outputItems(Items.GRANITE, 12_500)
                .outputItems(Items.TUFF, 99_999)
                .outputItems(Items.DEEPSLATE, 1_234_567)
                .outputItems(Items.CALCITE, 25_000_000)
                .EUt(1).duration(20)
                .save(provider);
    }
}
