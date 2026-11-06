package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftAe2CardRecipes {

    private PftAe2CardRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "advanced_card_pftae2cardrecipes", "2x ae2:advanced_card",
                new String[] { "WP ", "ACP", "SP " }, "W", "#c:fine_wires/red_alloy", "P", "#c:plates/stainless_steel",
                "A", "ae2:engineering_processor", "C", "#gtceu:circuits/hv", "S", "#c:fine_wires/silver");

        PftCrafting.shapeless(provider, "fuzzy_card_shapeless_pftae2cardrecipes", "ae2:fuzzy_card", "ae2:advanced_card",
                "#gtceu:circuits/ulv");

        PftCrafting.shapeless(provider, "inverter_card_shapeless_pftae2cardrecipes", "ae2:inverter_card",
                "ae2:advanced_card", "#c:fine_wires/red_alloy");

        PftCrafting.shapeless(provider, "speed_card_shapeless_pftae2cardrecipes", "ae2:speed_card", "ae2:advanced_card",
                "#gtceu:circuits/mv");

        PftCrafting.shapeless(provider, "energy_card_shapeless_pftae2cardrecipes", "ae2:energy_card",
                "ae2:advanced_card", "#gtceu:batteries/mv");

        PftCrafting.shaped(provider, "basic_card_pftae2cardrecipes", "2x ae2:basic_card",
                new String[] { "WP ", "ACP", "SP " }, "W", "#c:fine_wires/red_alloy", "P", "#c:plates/aluminium", "A",
                "ae2:engineering_processor", "C", "#gtceu:circuits/mv", "S", "#c:fine_wires/gold");

        PftCrafting.shapeless(provider, "capacity_card_shapeless_pftae2cardrecipes", "ae2:capacity_card",
                "ae2:basic_card", "#gtceu:circuits/lv");

        PftCrafting.shapeless(provider, "crafting_card_shapeless_pftae2cardrecipes", "ae2:crafting_card",
                "ae2:basic_card", "#gtceu:circuits/mv");

        PftCrafting.shapeless(provider, "redstone_card_shapeless_pftae2cardrecipes", "ae2:redstone_card",
                "ae2:basic_card", "#c:plates/red_alloy");
    }
}
