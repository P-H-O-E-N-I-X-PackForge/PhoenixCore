package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.astral.puzzle.StarChart;
import net.phoenix.core.integration.astral.puzzle.client.AstralPuzzleClient;

public class S2COpenCalibrationPacket implements CustomPacketPayload {

    public static final Type<S2COpenCalibrationPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_open_calibration_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenCalibrationPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2COpenCalibrationPacket::encode, S2COpenCalibrationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(S2COpenCalibrationPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AstralPuzzleClient.openCalibration(msg.pos, msg.kind, msg.stars, msg.calibrated);
        });
    }
}
