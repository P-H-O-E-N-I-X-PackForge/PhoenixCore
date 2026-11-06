package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftGreenhouseRecipes {

    private PftGreenhouseRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "greenhouse", "rubber_wood")
                .notConsumable("gtceu:rubber_sapling")
                .inputFluids("water 1000")
                .itemOutputs("32x gtceu:rubber_log",
                        "3x gtceu:rubber_sapling",
                        "16x gtceu:sticky_resin")
                .duration(800)
                .EUt(32)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "rubber_wood_fertiliser")
                .notConsumable("gtceu:rubber_sapling")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x gtceu:rubber_log",
                        "6x gtceu:rubber_sapling",
                        "8x gtceu:sticky_resin")
                .duration(900)
                .EUt(32)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "peanut")
                .notConsumable("phoenixcore:peanut")
                .inputFluids("water 1000")
                .itemOutputs("4x phoenixcore:peanut")
                .duration(500)
                .EUt(32)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "peanut_fertiliser")
                .notConsumable("phoenixcore:peanut")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("8x phoenixcore:peanut")
                .duration(650)
                .EUt(32)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "chorus")
                .notConsumable("minecraft:chorus_fruit")
                .inputFluids("water 1000")
                .itemOutputs("4x minecraft:chorus_fruit")
                .duration(300)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "chorus_fert")
                .notConsumable("minecraft:chorus_fruit")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("8x minecraft:chorus_fruit")
                .duration(200)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "potato")
                .notConsumable("minecraft:potato")
                .inputFluids("water 1000")
                .itemOutputs("16x minecraft:potato")
                .duration(300)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "potato_fert")
                .notConsumable("minecraft:potato")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x minecraft:potato")
                .duration(200)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "torchflower")
                .notConsumable("minecraft:torchflower_seeds")
                .inputFluids("water 1000")
                .itemOutputs("4x minecraft:torchflower")
                .duration(300)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "torchflower_fert")
                .notConsumable("minecraft:torchflower_seeds")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("8x minecraft:torchflower")
                .duration(200)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "blue_archwood")
                .notConsumable("ars_nouveau:blue_archwood_sapling")
                .inputFluids("water 1000")
                .itemOutputs("16x ars_nouveau:blue_archwood_log")
                .duration(400)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "blue_archwood_fert")
                .notConsumable("ars_nouveau:blue_archwood_sapling")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x ars_nouveau:blue_archwood_log")
                .duration(250)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "purple_archwood")
                .notConsumable("ars_nouveau:purple_archwood_sapling")
                .inputFluids("water 1000")
                .itemOutputs("16x ars_nouveau:purple_archwood_log")
                .duration(400)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "purple_archwood_fert")
                .notConsumable("ars_nouveau:purple_archwood_sapling")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x ars_nouveau:purple_archwood_log")
                .duration(250)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "green_archwood")
                .notConsumable("ars_nouveau:green_archwood_sapling")
                .inputFluids("water 1000")
                .itemOutputs("16x ars_nouveau:green_archwood_log")
                .duration(400)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "green_archwood_fert")
                .notConsumable("ars_nouveau:green_archwood_sapling")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x ars_nouveau:green_archwood_log")
                .duration(250)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "red_archwood")
                .notConsumable("ars_nouveau:red_archwood_sapling")
                .inputFluids("water 1000")
                .itemOutputs("16x ars_nouveau:red_archwood_log")
                .duration(400)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "red_archwood_fert")
                .notConsumable("ars_nouveau:red_archwood_sapling")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x ars_nouveau:red_archwood_log")
                .duration(250)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "azalea")
                .notConsumable("minecraft:azalea")
                .inputFluids("water 1000")
                .itemOutputs("16x minecraft:oak_log", "8x minecraft:azalea_leaves")
                .duration(400)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "azalea_fert")
                .notConsumable("minecraft:azalea")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x minecraft:oak_log", "16x minecraft:azalea_leaves")
                .duration(250)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "flowering_azalea")
                .notConsumable("minecraft:flowering_azalea")
                .inputFluids("water 1000")
                .itemOutputs("16x minecraft:oak_log", "8x minecraft:flowering_azalea_leaves")
                .duration(400)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "flowering_azalea_fert")
                .notConsumable("minecraft:flowering_azalea")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x minecraft:oak_log", "16x minecraft:flowering_azalea_leaves")
                .duration(250)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "menril")
                .notConsumable("integrateddynamics:menril_sapling")
                .inputFluids("water 1000")
                .itemOutputs("16x integrateddynamics:menril_log")
                .duration(450)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "menril_fert")
                .notConsumable("integrateddynamics:menril_sapling")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("32x integrateddynamics:menril_log")
                .duration(300)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "chorus_flower")
                .notConsumable("minecraft:chorus_flower")
                .inputFluids("water 1000")
                .itemOutputs("4x minecraft:chorus_flower")
                .duration(300)
                .EUt(120)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "chorus_flower_fert")
                .notConsumable("minecraft:chorus_flower")
                .itemInputs("4x gtceu:fertilizer")
                .inputFluids("water 1000")
                .itemOutputs("8x minecraft:chorus_flower")
                .duration(200)
                .EUt(120)
                .circuit(2)
                .save();

        PftRecipe.gt(provider, "greenhouse", "waterchestnut")
                .notConsumable("phoenixcore:water_chestnut")
                .inputFluids("water 1000")
                .itemOutputs("4x phoenixcore:water_chestnut")
                .duration(600)
                .EUt(32)
                .circuit(1)
                .save();

        PftRecipe.gt(provider, "greenhouse", "waterchestnut_fertiliser")
                .notConsumable("phoenixcore:water_chestnut")
                .inputFluids("water 1000")
                .itemOutputs("8x phoenixcore:water_chestnut")
                .duration(900)
                .EUt(32)
                .circuit(2)
                .save();
    }
}
