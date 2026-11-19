package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftEarthSieveRecipes {

    private PftEarthSieveRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "earth_sieve", "from_stone")
                .itemInputs("4x minecraft:stone")
                .chancedOutput("phoenixcore:redstone_vein_traces", 4500)
                .chancedOutput("phoenixcore:coal_vein_traces", 5000)
                .chancedOutput("phoenixcore:iron_vein_traces", 3500)
                .chancedOutput("phoenixcore:copper_vein_traces", 3000)
                .chancedOutput("phoenixcore:apatite_vein_traces", 4000)
                .chancedOutput("phoenixcore:mineral_sand_vein_traces", 4500)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_deepslate")
                .itemInputs("4x minecraft:deepslate")
                .chancedOutput("phoenixcore:sapphire_vein_traces", 3500)
                .chancedOutput("phoenixcore:olivine_vein_traces", 4000)
                .chancedOutput("phoenixcore:mica_vein_traces", 4500)
                .chancedOutput("phoenixcore:manganese_vein_traces", 3000)
                .chancedOutput("phoenixcore:lapis_vein_traces", 4500)
                .chancedOutput("phoenixcore:diamond_vein_traces", 1200)
                .chancedOutput("phoenixcore:nickel_vein_traces", 3000)
                .chancedOutput("phoenixcore:magnetite_vein_traces", 4000)
                .chancedOutput("phoenixcore:garnet_vein_traces", 4300)
                .chancedOutput("phoenixcore:cassiterite_vein_traces", 2500)
                .chancedOutput("phoenixcore:garnet_tin_vein_traces", 2000)
                .chancedOutput("phoenixcore:galena_vein_traces", 2700)
                .chancedOutput("phoenixcore:copper_tin_vein_traces", 3300)
                .chancedOutput("phoenixcore:topaz_vein_traces", 3500)
                .chancedOutput("phoenixcore:tetrahedrite_vein_traces", 3300)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_dirt")
                .itemInputs("4x minecraft:dirt")
                .chancedOutput("phoenixcore:oilsands_vein_traces", 4000)
                .chancedOutput("phoenixcore:salts_vein_traces", 3500)
                .chancedOutput("phoenixcore:coal_vein_traces", 1500)
                .chancedOutput("phoenixcore:apatite_vein_traces", 1200)
                .chancedOutput("phoenixcore:mineral_sand_vein_traces", 2500)
                .chancedOutput("phoenixcore:iron_vein_traces", 800)
                .chancedOutput("phoenixcore:copper_vein_traces", 400)
                .chancedOutput("phoenixcore:lapis_vein_traces", 100)
                .chancedOutput("phoenixcore:redstone_vein_traces", 300)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_gravel")
                .itemInputs("4x minecraft:gravel")
                .chancedOutput("phoenixcore:copper_vein_traces", 3500)
                .chancedOutput("phoenixcore:sapphire_vein_traces", 800)
                .chancedOutput("phoenixcore:redstone_vein_traces", 2500)
                .chancedOutput("phoenixcore:olivine_vein_traces", 1200)
                .chancedOutput("phoenixcore:mica_vein_traces", 1400)
                .chancedOutput("phoenixcore:manganese_vein_traces", 600)
                .chancedOutput("phoenixcore:nickel_vein_traces", 5000)
                .chancedOutput("phoenixcore:iron_vein_traces", 3000)
                .chancedOutput("phoenixcore:magnetite_vein_traces", 1000)
                .chancedOutput("phoenixcore:garnet_vein_traces", 1000)
                .chancedOutput("phoenixcore:cassiterite_vein_traces", 3000)
                .chancedOutput("phoenixcore:galena_vein_traces", 5000)
                .chancedOutput("phoenixcore:copper_tin_vein_traces", 5000)
                .chancedOutput("phoenixcore:coal_vein_traces", 7000)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "earth_sieve", "from_sand")
                .itemInputs("4x minecraft:sand")
                .chancedOutput("phoenixcore:oilsands_vein_traces", 3500)
                .chancedOutput("phoenixcore:salts_vein_traces", 6000)
                .chancedOutput("phoenixcore:mineral_sand_vein_traces", 7000)
                .chancedOutput("phoenixcore:copper_vein_traces", 3000)
                .chancedOutput("phoenixcore:apatite_vein_traces", 1500)
                .chancedOutput("phoenixcore:topaz_vein_traces", 8000)
                .chancedOutput("phoenixcore:tetrahedrite_vein_traces", 8000)
                .chancedOutput("phoenixcore:mineral_sand_vein_traces", 6000)
                .chancedOutput("phoenixcore:iron_vein_traces", 5000)
                .chancedOutput("phoenixcore:lapis_vein_traces", 1500)
                .chancedOutput("phoenixcore:diamond_vein_traces", 100)
                .duration(800)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_copper_traces")
                .itemInputs("phoenixcore:copper_vein_traces")
                .itemOutputs("gtceu:copper_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_sapphire_traces")
                .itemInputs("phoenixcore:sapphire_vein_traces")
                .itemOutputs("gtceu:sapphire_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_redstone_traces")
                .itemInputs("phoenixcore:redstone_vein_traces")
                .itemOutputs("minecraft:redstone")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_olivine_traces")
                .itemInputs("phoenixcore:olivine_vein_traces")
                .itemOutputs("gtceu:olivine_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_mica_traces")
                .itemInputs("phoenixcore:mica_vein_traces")
                .itemOutputs("gtceu:mica_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_manganese_traces")
                .itemInputs("phoenixcore:manganese_vein_traces")
                .itemOutputs("gtceu:manganese_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_lapis_traces")
                .itemInputs("phoenixcore:lapis_vein_traces")
                .itemOutputs("minecraft:lapis_lazuli")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_diamond_traces")
                .itemInputs("phoenixcore:diamond_vein_traces")
                .itemOutputs("minecraft:diamond")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_oilsands_traces")
                .itemInputs("phoenixcore:oilsands_vein_traces")
                .itemOutputs("gtceu:oilsands_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_salts_traces")
                .itemInputs("phoenixcore:salts_vein_traces")
                .itemOutputs("gtceu:salt_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_nickel_traces")
                .itemInputs("phoenixcore:nickel_vein_traces")
                .itemOutputs("gtceu:nickel_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_mineral_sand_traces")
                .itemInputs("phoenixcore:mineral_sand_vein_traces")
                .itemOutputs("gtceu:basaltic_mineral_sand_dust", "gtceu:granitic_mineral_sand_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_iron_traces")
                .itemInputs("phoenixcore:iron_vein_traces")
                .itemOutputs("gtceu:iron_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_magnetite_traces")
                .itemInputs("phoenixcore:magnetite_vein_traces")
                .itemOutputs("gtceu:magnetite_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_garnet_traces")
                .itemInputs("phoenixcore:garnet_vein_traces")
                .itemOutputs("gtceu:garnet_sand_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_cassiterite_traces")
                .itemInputs("phoenixcore:cassiterite_vein_traces")
                .itemOutputs("gtceu:cassiterite_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_garnet_tin_traces")
                .itemInputs("phoenixcore:garnet_tin_vein_traces")
                .itemOutputs("gtceu:garnet_sand_dust", "gtceu:tin_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_galena_traces")
                .itemInputs("phoenixcore:galena_vein_traces")
                .itemOutputs("gtceu:galena_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_copper_tin_traces")
                .itemInputs("phoenixcore:copper_tin_vein_traces")
                .itemOutputs("gtceu:copper_dust", "gtceu:tin_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_coal_traces")
                .itemInputs("phoenixcore:coal_vein_traces")
                .itemOutputs("minecraft:coal")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_apatite_traces")
                .itemInputs("phoenixcore:apatite_vein_traces")
                .itemOutputs("gtceu:apatite_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_topaz_traces")
                .itemInputs("phoenixcore:topaz_vein_traces")
                .itemOutputs("gtceu:topaz_dust")
                .duration(300)
                .EUt(8)
                .save();

        PftRecipe.gt(provider, "sifter", "sift_tetrahedrite_traces")
                .itemInputs("phoenixcore:tetrahedrite_vein_traces")
                .itemOutputs("gtceu:tetrahedrite_dust")
                .duration(300)
                .EUt(8)
                .save();
    }
}
