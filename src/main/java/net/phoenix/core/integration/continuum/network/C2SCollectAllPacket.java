package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.continuum.common.ContinuumMissions;

public class C2SCollectAllPacket implements CustomPacketPayload {

    public static final Type<C2SCollectAllPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_collect_all_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCollectAllPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCollectAllPacket::encode, C2SCollectAllPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public C2SCollectAllPacket() {}

    public C2SCollectAllPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(C2SCollectAllPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player != null) ContinuumMissions.collectAll(player);
        });
    }
}
