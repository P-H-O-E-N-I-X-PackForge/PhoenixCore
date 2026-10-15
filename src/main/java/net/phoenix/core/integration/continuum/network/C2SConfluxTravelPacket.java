package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.continuum.common.ConfluxTravel;

public class C2SConfluxTravelPacket implements CustomPacketPayload {

    public static final Type<C2SConfluxTravelPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_conflux_travel_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SConfluxTravelPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SConfluxTravelPacket::encode, C2SConfluxTravelPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(C2SConfluxTravelPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            String error = msg.back ? ConfluxTravel.back(player) : ConfluxTravel.travel(player, msg.body);
            if (error != null) player.displayClientMessage(Component.literal(error), true);
        });
    }
}
