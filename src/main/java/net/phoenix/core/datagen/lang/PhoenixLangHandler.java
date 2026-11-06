package net.phoenix.core.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class PhoenixLangHandler {

    public static void init(RegistrateLangProvider provider) {
        provider.add("metaarmor.message.step_assist.disabled", "PhoenixTech™ Suite: StepAssist Disabled");
        provider.add("metaarmor.message.step_assist.enabled", "PhoenixTech™ Suite: StepAssist Enabled");
        provider.add("item.gtceu.tool.ev_screwdriver", "%s Electric Screwdriver (EV)");
        provider.add("item.gtceu.tool.mv_screwdriver", "%s Electric Screwdriver (MV)");
        provider.add("item.gtceu.tool.luv_screwdriver", "%s Electric Screwdriver (LuV)");
        provider.add("item.gtceu.tool.zpm_screwdriver", "%s Electric Screwdriver (ZPM)");
        provider.add("item.gtceu.tool.zpm_drill", "%s Drill (ZPM)");
        provider.add("item.gtceu.tool.luv_drill", "%s Drill (LuV)");
        provider.add("item.gtceu.tool.mv_wrench", "%s Wrench (MV)");
        provider.add("item.gtceu.tool.ev_wrench", "%s Wrench (EV)");
        provider.add("item.gtceu.tool.luv_wrench", "%s Wrench (LuV)");
        provider.add("item.gtceu.tool.zpm_wrench", "%s Wrench (ZPM)");
        provider.add("item.gtceu.tool.mv_buzzsaw", "%s Buzzsaw (MV)");
        provider.add("item.gtceu.tool.ev_buzzsaw", "%s Buzzsaw  (EV)");
        provider.add("item.gtceu.tool.luv_buzzsaw", "%s Buzzsaw  (LuV)");
        provider.add("item.gtceu.tool.zpm_buzzsaw", "%s Buzzsaw  (ZPM)");
        provider.add("item.gtceu.tool.mv_chainsaw", "%s Buzzsaw  (MV)");
        provider.add("item.gtceu.tool.ev_chainsaw", "%s Buzzsaw  (EV)");
        provider.add("item.gtceu.tool.luv_chainsaw", "%s Buzzsaw  (LuV)");
        provider.add("item.gtceu.tool.zpm_chainsaw", "%s Buzzsaw  (ZPM)");
        provider.add("item.gtceu.tool.mv_wirecutter", "%s Wire Cutters  (MV)");
        provider.add("item.gtceu.tool.ev_wirecutter", "%s Wire Cutters  (EV)");
        provider.add("item.gtceu.tool.luv_wirecutter", "%s Wire Cutters (LuV)");
        provider.add("item.gtceu.tool.zpm_wirecutter", "%s Wire Cutters  (ZPM)");

        provider.add("shield.phoenixcore.type.normal", "Normal");
        provider.add("shield.phoenixcore.type.inactive", "Inactive");
        provider.add("shield.phoenixcore.type.decayed", "Decayed");
        provider.add("shield.phoenixcore.current_shield", "Shield Status: %s");

        provider.add("phoenixcore.ponder.hint", "View structure guide");

        provider.add("phoenixcore.ponder.ebf.title", "Electric Blast Furnace");
        provider.add("phoenixcore.ponder.ebf.step1_overview",
                "The Electric Blast Furnace smelts metals at extreme temperatures using heating coils.");
        provider.add("phoenixcore.ponder.ebf.step2_controller",
                "This is the controller block. Place it on the front-bottom of the structure.");
        provider.add("phoenixcore.ponder.ebf.step3_coils",
                "The two middle layers must be filled entirely with heating coil blocks.");
        provider.add("phoenixcore.ponder.ebf.step4_cupronickel",
                "Cupronickel coils provide the minimum temperature — enough for basic alloys.");
        provider.add("phoenixcore.ponder.ebf.step5_kanthal",
                "Kanthal coils raise the temperature cap, unlocking higher-tier recipes.");
        provider.add("phoenixcore.ponder.ebf.step6_hatches",
                "Place at least one Input Bus, Output Bus, Energy Hatch, and Muffler Hatch on the outer casing.");

        provider.add("phoenixcore.ponder.freezer.title", "Vacuum Freezer");
        provider.add("phoenixcore.ponder.freezer.step1_overview",
                "The Vacuum Freezer cools materials to extremely low temperatures.");
        provider.add("phoenixcore.ponder.freezer.step2_hatches",
                "Place your hatches on the outer casing — any face except the controller.");

        provider.add("phoenixcore.ponder.lcr.title", "Large Chemical Reactor");
        provider.add("phoenixcore.ponder.lcr.step1_overview",
                "The Large Chemical Reactor processes chemical recipes with higher efficiency than its single-block counterpart.");
        provider.add("phoenixcore.ponder.lcr.step2_pipe", "The centre of the middle layer must be a PTFE pipe casing.");
        provider.add("phoenixcore.ponder.lcr.step3_coil",
                "Exactly one heating coil must be placed somewhere in the cross layer.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_0", "Coil position variant 1 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_1", "Coil position variant 2 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_2", "Coil position variant 3 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_3", "Coil position variant 4 of 5.");
        provider.add("phoenixcore.ponder.lcr.step4_variant_4", "Coil position variant 5 of 5.");
        provider.add("phoenixcore.ponder.lcr.step5_hatches",
                "Hatches go on the outer casing faces. You need energy, item, and fluid IO.");

        provider.add("tooltip.phoenixcore.crystal_rose.generic", "A crystalline flower of immense power.");
        provider.add("tooltip.phoenixcore.crystal_rose.made_from", "Forged from %s.");
        provider.add("tooltip.phoenixcore.expanded_pattern_buffer.0",
                "§fHolds §672 §fAE2 patterns, and its shared tanks hold four times as much.");
        provider.add("tooltip.phoenixcore.expanded_pattern_buffer.1",
                "§7Up to 72 linked proxies, a sort button, a four-mode comparator output (crouch-screwdriver), and it keeps its chunk loaded while proxies are linked.");
        provider.add("tooltip.phoenixcore.expanded_pattern_buffer_proxy.0",
                "\u00a7fLinks machines to an \u00a76Expanded ME Pattern Buffer\u00a7f with a data stick.");
        provider.add("tooltip.phoenixcore.expanded_pattern_buffer_proxy.1",
                "\u00a77Screwdriver: cycle which slots of the buffer this proxy can use.");
        provider.add("tooltip.phoenixcore.expanded_pattern_buffer_proxy.2",
                "\u00a77Crouching screwdriver: priority 0-9, higher priority proxies are served first.");
        provider.add("gui.phoenixcore.expanded_proxy.priority", "Proxy priority: %s");
        provider.add("gui.phoenixcore.expanded_proxy.slots", "Proxy uses buffer slots %s to %s");
        provider.add("gui.phoenixcore.expanded_buffer.sort", "Sort");
        provider.add("gui.phoenixcore.expanded_buffer.comparator", "Comparator: %s");
        provider.add("gui.phoenixcore.expanded_buffer.comparator.filled", "Patterns filled");
        provider.add("gui.phoenixcore.expanded_buffer.comparator.busy", "Busy slots");
        provider.add("gui.phoenixcore.expanded_buffer.comparator.stored", "Holds items");
        provider.add("gui.phoenixcore.expanded_buffer.comparator.online", "ME online");
        provider.add("tooltip.phoenixcore.auto_charger.output",
                "Fully charged items are pushed out of the output side (set it with a wrench).");
        provider.add("tooltip.phoenixcore.nanites.generic", "Microscopic machines swarming with potential.");
        provider.add("tooltip.phoenixcore.nanites.made_from", "Constructed from %s.");
        provider.add("metaarmor.tooltip.wings", "Contains Phoenix Wings");
        provider.add("metaarmor.tooltip.tesla_connection", "Controls Tesla Network Connection");

        provider.add("gtceu.top.recipe_output", "Predicted Output:");
        provider.add("item.phoenixcore.jade.thread_header", "Alchemical Thread #%s: %s%%");
        provider.add("item.phoenixcore.jade.threads_active", "Active Alchemical Threads:");
        provider.add("gtceu.gui.content.range", "%s - %s");
        provider.add("gtceu.gui.content.times_item", "x %s");

        provider.add("jade.phoenixcore.source_container", "Source Energy");
        provider.add("config.jade.plugin_phoenixcore.phantasia_jade", "Phantasia Info");

        provider.add("jade.phoenixcore.source_tank_header", "Source Tank Content");
        provider.add("jade.phoenixcore.source_tank_format", "Capacity %s / %s - %d%%");
        provider.add("config.jade.plugin_phoenixcore.source_tank_info", "Source Tank Info");

        provider.add("block.phoenixcore.astral_ritual_pedestal", "§dAstral Ritual Pedestal");

        provider.add("config.jade.plugin_phoenixcore.source_hatch_info", "Source Hatch Info");
        provider.add("config.jade.plugin_phoenixcore.astral_thread_hatch_info", "Astral Thread Hatch Info");
        provider.add("config.jade.plugin_phoenixcore.growth_machine_info", "Growth Machine Info");
        provider.add("config.jade.plugin_phoenixcore.stellar_forge_info", "Stellar Forge Info");

        provider.add("key.categories.phoenixcore", "PhoenixCore");
        provider.add("key.phoenixcore.wing_flight_gui", "Wing Flight Settings");
        provider.add("key.phoenixcore.tesla_mode", "Enable Tesla Mode");
        provider.add("key.phoenixcore.tesla_discharge", "Activate Tesla Discharge");
        provider.add("key.phoenixcore.manipulator_menu", "Matter Manipulator Menu");
        provider.add("key.phoenixcore.open_ponder", "View Structure Guide");
        provider.add("key.phoenixcore.phantasia_menu", "Open Phantasia Menu");
        provider.add("key.phoenixcore.recipe_builder", "Open Recipe Builder");
        provider.add("key.phoenixcore.spray_can_menu", "Open Spray Can Menu");
        provider.add("key.phoenixcore.open_emi_favorite_pages", "Open EMI pages settings.");
        provider.add("key.phoenixcore.toggle_discipline_sky", "Toggle Discipline Sky Shaders (Debug)");
        provider.add("key.phoenixcore.show_shader_profiler", "Show Shader Profiler HUD (Debug)");

        provider.add("creativetab.phoenix_creative_tab", "Phoenix Core");
        provider.add("fluid.phoenixcore.prismatic_paint", "Prismatic Paint");
        provider.add("gui.phoenixcore.color_select.title", "Select Color");
        provider.add("behaviour.paintspray.chameleon.status.color", "§7Mode: §f%s");
        provider.add("behaviour.paintspray.chameleon.status.solvent", "§7Mode: §dSolvent");
        provider.add("behaviour.paintspray.chameleon.tooltip.current_color", "Current Color: %s");
        provider.add("behaviour.paintspray.chameleon.tooltip.solvent", "Current: Solvent");
        provider.add("behaviour.paintspray.chameleon.tooltip.info", "Scroll or use the Keybind to change color.");
        provider.add("behaviour.paintspray.chameleon.tooltip.fluid", "§7Paint: %s / %s mB");

        provider.add("behaviour.paintspray.chameleon.message.out_of_paint", "§cOut of Prismatic Paint!");
        provider.add("behaviour.paintspray.solvent.short", "Solvent");

        provider.add("item.phoenixcore.tesla_binder.linked", "§aLinked to: §f%s");
        provider.add("item.phoenixcore.tesla_binder.unlinked", "§cNot Linked");
        provider.add("item.phoenixcore.tesla_binder.frequency", "§7Frequency: §b%s");

        provider.add("item.phoenixcore.chameleon_spray_can.with_color", "%s (%s)");

        multiLang(provider, "gtceu.placeholder_info.shieldStability",
                "Returns the stability of the shield.",
                "Note that not having a shield projected may result in nonsense values of integrity.",
                "Usage:",
                "  {shieldStability} -> shield integrity: (integrity, in percent)");

        provider.add("phoenixcore.research.multiblock_locked",
                "Research required: unlock this machine before it can form.");
        provider.add("phoenixcore.condition.conflux_research", "Requires research: %s");
        for (net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier tier : net.phoenix.core.integration.ars_nouveau.logistics.SourcePipeTier
                .values()) {
            provider.add("block.phoenixcore." + tier.blockId(), tier.displayName());
        }
        for (net.phoenix.core.integration.conflux.ConfluxDataType type : net.phoenix.core.integration.conflux.ConfluxDataType
                .values()) {
            provider.add("block.phoenixcore." + type.id() + "_data_pipe", type.displayName + " Data Pipe");
        }
        provider.add("phoenixcore.research.discipline.none", "No Discipline");
        provider.add("phoenixcore.research.discipline.committed", "Committed");
        provider.add("phoenixcore.research.discipline.can_switch", "Switch Discipline");
        provider.add("phoenixcore.research.discipline.switch_cost", "Cost to abandon:");
        provider.add("phoenixcore.research.discipline.locked",
                "You are committed to your Discipline: abandoning it now costs ten times the data.");
        provider.add("phoenixcore.research.discipline.abandon_success",
                "Discipline abandoned. You may now choose a new path.");
        provider.add("phoenixcore.research.discipline.abandon_failed",
                "Cannot abandon Discipline: not enough research data.");
    }

    public static void multiLang(RegistrateLangProvider provider, String key, String... values) {
        for (var i = 0; i < values.length; i++) {
            provider.add(getSubKey(key, i), values[i]);
        }
    }

    protected static void multilineLang(RegistrateLangProvider provider, String key, String multiline) {
        var lines = multiline.split("\n");
        multiLang(provider, key, lines);
    }

    protected static String getSubKey(String key, int index) {
        return key + "." + index;
    }
}
