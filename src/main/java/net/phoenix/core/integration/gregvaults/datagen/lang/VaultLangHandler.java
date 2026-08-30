package net.phoenix.core.integration.gregvaults.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;

@SuppressWarnings("all")
public class VaultLangHandler {

    public static void init(RegistrateLangProvider provider) {
        provider.add(WirelessTerminalItem.KEY_VAULT_TERMINAL_TITLE, "Vault Terminal");
        provider.add(WirelessTerminalItem.KEY_VAULT_NOT_FORMED, "The vault is not formed.");
        provider.add(WirelessTerminalItem.KEY_VAULT_LINKED, "Vault linked!");
        provider.add(WirelessTerminalItem.KEY_NOT_LINKED, "Terminal is not linked to any vault.");
        provider.add(WirelessTerminalItem.KEY_DIMENSION_NOT_FOUND, "Linked dimension not found.");
        provider.add(WirelessTerminalItem.KEY_DIFFERENT_DIMENSION, "Vault is in a different dimension.");
        provider.add(WirelessTerminalItem.KEY_VAULT_NOT_FOUND, "Vault not found at linked position.");
        provider.add(WirelessTerminalItem.KEY_OUT_OF_RANGE, "Out of range (%d/%d blocks).");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_LINKED, "Linked");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_NOT_LINKED, "Not linked");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_HOW_TO_LINK, "Shift + right-click on vault to link");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_RANGE, "Range: %s blocks");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_EMITTER, "Emitter: %s");
        provider.add(WirelessTerminalItem.KEY_WIRELESS_DISABLED,
                "Wireless terminals are disabled for this Vault tier.");
        provider.add("tooltip.gregtechvaults.vault_interface",
                "§7Allows external systems to view and interact with stored items.");
        provider.add("tooltip.gregtechvaults.base_slots", "§aBase Slots: %d");
        provider.add("tooltip.gregtechvaults.interface_limit", "§bMax Interfaces: %d");
        provider.add("tooltip.gregtechvaults.wireless_enabled", "Wireless Terminal: §aEnabled");
        provider.add("tooltip.gregtechvaults.wireless_disabled_tooltip", "Wireless Terminal: §4Disabled");
        provider.add("tooltip.gregtechvaults.vault_core_mk1", "§7Adds %d §7item slots to the vault.");
        provider.add("tooltip.gregtechvaults.vault_core_mk2", "§7Adds %d §7item slots to the vault.");
        provider.add("tooltip.gregtechvaults.vault_core_mk3", "§7Adds %d §7item slots to the vault.");
        provider.add("key.categories.gregtechvaults", "GregTech Vaults");
        provider.add("key.gregtechvaults.open_terminal", "Open Wireless Vault Terminal");
        provider.add("config.jade.plugin_phoenixcore.vault_info", "Vaults Information");
        provider.add("tooltip.gregtechvaults.jade.total_slots", "%s Total Slots");
        provider.add("tooltip.gregtechvaults.jade.available_slots", "%s Available Slots");
    }
}
