package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;
import net.phoenix.core.common.data.recipe.pft.discipline.Discipline;
import net.phoenix.core.common.data.recipe.pft.discipline.DisciplineRecipes;

public final class PftCrucibleRecipes {

    private PftCrucibleRecipes() {}

    public static void init(RecipeOutput provider) {
        echoes(provider);
        disciplines(provider);
        controller(provider);
    }

    private static void echoes(RecipeOutput provider) {
        DisciplineRecipes.crucibleAny(provider, "echo_overworld")
                .inputFluids("phoenixcore:blazing_drilling_fluid  8000")
                .itemOutputs("23x gtceu:raw_chalcopyrite",
                        "18x gtceu:raw_cassiterite",
                        "14x gtceu:raw_galena",
                        "14x gtceu:raw_sphalerite",
                        "29x gtceu:raw_magnetite",
                        "14x minecraft:raw_gold",
                        "14x gtceu:raw_silver",
                        "14x gtceu:raw_pentlandite",
                        "39x gtceu:raw_coal",
                        "23x gtceu:raw_redstone",
                        "23x gtceu:raw_lapis",
                        "14x gtceu:raw_graphite",
                        "23x gtceu:raw_sulfur",
                        "14x gtceu:raw_diamond")
                .duration(15000)
                .EUt(GTValues.VA[GTValues.EV] / 2)

                .save();

        DisciplineRecipes.crucibleAny(provider, "echo_nether")
                .inputFluids("phoenixcore:blazing_drilling_fluid  8000")
                .itemOutputs("29x gtceu:raw_sulfur",
                        "23x gtceu:raw_pyrite",
                        "20x phoenixcore:raw_dormant_ember",
                        "14x gtceu:raw_cinnabar",
                        "29x gtceu:raw_nether_quartz",
                        "23x gtceu:raw_magnetite",
                        "14x gtceu:raw_barite",
                        "14x gtceu:raw_tetrahedrite",
                        "14x gtceu:raw_stibnite",
                        "14x gtceu:raw_bastnasite",
                        "14x gtceu:raw_redstone",
                        "14x gtceu:raw_electrotine",
                        "14x gtceu:raw_saltpeter",
                        "14x minecraft:raw_gold",
                        "14x gtceu:raw_chalcocite",
                        "14x gtceu:raw_sphalerite")
                .duration(17000)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .circuit(1)

                .save();

        DisciplineRecipes.crucibleAny(provider, "echo_end")
                .inputFluids("phoenixcore:blazing_drilling_fluid  8000")
                .itemOutputs("17x gtceu:raw_naquadah",
                        "20x phoenixcore:raw_permafrost",
                        "17x gtceu:raw_palladium",
                        "17x gtceu:raw_platinum",
                        "17x gtceu:raw_molybdenite",
                        "29x gtceu:raw_magnetite",
                        "17x gtceu:raw_aluminium",
                        "17x gtceu:raw_plutonium",
                        "17x gtceu:raw_chromite",
                        "17x gtceu:raw_vanadium_magnetite",
                        "17x gtceu:raw_lithium",
                        "17x gtceu:raw_scheelite",
                        "17x gtceu:raw_cooperite",
                        "17x gtceu:raw_bornite",
                        "17x gtceu:raw_pitchblende",
                        "17x gtceu:raw_uraninite")
                .circuit(1)
                .duration(18000)
                .EUt(GTValues.VA[GTValues.EV] / 2)

                .save();

        DisciplineRecipes.crucibleAny(provider, "echo_moon")
                .inputFluids("phoenixcore:voidic_drilling_fluid  8000")
                .itemOutputs("28x gtceu:raw_ilmenite",
                        "28x gtceu:raw_bauxite",
                        "17x phoenixcore:raw_fluorite",
                        "28x gtceu:raw_magnetite",
                        "28x gtceu:raw_cooperite",
                        "12x phoenixcore:raw_nevvonian_iron",
                        "12x phoenixcore:raw_ignisium")
                .duration(12000)
                .circuit(1)
                .EUt(GTValues.VA[GTValues.EV] / 2)

                .save();
    }

    private static void disciplines(RecipeOutput provider) {
        int circuit = 1;
        for (Discipline discipline : Discipline.values()) {
            DisciplineRecipes.commonOres(provider, discipline, 6)
                    .inputFluids("gtceu:distilled_water 2000")
                    .circuit(circuit++)
                    .duration(DisciplineRecipes.DEFAULT_DURATION)
                    .EUt(DisciplineRecipes.DEFAULT_EUT)
                    .save();
        }
    }

    private static void controller(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembler", "aether_crucible_controller")
                .itemInputs("1x gtceu:ev_machine_hull",
                        "2x phoenixcore:source_fiber_machine_casing",
                        "1x gtceu:ev_field_generator",
                        "2x gtceu:ev_electric_pump",
                        "4x gtceu:nickel_plate",
                        "4x gtceu:zinc_plate",
                        "2x gtceu:silver_plate")
                .inputFluids("gtceu:soldering_alloy 288")
                .itemOutputs("1x phoenixcore:aether_crucible")
                .duration(600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();
    }
}
