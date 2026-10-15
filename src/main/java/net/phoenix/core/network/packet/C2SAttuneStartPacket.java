package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;

public class C2SAttuneStartPacket implements CustomPacketPayload {

    public static final Type<C2SAttuneStartPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_attune_start_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SAttuneStartPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SAttuneStartPacket::encode, C2SAttuneStartPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final int difficulty;

    public C2SAttuneStartPacket(BlockPos pos, int difficulty) {
        this.pos = pos;
        this.difficulty = difficulty;
    }

    public C2SAttuneStartPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.difficulty = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(difficulty);
    }

    public static void handle(C2SAttuneStartPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) return;
            if (player.level().getBlockEntity(msg.pos) instanceof HarmonicLockMachine lock) {
                lock.startAttunement(player, msg.difficulty);
            }
        });
    }
}
