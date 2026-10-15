package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.continuum.common.ContinuumTeamData;
import net.phoenix.core.utils.TeamUtils;

public class C2SRequestHistoryPacket implements CustomPacketPayload {

    public static final Type<C2SRequestHistoryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_request_history_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestHistoryPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SRequestHistoryPacket::encode, C2SRequestHistoryPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public C2SRequestHistoryPacket() {}

    public C2SRequestHistoryPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(C2SRequestHistoryPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            var team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
            PacketDistributor.sendToPlayer(player,
                    new S2CHistoryPacket(ContinuumTeamData.get(player.server).history(team)));
        });
    }
}
