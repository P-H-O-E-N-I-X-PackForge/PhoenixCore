package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftBuildingGadgetRecipes {

    private PftBuildingGadgetRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "gadget_building_pftbuildinggadgetrecipes", "buildinggadgets2:gadget_building",
                new String[] { "PLP", "DCD", "PKP" }, "P", "#c:plates/steel", "L", "#c:plates/lapis", "D",
                "#c:plates/diamond", "C", "#gtceu:circuits/lv", "K", "gtceu:tin_single_cable");

        PftCrafting.shaped(provider, "gadget_exchanging_pftbuildinggadgetrecipes", "buildinggadgets2:gadget_exchanging",
                new String[] { "PLP", "DCD", "PKP" }, "P", "#c:plates/steel", "L", "#c:plates/gold", "D",
                "#c:plates/diamond", "C", "#gtceu:circuits/lv", "K", "gtceu:tin_single_cable");

        PftCrafting.shaped(provider, "gadget_copy_paste_pftbuildinggadgetrecipes", "buildinggadgets2:gadget_copy_paste",
                new String[] { "PLP", "DCD", "PKP" }, "P", "#c:plates/steel", "L", "#c:plates/redstone", "D",
                "#c:plates/emerald", "C", "#gtceu:circuits/lv", "K", "gtceu:tin_single_cable");

        PftCrafting.shaped(provider, "template_manager_pftbuildinggadgetrecipes", "buildinggadgets2:template_manager",
                new String[] { "KCK", "CHC", "KCK" }, "K", "gtceu:tin_single_cable", "C", "#gtceu:circuits/lv", "H",
                "gtceu:lv_machine_hull");

        PftCrafting.shaped(provider, "gadget_destruction_pftbuildinggadgetrecipes",
                "buildinggadgets2:gadget_destruction", new String[] { "PLP", "DCD", "PKP" }, "P", "#c:plates/aluminium",
                "L", "#c:plates/nether_quartz", "D", "#c:plates/ender_pearl", "C", "#gtceu:circuits/mv", "K",
                "gtceu:copper_single_cable");

        PftCrafting.shaped(provider, "gadget_cut_paste_pftbuildinggadgetrecipes", "buildinggadgets2:gadget_cut_paste",
                new String[] { "PLP", "DCD", "PKP" }, "P", "#c:plates/aluminium", "L", "#c:plates/lapis", "D",
                "gtceu:iron_buzz_saw_blade", "C", "#gtceu:circuits/mv", "K", "gtceu:copper_single_cable");
    }
}
