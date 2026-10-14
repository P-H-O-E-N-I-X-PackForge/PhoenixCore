package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;

import java.util.function.Supplier;

/** A player asked a Harmonic Lock to start its attunement game. */
public class C2SAttuneStartPacket {

    private final BlockPos pos;

    public C2SAttuneStartPacket(BlockPos pos) {
        this.pos = pos;
    }

    public C2SAttuneStartPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void handle(C2SAttuneStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) return;
            if (player.level().getBlockEntity(msg.pos) instanceof HarmonicLockMachine lock) {
                lock.startAttunement(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
