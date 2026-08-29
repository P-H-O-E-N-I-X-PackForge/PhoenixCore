package net.phoenix.core.integration.gregpacks.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

@SuppressWarnings("all")
public class GregPacksLangHandler {

    public static void init(RegistrateLangProvider provider) {
        provider.add("item.gregpacks.omnipack.tooltip.slots", "§6Item Slots:§r %s");
        provider.add("item.gregpacks.omnipack.tooltip.fluid", "§9Fluid Capacity:§r %s mB");
        provider.add("item.gregpacks.omnipack.tooltip.energy", "§cEnergy Capacity:§r %s EU");
        provider.add("item.gregpacks.omnipack.tooltip.upgrades", "§aMax Upgrades:§r %s");
        multilineLang(provider, "item.gregpacks.omnipack.tooltip.place",
                "\n---------------------------\n\n§7Right-Click to open OmniPack GUI\n§7Shift-Right-Click to place");

        provider.add("container.gregpacks.tab.inventory", "Omnipack Inventory");
        provider.add("container.gregpacks.tab.upgrades", "Upgrade Modules");
        provider.add("container.gregpacks.omnipack", "OmniPack Inventory");
        provider.add("container.gregpacks.upgrades", "Upgrade Modules");

        provider.add("item.gregpacks.module_extruder_mold.tooltip", "Extruder shape for making Module Bases");
        provider.add("item.gregpacks.module_casting_mold.tooltip", "Mold for making Module Bases");

        provider.add("item.gregpacks.item_module_1.tooltip",
                "An upgrade module that adds more item slots to your OmniPack");
        provider.add("item.gregpacks.item_module_2.tooltip",
                "An upgrade module that adds more item slots to your OmniPack");
        provider.add("item.gregpacks.item_module_3.tooltip",
                "An upgrade module that adds more item slots to your OmniPack");
        provider.add("item.gregpacks.fluid_module_1.tooltip",
                "An upgrade module that adds more fluid capacity to your OmniPack");
        provider.add("item.gregpacks.fluid_module_2.tooltip",
                "An upgrade module that adds more fluid capacity to your OmniPack");
        provider.add("item.gregpacks.fluid_module_3.tooltip",
                "An upgrade module that adds more fluid capacity to your OmniPack");
        provider.add("item.gregpacks.energy_module_1.tooltip",
                "An upgrade module that adds more EU capacity to your OmniPack");
        provider.add("item.gregpacks.energy_module_2.tooltip",
                "An upgrade module that adds more EU capacity to your OmniPack");
        provider.add("item.gregpacks.energy_module_3.tooltip",
                "An upgrade module that adds more EU capacity to your OmniPack");
        provider.add("item.gregpacks.crafting_module.tooltip",
                "An upgrade module that adds a crafting table interface to your OmniPack");
        provider.add("item.gregpacks.feeding_module.tooltip",
                "An upgrade module that allows your OmniPack to consume food for you automatically");
        provider.add("item.gregpacks.jetpack_module_1.tooltip",
                "An upgrade module that grants creative flight");
        provider.add("item.gregpacks.jetpack_module_2.tooltip",
                "An upgrade module that grants creative flight with speed configurability");
        provider.add("item.gregpacks.maintenance_module.tooltip",
                "An upgrade module that allows you to use your OmniPack to satisfy maintenance requirements");
        provider.add("item.gregpacks.magnet_module_1.tooltip",
                "An upgrade module that increases your pickup range");
        provider.add("item.gregpacks.magnet_module_2.tooltip",
                "An upgrade module that increases your pickup range with configurable range and filtering");
        provider.add("item.gregpacks.processing_module.tooltip",
                "An upgrade module that can be combined with singleblock machines for portable processing in your OmniPack");

        provider.add("message.gregpacks.maintenance_fixed",
                "\"Maintenance fixed!\"");

        provider.add("message.gregpacks.maintenance_failed",
                "\"Some problems fixed, missing tools in pack!\"");

        provider.add("key.gregpacks.open_omnipack", "Open OmniPack Inventory");
        provider.add("key.category.gregpacks", "GregPacks");

        provider.add("gregpacks.jade.energy", "Energy: %s / %s EU");
        provider.add("gregpacks.jade.fluids", "Fluids:");
        provider.add("gregpacks.jade.inventory", "Inventory:");
        provider.add("config.jade.plugin_phoenixcore", "PhoenixCore Integration");
        provider.add("config.jade.plugin_phoenixcore.omnipack_provider", "OmniPack Info");
    }

    protected static void multilineLang(RegistrateLangProvider provider, String key, String multiline) {
        var lines = multiline.split("\n");
        multiLang(provider, key, lines);
    }

    protected static void multiLang(RegistrateLangProvider provider, String key, String... values) {
        for (var i = 0; i < values.length; i++) {
            provider.add(getSubKey(key, i), values[i]);
        }
    }

    protected static String getSubKey(String key, int index) {
        return key + "." + index;
    }
}
