package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftEarthSieveRecipes {

    private PftEarthSieveRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "earth_sieve", "from_stone")
                .itemInputs("4x minecraft:stone")
                .chancedOutput("kubejs:redstone_vein_traces", 4500)
                .chancedOutput("kubejs:coal_vein_traces", 5000)
                .chancedOutput("kubejs:iron_vein_traces", 3500)
                .chancedOutput("kubejs:copper_vein_traces", 3000)
                .chancedOutput("kubejs:apatite_vein_traces", 4000)
                .chancedOutput("kubejs:mineral_sand_vein_traces", 4500)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_deepslate")
                .itemInputs("4x minecraft:deepslate")
                .chancedOutput("kubejs:sapphire_vein_traces", 3500)
                .chancedOutput("kubejs:olivine_vein_traces", 4000)
                .chancedOutput("kubejs:mica_vein_traces", 4500)
                .chancedOutput("kubejs:manganese_vein_traces", 3000)
                .chancedOutput("kubejs:lapis_vein_traces", 4500)
                .chancedOutput("kubejs:diamond_vein_traces", 1200)
                .chancedOutput("kubejs:nickel_vein_traces", 3000)
                .chancedOutput("kubejs:magnetite_vein_traces", 4000)
                .chancedOutput("kubejs:garnet_vein_traces", 4300)
                .chancedOutput("kubejs:cassiterite_vein_traces", 2500)
                .chancedOutput("kubejs:garnet_tin_vein_traces", 2000)
                .chancedOutput("kubejs:galena_vein_traces", 2700)
                .chancedOutput("kubejs:copper_tin_vein_traces", 3300)
                .chancedOutput("kubejs:topaz_vein_traces", 3500)
                .chancedOutput("kubejs:tetrahedrite_vein_traces", 3300)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_dirt")
                .itemInputs("4x minecraft:dirt")
                .chancedOutput("kubejs:oilsands_vein_traces", 4000)
                .chancedOutput("kubejs:salts_vein_traces", 3500)
                .chancedOutput("kubejs:coal_vein_traces", 1500)
                .chancedOutput("kubejs:apatite_vein_traces", 1200)
                .chancedOutput("kubejs:mineral_sand_vein_traces", 2500)
                .chancedOutput("kubejs:iron_vein_traces", 800)
                .chancedOutput("kubejs:copper_vein_traces", 400)
                .chancedOutput("kubejs:lapis_vein_traces", 100)
                .chancedOutput("kubejs:redstone_vein_traces", 300)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_gravel")
                .itemInputs("4x minecraft:gravel")
                .chancedOutput("kubejs:copper_vein_traces", 3500)
                .chancedOutput("kubejs:sapphire_vein_traces", 800)
                .chancedOutput("kubejs:redstone_vein_traces", 2500)
                .chancedOutput("kubejs:olivine_vein_traces", 1200)
                .chancedOutput("kubejs:mica_vein_traces", 1400)
                .chancedOutput("kubejs:manganese_vein_traces", 600)
                .chancedOutput("kubejs:nickel_vein_traces", 5000)
                .chancedOutput("kubejs:iron_vein_traces", 3000)
                .chancedOutput("kubejs:magnetite_vein_traces", 1000)
                .chancedOutput("kubejs:garnet_vein_traces", 1000)
                .chancedOutput("kubejs:cassiterite_vein_traces", 3000)
                .chancedOutput("kubejs:galena_vein_traces", 5000)
                .chancedOutput("kubejs:copper_tin_vein_traces", 5000)
                .chancedOutput("kubejs:coal_vein_traces", 7000)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_sand")
                .itemInputs("4x minecraft:sand")
                .chancedOutput("kubejs:oilsands_vein_traces", 3500)
                .chancedOutput("kubejs:salts_vein_traces", 6000)
                .chancedOutput("kubejs:mineral_sand_vein_traces", 7000)
                .chancedOutput("kubejs:copper_vein_traces", 3000)
                .chancedOutput("kubejs:apatite_vein_traces", 1500)
                .chancedOutput("kubejs:topaz_vein_traces", 8000)
                .chancedOutput("kubejs:tetrahedrite_vein_traces", 8000)
                .chancedOutput("kubejs:mineral_sand_vein_traces", 6000)
                .chancedOutput("kubejs:iron_vein_traces", 5000)
                .chancedOutput("kubejs:lapis_vein_traces", 1500)
                .chancedOutput("kubejs:diamond_vein_traces", 100)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_copper_traces")
                .itemInputs("kubejs:copper_vein_traces")
                .itemOutputs("gtceu:copper_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_sapphire_traces")
                .itemInputs("kubejs:sapphire_vein_traces")
                .itemOutputs("gtceu:sapphire_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_redstone_traces")
                .itemInputs("kubejs:redstone_vein_traces")
                .itemOutputs("minecraft:redstone")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_olivine_traces")
                .itemInputs("kubejs:olivine_vein_traces")
                .itemOutputs("gtceu:olivine_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_mica_traces")
                .itemInputs("kubejs:mica_vein_traces")
                .itemOutputs("gtceu:mica_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_manganese_traces")
                .itemInputs("kubejs:manganese_vein_traces")
                .itemOutputs("gtceu:manganese_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_lapis_traces")
                .itemInputs("kubejs:lapis_vein_traces")
                .itemOutputs("minecraft:lapis_lazuli")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_diamond_traces")
                .itemInputs("kubejs:diamond_vein_traces")
                .itemOutputs("minecraft:diamond")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_oilsands_traces")
                .itemInputs("kubejs:oilsands_vein_traces")
                .itemOutputs("gtceu:oilsands_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_salts_traces")
                .itemInputs("kubejs:salts_vein_traces")
                .itemOutputs("gtceu:salt_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_nickel_traces")
                .itemInputs("kubejs:nickel_vein_traces")
                .itemOutputs("gtceu:nickel_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_mineral_sand_traces")
                .itemInputs("kubejs:mineral_sand_vein_traces")
                .itemOutputs("gtceu:basaltic_mineral_sand_dust", "gtceu:granitic_mineral_sand_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_iron_traces")
                .itemInputs("kubejs:iron_vein_traces")
                .itemOutputs("gtceu:iron_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_magnetite_traces")
                .itemInputs("kubejs:magnetite_vein_traces")
                .itemOutputs("gtceu:magnetite_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_garnet_traces")
                .itemInputs("kubejs:garnet_vein_traces")
                .itemOutputs("gtceu:garnet_sand_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_cassiterite_traces")
                .itemInputs("kubejs:cassiterite_vein_traces")
                .itemOutputs("gtceu:cassiterite_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_garnet_tin_traces")
                .itemInputs("kubejs:garnet_tin_vein_traces")
                .itemOutputs("gtceu:garnet_sand_dust", "gtceu:tin_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_galena_traces")
                .itemInputs("kubejs:galena_vein_traces")
                .itemOutputs("gtceu:galena_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_copper_tin_traces")
                .itemInputs("kubejs:copper_tin_vein_traces")
                .itemOutputs("gtceu:copper_dust", "gtceu:tin_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_coal_traces")
                .itemInputs("kubejs:coal_vein_traces")
                .itemOutputs("minecraft:coal")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_apatite_traces")
                .itemInputs("kubejs:apatite_vein_traces")
                .itemOutputs("gtceu:apatite_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_topaz_traces")
                .itemInputs("kubejs:topaz_vein_traces")
                .itemOutputs("gtceu:topaz_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_tetrahedrite_traces")
                .itemInputs("kubejs:tetrahedrite_vein_traces")
                .itemOutputs("gtceu:tetrahedrite_dust")
                .duration(300)
                .EUt(8)
                .save();
    }
}
