package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftIntegratedDynamicsRecipes {

    private PftIntegratedDynamicsRecipes() {}

    private static String id(String item) {
        return "integrateddynamics:" + item;
    }

    private static String gt(String item) {
        return "gtceu:" + item;
    }

    private static final String MENRIL = "crystalized_menril_chunk";

    private static void centrifuge(RecipeOutput provider, String name, String input, String output, String fluid,
                                   int amount) {
        PftRecipe.gt(provider, "centrifuge", "id/" + name)
                .itemInputs(input)
                .itemOutputs(output)
                .outputFluids(id(fluid) + " " + amount)
                .duration(100)
                .EUt(GTValues.VA[GTValues.ULV])
                .save();
    }

    private static void solidify(RecipeOutput provider, String name, String fluid, String output) {
        PftRecipe.gt(provider, "fluid_solidifier", "id/" + name)
                .notConsumable(gt("block_casting_mold"))
                .inputFluids(id(fluid) + " 1000")
                .itemOutputs(id(output))
                .duration(100)
                .EUt(GTValues.VA[GTValues.ULV])
                .save();
    }

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "variable_transformer_input_pft", id("variable_transformer_input"),
                new String[] { " A ", "BCB", " A " }, "A", id("variable"), "B", id(MENRIL), "C",
                gt("lv_conveyor_module"));
        PftCrafting.shaped(provider, "variable_transformer_output_pft", id("variable_transformer_output"),
                new String[] { " A ", "BCB", " A " }, "A", id(MENRIL), "B", id("variable"), "C",
                gt("lv_conveyor_module"));
        PftCrafting.shapeless(provider, "variable_transformer_input_swap_pft", id("variable_transformer_input"),
                id("variable_transformer_output"));
        PftCrafting.shapeless(provider, "variable_transformer_output_swap_pft", id("variable_transformer_output"),
                id("variable_transformer_input"));

        PftCrafting.shaped(provider, "interface_item_pft", "integratedtunnels:part_interface_item",
                new String[] { "ABA", "AAA" }, "A", id(MENRIL), "B", gt("mv_conveyor_module"));
        PftCrafting.shaped(provider, "interface_fluid_pft", "integratedtunnels:part_interface_fluid",
                new String[] { "ABA", "AAA" }, "A", id(MENRIL), "B", gt("mv_electric_pump"));
        PftCrafting.shaped(provider, "cable_pft", "8x " + id("cable"), new String[] { "AAA", "BCB", "AAA" }, "A",
                id(MENRIL),
                "B", "#c:rods/aluminfrost", "C", "minecraft:redstone");
        PftCrafting.shaped(provider, "logic_programmer_pft", id("logic_programmer"),
                new String[] { "ABA", "ACA", "ADA" },
                "A", id(MENRIL), "B", gt("computer_monitor_cover"), "C", gt("mv_machine_hull"), "D", id("variable"));
        PftCrafting.shaped(provider, "logic_director_pft", id("logic_director"), new String[] { "ABA", "ACA", "ABA" },
                "A",
                id("crystalized_chorus_chunk"), "B", id(MENRIL), "C", "#gtceu:circuits/hv");

        centrifuge(provider, "menril_log", id("menril_log"), "3x " + id(MENRIL), "menril_resin", 1000);
        centrifuge(provider, "menril_planks", id("menril_planks"), id(MENRIL), "menril_resin", 250);
        centrifuge(provider, "proto_chorus", id("proto_chorus"), id("crystalized_chorus_chunk"), "liquid_chorus", 125);
        centrifuge(provider, "popped_chorus_fruit", "minecraft:popped_chorus_fruit",
                "3x " + id("crystalized_chorus_chunk"), "liquid_chorus", 125);
        solidify(provider, "menril_resin_block", "menril_resin", "crystalized_menril_block");
        solidify(provider, "chorus_block", "liquid_chorus", "crystalized_chorus_block");
    }
}
