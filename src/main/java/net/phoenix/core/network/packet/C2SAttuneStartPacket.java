package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;

import java.util.function.Supplier;

public class C2SAttuneStartPacket {

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

    public static void handle(C2SAttuneStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) return;
            if (player.level().getBlockEntity(msg.pos) instanceof HarmonicLockMachine lock) {
                lock.startAttunement(player, msg.difficulty);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
