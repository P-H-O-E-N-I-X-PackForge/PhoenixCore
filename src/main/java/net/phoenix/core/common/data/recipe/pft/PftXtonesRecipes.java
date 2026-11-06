package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCooking;
import net.phoenix.core.common.data.PftRecipe;

public final class PftXtonesRecipes {

    private PftXtonesRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "mixer", "xtonesreworked:xtone_tile")
                .itemInputs("cobblestone", "stone")
                .itemOutputs("4x xtonesreworked:xtone_tile")
                .EUt(2)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "xtonesreworked:xtone_tile")
                .itemInputs("cobblestone", "stone")
                .itemOutputs("16x xtonesreworked:xtone_tile")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        PftRecipe.gt(provider, "construction_core", "xtonesreworked:flat_lamp")
                .itemInputs("xtonesreworked:xtone_tile", "glowstone")
                .itemOutputs("4x xtonesreworked:flat_lamp")
                .inputFluids("gtceu:construction_foam 100")
                .EUt(8)
                .duration(20)
                .save();

        String[][] materials = { { "#c:dusts/stone", "agon" }, { "#c:dyes/blue", "azur" }, { "#c:dusts/coal", "bitt" },
                { "#c:dusts/clay", "cray" }, { "#c:dusts/brick", "fort" }, { "#c:dusts/glass", "glaxx" },
                { "#c:dusts/deepslate", "iszm" }, { "#c:dyes/orange", "jelt" }, { "#c:dusts/flint", "korp" },
                { "gtceu:fertilizer", "kryp" }, { "#c:dusts/netherrack", "lair" }, { "#c:dusts/ice", "lave" },
                { "#c:dyes/lime", "mint" }, { "#c:dusts/wood", "myst" }, { "#c:dusts/redstone", "reds" },
                { "#c:dusts/sugar", "reed" }, { "#c:dusts/quartz_sand", "roen" }, { "#c:dyes/yellow", "sols" },
                { "#c:dyes/green", "sync" }, { "#c:dyes/gray", "tank" }, { "#c:dyes/black", "vect" },
                { "#c:dyes/light_blue", "vena" }, { "#c:ingots/clay", "zane" }, { "#c:dusts/iron", "zech" },
                { "snowball", "zest" }, { "string", "zeta" }, { "#c:dusts/charcoal", "zion" }, { "bone", "zkul" },
                { "#c:dusts/andesite", "zoea" }, { "#c:dusts/gunpowder", "zome" }, { "#c:dusts/granite", "zone" },
                { "terracotta", "zorg" }, { "#c:dyes/light_gray", "ztyl" }, { "stick", "zyth" } };
        for (String[] m : materials) {
            String material = m[1];
            PftRecipe.gt(provider, "mixer", "mixing_" + material)
                    .itemInputs("xtonesreworked:xtone_tile", m[0])
                    .itemOutputs("4x xtonesreworked:" + material + "_block_0")
                    .EUt(2)
                    .duration(20)
                    .save();
            PftRecipe.gt(provider, "construction_core", "construction_" + material)
                    .itemInputs("xtonesreworked:xtone_tile", m[0])
                    .itemOutputs("16x xtonesreworked:" + material + "_block_0")
                    .inputFluids("gtceu:construction_foam 100")
                    .EUt(8)
                    .duration(20)
                    .save();
            for (int i = 0; i < 16; i++) {
                PftCooking.stonecutting(provider, "xtones_" + material + "_block_" + i,
                        "xtonesreworked:" + material + "_block_" + i, "#xtonesreworked:" + material);
            }
        }
    }
}
