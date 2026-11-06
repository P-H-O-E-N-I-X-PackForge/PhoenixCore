package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.common.data.PftCrafting;

public final class PftAe2TerminalRecipes {

    private PftAe2TerminalRecipes() {}

    public static void init(RecipeOutput provider) {
        PftCrafting.shaped(provider, "wireless_terminal_pftae2terminalrecipes", "ae2:wireless_terminal",
                new String[] { "wRd", "CTC", "KLK" }, "w", "#c:tools/wrench", "R", "ae2:wireless_receiver", "d",
                "#c:tools/screwdriver", "C", "#gtceu:circuits/hv", "T", "ae2:terminal", "K", "ae2:fluix_glass_cable",
                "L", "gtceu:lapotron_crystal");

        PftCrafting.shaped(provider, "wireless_crafting_terminal_pftae2terminalrecipes",
                "ae2:wireless_crafting_terminal", new String[] { "TC ", "Kd ", "   " }, "d", "#c:tools/screwdriver",
                "T", "ae2:wireless_terminal", "C", "crafting_table", "K", "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "storage_bus_pftae2terminalrecipes", "ae2:storage_bus", new String[] { "IPC" },
                "I", "ae2:interface", "P", "gtceu:lv_electric_piston", "C", "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "import_bus_pftae2terminalrecipes", "ae2:import_bus", new String[] { "IPC" }, "I",
                "ae2:annihilation_core", "P", "gtceu:lv_robot_arm", "C", "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "export_bus_pftae2terminalrecipes", "ae2:export_bus", new String[] { "IPC" }, "I",
                "ae2:formation_core", "P", "gtceu:lv_robot_arm", "C", "ae2:fluix_glass_cable");

        PftCrafting.shaped(provider, "formation_plane_pftae2terminalrecipes", "ae2:formation_plane",
                new String[] { "EFC", "PPP" }, "E", "gtceu:lv_electric_piston", "F", "ae2:formation_core", "C",
                "#c:gears/fluix", "P", "#c:plates/steel");

        PftCrafting.shaped(provider, "annihilation_plane_pftae2terminalrecipes", "ae2:annihilation_plane",
                new String[] { "EFC", "PPP" }, "E", "gtceu:lv_electric_piston", "F", "ae2:annihilation_core", "C",
                "#c:gears/fluix", "P", "#c:plates/steel");

        PftCrafting.shaped(provider, "terminal_pftae2terminalrecipes", "ae2:terminal",
                new String[] { "BCB", "xPd", "CDC" }, "B", "#c:bolts/tin_alloy", "C", "#gtceu:circuits/lv", "x",
                "#c:tools/wire_cutter", "P", "ae2:semi_dark_monitor", "d", "#c:tools/screwdriver", "D",
                "#c:double_plates/steel");

        PftCrafting.shaped(provider, "terminal_pftae2terminalrecipes_2", "ae2:terminal", new String[] { "PB ", "Cd " },
                "B", "ae2:calculation_processor", "C", "ae2:logic_processor", "P", "ae2:semi_dark_monitor", "d",
                "#c:tools/screwdriver");

        PftCrafting.shaped(provider, "crafting_terminal_pftae2terminalrecipes", "ae2:crafting_terminal",
                new String[] { "PB ", "Cd " }, "P", "ae2:terminal", "B", "crafting_table", "C",
                "ae2:calculation_processor", "d", "#c:tools/screwdriver");

        PftCrafting.shaped(provider, "pattern_encoding_terminal_pftae2terminalrecipes", "ae2:pattern_encoding_terminal",
                new String[] { "PB ", "Cd " }, "P", "ae2:terminal", "B", "ae2:blank_pattern", "C",
                "ae2:engineering_processor", "d", "#c:tools/screwdriver");

        PftCrafting.shaped(provider, "pattern_access_terminal_pftae2terminalrecipes", "ae2:pattern_access_terminal",
                new String[] { "PB ", "Cd " }, "P", "ae2:terminal", "B", "gtceu:polyvinyl_chloride_plate", "C",
                "ae2:fluix_glass_cable", "d", "#c:tools/screwdriver");

        PftCrafting.shaped(provider, "me_p2p_tunnel_pftae2terminalrecipes", "2x ae2:me_p2p_tunnel",
                new String[] { " P ", "PEP", "PCP" }, "P", "#c:plates/tungsten_steel", "E", "ae2:engineering_processor",
                "C", "ae2:fluix_glass_cable");
    }
}
