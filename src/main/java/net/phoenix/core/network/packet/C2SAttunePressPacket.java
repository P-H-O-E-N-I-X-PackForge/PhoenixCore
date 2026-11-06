package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;

public class C2SAttunePressPacket implements CustomPacketPayload {

    public static final Type<C2SAttunePressPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_attune_press_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SAttunePressPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SAttunePressPacket::encode, C2SAttunePressPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final int lane;
    private final int elapsedMs;
    private final boolean release;

    public C2SAttunePressPacket(BlockPos pos, int lane, int elapsedMs) {
        this(pos, lane, elapsedMs, false);
    }

    public C2SAttunePressPacket(BlockPos pos, int lane, int elapsedMs, boolean release) {
        this.pos = pos;
        this.lane = lane;
        this.elapsedMs = elapsedMs;
        this.release = release;
    }

    public C2SAttunePressPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.lane = buf.readByte();
        this.elapsedMs = buf.readVarInt();
        this.release = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(lane);
        buf.writeVarInt(elapsedMs);
        buf.writeBoolean(release);
    }

    public static void handle(C2SAttunePressPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.level().getBlockEntity(msg.pos) instanceof HarmonicLockMachine lock) {
                if (msg.release) lock.attuneRelease(player, msg.lane, msg.elapsedMs);
                else lock.attunePress(player, msg.lane, msg.elapsedMs);
            }
        });
    }
}
