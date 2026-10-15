package net.phoenix.core.integration.continuum.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.integration.continuum.common.ContinuumMissions;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SCollectMissionPacket implements CustomPacketPayload {
    public static final Type<C2SCollectMissionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_collect_mission_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCollectMissionPacket> STREAM_CODEC = StreamCodec.ofMember(C2SCollectMissionPacket::encode, C2SCollectMissionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final UUID mission;

    public C2SCollectMissionPacket(UUID mission) {
        this.mission = mission;
    }

    public C2SCollectMissionPacket(FriendlyByteBuf buf) {
        this.mission = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(mission);
    }

    public static void handle(C2SCollectMissionPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            String error = ContinuumMissions.collect(player, msg.mission);
            if (error != null) {
                player.sendSystemMessage(Component.literal(error).withStyle(ChatFormatting.RED));
            }
        });
        }
}
