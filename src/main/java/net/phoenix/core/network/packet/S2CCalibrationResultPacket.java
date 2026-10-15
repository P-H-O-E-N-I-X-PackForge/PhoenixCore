package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.astral.puzzle.client.AstralPuzzleClient;

public class S2CCalibrationResultPacket implements CustomPacketPayload {

    public static final Type<S2CCalibrationResultPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_calibration_result_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCalibrationResultPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CCalibrationResultPacket::encode, S2CCalibrationResultPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final boolean solved;
    private final boolean aligned;
    private final int right;
    private final int total;
    private final int drawn;
    private final String lost;

    public S2CCalibrationResultPacket(BlockPos pos, boolean solved, boolean aligned, int right, int total,
                                      int drawn, String lost) {
        this.pos = pos;
        this.solved = solved;
        this.aligned = aligned;
        this.right = right;
        this.total = total;
        this.drawn = drawn;
        this.lost = lost;
    }

    public S2CCalibrationResultPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.solved = buf.readBoolean();
        this.aligned = buf.readBoolean();
        this.right = buf.readVarInt();
        this.total = buf.readVarInt();
        this.drawn = buf.readVarInt();
        this.lost = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(solved);
        buf.writeBoolean(aligned);
        buf.writeVarInt(right);
        buf.writeVarInt(total);
        buf.writeVarInt(drawn);
        buf.writeUtf(lost, 64);
    }

    public static void handle(S2CCalibrationResultPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;
            AstralPuzzleClient.calibrationResult(msg.pos, msg.solved, msg.aligned, msg.right, msg.total, msg.drawn,
                    msg.lost);
        });
    }
}
