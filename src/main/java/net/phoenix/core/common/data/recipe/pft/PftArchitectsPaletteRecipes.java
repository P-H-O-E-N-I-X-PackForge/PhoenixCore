package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftArchitectsPaletteRecipes {

    private PftArchitectsPaletteRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "mixer", "abyssaline")
                .itemInputs("gtceu:obsidian_dust", "minecraft:prismarine_shard")
                .itemOutputs("2x architects_palette:abyssaline")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "abyssaline")
                .itemInputs("gtceu:obsidian_dust", "minecraft:prismarine_shard")
                .itemOutputs("12x architects_palette:abyssaline")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "assembler", "plating_block")
                .itemInputs("4x #c:plates/iron")
                .itemOutputs("4x architects_palette:plating_block")
                .circuit(10)
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "plating_block")
                .itemInputs("4x #c:plates/iron")
                .itemOutputs("16x architects_palette:plating_block")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "myonite")
                .itemInputs("minecraft:stone", "#c:mushrooms")
                .itemOutputs("4x architects_palette:myonite")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "myonite")
                .itemInputs("minecraft:stone", "#c:mushrooms")
                .itemOutputs("16x architects_palette:myonite")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "olivestone_bricks")
                .itemInputs("minecraft:stone", "#c:dyes/green")
                .itemOutputs("4x architects_palette:olivestone_bricks")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "olivestone_bricks")
                .itemInputs("minecraft:stone", "#c:dyes/green")
                .itemOutputs("16x architects_palette:olivestone_bricks")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "illuminated_olivestone")
                .itemInputs("#architects_palette:olivestone", "minecraft:glowstone")
                .itemOutputs("architects_palette:illuminated_olivestone")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "overgrown_algal_bricks")
                .itemInputs("architects_palette:algal_bricks", "minecraft:kelp")
                .itemOutputs("architects_palette:overgrown_algal_bricks")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "algal_lamp")
                .itemInputs("architects_palette:algal_brick", "minecraft:glowstone")
                .itemOutputs("architects_palette:algal_lamp")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "canner", "lit_osseous_skull")
                .itemInputs("architects_palette:osseous_skull", "minecraft:torch")
                .itemOutputs("architects_palette:lit_osseous_skull")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "canner", "lit_withered_osseous_skull")
                .itemInputs("architects_palette:withered_osseous_skull",
                        "minecraft:soul_torch")
                .itemOutputs("architects_palette:lit_withered_osseous_skull")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "gilded_sandstone")
                .itemInputs("minecraft:sandstone", "minecraft:gold_nugget")
                .itemOutputs("2x architects_palette:gilded_sandstone")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "gilded_sandstone")
                .itemInputs("minecraft:sandstone", "minecraft:gold_nugget")
                .itemOutputs("8x architects_palette:gilded_sandstone")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "weeping_blackstone")
                .itemInputs("minecraft:blackstone", "minecraft:weeping_vines")
                .itemOutputs("1x architects_palette:weeping_blackstone")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "weeping_blackstone")
                .itemInputs("minecraft:blackstone", "minecraft:weeping_vines")
                .itemOutputs("8x architects_palette:weeping_blackstone")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "twisting_blackstone")
                .itemInputs("minecraft:blackstone", "minecraft:twisting_vines")
                .itemOutputs("1x architects_palette:twisting_blackstone")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "twisting_blackstone")
                .itemInputs("minecraft:blackstone", "minecraft:twisting_vines")
                .itemOutputs("8x architects_palette:twisting_blackstone")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "choral_end_stone_bricks")
                .itemInputs("minecraft:end_stone_bricks", "minecraft:chorus_fruit")
                .itemOutputs("1x architects_palette:choral_end_stone_bricks")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "choral_end_stone_bricks")
                .itemInputs("minecraft:end_stone_bricks", "minecraft:chorus_fruit")
                .itemOutputs("4x architects_palette:choral_end_stone_bricks")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "sunstone")
                .itemInputs("minecraft:basalt", "architects_palette:sunmetal_blend")
                .itemOutputs("2x architects_palette:sunstone")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "sunstone")
                .itemInputs("minecraft:basalt", "architects_palette:sunmetal_blend")
                .itemOutputs("8x architects_palette:sunstone")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "molten_nether_bricks")
                .itemInputs("minecraft:nether_bricks", "minecraft:magma_block")
                .itemOutputs("2x architects_palette:molten_nether_bricks")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "molten_nether_bricks")
                .itemInputs("minecraft:nether_bricks", "minecraft:magma_block")
                .itemOutputs("8x architects_palette:molten_nether_bricks")
                .inputFluids("minecraft:lava 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "coarse_snow")
                .itemInputs("minecraft:snow_block", "minecraft:gravel")
                .itemOutputs("2x architects_palette:coarse_snow")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "coarse_snow")
                .itemInputs("minecraft:snow_block", "minecraft:gravel")
                .itemOutputs("8x architects_palette:coarse_snow")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "macerator", "unobtanium")
                .itemInputs("architects_palette:unobtanium_block")
                .itemOutputs("4x architects_palette:unobtanium")
                .chancedOutput("1x architects_palette:unobtanium", 100)
                .EUt(GTValues.VA[GTValues.HV])
                .duration(2000)
                .save();

        PftRecipe.gt(provider, "mixer", "ancient_plating")
                .itemInputs("minecraft:ancient_debris",
                        "4x architects_palette:nether_brass_ingot",
                        "4x architects_palette:nether_brass_nugget")
                .itemOutputs("32x architects_palette:ancient_plating")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "ancient_plating")
                .itemInputs("minecraft:ancient_debris",
                        "4x architects_palette:nether_brass_ingot")
                .itemOutputs("64x architects_palette:ancient_plating")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "tread_plate")
                .itemInputs("architects_palette:plating_block", "#c:dusts/iron")
                .itemOutputs("2x architects_palette:tread_plate")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "tread_plate")
                .itemInputs("architects_palette:plating_block", "#c:dusts/iron")
                .itemOutputs("8x architects_palette:tread_plate")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "hazard_block")
                .itemInputs("yellow_concrete", "black_concrete")
                .itemOutputs("2x architects_palette:hazard_block")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "hazard_block")
                .itemInputs("yellow_concrete", "black_concrete")
                .itemOutputs("8x architects_palette:hazard_block")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "compressor", "sheet_metal_block")
                .itemInputs("iron_block")
                .itemOutputs("32x architects_palette:sheet_metal_block")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "sheet_metal_block")
                .itemInputs("iron_block")
                .itemOutputs("64x architects_palette:sheet_metal_block")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "oracle_lamp")
                .itemInputs("architects_palette:oracle_jelly", "minecraft:end_rod")
                .itemOutputs("architects_palette:oracle_lamp")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "oracle_lamp")
                .itemInputs("architects_palette:oracle_jelly", "minecraft:end_rod")
                .itemOutputs("8x architects_palette:oracle_lamp")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "dark_oracle_bricks")
                .itemInputs("architects_palette:oracle_bricks", "#c:dyes/black")
                .itemOutputs("architects_palette:dark_oracle_bricks")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "dark_oracle_bricks")
                .itemInputs("architects_palette:oracle_bricks", "#c:dyes/black")
                .itemOutputs("8x architects_palette:dark_oracle_bricks")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "framed_oracle_block")
                .itemInputs("architects_palette:oracle_block",
                        "architects_palette:cerebral_plate")
                .itemOutputs("2x architects_palette:framed_oracle_block")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "framed_oracle_block")
                .itemInputs("architects_palette:oracle_block",
                        "architects_palette:cerebral_plate")
                .itemOutputs("8x architects_palette:framed_oracle_block")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "algal_blend")
                .itemInputs("clay_ball", "kelp")
                .itemOutputs("2x architects_palette:algal_blend")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "algal_blend")
                .itemInputs("clay_ball", "kelp")
                .itemOutputs("16x architects_palette:algal_blend")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "lathe", "entwine_rod")
                .itemInputs("minecraft:ender_pearl")
                .itemOutputs("2x architects_palette:entwine_rod")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "entwine_rod")
                .itemInputs("ender_pearl")
                .itemOutputs("8x architects_palette:entwine_rod")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "sunmetal_blend")
                .itemInputs("soul_sand", "#c:dusts/gold")
                .itemOutputs("4x architects_palette:sunmetal_blend")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "sunmetal_blend")
                .itemInputs("soul_sand", "#c:dusts/gold")
                .itemOutputs("16x architects_palette:sunmetal_blend")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "nether_brass_blend")
                .itemInputs("soul_sand", "#c:dusts/brass")
                .itemOutputs("4x architects_palette:nether_brass_blend")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "nether_brass_blend")
                .itemInputs("soul_sand", "#c:dusts/brass")
                .itemOutputs("16x architects_palette:nether_brass_blend")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "wardstone")
                .itemInputs("nether_wart", "#c:dusts/lapis")
                .itemOutputs("4x architects_palette:wardstone")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "wardstone")
                .itemInputs("nether_wart", "#c:dusts/lapis")
                .itemOutputs("16x architects_palette:wardstone")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "cerebral_plate")
                .itemInputs("tuff", "#c:dusts/nether_quartz")
                .itemOutputs("4x architects_palette:cerebral_plate")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "cerebral_plate")
                .itemInputs("tuff", "#c:dusts/nether_quartz")
                .itemOutputs("16x architects_palette:cerebral_plate")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "mixer", "oracle_jelly")
                .itemInputs("sugar", "#c:dusts/amethyst")
                .itemOutputs("4x architects_palette:oracle_jelly")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "oracle_jelly")
                .itemInputs("sugar", "#c:dusts/amethyst")
                .itemOutputs("16x architects_palette:oracle_jelly")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();
    }
}
