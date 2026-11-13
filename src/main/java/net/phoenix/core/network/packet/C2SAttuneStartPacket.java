package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;
import net.phoenix.core.network.PacketCodecs;

public record C2SAttuneStartPacket(BlockPos pos, int difficulty) implements CustomPacketPayload {

    public static final Type<C2SAttuneStartPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_attune_start_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SAttuneStartPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SAttuneStartPacket::pos,
            PacketCodecs.BYTE_INT, C2SAttuneStartPacket::difficulty,
            C2SAttuneStartPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SAttuneStartPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.distanceToSqr(msg.pos().getX() + 0.5, msg.pos().getY() + 0.5, msg.pos().getZ() + 0.5) > 64)
                return;
            if (player.level().getBlockEntity(msg.pos()) instanceof HarmonicLockMachine lock) {
                lock.startAttunement(player, msg.difficulty());
            }
        });
    }
}
