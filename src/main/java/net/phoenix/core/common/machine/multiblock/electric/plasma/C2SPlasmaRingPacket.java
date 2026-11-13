package net.phoenix.core.common.machine.multiblock.electric.plasma;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.machine.multiblock.electric.HighPressurePlasmaArcFurnaceMachine;

public record C2SPlasmaRingPacket(BlockPos pos, int action, int cell) implements CustomPacketPayload {

    public static final int TUNE = 0;
    public static final int RESET = 1;

    public static final Type<C2SPlasmaRingPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_plasma_ring"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPlasmaRingPacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.pos);
                buf.writeVarInt(msg.action);
                buf.writeVarInt(msg.cell);
            }, buf -> new C2SPlasmaRingPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPlasmaRingPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64 * 64) {
                return;
            }
            HighPressurePlasmaArcFurnaceMachine machine = HighPressurePlasmaArcFurnaceMachine.at(player.level(),
                    msg.pos);
            if (machine == null || !machine.isFormed()) return;

            String message = "";
            boolean solved = false;
            switch (msg.action) {
                case TUNE -> {
                    var result = machine.ringTune(msg.cell);
                    message = result.message();
                    solved = result.solved();
                }
                case RESET -> {
                    machine.ringReset();
                    message = "The ring re-scrambles.";
                }
                default -> {}
            }
            S2CPlasmaRingPacket.send(player, S2CPlasmaRingPacket.of(machine, false, message, solved));
        });
    }
}
