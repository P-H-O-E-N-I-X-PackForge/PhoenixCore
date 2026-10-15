package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;

import java.util.function.Supplier;

public class C2SAttunePressPacket {

    private final BlockPos pos;
    private final int lane;
    private final int elapsedMs;

    public C2SAttunePressPacket(BlockPos pos, int lane, int elapsedMs) {
        this.pos = pos;
        this.lane = lane;
        this.elapsedMs = elapsedMs;
    }

    public C2SAttunePressPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.lane = buf.readByte();
        this.elapsedMs = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(lane);
        buf.writeVarInt(elapsedMs);
    }

    public static void handle(C2SAttunePressPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (player.level().getBlockEntity(msg.pos) instanceof HarmonicLockMachine lock) {
                lock.attunePress(player, msg.lane, msg.elapsedMs);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
