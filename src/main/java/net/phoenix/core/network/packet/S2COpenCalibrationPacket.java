package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.astral.puzzle.StarChart;
import net.phoenix.core.integration.astral.puzzle.client.AstralPuzzleClient;

import java.util.function.Supplier;

/** Opens the calibration screen: the machine's stars (never its lines) and whether it is already calibrated. */
public class S2COpenCalibrationPacket {

    private final BlockPos pos;
    private final StarChart.Kind kind;
    private final int[] stars;
    private final boolean calibrated;

    public S2COpenCalibrationPacket(BlockPos pos, StarChart.Kind kind, int[] stars, boolean calibrated) {
        this.pos = pos;
        this.kind = kind;
        this.stars = stars;
        this.calibrated = calibrated;
    }

    public S2COpenCalibrationPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.kind = StarChart.Kind.values()[Math.max(0, Math.min(buf.readByte(), StarChart.Kind.values().length - 1))];
        this.stars = buf.readVarIntArray(StarChart.MAX_LINES);
        this.calibrated = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(kind.ordinal());
        buf.writeVarIntArray(stars);
        buf.writeBoolean(calibrated);
    }

    public static void handle(S2COpenCalibrationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AstralPuzzleClient.openCalibration(msg.pos, msg.kind, msg.stars, msg.calibrated);
        });
        ctx.get().setPacketHandled(true);
    }
}
