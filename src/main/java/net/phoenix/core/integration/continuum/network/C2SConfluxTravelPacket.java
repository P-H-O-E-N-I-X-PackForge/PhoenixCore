package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.common.ConfluxTravel;

import java.util.function.Supplier;

public class C2SConfluxTravelPacket {

    private final boolean back;
    private final ResourceLocation body;

    public C2SConfluxTravelPacket(boolean back, ResourceLocation body) {
        this.back = back;
        this.body = body;
    }

    public C2SConfluxTravelPacket(FriendlyByteBuf buf) {
        this.back = buf.readBoolean();
        this.body = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(back);
        buf.writeResourceLocation(body);
    }

    public static void handle(C2SConfluxTravelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            String error = msg.back ? ConfluxTravel.back(player) : ConfluxTravel.travel(player, msg.body);
            if (error != null) player.displayClientMessage(Component.literal(error), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
