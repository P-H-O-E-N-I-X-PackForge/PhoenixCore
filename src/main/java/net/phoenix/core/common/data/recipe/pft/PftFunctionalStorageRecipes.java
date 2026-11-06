package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftFunctionalStorageRecipes {

    private PftFunctionalStorageRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "controller_extension_pft", "functionalstorage:controller_extension",
                new String[] { "III", "CDC", "IGI" }, "I", "gtceu:iron_plate", "C", "#gtceu:circuits/lv", "D",
                "#functionalstorage:drawer", "G", "#c:storage_blocks/gold");
        PftCrafting.shaped(provider, "armory_cabinet_pft", "functionalstorage:armory_cabinet",
                new String[] { "ICI", "PDP", "III" }, "I", "gtceu:iron_plate", "C", "#gtceu:circuits/hv", "D",
                "#functionalstorage:drawer", "P", "gtceu:hv_electric_piston");
        PftCrafting.shaped(provider, "ender_drawer_pft", "functionalstorage:ender_drawer",
                new String[] { "ICI", "PEP", "III" }, "I", "gtceu:iron_plate", "C", "#gtceu:circuits/mv", "E",
                "enderchests:ender_chest", "P", "gtceu:mv_electric_piston");

        String[] cores = { "#c:storage_blocks/diamond", "#c:storage_blocks/emerald" };
        for (int i = 0; i < cores.length; i++) {
            PftCrafting.shaped(provider, "storage_controller_" + i + "_pft", "functionalstorage:storage_controller",
                    new String[] { "III", "CDC", "IEI" }, "I", "gtceu:iron_plate", "C", "#gtceu:circuits/lv", "D",
                    "#functionalstorage:drawer", "E", cores[i]);
        }

        for (String block : new String[] { "storage_controller", "controller_extension" }) {
            PftCrafting.shaped(provider, "framed_" + block + "_pft", "functionalstorage:framed_" + block,
                    new String[] { "III", "IBI", "III" }, "I", "minecraft:iron_nugget", "B",
                    "functionalstorage:" + block);
        }

        String[][] tiers = { { "copper", "lead", "bronze" }, { "gold", "tin", "steel" },
                { "diamond", "copper", "aluminium" }, { "netherite", "gold", "stainless_steel" } };
        for (int i = 0; i < tiers.length; i++) {
            String previous = i == 0 ? "#functionalstorage:drawer" :
                    "functionalstorage:" + tiers[i - 1][0] + "_upgrade";
            PftCrafting.shaped(provider, tiers[i][0] + "_upgrade_pft",
                    "2x functionalstorage:" + tiers[i][0] + "_upgrade",
                    new String[] { "PUP", "PGP", "PUP" }, "P", "#c:plates/" + tiers[i][2], "G",
                    "#c:double_plates/" + tiers[i][1], "U", previous);
        }
    }
}
