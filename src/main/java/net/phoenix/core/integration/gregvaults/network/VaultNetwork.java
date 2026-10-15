package net.phoenix.core.integration.gregvaults.network;


import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;

@SuppressWarnings("all")
public class VaultNetwork {

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(CPacketVaultAction.TYPE, CPacketVaultAction.STREAM_CODEC, CPacketVaultAction::handle);
        registrar.playToServer(CPacketOpenTerminal.TYPE, CPacketOpenTerminal.STREAM_CODEC, CPacketOpenTerminal::handle);
        registrar.playToServer(CPacketFillCraftingGrid.TYPE, CPacketFillCraftingGrid.STREAM_CODEC, CPacketFillCraftingGrid::handle);

        registrar.playToClient(SPacketVaultContents.TYPE, SPacketVaultContents.STREAM_CODEC, SPacketVaultContents::handle);
        registrar.playToClient(SPacketVaultDelta.TYPE, SPacketVaultDelta.STREAM_CODEC, SPacketVaultDelta::handle);
        registrar.playToServer(CPacketVaultDisplayMode.TYPE, CPacketVaultDisplayMode.STREAM_CODEC, CPacketVaultDisplayMode::handle);
        registrar.playToServer(CPacketStackedPickup.TYPE, CPacketStackedPickup.STREAM_CODEC, CPacketStackedPickup::handle);
    }
}
