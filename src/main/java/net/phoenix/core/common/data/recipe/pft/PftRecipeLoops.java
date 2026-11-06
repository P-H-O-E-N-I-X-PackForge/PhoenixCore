package net.phoenix.core.common.data.recipe.pft;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftRecipe;

public final class PftRecipeLoops {

    private PftRecipeLoops() {}

    private static final String[][] TREES = {
            { "minecraft:oak_sapling", "minecraft:oak_log" },
            { "minecraft:spruce_sapling", "minecraft:spruce_log" },
            { "minecraft:birch_sapling", "minecraft:birch_log" },
            { "minecraft:jungle_sapling", "minecraft:jungle_log" },
            { "minecraft:acacia_sapling", "minecraft:acacia_log" },
            { "minecraft:dark_oak_sapling", "minecraft:dark_oak_log" },
            { "minecraft:mangrove_propagule", "minecraft:mangrove_log" },
            { "minecraft:cherry_sapling", "minecraft:cherry_log" },
            { "forbidden_arcanus:aurum_sapling", "forbidden_arcanus:aurum_log" },
    };

    private static final String[][] PLANTS = {
            { "minecraft:pumpkin_seeds", "minecraft:pumpkin", "6" },
            { "minecraft:beetroot_seeds", "minecraft:beetroot", "16" },
            { "minecraft:sweet_berries", "minecraft:sweet_berries", "16" },
            { "minecraft:glow_berries", "minecraft:glow_berries", "8" },
            { "minecraft:wheat_seeds", "minecraft:wheat", "16" },
            { "minecraft:melon_seeds", "minecraft:melon", "6" },
            { "minecraft:carrot", "minecraft:carrot", "12" },
            { "minecraft:sugar_cane", "minecraft:sugar_cane", "12" },
            { "minecraft:kelp", "minecraft:kelp", "12" },
            { "minecraft:cactus", "minecraft:cactus", "12" },
            { "minecraft:brown_mushroom", "minecraft:brown_mushroom", "12" },
            { "minecraft:red_mushroom", "minecraft:red_mushroom", "12" },
            { "minecraft:nether_wart", "minecraft:nether_wart", "12" },
            { "minecraft:bamboo", "minecraft:bamboo", "16" },
            { "minecraft:dandelion", "minecraft:dandelion", "12" },
            { "minecraft:poppy", "minecraft:poppy", "12" },
            { "minecraft:blue_orchid", "minecraft:blue_orchid", "12" },
            { "minecraft:allium", "minecraft:allium", "12" },
            { "minecraft:azure_bluet", "minecraft:azure_bluet", "12" },
            { "minecraft:red_tulip", "minecraft:red_tulip", "12" },
            { "minecraft:orange_tulip", "minecraft:orange_tulip", "12" },
            { "minecraft:white_tulip", "minecraft:white_tulip", "12" },
            { "minecraft:pink_tulip", "minecraft:pink_tulip", "12" },
            { "minecraft:oxeye_daisy", "minecraft:oxeye_daisy", "12" },
            { "minecraft:cornflower", "minecraft:cornflower", "12" },
            { "minecraft:lily_of_the_valley", "minecraft:lily_of_the_valley", "12" },
            { "minecraft:wither_rose", "minecraft:wither_rose", "12" },
            { "minecraft:sunflower", "minecraft:sunflower", "8" },
            { "minecraft:lilac", "minecraft:lilac", "8" },
            { "minecraft:rose_bush", "minecraft:rose_bush", "8" },
            { "minecraft:peony", "minecraft:peony", "8" },
    };

    public static void init(RecipeOutput provider) {
        greenhouse(provider);
        fuelPellets(provider);
        essenceCanisters(provider);
        reactorParts(provider);
        woodUnification(provider);
    }

    private static void greenhouse(RecipeOutput provider) {
        for (String[] tree : TREES) {
            String sapling = tree[0];
            String log = tree[1];
            PftRecipe.gt(provider, "greenhouse", sapling)
                    .notConsumable(sapling)
                    .inputFluids("water 1000")
                    .itemOutputs("64x " + log, "6x " + sapling)
                    .duration(600)
                    .EUt(32)
                    .circuit(1)
                    .save();
            PftRecipe.gt(provider, "greenhouse", sapling + "_fertiliser")
                    .notConsumable(sapling)
                    .itemInputs("4x gtceu:fertilizer")
                    .inputFluids("water 1000")
                    .itemOutputs("64x " + log, "64x " + log, "12x " + sapling)
                    .duration(900)
                    .EUt(40)
                    .circuit(2)
                    .save();
        }
        for (String[] plant : PLANTS) {
            String seed = plant[0];
            String crop = plant[1];
            int count = Integer.parseInt(plant[2]);
            PftRecipe.gt(provider, "greenhouse", seed)
                    .notConsumable(seed)
                    .inputFluids("water 1000")
                    .itemOutputs(count + "x " + crop)
                    .duration(600)
                    .EUt(32)
                    .circuit(1)
                    .save();
            PftRecipe.gt(provider, "greenhouse", seed + "_fertilizer")
                    .notConsumable(seed)
                    .itemInputs("4x gtceu:fertilizer")
                    .inputFluids("water 1000")
                    .itemOutputs(2 * count + "x " + crop)
                    .duration(900)
                    .EUt(32)
                    .circuit(2)
                    .save();
        }
    }

    private static void fuelPellets(RecipeOutput provider) {
        String[][] pellets = {
                { "thorium", "9x gtceu:thorium_nugget", "phoenixcore:cryo_graphite_binding_solution 1000", "mv" },
                { "uranium_235", "9x gtceu:uranium_235_nugget", "phoenixcore:cryo_zirconium_binding_solution 1000",
                        "mv" },
                { "uranium_233", "9x phoenixcore:uranium_233_nugget",
                        "phoenixcore:cryo_zirconium_binding_solution 1000", "mv" },
                { "plutonium_241", "9x gtceu:plutonium_241_nugget", "phoenixcore:cryo_zirconium_binding_solution 1000",
                        "iv" },
                { "uranium_236", "9x phoenixcore:uranium_236_nugget", "phoenixcore:cryo_graphite_binding_solution 1000",
                        "iv" },
        };
        for (String[] p : pellets) {
            long volt = p[3].equals("mv") ? GTValues.VA[GTValues.MV] : GTValues.VA[GTValues.IV];
            PftRecipe.gt(provider, "large_chemical_reactor", p[0] + "_fuel_pellet_crafting")
                    .itemInputs(p[1])
                    .inputFluids(p[2])
                    .itemOutputs("9x phoenixcore:" + p[0].replace("uranium", "u") + "_fuel_pellet")
                    .duration(100)
                    .EUt(volt)
                    .save();
        }
    }

    private static void mixCanister(RecipeOutput provider, String id, String base, String[] items, String[] fluids,
                                    int duration) {
        String[] inputs = new String[items.length + 1];
        inputs[0] = base;
        for (int i = 0; i < items.length; i++) inputs[i + 1] = "32x " + items[i];
        PftRecipe.gt(provider, "mixer", id)
                .itemInputs((Object[]) inputs)
                .inputFluids((Object[]) fluids)
                .itemOutputs("phoenixcore:" + id)
                .duration(duration)
                .EUt(GTValues.LV)
                .save();
    }

    private static void assembleCanister(RecipeOutput provider, String type, String[] items, String[] fluids) {
        String[] inputs = new String[4 + items.length];
        inputs[0] = "2x phoenixcore:dense_source_imbued_titanium_plate";
        inputs[1] = "16x phoenixcore:source_titanium_filament_double_wire";
        inputs[2] = "16x phoenixcore:superconductive_honey_double_wire";
        inputs[3] = "16x phoenixcore:extremely_modified_space_grade_steel_double_wire";
        System.arraycopy(items, 0, inputs, 4, items.length);
        PftRecipe.gt(provider, "assembler", "empty_" + type + "_mob_essence_canister")
                .itemInputs((Object[]) inputs)
                .inputFluids((Object[]) fluids)
                .itemOutputs("phoenixcore:empty_" + type + "_mob_essence_canister")
                .duration(300)
                .EUt(GTValues.VA[GTValues.IV])
                .save();
    }

    private static void essenceCanisters(RecipeOutput provider) {
        String[] hostileFluids = { "gtceu:hydrofluoric_acid 250", "gtceu:polytetrafluoroethylene 144" };
        String[] passiveFluids = { "phoenixcore:honey 250", "phoenixcore:cream 144" };
        String[] neutralFluids = { "phoenixcore:resonant_ender 144", "gtceu:liquid_ender_air 100" };

        mixCanister(provider, "midway_hostile_mob_essence_canister", "phoenixcore:empty_hostile_mob_essence_canister",
                new String[] { "minecraft:rotten_flesh", "minecraft:bone", "minecraft:arrow", "minecraft:gunpowder",
                        "minecraft:string" },
                hostileFluids, 100);
        mixCanister(provider, "full_hostile_mob_essence_canister", "phoenixcore:midway_hostile_mob_essence_canister",
                new String[] { "minecraft:spider_eye", "minecraft:slime_ball", "minecraft:ghast_tear",
                        "minecraft:blaze_rod", "minecraft:wither_skeleton_skull" },
                hostileFluids, 200);
        mixCanister(provider, "midway_passive_mob_essence_canister", "phoenixcore:empty_passive_mob_essence_canister",
                new String[] { "minecraft:feather", "minecraft:leather", "minecraft:honey_bottle",
                        "minecraft:milk_bucket" },
                passiveFluids, 100);
        mixCanister(provider, "full_passive_mob_essence_canister", "phoenixcore:midway_passive_mob_essence_canister",
                new String[] { "minecraft:bone_meal", "minecraft:white_wool", "minecraft:egg",
                        "minecraft:rabbit_hide" },
                passiveFluids, 200);
        mixCanister(provider, "midway_neutral_mob_essence_canister", "phoenixcore:empty_neutral_mob_essence_canister",
                new String[] { "minecraft:slime_ball", "minecraft:ender_pearl", "minecraft:spider_eye",
                        "minecraft:ink_sac" },
                neutralFluids, 100);
        mixCanister(provider, "full_neutral_mob_essence_canister", "phoenixcore:midway_neutral_mob_essence_canister",
                new String[] { "minecraft:iron_ingot", "minecraft:sugar", "minecraft:glowstone_dust",
                        "minecraft:emerald" },
                neutralFluids, 200);

        assembleCanister(provider, "hostile", new String[] { "gtceu:quantum_star", "gtceu:active_transformer" },
                new String[] { "phoenixcore:void_touched_tungsten_steel 288", "phoenixcore:honey 500" });
        assembleCanister(provider, "passive", new String[] { "minecraft:golden_apple", "minecraft:bee_nest" },
                new String[] { "phoenixcore:honey 1000", "phoenixcore:cream 288" });
        assembleCanister(provider, "neutral", new String[] { "minecraft:ender_pearl", "phoenixcore:swarm_nurturer" },
                new String[] { "phoenixcore:resonant_ender 288", "gtceu:liquid_ender_air 200" });
    }

    private static void reactorParts(RecipeOutput provider) {
        String[][] rods = {
                { "t1_fuel_rod", "hv", "gtceu:uranium_dust", "phoenixcore:frost_reinforced_stained_steel", "480" },
                { "t2_fuel_rod", "ev", "gtceu:uranium_dust", "phoenixcore:source_imbued_titanium", "1920" },
                { "t3_fuel_rod", "iv", "phoenixcore:u235_fuel_pellet", "phoenixcore:source_imbued_titanium", "7680" },
                { "t4_fuel_rod", "luv", "phoenixcore:plutonium_241_fuel_pellet", "phoenixcore:resonant_rhodium_alloy",
                        "30720" },
                { "t5_fuel_rod", "zpm", "phoenixcore:u236_fuel_pellet", "phoenixcore:advanced_quin_naquadian_alloy",
                        "122880" },
        };
        for (String[] r : rods) {
            PftRecipe.gt(provider, "assembler", r[0])
                    .itemInputs("gtceu:" + r[1] + "_machine_casing", "4x " + r[3] + "_plate", r[2])
                    .itemOutputs("phoenixcore:" + r[0])
                    .duration(200)
                    .EUt(Long.parseLong(r[4]))
                    .save();
        }

        String[][] blankets = {
                { "thorium", "hv", "2x gtceu:lead_plate", "phoenixcore:thorium_fuel_pellet", "480", "400" },
                { "uranium", "ev", "2x phoenixcore:source_imbued_titanium_plate", "phoenixcore:u236_fuel_pellet",
                        "1920", "400" },
                { "neptunium", "iv", "phoenixcore:frost_reinforced_stained_steel_plate", "gtceu:lead_dust", "7680",
                        "400" },
                { "plutonium", "luv", "2x phoenixcore:resonant_rhodium_alloy_plate", "gtceu:uranium_dust", "30720",
                        "600" },
                { "americium", "zpm", "2x phoenixcore:advanced_quin_naquadian_alloy_plate",
                        "phoenixcore:u236_fuel_pellet", "122880", "800" },
        };
        for (String[] b : blankets) {
            PftRecipe.gt(provider, "assembler", b[0] + "_blanket")
                    .itemInputs("gtceu:" + b[1] + "_machine_casing", b[2], b[3])
                    .itemOutputs("phoenixcore:" + b[0] + "_blanket")
                    .duration(Integer.parseInt(b[5]))
                    .EUt(Long.parseLong(b[4]))
                    .save();
        }

        String[][] coolers = {
                { "cooler_basic", "hv", "gtceu:tin", "480" },
                { "cooler_ev", "ev", "phoenixcore:source_imbued_titanium", "1920" },
                { "cooler_iv", "iv", "phoenixcore:dense_source_imbued_titanium", "7680" },
                { "cooler_luv", "luv", "phoenixcore:resonant_rhodium_alloy", "30720" },
        };
        for (String[] c : coolers) {
            PftRecipe.gt(provider, "assembler", c[0])
                    .itemInputs("gtceu:" + c[1] + "_machine_casing", "4x " + c[2] + "_plate",
                            "4x gtceu:copper_single_wire")
                    .itemOutputs("phoenixcore:" + c[0])
                    .duration(150)
                    .EUt(Long.parseLong(c[3]))
                    .save();
        }
    }

    private static void woodUnification(RecipeOutput provider) {
        for (String wood : new String[] { "warped", "crimson", "mangrove", "cherry" }) {
            PftRecipe.gt(provider, "assembler", wood + "_fence")
                    .itemInputs(wood + "_planks", "stick")
                    .itemOutputs(wood + "_fence")
                    .circuit(1)
                    .duration(100)
                    .EUt(4)
                    .save();
            PftRecipe.gt(provider, "assembler", wood + "_fence_gate")
                    .itemInputs("2x " + wood + "_planks", "2x stick")
                    .itemOutputs(wood + "_fence_gate")
                    .circuit(2)
                    .duration(100)
                    .EUt(4)
                    .save();
            PftRecipe.gt(provider, "assembler", wood + "_stairs")
                    .itemInputs("6x " + wood + "_planks")
                    .itemOutputs("4x " + wood + "_stairs")
                    .circuit(1)
                    .duration(100)
                    .EUt(1)
                    .save();
        }
    }
}
