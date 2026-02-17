package net.phoenix.core.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class PhoenixMachineLangHandler {

    public static void init(RegistrateLangProvider provider) {
        // Tesla & Laser Tech
        provider.add("emi_info.phoenixcore.required_shield", "Required Shield: %s");
        provider.add("emi_info.phoenixcore.shield_heal", "Shield Health Restored: +%s");
        provider.add("emi_info.phoenixcore.shield_damage", "Shield Damage Applied: -%s");

        provider.add("tooltip.phoenixcore.tesla_hatch.laser_input",
                "§bOptical Collimator§r: Concentrates energy into a coherent Tesla-Laser beam.");
        provider.add("tooltip.phoenixcore.tesla_hatch.laser_output",
                "§bPhotonic Receptor§r: Decodes high-frequency laser flux back into EU.");
        provider.add("tooltip.phoenixcore.tesla_hatch.input",
                "§bWireless Transmitter§r: Siphons energy into the Tesla Cloud.");
        provider.add("tooltip.phoenixcore.tesla_hatch.output",
                "§bWireless Receiver§r: Broadcasts energy from the Tesla Cloud.");
        provider.add("tooltip.phoenixcore.tesla_hatch.lore", "§6Nevvonian Core Tech: Frequency Locked.");

        provider.add("tech.phoenixcore.laser.input.low", "Tesla Optical Collimator");
        provider.add("tech.phoenixcore.laser.input.mid", "Tesla Optical Collimation Grid");
        provider.add("tech.phoenixcore.laser.input.high", "Tesla Phased Beam Matrix");

        provider.add("tech.phoenixcore.laser.output.low", "Tesla Photonic Coalescer");
        provider.add("tech.phoenixcore.laser.output.mid", "Tesla Photonic Coalescence Array");
        provider.add("tech.phoenixcore.laser.output.high", "Tesla Photonic Coalescence Matrix");

        // Source System
        provider.add("gui.phoenixcore.source_hatch.label.import", "Source Input Hatch");
        provider.add("gui.phoenixcore.source_hatch.label.export", "Source Output Hatch");
        provider.add("gui.phoenixcore.source_hatch.source", "Source Stored: %s");
        provider.add("phoenix.core.recipe.source_in", "Source Necessary: %s.");
        provider.add("phoenix.core.recipe.source_out", "Source Given: %s.");
        provider.add("tooltip.phoenixcore.source_hatch.consumption", "§cMax Source Consumption:§d %s");
        provider.add("tooltip.phoenixcore.source_hatch.capacity", "§cMax Source Capacity:§d %s");

        // Fission Reactor System
        provider.add("phoenix.fission.not_formed", "Structure not formed!");
        provider.add("phoenix.fission.status.safe_idle", "Status: §aIDLE");
        provider.add("phoenix.fission.status.safe_working", "Status: §6ACTIVE");
        provider.add("phoenix.fission.status.danger_timer", "§cCRITICAL: Meltdown in %s seconds!");
        provider.add("phoenix.fission.status.no_coolant", "§eWARNING: Coolant Supply Exhausted");
        provider.add("phoenix.fission.blanket_outputs", "§7Possible Products:");
        provider.add("phoenix.fission.coolant_output", "Hot Coolant Produced: %s");
        provider.add("phoenix.fission.neutron_bias", "§7Neutron Bias: §f%s");
        provider.add("phoenix.fission.spectrum_shift", "§7Spectrum Shift: §f%s");
        provider.add("phoenix.fission.current_heat", "Core Temperature: %s HU");
        provider.add("phoenix.fission.net_heat", "Net Heat Change: %s HU/t");
        provider.add("phoenix.fission.eu_generation", "Output: %s EU/t");
        provider.add("phoenix.fission.parallels", "Parallel Processing: %sx");
        provider.add("phoenix.fission.heat_production", "Heat Production: %s");
        provider.add("phoenix.fission.nuke_radius", "Blast area: %s");
        provider.add("phoenix.fission.moderator", "Primary Moderator: %s");
        provider.add("phoenix.fission.moderator_fuel_discount", "Fuel Efficiency: +%s%%");
        provider.add("phoenix.fission.cooler", "Primary Cooling: %s");
        provider.add("phoenix.fission.coolant", "Coolant: %s");
        provider.add("phoenix.fission.coolant_rate", "Coolant Flow: %s mb/t");
        provider.add("phoenix.fission.coolant_status.ok", "§aCoolant Supply OK");
        provider.add("phoenix.fission.coolant_status.empty", "§cCoolant Supply Depleted");
        provider.add("phoenix.fission.summary", "Cooling: %s / %s HU/t");
        provider.add("phoenix.fission.blanket_input", "§7Target Material: §f%s");
        provider.add("phoenix.fission.blanket_output", "§7Breeding Product: §f%s");
        provider.add("phoenix.fission.blanket_desc", "Irradiate target materials to produce specialized isotopes.");
        provider.add("phoenix.fission.fuel_cycle", "Consumes §f%s§7 units every §6%s§7 seconds");
        provider.add("phoenix.fission.depleted_fuel", "§7Depleted Fuel: §f%s");
        provider.add("phoenix.fission.blanket_cycle", "Transmutes §f%s§7 units every §6%s§7 seconds");
        provider.add("phoenix.fission.fuel_usage", "Fuel Consumption: §f%s");
        provider.add("phoenix.fission.fuel_required", "§7Requires Fuel: §f%s");
        provider.add("phoenix.fission.coolant_required", "§3Required Coolant: §f%s");
        provider.add("phoenix.fission.cooling_power", "§bCooling Capacity: §f%s HU/t");

        provider.add("block.phoenix_fission.fission_cooler.capacity", "§bCooling Capacity: §f%s HU/t");
        provider.add("block.phoenix_fission.fission_cooler.required_coolant", "§3Required Coolant: §f%s");
        provider.add("block.phoenix_fission.fission_moderator.multiplier", "§6Heat Multiplier: §f%sx");
        provider.add("block.phoenix_fission.fission_moderator.parallel", "§aParallel Bonus: §f+%s");
        provider.add("block.phoenix_fission.fission_moderator.shift", "Hold Shift for details");
        provider.add("block.phoenix_fission.fission_moderator.info_header", "Fission Moderator");
        provider.add("block.phoenix_fission.fission_moderator.boost", "EU Boost: %s");
        provider.add("block.phoenix_fission.fission_moderator.fuel_discount", "Fuel Discount: %s");

        provider.add("phoenix.multiblock.pattern.info.multiple_fuel_rods",
                "Requires Fuel Rods. These generate base heat and determine recipe parallels.");
        provider.add("phoenix.multiblock.pattern.info.multiple_blankets",
                "Requires Blanket Rods. These act as targets for transmutation in Breeder cycles.");
        provider.add("phoenix.multiblock.pattern.info.multiple_moderators",
                "Moderators adjust heat generation and can provide EU or Parallel bonuses.");
        provider.add("phoenix.multiblock.pattern.info.multiple_coolers",
                "Coolers remove heat based on their tier and provided coolant fluid.");

        // Recipe Types
        provider.add("gtceu.recipe_type.phoenixcore.high_performance_breeder_reactor",
                "High-Performance Breeder Reactor");
        provider.add("gtceu.recipe_type.phoenix_fission.high_performance_breeder_reactor",
                "High-Performance Breeder Reactor");
        provider.add("gtceu.recipe_type.phoenix_fission.advanced_pressurized_fission_reactor",
                "Advanced Pressurized Fission Reactor");
        provider.add("gtceu.recipe_type.phoenix_fission.pressurized_fission_reactor", "Pressurized Fission Reactor");

        provider.add("gtceu.recipe_type.phoenixcore.honey_chamber", "Honey Chamber");
        provider.add("gtceu.recipe_type.phoenixcore.please", "Please Multiblock");
        provider.add("gtceu.recipe_type.phoenixcore.simulated_colony", "Simulated Colony");
        provider.add("gtceu.recipe_type.phoenixcore.comb_decanting", "Comb Decanter");
        provider.add("gtceu.recipe_type.phoenixcore.swarm_nurturing", "Swarm Nurturing Chamber");
        provider.add("gtceu.recipe_type.phoenixcore.apis_progenitor", "Apis Progenitor");

        provider.add("gtceu.tooltip.tier", "Tier: %s");

        // Jade Integration
        provider.add("config.jade.plugin_phoenixcore.source_hatch_info", "Source Stored: %s");
        provider.add("config.jade.plugin_phoenixcore.plasma_furnace_info", "High-Pressure Plasma Arc Furnace Info");
        provider.add("config.jade.plugin_phoenixcore.tesla_network_info", "Tesla Network Information");
        provider.add("config.jade.plugin_phoenixcore.fission_machine_info", "Fission Machine Info");

        provider.add("jade.phoenixcore.plasma_boost_active", "Plasma Boost: %s Active");
        provider.add("jade.phoenixcore.no_plasma_boost", "No Plasma Catalyst");
        provider.add("jade.phoenixcore.tesla_stored", "Stored: ");
        provider.add("jade.phoenixcore.tesla_receiving", "Receiving: %s EU/t");
        provider.add("jade.phoenixcore.tesla_providing", "Providing: %s EU/t");

        provider.add("jade.phoenixcore.blanket_input", "Blanket Fuel: %s");
        provider.add("jade.phoenixcore.blanket_output", "Breeding Product: %s");
        provider.add("jade.phoenixcore.blanket_amount", "Base per cycle: %s");
        provider.add("jade.phoenixcore.heat", "§cCore Heat: %s HU");
        provider.add("jade.phoenixcore.fission_meltdown_timer", "§6MELTDOWN: %s seconds!");
        provider.add("jade.phoenixcore.fission_safe", "§aCore Stable");
        provider.add("jade.phoenixcore.fission_no_coolant", "§cNO COOLANT DETECTED");
        provider.add("jade.phoenixcore.fission_heating", "§eCORE HEATING UP");

        // Multi-line Tooltips
        PhoenixLangHandler.multiLang(provider, "tooltip.phoenixcore.shield_stability_hatch", "Outputs shield stability",
                "as a redstone signal.");
    }
}
