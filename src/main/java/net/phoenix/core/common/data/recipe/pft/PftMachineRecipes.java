package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftMachineRecipes {

    private PftMachineRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "construction_core_pftmachinerecipes", "gtceu:construction_core",
                new String[] { "PRP", "CHC", "MCM" }, "M", "gtceu:lv_electric_motor", "C", "#gtceu:circuits/lv", "H",
                "gtceu:lv_machine_hull", "P", "gtceu:manganese_phosphide_single_wire", "R", "gtceu:lv_robot_arm");

        PftCrafting.shaped(provider, "greenhouse_pftmachinerecipes", "gtceu:greenhouse",
                new String[] { "GGG", "CHC", "PUP" }, "G", "gtceu:tempered_glass", "C", "#gtceu:circuits/lv", "H",
                "gtceu:lv_machine_hull", "P", "gtceu:lv_electric_piston", "U", "gtceu:lv_electric_pump");
    }
}
