package net.phoenix.core.integration.gregvaults.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@SuppressWarnings("all")
public class VaultNetwork {

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(CPacketOpenTerminal.TYPE, CPacketOpenTerminal.STREAM_CODEC, CPacketOpenTerminal::handle);
        registrar.playToServer(CPacketFillCraftingGrid.TYPE, CPacketFillCraftingGrid.STREAM_CODEC,
                CPacketFillCraftingGrid::handle);
    }
}
