package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.phoenix.core.PhoenixCore;

@SuppressWarnings("all")
public class VaultNetwork {

    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PhoenixCore.MOD_ID, "vault"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++, CPacketVaultAction.class,
                CPacketVaultAction::encode, CPacketVaultAction::decode, CPacketVaultAction::handle);
        CHANNEL.registerMessage(id++, CPacketOpenTerminal.class,
                CPacketOpenTerminal::encode, CPacketOpenTerminal::decode, CPacketOpenTerminal::handle);
        CHANNEL.registerMessage(id++, CPacketFillCraftingGrid.class,
                CPacketFillCraftingGrid::encode, CPacketFillCraftingGrid::decode, CPacketFillCraftingGrid::handle);

        CHANNEL.registerMessage(id++, SPacketVaultContents.class,
                SPacketVaultContents::encode, SPacketVaultContents::decode, SPacketVaultContents::handle);
        CHANNEL.registerMessage(id++, SPacketVaultDelta.class,
                SPacketVaultDelta::encode, SPacketVaultDelta::decode, SPacketVaultDelta::handle);
        CHANNEL.registerMessage(id++, CPacketVaultDisplayMode.class,
                CPacketVaultDisplayMode::encode, CPacketVaultDisplayMode::decode, CPacketVaultDisplayMode::handle);
        CHANNEL.registerMessage(id++, CPacketStackedPickup.class,
                CPacketStackedPickup::encode, CPacketStackedPickup::decode, CPacketStackedPickup::handle);
    }
}
