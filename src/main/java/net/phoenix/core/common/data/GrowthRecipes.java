package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.growth.GrowthItems;
import net.phoenix.core.integration.growth.GrowthMultiblockMachine;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Water;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

public final class GrowthRecipes {

    private GrowthRecipes() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        seeds(provider);
        crystalGarden(provider);
        verdantBloom(provider);
        bloomvineCore(provider);
    }

    private static void seeds(Consumer<FinishedRecipe> provider) {
        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("growth_seed"))
                .inputItems(Items.BONE_MEAL, 8)
                .inputItems(Items.WHEAT_SEEDS, 8)
                .inputItems(Items.GLOW_BERRIES, 2)
                .inputFluids(Water.getFluid(1000))
                .outputItems(GrowthItems.GROWTH_SEED.asStack(4))
                .EUt(30).duration(200)
                .save(provider);

        ASSEMBLER_RECIPES.recipeBuilder(PhoenixCore.id("crystal_seed"))
                .inputItems(Items.AMETHYST_SHARD, 4)
                .inputItems(Items.QUARTZ, 4)
                .inputItems(Items.BONE_MEAL, 4)
                .outputItems(GrowthItems.CRYSTAL_SEED.asStack(2))
                .EUt(60).duration(240)
                .save(provider);
    }

    private static void at(GTRecipeType type, String id, int stage, Consumer<com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder> recipe, Consumer<FinishedRecipe> provider) {
        var builder = type.recipeBuilder(PhoenixCore.id(id)).addData(GrowthMultiblockMachine.MIN_STAGE_KEY, stage);
        recipe.accept(builder);
        builder.save(provider);
    }

    private static void crystalGarden(Consumer<FinishedRecipe> provider) {
        GTRecipeType type = PhoenixRecipeTypes.GROWTH_RECIPES;
        Item seed = GrowthItems.CRYSTAL_SEED.get();

        at(type, "crystal_amethyst", 0, r -> r.inputItems(seed, 1).inputItems(Items.QUARTZ, 8)
                .inputFluids(Water.getFluid(1000)).outputItems(Items.AMETHYST_SHARD, 6).EUt(60).duration(300),
                provider);
        at(type, "crystal_emerald", 2, r -> r.inputItems(seed, 2).inputItems(Items.LAPIS_BLOCK, 2)
                .inputItems(Items.BONE_MEAL, 8).outputItems(Items.EMERALD, 3).EUt(240).duration(600), provider);
        at(type, "crystal_diamond", 3, r -> r.inputItems(seed, 2).inputItems(Items.COAL_BLOCK, 8)
                .outputItems(Items.DIAMOND, 3).EUt(480).duration(800), provider);
        at(type, "crystal_echo", 4, r -> r.inputItems(seed, 3).inputItems(Items.AMETHYST_BLOCK, 4)
                .inputItems(Items.SCULK, 8).outputItems(Items.ECHO_SHARD, 2).EUt(1920).duration(1200), provider);
    }

    private static void verdantBloom(Consumer<FinishedRecipe> provider) {
        GTRecipeType type = PhoenixRecipeTypes.BOTANICAL_GROWTH_RECIPES;
        Item seed = GrowthItems.GROWTH_SEED.get();

        at(type, "botanical_logs", 0, r -> r.inputItems(seed, 1).inputItems(Items.BONE_MEAL, 4)
                .inputFluids(Water.getFluid(1000)).outputItems(Items.OAK_LOG, 16).EUt(30).duration(200), provider);
        at(type, "botanical_flowers", 1, r -> r.inputItems(seed, 1).inputItems(Items.BONE_MEAL, 8)
                .outputItems(Items.DANDELION, 6).outputItems(Items.POPPY, 6).outputItems(Items.BLUE_ORCHID, 6)
                .EUt(60).duration(300), provider);
        at(type, "botanical_moss", 2, r -> r.inputItems(seed, 2).inputItems(Items.BONE_MEAL, 16)
                .inputFluids(Water.getFluid(2000)).outputItems(Items.MOSS_BLOCK, 16)
                .outputItems(Items.GLOW_BERRIES, 8).outputItems(Items.FLOWERING_AZALEA, 4).EUt(120).duration(500),
                provider);
        at(type, "botanical_cherry", 3, r -> r.inputItems(seed, 2).inputItems(Items.BONE_MEAL, 16)
                .outputItems(Items.CHERRY_LOG, 16).outputItems(Items.PINK_PETALS, 16).EUt(240).duration(700),
                provider);
        at(type, "botanical_ancient", 4, r -> r.inputItems(seed, 4).inputItems(Items.BONE_BLOCK, 4)
                .outputItems(Items.PITCHER_PLANT, 2).outputItems(Items.TORCHFLOWER, 2)
                .outputItems(Items.SPORE_BLOSSOM, 2).EUt(960).duration(1000), provider);
    }

    private static void bloomvineCore(Consumer<FinishedRecipe> provider) {
        GTRecipeType type = PhoenixRecipeTypes.VINE_GROWTH_RECIPES;
        Item seed = GrowthItems.GROWTH_SEED.get();

        at(type, "vine_vines", 0, r -> r.inputItems(seed, 1).inputItems(Items.BONE_MEAL, 2)
                .outputItems(Items.VINE, 16).EUt(30).duration(160), provider);
        at(type, "vine_slime", 1, r -> r.inputItems(seed, 1).inputItems(Items.BONE_MEAL, 2)
                .inputFluids(Water.getFluid(1000)).outputItems(Items.SLIME_BALL, 12).EUt(60).duration(240), provider);
        at(type, "vine_honey", 2, r -> r.inputItems(seed, 1).inputItems(Items.SUGAR, 8)
                .outputItems(Items.HONEYCOMB, 6).EUt(120).duration(320), provider);
        at(type, "vine_glow", 3, r -> r.inputItems(seed, 2).inputItems(Items.GLOWSTONE_DUST, 8)
                .outputItems(Items.GLOW_BERRIES, 12).outputItems(Items.SHROOMLIGHT, 4).EUt(240).duration(400),
                provider);
        at(type, "vine_weeping", 4, r -> r.inputItems(seed, 2).inputItems(Items.NETHER_WART, 8)
                .outputItems(Items.WEEPING_VINES, 16).outputItems(Items.TWISTING_VINES, 16).EUt(480).duration(500),
                provider);
    }
}
