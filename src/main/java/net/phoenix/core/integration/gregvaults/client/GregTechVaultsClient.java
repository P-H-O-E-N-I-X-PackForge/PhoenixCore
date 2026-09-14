package net.phoenix.core.integration.gregvaults.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
        modEventBus.addListener(GregTechVaultsClient::clientSetup);
        modEventBus.addListener(VaultKeyBindings::register);
        MinecraftForge.EVENT_BUS.register(VaultKeyBindings.TickHandler.class);
    }

    private static void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(VaultRegistry.VAULT_MENU.get(), VaultScreen::new);
            MenuScreens.register(VaultRegistry.VAULT_TERMINAL_MENU.get(), VaultTerminalScreen::new);
        });
    }
}
