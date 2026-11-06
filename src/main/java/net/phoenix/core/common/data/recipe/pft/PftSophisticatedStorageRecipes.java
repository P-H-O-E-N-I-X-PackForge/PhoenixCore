package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftSophisticatedStorageRecipes {

    private PftSophisticatedStorageRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "backpack_pftsophisticatedstoragerecipes", "sophisticatedbackpacks:backpack",
                new String[] { " N ", "ICI", "N N" }, "N", "#c:leathers", "I", "minecraft:iron_ingot", "C",
                "#c:chests/wooden");

        tierUpgrades(provider);

        PftCrafting.shaped(provider, "controller_pftsophisticatedstoragerecipes", "sophisticatedstorage:controller",
                new String[] { "III", "CDC", "IEI" }, "I", "#c:plates/steel", "C", "#gtceu:circuits/lv", "D",
                "#sophisticatedstorage:base_tier_wooden_storage", "E", "#c:storage_blocks/diamond");

        PftCrafting.shaped(provider, "controller_pftsophisticatedstoragerecipes_2", "sophisticatedstorage:controller",
                new String[] { "III", "CDC", "IEI" }, "I", "#c:plates/steel", "C", "#gtceu:circuits/lv", "D",
                "#sophisticatedstorage:base_tier_wooden_storage", "E", "#c:storage_blocks/emerald");

        PftCrafting.shaped(provider, "storage_input_pftsophisticatedstoragerecipes",
                "sophisticatedstorage:storage_input", new String[] { "III", "CDC", "IEI" }, "I", "#c:plates/steel", "C",
                "#gtceu:circuits/lv", "D", "#sophisticatedstorage:base_tier_wooden_storage", "E",
                "#c:storage_blocks/gold");

        PftCrafting.shaped(provider, "storage_output_pftsophisticatedstoragerecipes",
                "sophisticatedstorage:storage_output", new String[] { "IEI", "CDC", "III" }, "I", "#c:plates/steel",
                "C", "#gtceu:circuits/lv", "D", "#sophisticatedstorage:base_tier_wooden_storage", "E",
                "#c:storage_blocks/gold");

        PftCrafting.shaped(provider, "storage_io_pftsophisticatedstoragerecipes", "sophisticatedstorage:storage_io",
                new String[] { "III", "SDT", "III" }, "I", "#c:plates/steel", "S", "sophisticatedstorage:storage_input",
                "T", "sophisticatedstorage:storage_output", "D", "#sophisticatedstorage:base_tier_wooden_storage");

        stackUpgrades(provider);

        PftCrafting.shaped(provider, "hopper_upgrade_pftsophisticatedstoragerecipes",
                "4x sophisticatedstorage:hopper_upgrade", new String[] { " H ", "IUI", "RRR" }, "U",
                "sophisticatedstorage:upgrade_base", "I", "minecraft:iron_ingot", "H", "minecraft:hopper", "R",
                "gtceu:red_alloy_plate");

        PftCrafting.shaped(provider, "advanced_hopper_upgrade_pftsophisticatedstoragerecipes",
                "sophisticatedstorage:advanced_hopper_upgrade", new String[] { " R ", "IUI", "RRR" }, "U",
                "sophisticatedstorage:upgrade_base", "I", "gtceu:lv_conveyor_module", "R", "gtceu:steel_plate");
    }

    private static final String[][] MATERIALS = { { "", null, null }, { "iron_", "fiery_bronze", "lead" },
            { "gold_", "aurum_steel", "tin" }, { "diamond_", "aluminfrost", "copper" },
            { "netherite_", "stainless_steel", "gold" } };

    private static void tierUpgrades(RecipeOutput provider) {
        for (int to = 1; to < MATERIALS.length; to++) {
            for (int from = 0; from < to; from++) {
                String fromName = from == 0 ? "basic_" : MATERIALS[from][0];
                String prevName = to - 1 == 0 ? "basic_" : MATERIALS[to - 1][0];
                String toName = MATERIALS[to][0];
                String center = fromName.equals(prevName) ? "minecraft:redstone_torch" :
                        "sophisticatedstorage:" + fromName + "to_" + prevName + "tier_upgrade";
                PftCrafting.shaped(provider, fromName + "to_" + toName + "tier_upgrade_pft",
                        "sophisticatedstorage:" + fromName + "to_" + toName + "tier_upgrade",
                        new String[] { "IPI", "ICI", "IPI" }, "I", "#c:ingots/" + MATERIALS[to][1], "P",
                        "#c:plates/" + MATERIALS[to][2], "C", center);
            }
        }
    }

    private static void stackUpgrades(RecipeOutput provider) {
        String[][] stack = { { "stack_upgrade_tier_1", "phoenixcore:aurum_steel", "upgrade_base" },
                { "stack_upgrade_tier_2", "phoenixcore:frost_reinforced_stained_steel", "stack_upgrade_tier_1" },
                { "stack_upgrade_tier_3", "phoenixcore:void_touched_tungsten_steel", "stack_upgrade_tier_2" },
                { "stack_upgrade_tier_4", "phoenixcore:osmiridium", "stack_upgrade_tier_3" } };
        for (String mod : new String[] { "sophisticatedstorage", "sophisticatedbackpacks" }) {
            for (String[] m : stack) {
                PftCrafting.shaped(provider, mod + "_" + m[0] + "_pft", mod + ":" + m[0],
                        new String[] { "III", "IUI", "BIB" }, "I", m[1] + "_plate", "B", m[1] + "_gear", "U",
                        mod + ":" + m[2]);
            }
        }
        PftCrafting.shaped(provider, "stack_upgrade_tier_5_pft", "sophisticatedstorage:stack_upgrade_tier_5",
                new String[] { "III", "IUI", "BIB" }, "I", "gtceu:naquadah_alloy_plate", "B",
                "gtceu:naquadah_alloy_gear", "U", "sophisticatedstorage:stack_upgrade_tier_4");
    }
}
