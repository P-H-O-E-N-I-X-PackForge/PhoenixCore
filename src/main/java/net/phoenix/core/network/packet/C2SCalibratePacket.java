package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;
import net.phoenix.core.integration.astral.puzzle.StarChart;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class C2SCalibratePacket {

    private final BlockPos pos;
    private final List<int[]> lines;

    public C2SCalibratePacket(BlockPos pos, List<int[]> lines) {
        this.pos = pos;
        this.lines = lines;
    }

    public C2SCalibratePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        int count = Math.min(buf.readVarInt(), StarChart.MAX_LINES);
        this.lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) lines.add(new int[] { buf.readByte(), buf.readByte() });
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(lines.size());
        for (int[] l : lines) {
            buf.writeByte(l[0]);
            buf.writeByte(l[1]);
        }
    }

    public static void handle(C2SCalibratePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) return;
            if (MetaMachine.getMachine(player.level(), msg.pos) instanceof AstralCalibratedMachine machine) {
                machine.attempt(player, msg.lines);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
