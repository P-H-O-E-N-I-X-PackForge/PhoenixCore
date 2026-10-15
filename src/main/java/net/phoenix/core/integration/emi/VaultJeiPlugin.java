package net.phoenix.core.integration.emi;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregvaults.client.screen.VaultContainerMenu;
import net.phoenix.core.integration.gregvaults.client.screen.VaultTerminalMenu;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;

@JeiPlugin
public class VaultJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(
                new VaultJeiTransferHandler<>(VaultContainerMenu.class, VaultRegistry.VAULT_MENU.get()));
        registration.addUniversalRecipeTransferHandler(
                new VaultJeiTransferHandler<>(VaultTerminalMenu.class, VaultRegistry.VAULT_TERMINAL_MENU.get()));
    }
}
