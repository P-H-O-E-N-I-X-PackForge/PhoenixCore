package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.phoenix.core.PhoenixCore;

@SuppressWarnings("all")
public class GregPacksNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PhoenixCore.MOD_ID, "gregpacks"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void init() {
        CHANNEL.messageBuilder(CPacketOpenOmniPack.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketOpenOmniPack::encode)
                .decoder(CPacketOpenOmniPack::decode)
                .consumerMainThread(CPacketOpenOmniPack::handle)
                .add();

        CHANNEL.messageBuilder(CPacketFluidInteract.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketFluidInteract::encode)
                .decoder(CPacketFluidInteract::decode)
                .consumerMainThread(CPacketFluidInteract::handle)
                .add();

        CHANNEL.messageBuilder(SPacketFluidSync.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketFluidSync::encode)
                .decoder(SPacketFluidSync::decode)
                .consumerMainThread(SPacketFluidSync::handle)
                .add();

        CHANNEL.messageBuilder(SPacketJetpackThrust.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPacketJetpackThrust::encode)
                .decoder(SPacketJetpackThrust::decode)
                .consumerMainThread(SPacketJetpackThrust::handle)
                .add();

        CHANNEL.messageBuilder(CPacketKeyState.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPacketKeyState::encode)
                .decoder(CPacketKeyState::decode)
                .consumerMainThread(CPacketKeyState::handle)
                .add();
    }
}
