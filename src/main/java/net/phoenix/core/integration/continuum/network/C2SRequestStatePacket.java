package net.phoenix.core.integration.continuum.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.integration.continuum.common.ContinuumServerEvents;

import java.util.function.Supplier;

public class C2SRequestStatePacket implements CustomPacketPayload {
    public static final Type<C2SRequestStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_request_state_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestStatePacket> STREAM_CODEC = StreamCodec.ofMember(C2SRequestStatePacket::encode, C2SRequestStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public C2SRequestStatePacket() {}

    public C2SRequestStatePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(C2SRequestStatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player != null) ContinuumServerEvents.sendState(player);
        });
        }
}
