package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.common.ContinuumServerEvents;

import java.util.function.Supplier;

public class C2SRequestStatePacket {

    public C2SRequestStatePacket() {}

    public C2SRequestStatePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(C2SRequestStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ContinuumServerEvents.sendState(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
