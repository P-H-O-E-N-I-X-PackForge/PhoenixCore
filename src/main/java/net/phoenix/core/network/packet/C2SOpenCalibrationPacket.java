package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import java.util.function.Supplier;

/** A player asked an Astral machine for its calibration screen. */
public class C2SOpenCalibrationPacket {

    private final BlockPos pos;

    public C2SOpenCalibrationPacket(BlockPos pos) {
        this.pos = pos;
    }

    public C2SOpenCalibrationPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void handle(C2SOpenCalibrationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) return;
            if (MetaMachine.getMachine(player.level(), msg.pos) instanceof AstralCalibratedMachine machine) {
                machine.openCalibration(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
