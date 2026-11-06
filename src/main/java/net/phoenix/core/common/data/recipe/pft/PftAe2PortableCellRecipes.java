package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftAe2PortableCellRecipes {

    private PftAe2PortableCellRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "portable_item_cell_1k_pftae2portablecellrecipes", "ae2:portable_item_cell_1k",
                new String[] { "BGB", "CHC", "PWP" }, "B", "#c:bolts/red_alloy", "G", "minecraft:glass", "C",
                "#gtceu:circuits/ulv", "H", "#c:chests/wooden", "P", "#c:plates/wrought_iron", "W",
                "gtceu:red_alloy_single_cable");

        PftCrafting.shaped(provider, "portable_item_cell_4k_pftae2portablecellrecipes", "ae2:portable_item_cell_4k",
                new String[] { "BGB", "CHC", "PWP" }, "B", "#c:bolts/invar", "G", "ae2:quartz_glass", "C",
                "#gtceu:circuits/lv", "H", "gtceu:steel_crate", "P", "#c:plates/black_steel", "W",
                "gtceu:tin_single_cable");

        PftCrafting.shaped(provider, "portable_item_cell_16k_pftae2portablecellrecipes", "ae2:portable_item_cell_16k",
                new String[] { "BGB", "CHC", "PWP" }, "B", "#c:bolts/rose_gold", "G", "gtceu:tempered_glass", "C",
                "#gtceu:circuits/mv", "H", "gtceu:aluminium_crate", "P", "#c:plates/vanadium_steel", "W",
                "gtceu:annealed_copper_single_cable");

        PftCrafting.shaped(provider, "portable_item_cell_64k_pftae2portablecellrecipes", "ae2:portable_item_cell_64k",
                new String[] { "BGB", "CHC", "PWP" }, "B", "#c:bolts/blue_alloy", "G", "gtceu:cleanroom_glass", "C",
                "#gtceu:circuits/hv", "H", "gtceu:stainless_steel_crate", "P", "#c:plates/blue_steel", "W",
                "gtceu:kanthal_single_cable");

        PftCrafting.shaped(provider, "portable_item_cell_256k_pftae2portablecellrecipes", "ae2:portable_item_cell_256k",
                new String[] { "BGB", "CHC", "PWP" }, "B", "#c:bolts/platinum", "G", "gtceu:laminated_glass", "C",
                "#gtceu:circuits/ev", "H", "gtceu:titanium_crate", "P", "#c:plates/tungsten", "W",
                "gtceu:black_steel_single_cable");
    }
}
