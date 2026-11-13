package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;

public record C2SOpenCalibrationPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<C2SOpenCalibrationPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_open_calibration_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SOpenCalibrationPacket> STREAM_CODEC = StreamCodec
            .composite(
                    BlockPos.STREAM_CODEC, C2SOpenCalibrationPacket::pos,
                    C2SOpenCalibrationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SOpenCalibrationPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.distanceToSqr(msg.pos().getX() + 0.5, msg.pos().getY() + 0.5, msg.pos().getZ() + 0.5) > 64)
                return;
            if (MetaMachine.getMachine(player.level(), msg.pos()) instanceof AstralCalibratedMachine machine) {
                machine.openCalibration(player);
            }
        });
    }
}
