package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;
import net.phoenix.core.common.data.PftRecipe;

public final class PftCcTweakedRecipes {

    private PftCcTweakedRecipes() {}

    private static String cc(String item) {
        return "computercraft:" + item;
    }

    private static String ap(String item) {
        return "advancedperipherals:" + item;
    }

    private static String gt(String item) {
        return "gtceu:" + item;
    }

    private static void assembler(RecipeOutput provider, String id, int duration, long eut, String[] inputs,
                                  String... outputs) {
        PftRecipe.gt(provider, "assembler", id)
                .itemInputs((Object[]) inputs)
                .itemOutputs((Object[]) outputs)
                .duration(duration)
                .EUt(eut)
                .save();
    }

    private static void peripheral(RecipeOutput provider, String name, String... extra) {
        String[] inputs = new String[1 + extra.length];
        inputs[0] = ap("peripheral_casing");
        System.arraycopy(extra, 0, inputs, 1, extra.length);
        assembler(provider, "assembler/ap/" + name, 100, 120, inputs, ap(name));
    }

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "computer_normal_cc", cc("computer_normal"), new String[] { "AAA", "ABA", "ACA" },
                "A", "#c:plates/aurum_steel", "B", gt("computer_monitor_cover"), "C", "#gtceu:circuits/mv");
        PftCrafting.shaped(provider, "computer_advanced_cc", cc("computer_advanced"),
                new String[] { "AAA", "ABA", "ACA" }, "A", "#c:plates/aluminfrost", "B", gt("computer_monitor_cover"),
                "C",
                "#gtceu:circuits/hv");
        PftCrafting.shaped(provider, "monitor_normal_cc", "4x " + cc("monitor_normal"),
                new String[] { "AAA", "ABA", "AAA" }, "A", "#c:plates/aurum_steel", "B", gt("computer_monitor_cover"));
        PftCrafting.shaped(provider, "monitor_advanced_cc", "4x " + cc("monitor_advanced"),
                new String[] { "AAA", "ABA", "AAA" }, "A", "#c:plates/aluminfrost", "B", gt("computer_monitor_cover"));
        PftCrafting.shaped(provider, "wired_modem_cc", cc("wired_modem"), new String[] { "CA", "A " }, "A", cc("cable"),
                "C", gt("lv_machine_hull"));
        PftCrafting.shapeless(provider, "wired_modem_full_cc", cc("wired_modem_full"), cc("wired_modem"));

        assembler(provider, "assembler/cc/cable", 20, 7,
                new String[] { gt("copper_single_wire"), gt("polyethylene_plate") },
                "4x " + cc("cable"));
        assembler(provider, "assembler/cc/monitor_advanced_upgrade", 100, 30,
                new String[] { "4x " + cc("monitor_normal"), "8x #c:plates/aluminfrost" },
                "4x " + cc("monitor_advanced"));
        assembler(provider, "assembler/cc/turtle_normal", 100, 30,
                new String[] { cc("computer_normal"), gt("lv_sensor") },
                cc("turtle_normal"));
        assembler(provider, "assembler/cc/turtle_advanced", 100, 30,
                new String[] { cc("computer_advanced"), gt("lv_sensor") }, cc("turtle_advanced"));
        assembler(provider, "assembler/cc/wireless_modem_normal", 100, 30,
                new String[] { cc("wired_modem"), gt("mv_sensor"), gt("mv_emitter") }, cc("wireless_modem_normal"));
        assembler(provider, "assembler/cc/wireless_modem_advanced", 100, 30,
                new String[] { cc("wired_modem"), gt("hv_sensor"), gt("hv_emitter") }, cc("wireless_modem_advanced"));
        assembler(provider, "assembler/cc/pocket_computer_normal", 100, 30,
                new String[] { cc("computer_normal"), gt("mv_sensor"), gt("mv_emitter") },
                cc("pocket_computer_normal"));
        assembler(provider, "assembler/cc/pocket_computer_advanced", 100, 30,
                new String[] { cc("computer_advanced"), gt("hv_sensor"), gt("hv_emitter") },
                cc("pocket_computer_advanced"));

        PftCrafting.shaped(provider, "disk_drive_cc", cc("disk_drive"), new String[] { "ABA", "ACA", "ABA" }, "A",
                "#c:plates/aurum_steel", "B", cc("cable"), "C", gt("lv_machine_hull"));
        PftCrafting.shaped(provider, "speaker_cc", cc("speaker"), new String[] { "ABA", "ACA", "ADA" }, "A",
                "#c:plates/aurum_steel", "B", "minecraft:note_block", "C", gt("lv_machine_hull"), "D", cc("cable"));
        PftCrafting.shaped(provider, "printer_cc", cc("printer"), new String[] { "ABA", "ACA", "ADA" }, "A",
                "#c:plates/aurum_steel", "B", "#c:dyes", "C", gt("lv_machine_hull"), "D", cc("cable"));

        assembler(provider, "assembler/ap/peripheral_casing", 100, 120,
                new String[] { gt("hv_machine_hull"), "16x " + cc("cable"), gt("hv_sensor"), gt("hv_emitter") },
                ap("peripheral_casing"));
        peripheral(provider, "inventory_manager", gt("hv_conveyor_module"), gt("hv_sensor"), gt("hv_emitter"));
        peripheral(provider, "energy_detector", gt("energy_detector_cover"));
        peripheral(provider, "environment_detector", gt("portable_scanner"));
        peripheral(provider, "chat_box", cc("speaker"));
        peripheral(provider, "player_detector", "minecraft:sculk_sensor");
        peripheral(provider, "redstone_integrator", gt("ender_redstone_link_cover"));
        peripheral(provider, "geo_scanner", gt("prospector.hv"));
        assembler(provider, "assembler/ap/block_reader/full", 100, 120,
                new String[] { ap("peripheral_casing"), cc("wired_modem_full") }, ap("block_reader"));
        assembler(provider, "assembler/ap/block_reader", 100, 120,
                new String[] { ap("peripheral_casing"), cc("wired_modem") }, ap("block_reader"));
        peripheral(provider, "nbt_storage", cc("disk_drive"));
        PftCrafting.shaped(provider, "chunk_controller_ap", ap("chunk_controller"),
                new String[] { "ABA", "BCB", "ABA" },
                "A", "#c:plates/source_embued_titanium", "B", "minecraft:redstone", "C", gt("quantum_eye"));
    }
}
