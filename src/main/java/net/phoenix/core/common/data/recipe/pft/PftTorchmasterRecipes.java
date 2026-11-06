package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftTorchmasterRecipes {

    private PftTorchmasterRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "chemical_bath", "frozen_pearl")
                .itemInputs("minecraft:ender_pearl")
                .inputFluids("gtceu:ice 1152")
                .itemOutputs("torchmaster:frozen_pearl")
                .duration(100)
                .EUt(16)
                .save();

        PftCrafting.shaped(provider, "megatorch_pfttorchmasterrecipes", "torchmaster:megatorch",
                new String[] { "SDR", "IBD", "RIS" }, "S", "#c:screws/iron", "D", "#c:gems/diamond", "R",
                "#c:rings/gold", "I", "minecraft:stick", "B", "#c:storage_blocks/coke");

        PftCrafting.shaped(provider, "dreadlamp_pfttorchmasterrecipes", "torchmaster:dreadlamp",
                new String[] { "PPP", "GLG", "PPP" }, "P", "#c:plates/obsidian", "L", "minecraft:glowstone", "G",
                "#c:glass_panes");

        PftCrafting.shaped(provider, "feral_flare_lantern_pfttorchmasterrecipes", "torchmaster:feral_flare_lantern",
                new String[] { "SPR", "INP", "RIS" }, "S", "#c:screws/iron", "P", "#c:plates/glowstone", "R",
                "#c:rings/steel", "I", "#c:rods/steel", "N", "#c:small_springs/gold");
    }
}
