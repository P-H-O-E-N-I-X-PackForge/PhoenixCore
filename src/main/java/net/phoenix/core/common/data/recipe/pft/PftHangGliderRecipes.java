package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftHangGliderRecipes {

    private PftHangGliderRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "glider_wing_pfthanggliderrecipes", "hangglider:glider_wing",
                new String[] { "hRL", "RLL", "LLL" }, "h", "#c:tools/hammer", "R", "#c:rods/steel", "L",
                "minecraft:leather");

        PftCrafting.shaped(provider, "glider_framework_pfthanggliderrecipes", "hangglider:glider_framework",
                new String[] { " R ", "RhR", "RRR" }, "h", "#c:tools/hammer", "R", "#c:rods/steel");

        PftCrafting.shaped(provider, "hang_glider_pfthanggliderrecipes", "hangglider:hang_glider",
                new String[] { "SSS", "LML", "SSS" }, "S", "#c:screws/iron", "L", "hangglider:glider_wing", "M",
                "hangglider:glider_framework");

        PftCrafting.shaped(provider, "reinforced_hang_glider_pfthanggliderrecipes", "hangglider:reinforced_hang_glider",
                new String[] { "SVS", "LML", "SVS" }, "S", "#c:screws/vanadium_steel", "V", "#c:rods/vanadium_steel",
                "L", "hangglider:glider_wing", "M", "hangglider:hang_glider");
    }
}
