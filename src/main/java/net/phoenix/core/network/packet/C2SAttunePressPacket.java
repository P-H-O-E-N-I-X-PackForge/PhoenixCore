package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;
import net.phoenix.core.network.PacketCodecs;

public record C2SAttunePressPacket(BlockPos pos, int lane, int elapsedMs, boolean release)
        implements CustomPacketPayload {

    public static final Type<C2SAttunePressPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_attune_press_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SAttunePressPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SAttunePressPacket::pos,
            PacketCodecs.BYTE_INT, C2SAttunePressPacket::lane,
            ByteBufCodecs.VAR_INT, C2SAttunePressPacket::elapsedMs,
            ByteBufCodecs.BOOL, C2SAttunePressPacket::release,
            C2SAttunePressPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public C2SAttunePressPacket(BlockPos pos, int lane, int elapsedMs) {
        this(pos, lane, elapsedMs, false);
    }

    public static void handle(C2SAttunePressPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.level().getBlockEntity(msg.pos()) instanceof HarmonicLockMachine lock) {
                if (msg.release()) lock.attuneRelease(player, msg.lane(), msg.elapsedMs());
                else lock.attunePress(player, msg.lane(), msg.elapsedMs());
            }
        });
    }
}
