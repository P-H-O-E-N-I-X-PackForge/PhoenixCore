package net.phoenix.core.integration.gregvaults.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregvaults.client.screen.VaultScreen;
import net.phoenix.core.integration.gregvaults.client.screen.VaultTerminalScreen;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

@OnlyIn(Dist.CLIENT)
public final class GregTechVaultsClient {

    private GregTechVaultsClient() {}

    public static void init(IEventBus modEventBus) {
        DynamicRenderManager.register(PhoenixCore.id("vault_overlay"), VaultOverlayRender.TYPE);
        modEventBus.addListener(VaultOverlayRender::registerModel);
        modEventBus.addListener(GregTechVaultsClient::registerMenuScreens);
        modEventBus.addListener(VaultKeyBindings::register);
        NeoForge.EVENT_BUS.register(VaultKeyBindings.TickHandler.class);
    }

    private static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(VaultRegistry.VAULT_MENU.get(), VaultScreen::new);
        event.register(VaultRegistry.VAULT_TERMINAL_MENU.get(), VaultTerminalScreen::new);
    }
}
