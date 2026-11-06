package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftIvRecipes {

    private PftIvRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "assembly_line", "melferious_matrix")
                .itemInputs("8x gtceu:sturdy_machine_casing", "16x phoenixcore:resonant_rhodium_alloy_plate",
                        "64x phoenixcore:honey_treat", "4x kubejs:conductive_honey_coil",
                        "1x phoenixcore:extremely_modified_space_grade_steel_quadruple_wire",
                        "4x phoenixcore:void_touched_tungsten_steel_rotor", "2x phoenixcore:simulated_colony")
                .inputFluids("phoenixcore:honey 24000", "gtceu:soldering_alloy 4000")
                .itemOutputs("phoenixcore:melliferious_matrix")
                .stationResearch("phoenixcore:simulated_colony", 131000, 1, 1, "gtceu:data_stick")
                .EUt(GTValues.VA[GTValues.LuV])
                .duration(1000)
                .save();

        PftRecipe.gt(provider, "assembly_line", "advanced_cracker")
                .itemInputs("16x gtceu:hssg_coil_block", "4x #gtceu:circuits/luv",
                        "gtceu:luv_machine_hull", "4x gtceu:luv_electric_pump",
                        "8x gtceu:luv_emitter", "16x phoenixcore:resonant_rhodium_alloy_gear")
                .inputFluids("gtceu:soldering_alloy 4000", "gtceu:vanadium_gallium 1000", "gtceu:hssg 2880")
                .itemOutputs("phoenixcore:advanced_cracking_unit")
                .stationResearch("gtceu:cracker", 131000, 1, 1, "gtceu:data_stick")
                .EUt(GTValues.VA[GTValues.LuV])
                .duration(900)
                .save();

        PftRecipe.gt(provider, "assembly_line", "luv_confectionery_fabricator")
                .itemInputs("4x phoenixcore:resonant_rhodium_alloy_rotor", "4x phoenixcore:honey_treat",
                        "2x #gtceu:circuits/luv", "gtceu:luv_machine_hull", "1x gtceu:luv_sensor")
                .itemOutputs("gtceu:luv_confectionery_fabricator")
                .inputFluids("phoenixcore:honey 1700", "gtceu:soldering_alloy 4000")
                .stationResearch("gtceu:iv_confectionery_fabricator", 131000, 1, 1, "gtceu:data_stick")
                .EUt(GTValues.VA[GTValues.LuV])
                .duration(600)
                .save();

        PftRecipe.gt(provider, "assembly_line", "zpm_confectionery_fabricator")
                .itemInputs("4x phoenixcore:advanced_quin_naquadian_alloy_rotor", "16x phoenixcore:honey_treat",
                        "2x #gtceu:circuits/zpm", "gtceu:zpm_machine_hull", "1x gtceu:zpm_sensor")
                .itemOutputs("gtceu:zpm_confectionery_fabricator")
                .inputFluids("phoenixcore:honey 1700", "gtceu:soldering_alloy 2500")
                .stationResearch("gtceu:luv_confectionery_fabricator", 131000, 1, 1, null)
                .EUt(GTValues.VA[GTValues.ZPM])
                .duration(600)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "oleum")
                .inputFluids("gtceu:sulfur_trioxide 244", "gtceu:sulfuric_acid 1000")
                .outputFluids("phoenixcore:oleum 1000")
                .duration(134)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "concentrated_sulfuric_acid")
                .inputFluids("phoenixcore:oleum 300", "minecraft:water 700")
                .outputFluids("phoenixcore:concentrated_sulfuric_acid 800")
                .duration(80)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "electrolyzer", "concentrated_sulfuric_acid_to_hydrogen_peroxide")
                .inputFluids("phoenixcore:concentrated_sulfuric_acid 1000")
                .outputFluids("gtceu:hydrogen_peroxide 100", "minecraft:water 300", "gtceu:sulfuric_acid 500")
                .duration(550)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "acetone_cyanohydrin")
                .inputFluids("gtceu:acetone 1000", "gtceu:hydrogen_cyanide 1000")
                .outputFluids("phoenixcore:acetone_cyanohydrin 1000")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "methyl_methacrylate")
                .inputFluids("phoenixcore:acetone_cyanohydrin 1000", "gtceu:sulfuric_acid 1000", "gtceu:methanol 750")
                .outputFluids("phoenixcore:methyl_methacrylate 1000", "phoenixcore:ammonium_bisulfate_solution 1000")
                .duration(300)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "distillation_tower", "ammonium_bisulfate_solution_to_dust")
                .inputFluids("phoenixcore:ammonium_bisulfate_solution 1000")
                .outputFluids("gtceu:sulfuric_acid 500", "minecraft:water 600", "gtceu:ammonia 120")
                .itemOutputs("12x phoenixcore:ammonium_bisulfate_dust")
                .duration(240)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "electrolyzer", "ammonium_bisulfate_dust_to_ammonium_persulfate")
                .itemInputs("7x phoenixcore:ammonium_bisulfate_dust")
                .outputFluids("gtceu:hydrogen 1000", "phoenixcore:ammonium_persulfate 250")
                .duration(130)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "ammonium_bisulfate_solution")
                .inputFluids("gtceu:sulfuric_acid 200", "gtceu:ammonia 750")
                .outputFluids("phoenixcore:ammonium_bisulfate_solution 250")
                .duration(60)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "advanced_pmma_polymerization")
                .inputFluids("phoenixcore:methyl_methacrylate 144", "phoenixcore:ammonium_persulfate 750",
                        "gtceu:hydrogen_peroxide 250")
                .outputFluids("phoenixcore:polymethyl_methacrylate 216")
                .duration(800)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "alloy_blast_smelter", "liquid_void_touched_tungsten_steel")
                .itemInputs("4x gtceu:tungsten_dust", "4x phoenixcore:voidglass_shard_dust", "2x gtceu:molybdenum_dust")
                .circuit(5)
                .inputFluids("gtceu:liquid_ender_air 100")
                .outputFluids("phoenixcore:molten_void_touched_tungsten_steel 1152")
                .duration(6000)
                .blastFurnaceTemp(3600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "alloy_blast_smelter", "liquid_void_touched_tungsten_steel_boosted")
                .itemInputs("4x gtceu:tungsten_dust", "4x phoenixcore:voidglass_shard_dust", "2x gtceu:molybdenum_dust")
                .circuit(15)
                .inputFluids("gtceu:liquid_ender_air 100", "gtceu:helium 100")
                .outputFluids("phoenixcore:molten_void_touched_tungsten_steel 1152")
                .duration(4200)
                .blastFurnaceTemp(3600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "mixer", "extremely_modified_space_grade_steel")
                .itemInputs("4x phoenixcore:source_imbued_titanium_dust", "2x gtceu:ruthenium_dust",
                        "4x phoenixcore:space_grade_steel_dust", "32x minecraft:tnt")
                .inputFluids("gtceu:radon 250")
                .itemOutputs("8x phoenixcore:extremely_modified_space_grade_steel_dust")
                .duration(400)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "alloy_blast_smelter", "molten_extremely_modified_space_grade_steel")
                .itemInputs("4x phoenixcore:source_imbued_titanium_dust", "2x gtceu:ruthenium_dust",
                        "4x phoenixcore:space_grade_steel_dust", "32x minecraft:tnt")
                .circuit(5)
                .inputFluids("gtceu:radon 250")
                .outputFluids("phoenixcore:molten_extremely_modified_space_grade_steel 1152")
                .duration(4800)
                .blastFurnaceTemp(3400)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "alloy_blast_smelter", "molten_extremely_modified_space_grade_steel_boosted")
                .itemInputs("4x phoenixcore:source_imbued_titanium_dust", "2x gtceu:ruthenium_dust",
                        "4x phoenixcore:space_grade_steel_dust", "32x minecraft:tnt")
                .circuit(15)
                .inputFluids("gtceu:radon 250", "gtceu:helium 1000")
                .outputFluids("phoenixcore:molten_extremely_modified_space_grade_steel 1440")
                .duration(3600)
                .blastFurnaceTemp(3600)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "mixer", "resonant_rhodium_alloy")
                .itemInputs("3x gtceu:rhodium_dust", "4x gtceu:palladium_dust",
                        "2x phoenixcore:polarity_flipped_bismuthite_dust", "4x gtceu:cerium_dust")
                .inputFluids("phoenixcore:resonant_ender 144")
                .itemOutputs("13x phoenixcore:resonant_rhodium_alloy_dust")
                .duration(360)
                .circuit(4)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "alloy_blast_smelter", "molten_resonant_rhodium_alloy_gas")
                .itemInputs("3x gtceu:rhodium_dust", "4x gtceu:palladium_dust",
                        "2x phoenixcore:polarity_flipped_bismuthite_dust", "4x gtceu:cerium_dust")
                .circuit(15)
                .inputFluids("phoenixcore:resonant_ender 1144", "gtceu:argon 100")
                .outputFluids("phoenixcore:molten_resonant_rhodium_alloy 2016")
                .duration(3800)
                .blastFurnaceTemp(3600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "alloy_blast_smelter", "molten_resonant_rhodium_alloy")
                .itemInputs("3x gtceu:rhodium_dust", "4x gtceu:palladium_dust",
                        "2x phoenixcore:polarity_flipped_bismuthite_dust", "4x gtceu:cerium_dust")
                .circuit(5)
                .inputFluids("phoenixcore:resonant_ender 1144")
                .outputFluids("phoenixcore:molten_resonant_rhodium_alloy 2016")
                .duration(5400)
                .blastFurnaceTemp(3600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "advanced_quin_naquadian_alloy_synthesis")
                .itemInputs("2x gtceu:naquadah_alloy_dust", "1x phoenixcore:exotic_fissile_materials_clump_dust",
                        "1x phoenixcore:nevvonian_iron_dust")
                .inputFluids("phoenixcore:critical_steam 500")
                .itemOutputs("5x phoenixcore:advanced_quin_naquadian_alloy_dust")
                .duration(200)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "mixer", "melting_catalyst")
                .itemInputs("2x gtceu:carbon_dust", "1x minecraft:glowstone_dust")
                .circuit(4)
                .inputFluids("minecraft:water 2500", "gtceu:naphtha 250")
                .outputFluids("phoenixcore:wax_melting_catalyst 2500")
                .duration(100)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "mixer", "permafrost")
                .itemInputs("minecraft:packed_ice", "minecraft:blue_ice", "minecraft:ice")
                .itemOutputs("14x phoenixcore:permafrost_dust")
                .duration(100)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "mixer", "dormant_ember")
                .itemInputs("minecraft:coal", "minecraft:coal_block", "phoenixcore:small_dormant_ember_dust")
                .inputFluids("gtceu:distilled_water 1000")
                .itemOutputs("16x phoenixcore:dormant_ember_dust")
                .duration(350)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "mixer", "honey_comb_base_mixture")
                .inputFluids("phoenixcore:honey 500", "phoenixcore:polymethyl_methacrylate 244")
                .itemInputs("phoenixcore:source_imbued_titanium_dust", "gtceu:niobium_dust")
                .outputFluids("phoenixcore:honey_comb_base_mixture 250")
                .duration(60)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "fluid_solidifier", "honey_comb_base")
                .inputFluids("phoenixcore:honey_comb_base_mixture 1008")
                .notConsumable("phoenixcore:honey_comb_base_mold")
                .itemOutputs("phoenixcore:honey_comb_base")
                .duration(2000)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();

        PftRecipe.gt(provider, "compressor", "honey_comb_bases_solid")
                .notConsumable("12x phoenixcore:dense_void_touched_tungsten_steel_plate")
                .itemOutputs("phoenixcore:honey_comb_base_mold")
                .duration(2000)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "superconductive_honey_creation")
                .itemInputs("8x phoenixcore:honey_dust", "4x gtceu:niobium_titanium_dust", "2x gtceu:iridium_dust")
                .inputFluids("gtceu:helium 500", "phoenixcore:cryo_ember_fluid 100")
                .itemOutputs("12x phoenixcore:superconductive_honey_dust")
                .duration(300)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "implosion_compressor", "superconductive_honey_ingot_normal")
                .itemInputs("1x phoenixcore:superconductive_honey_dust", "16x minecraft:tnt")
                .itemOutputs("1x phoenixcore:superconductive_honey_ingot")
                .duration(450)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "implosion_compressor", "superconductive_honey_ingot_itnt")
                .itemInputs("1x phoenixcore:superconductive_honey_dust", "4x gtceu:industrial_tnt")
                .itemOutputs("1x phoenixcore:superconductive_honey_ingot")
                .duration(350)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "assembler", "superconductive_honey_coil_block")
                .itemInputs("8x phoenixcore:superconductive_honey_double_wire", "8x gtceu:hsss_foil")
                .inputFluids("phoenixcore:void_touched_tungsten_steel 144")
                .itemOutputs("kubejs:conductive_honey_coil")
                .duration(750)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "sugar_to_sucrose_enhanced")
                .itemInputs("8x minecraft:sugar", "1x minecraft:glowstone_dust")
                .circuit(1)
                .inputFluids("minecraft:water 500")
                .outputFluids("phoenixcore:sucrose 1200")
                .chancedOutput("1x minecraft:glowstone_dust", 8500)
                .duration(1200)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "sucrose_inversion")
                .inputFluids("phoenixcore:sucrose", "gtceu:distilled_water 500", "gtceu:sulfuric_acid 10")
                .circuit(4)
                .outputFluids("phoenixcore:invert_sugar_solution 1000")
                .duration(1200)
                .EUt(GTValues.VA[GTValues.EV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "protein_solution_from_mince_meat")
                .itemInputs("32x gtceu:meat_dust")
                .circuit(12)
                .inputFluids("gtceu:acetic_acid 1008")
                .outputFluids("phoenixcore:protein_solution 1000")
                .duration(900)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "extractor", "resonant_ender")
                .itemInputs("1x minecraft:ender_pearl")
                .outputFluids("phoenixcore:resonant_ender 250")
                .duration(300)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "amino_acids_from_protein_solution")
                .inputFluids("phoenixcore:protein_solution 1500", "gtceu:hydrofluoric_acid 250")
                .circuit(2)
                .itemInputs("16x gtceu:activated_carbon_dust")
                .outputFluids("phoenixcore:amino_acids 1000")
                .duration(950)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "pollen_concentrate_fluid_from_flowers")
                .inputFluids("gtceu:ethanol 2000")
                .circuit(4)
                .itemInputs("4x #minecraft:flowers")
                .outputFluids("phoenixcore:pollen_concentrate_fluid 900")
                .duration(850)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "pollen_concentrate_fluid_from_plant_balls")
                .inputFluids("gtceu:ethanol 2000")
                .circuit(4)
                .itemInputs("1x gtceu:plant_ball")
                .outputFluids("phoenixcore:pollen_concentrate_fluid 750")
                .duration(825)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "honey_catalyst")
                .inputFluids("phoenixcore:amino_acids 4500", "minecraft:water 32000", "gtceu:sodium_potassium 1700")
                .circuit(24)
                .itemInputs("6x gtceu:magnesium_dust", "12x gtceu:calcium_dust")
                .outputFluids("phoenixcore:honey_catalyst 750")
                .duration(650)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "honey")
                .inputFluids("phoenixcore:honey_catalyst 3000", "phoenixcore:invert_sugar_solution 5000",
                        "phoenixcore:pollen_concentrate_fluid 8500")
                .circuit(24)
                .outputFluids("phoenixcore:honey 1500")
                .duration(1600)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "assembler", "honeycomb_assembly")
                .inputFluids("phoenixcore:honey 588", "phoenixcore:invert_sugar_solution 144")
                .notConsumable("phoenixcore:honey_comb_base")
                .itemOutputs("minecraft:honeycomb")
                .duration(700)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "distillation_tower", "invert_sugar_solution_separation")
                .inputFluids("phoenixcore:invert_sugar_solution 1000")
                .outputFluids("phoenixcore:glucose 500", "phoenixcore:fructose 500")
                .duration(580)
                .EUt(GTValues.VA[GTValues.IV] / 2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "sugar_water")
                .inputFluids("phoenixcore:glucose 250", "phoenixcore:fructose 250", "gtceu:distilled_water 1000")
                .outputFluids("phoenixcore:sugar_water 15000")
                .duration(480)
                .EUt(GTValues.VA[GTValues.HV] / 2)
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "sugar_and_molasses_production")
                .inputFluids("phoenixcore:sugar_water 1000")
                .itemOutputs("64x minecraft:sugar")
                .outputFluids("phoenixcore:molasses 200")
                .duration(680)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "cream_from_milk")
                .inputFluids("minecraft:milk 1000")
                .outputFluids("phoenixcore:cream 250", "phoenixcore:skim_milk 750")
                .duration(600)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "mixer", "dough")
                .inputFluids("phoenixcore:skim_milk 144", "minecraft:water 500")
                .itemInputs("8x gtceu:wheat_dust", "gtceu:salt_dust", "minecraft:sugar")
                .itemOutputs("10x gtceu:dough")
                .duration(300)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "confectionery_fabricator", "honeytreat")
                .itemInputs("5x gtceu:dough", "2x minecraft:honeycomb")
                .inputFluids("phoenixcore:honey 15000", "phoenixcore:molasses 2500", "phoenixcore:cream 8000",
                        "phoenixcore:pollen_concentrate_fluid 6500", "phoenixcore:peanut_butter 12250")
                .itemOutputs("32x phoenixcore:honey_treat")
                .duration(650)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "macerator", "peanut_dust")
                .itemInputs("phoenixcore:roasted_peanut")
                .itemOutputs("phoenixcore:peanut_dust")
                .duration(40)
                .EUt(GTValues.VA[GTValues.LV])
                .save();

        PftRecipe.gt(provider, "mixer", "nut_butter_from_dust")
                .itemInputs("8x phoenixcore:peanut_dust", "2x minecraft:sugar")
                .inputFluids("minecraft:water 500", "gtceu:seed_oil 250")
                .outputFluids("phoenixcore:peanut_butter 500")
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "fluid_solidifier", "solidified_honey")
                .inputFluids("phoenixcore:honey 144")
                .notConsumable("gtceu:ingot_casting_mold")
                .itemOutputs("phoenixcore:honey_ingot")
                .duration(200)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "mixer", "cryo_zirconium_binding_solution")
                .inputFluids("phoenixcore:frost 288", "minecraft:water 2000")
                .itemInputs("2x phoenixcore:crystallized_fluxstone_dust", "1x gtceu:zirconium_dust")
                .outputFluids("phoenixcore:cryo_zirconium_binding_solution 2000")
                .duration(150)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "mixer", "cryo_graphite_binding_solution")
                .inputFluids("phoenixcore:frost 288", "minecraft:water 2000")
                .itemInputs("2x phoenixcore:crystallized_fluxstone_dust", "1x gtceu:graphite_dust")
                .outputFluids("phoenixcore:cryo_graphite_binding_solution 3000")
                .duration(175)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "reprocessing/irradiated_uranium_236_nuggets")
                .itemInputs("16x phoenixcore:irradiated_uranium_236_nugget")
                .inputFluids("gtceu:nitric_acid 1000")
                .itemOutputs("10x phoenixcore:spent_uranium_236_dust", "8x phoenixcore:plutonium_fission_ash_dust")
                .duration(300)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "reprocessing/depleted_plutonium_241_nuggets")
                .itemInputs("4x phoenixcore:depleted_plutonium_241_nugget")
                .inputFluids("gtceu:nitric_acid 500")
                .itemOutputs("2x gtceu:plutonium_241_dust", "1x phoenixcore:americium_241_dust")
                .duration(200)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "americium_hexafluoride_production")
                .itemInputs("8x phoenixcore:americium_241_dust")
                .inputFluids("gtceu:fluorine 600")
                .outputFluids("phoenixcore:americium_hexafluoride 1000")
                .duration(300)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "spent_uranium_reprocessing")
                .itemInputs("4x phoenixcore:depleted_uranium_dust")
                .inputFluids("gtceu:nitric_acid 1500")
                .itemOutputs("2x phoenixcore:uranium_236_dust", "1x gtceu:strontium_dust", "1x gtceu:caesium_dust")
                .outputFluids("phoenixcore:fission_products_fluid 500", "phoenixcore:radioactive_gas_mixture 100")
                .duration(120)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "reprocessing/thorium_rods_to_uranium_233")
                .itemInputs("16x phoenixcore:irradiated_thorium_nugget")
                .inputFluids("gtceu:nitric_acid 500")
                .itemOutputs("4x phoenixcore:uranium_233_dust", "12x phoenixcore:depleted_thorium_dust")
                .outputFluids("phoenixcore:fission_products_fluid 250")
                .duration(600)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "reprocessing/spent_uranium_235_rods")
                .itemInputs("16x phoenixcore:spent_uranium_235_nugget")
                .inputFluids("gtceu:nitric_acid 200")
                .itemOutputs("10x phoenixcore:depleted_uranium_dust", "6x gtceu:uranium_235_dust")
                .outputFluids("phoenixcore:fission_products_fluid 100")
                .duration(400)
                .EUt(GTValues.VA[GTValues.HV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "reprocessing/spent_uranium_233_rods")
                .itemInputs("16x phoenixcore:spent_uranium_233_nugget")
                .inputFluids("gtceu:nitric_acid 200")
                .itemOutputs("14x phoenixcore:uranium_233_dust", "4x phoenixcore:depleted_uranium_dust")
                .outputFluids("phoenixcore:fission_products_fluid 100")
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "distillation_tower", "fission_products_fluid_processing")
                .inputFluids("phoenixcore:fission_products_fluid 1000")
                .itemOutputs("2x phoenixcore:fissile_ash_dust")
                .outputFluids("phoenixcore:radioactive_gas_mixture 500")
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "radioactive_gas_mixture_separation")
                .inputFluids("phoenixcore:radioactive_gas_mixture 500", "gtceu:nitrogen 100")
                .outputFluids("gtceu:xenon 100", "gtceu:iodine 150", "gtceu:krypton 50",
                        "phoenixcore:inert_gas_waste 250")
                .duration(150)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "distillation_tower", "inert_gas_waste_recycling")
                .inputFluids("phoenixcore:inert_gas_waste 1000")
                .outputFluids("gtceu:nitrogen 300", "gtceu:oxygen 100", "gtceu:chlorine 500", "gtceu:argon 50")
                .duration(100)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "plutonium_fission_ash_dissolution")
                .itemInputs("4x phoenixcore:plutonium_fission_ash_dust")
                .inputFluids("gtceu:aqua_regia 500", "gtceu:hydrofluoric_acid 200")
                .outputFluids("phoenixcore:rhodium_palladium_solution 250",
                        "phoenixcore:technetium_strontium_solution 250", "phoenixcore:radioactive_sludge 100",
                        "phoenixcore:gaseous_fission_byproducts 100")
                .duration(600)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "rhodium_palladium_extraction")
                .inputFluids("phoenixcore:rhodium_palladium_solution 250", "gtceu:chlorine 100")
                .itemOutputs("1x gtceu:rhodium_dust", "1x gtceu:palladium_dust")
                .outputFluids("phoenixcore:acidic_waste 50")
                .duration(200)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "technetium_strontium_separation")
                .inputFluids("phoenixcore:technetium_strontium_solution 250")
                .itemOutputs("1x gtceu:technetium_dust", "1x gtceu:strontium_dust")
                .outputFluids("phoenixcore:radioactive_sludge 25")
                .duration(150)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "distillation_tower", "gaseous_fission_byproducts_separation")
                .inputFluids("phoenixcore:gaseous_fission_byproducts 1000")
                .outputFluids("gtceu:krypton 100", "gtceu:xenon 150", "gtceu:iodine 50",
                        "phoenixcore:trace_fission_gases 700")
                .duration(250)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "radioactive_sludge_processing")
                .inputFluids("phoenixcore:radioactive_sludge 1000", "minecraft:water 500")
                .itemOutputs("1x gtceu:rare_earth_dust", "1x phoenixcore:trace_actinides_dust")
                .duration(400)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "acidic_waste_processing")
                .inputFluids("phoenixcore:acidic_waste 1000", "gtceu:acetone 10")
                .itemOutputs("1x gtceu:calcium_dust")
                .outputFluids("gtceu:hydrofluoric_acid 100", "gtceu:nitric_acid 900")
                .duration(250)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "distillation_tower", "trace_fission_gases_processing")
                .inputFluids("phoenixcore:trace_fission_gases 1000")
                .outputFluids("gtceu:argon 50", "gtceu:neon 25", "gtceu:helium 25", "phoenixcore:inert_gas_waste 900")
                .duration(300)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "depleted_plutonium_recycling")
                .itemInputs("8x phoenixcore:depleted_plutonium_241_dust")
                .inputFluids("gtceu:nitric_acid 500", "gtceu:fluorine 100")
                .itemOutputs("2x gtceu:plutonium_241_dust")
                .duration(500)
                .EUt(GTValues.VA[GTValues.LuV])
                .save();

        PftRecipe.gt(provider, "centrifuge", "fissile_ash_processing")
                .itemInputs("2x phoenixcore:fissile_ash_dust")
                .inputFluids("gtceu:aqua_regia 100")
                .itemOutputs("1x gtceu:technetium_dust", "4x gtceu:rare_earth_dust")
                .outputFluids("phoenixcore:radioactive_sludge 50")
                .duration(300)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "electric_blast_furnace", "depleted_thorium_recycling")
                .itemInputs("4x phoenixcore:depleted_thorium_ingot")
                .circuit(2)
                .inputFluids("gtceu:fluorine 250")
                .itemOutputs("3x gtceu:thorium_dust")
                .outputFluids("phoenixcore:radioactive_sludge 250")
                .blastFurnaceTemp(1200)
                .duration(300)
                .EUt(GTValues.VA[GTValues.EV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "exotic_fission_concentrate_production")
                .itemInputs("2x gtceu:technetium_dust", "2x gtceu:strontium_dust", "4x gtceu:rare_earth_dust")
                .inputFluids("gtceu:iodine 500", "gtceu:fluoroantimonic_acid 250")
                .itemOutputs("12x phoenixcore:exotic_fission_concentrate_dust")
                .duration(300)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "exotic_fissile_materials_clump_production")
                .itemInputs("1x phoenixcore:exotic_fission_concentrate_dust")
                .inputFluids("phoenixcore:cryo_zirconium_binding_solution 1000",
                        "phoenixcore:cryo_graphite_binding_solution 1000")
                .itemOutputs("4x phoenixcore:exotic_fissile_materials_clump_dust")
                .duration(400)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "assembler", "active_transformer")
                .itemInputs("gtceu:iv_transformer_16a", "8x #gtceu:circuits/iv",
                        "8x phoenixcore:superconductive_honey_double_wire", "10x gtceu:hpic_chip")
                .inputFluids("gtceu:pcb_coolant 1000")
                .itemOutputs("gtceu:active_transformer")
                .duration(450)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "assembler", "comb_decanter_recipe_assembler")
                .itemInputs("4x minecraft:honey_block", "kubejs:conductive_honey_coil",
                        "2x gtceu:samarium_iron_arsenic_oxide_double_wire", "2x gtceu:iv_field_generator",
                        "2x #gtceu:circuits/iv")
                .inputFluids("gtceu:soldering_alloy 5044")
                .itemOutputs("phoenixcore:comb_decanter")
                .EUt(GTValues.VA[GTValues.IV])
                .duration(400)
                .save();

        PftRecipe.gt(provider, "assembler", "apis_progenitor_recipe_assembler")
                .itemInputs("4x phoenixcore:honey_treat", "4x #minecraft:flowers", "#gtceu:circuits/iv",
                        "gtceu:iv_machine_hull", "32x gtceu:fine_platinum_wire")
                .inputFluids("phoenixcore:honey 1000")
                .itemOutputs("phoenixcore:apis_progenitor")
                .EUt(GTValues.VA[GTValues.IV])
                .duration(600)
                .save();

        PftRecipe.gt(provider, "assembler", "swarm_nurturer_recipe_assembler")
                .itemInputs("gtceu:iv_machine_hull", "#gtceu:circuits/iv", "2x gtceu:stainless_steel_frame",
                        "12x phoenixcore:source_imbued_titanium_screw", "gtceu:rtm_alloy_coil_block")
                .inputFluids("gtceu:vanadium_gallium 500")
                .itemOutputs("phoenixcore:swarm_nurturer")
                .EUt(GTValues.VA[GTValues.IV])
                .duration(800)
                .save();

        PftRecipe.gt(provider, "assembler", "simulated_colony_recipe_assembler")
                .itemInputs("2x phoenixcore:source_imbued_titanium_gear", "32x #minecraft:flowers", "12x #c:dyes",
                        "3x #gtceu:circuits/iv", "gtceu:iv_machine_hull", "16x phoenixcore:honey_treat")
                .inputFluids("phoenixcore:sugar_water 1000")
                .itemOutputs("phoenixcore:simulated_colony")
                .EUt(GTValues.VA[GTValues.IV])
                .duration(900)
                .save();

        PftRecipe.gt(provider, "assembler", "breeder_reactor_controller")
                .itemInputs("phoenixcore:pressurized_fission_reactor",
                        "2x phoenixcore:void_touched_tungsten_steel_rotor", "4x #gtceu:circuits/iv",
                        "2x phoenixcore:zircalloy_gear", "2x gtceu:iv_machine_hull", "16x gtceu:uranium_235_rod")
                .inputFluids("phoenixcore:cryo_zirconium_binding_solution 6000")
                .itemOutputs("phoenixcore:high_performance_breeder_reactor")
                .duration(800)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "assembler", "iv_confectionery_fabricator")
                .itemInputs("4x phoenixcore:void_touched_tungsten_steel_rotor", "1x minecraft:honeycomb",
                        "2x #gtceu:circuits/iv", "gtceu:iv_machine_hull", "1x gtceu:iv_sensor")
                .inputFluids("phoenixcore:honey 1700")
                .itemOutputs("gtceu:iv_confectionery_fabricator")
                .EUt(GTValues.VA[GTValues.IV])
                .duration(600)
                .save();

        PftRecipe.gt(provider, "assembler", "fully_charged_mob_essence_capsule_recipe")
                .itemInputs("kubejs:full_passive_mob_essence_canister", "kubejs:full_neutral_mob_essence_canister",
                        "kubejs:full_hostile_mob_essence_canister")
                .itemOutputs("kubejs:fully_charged_mob_essence_capsule")
                .duration(400)
                .EUt(GTValues.VA[GTValues.IV])
                .save();

        PftRecipe.gt(provider, "assembler", "graphite_moderator")
                .itemInputs("gtceu:hv_machine_casing", "16x gtceu:graphite_dust")
                .itemOutputs("phoenixcore:graphite_moderator")
                .duration(200)
                .EUt(480)
                .save();

        PftRecipe.gt(provider, "assembler", "beryllium_moderator")
                .itemInputs("gtceu:ev_machine_casing", "16x gtceu:beryllium_dust")
                .itemOutputs("phoenixcore:beryllium_moderator")
                .duration(200)
                .EUt(1920)
                .save();

        PftRecipe.gt(provider, "assembler", "heavy_water_moderator")
                .itemInputs("gtceu:iv_machine_casing", "2x gtceu:tungsten_steel_large_fluid_pipe")
                .inputFluids("minecraft:water 1000")
                .itemOutputs("phoenixcore:heavy_water_moderator")
                .duration(300)
                .EUt(7680)
                .save();

        PftRecipe.gt(provider, "assembler", "niobium_sic_moderator")
                .itemInputs("gtceu:luv_machine_casing", "4x gtceu:niobium_nitride_plate",
                        "8x phoenixcore:niobium_modified_silicon_carbide_rod")
                .itemOutputs("phoenixcore:niobium_sic_moderator")
                .duration(600)
                .EUt(30720)
                .save();
    }
}
