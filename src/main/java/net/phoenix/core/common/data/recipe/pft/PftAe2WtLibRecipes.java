package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftAe2WtLibRecipes {

    private PftAe2WtLibRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "quantum_bridge_card_pftae2wtlibrecipes", "ae2wtlib:quantum_bridge_card",
                new String[] { "AS", "Ed" }, "A", "ae2:advanced_card", "S", "gtceu:ev_sensor", "E", "gtceu:ev_emitter",
                "d", "#c:tools/screwdriver");

        PftCrafting.shaped(provider, "magnet_card_pftae2wtlibrecipes", "ae2wtlib:magnet_card",
                new String[] { "RAB", "DMD", "PDP" }, "R", "#c:dyes/red", "A", "ae2:annihilation_plane", "B",
                "#c:dyes/blue", "D", "#c:double_plates/titanium", "M", "gtceu:hv_item_magnet", "P",
                "#c:plates/titanium");
    }
}
