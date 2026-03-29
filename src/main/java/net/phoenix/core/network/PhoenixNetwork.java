package net.phoenix.core.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.phoenix.core.network.packet.UpdateWingSettingsPacket;

import java.util.Optional;

public class PhoenixNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("phoenixcore", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++,
                UpdateWingSettingsPacket.class,
                UpdateWingSettingsPacket::encode,
                UpdateWingSettingsPacket::new,
                UpdateWingSettingsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
