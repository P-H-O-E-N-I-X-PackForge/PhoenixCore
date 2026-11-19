package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftMiscRecipes {

    private PftMiscRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "travel_anchor_pftmiscrecipes", "travelanchors:travel_anchor",
                new String[] { "SGS", "GPG", "SGS" }, "S", "#c:plates/black_steel", "G", "#c:plates/green_sapphire",
                "P", "ender_pearl");

        PftCrafting.shaped(provider, "travel_staff_pftmiscrecipes", "travelanchors:travel_staff",
                new String[] { "  P", " R ", "R  " }, "R", "#c:rods/black_steel", "P", "gtceu:green_sapphire_gem");

        PftCrafting.shapeless(provider, "crafting_station_shapeless_pftmiscrecipes", "craftingstation:crafting_station",
                "minecraft:crafting_table", "#c:tools/saw");

        PftCrafting.shapeless(provider, "crafting_station_slab_shapeless_pftmiscrecipes",
                "craftingstation:crafting_station_slab", "craftingstation:crafting_station", "#c:tools/saw");

        PftCrafting.shaped(provider, "belt_pftmiscrecipes", "toolbelt:belt", new String[] { "SLS", "C C", "LIL" }, "S",
                "minecraft:string", "L", "minecraft:leather", "I", "#c:plates/steel", "C", "#c:screws/steel");

        PftCrafting.shaped(provider, "pouch_pftmiscrecipes", "toolbelt:pouch", new String[] { " G ", "LSL", "   " },
                "G", "#c:rings/gold", "L", "minecraft:leather", "S", "#c:plates/steel");

        PftRecipe.gt(provider, "assembler", "research_terminal")
                .itemInputs("1x gtceu:lv_machine_hull", "2x #gtceu:circuits/lv", "1x gtceu:lv_sensor")
                .itemOutputs("phoenixconflux:research_terminal")
                .duration(200)
                .EUt(GTValues.VA[GTValues.LV])
                .save();
    }
}
