package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SPanopticAnswerPacket(BlockPos controller, int[] values) implements CustomPacketPayload {

    public static final Type<C2SPanopticAnswerPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_panoptic_answer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPanopticAnswerPacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> encode(msg, buf), C2SPanopticAnswerPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(C2SPanopticAnswerPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.controller);
        buf.writeVarIntArray(m.values);
    }

    private static C2SPanopticAnswerPacket decode(FriendlyByteBuf buf) {
        return new C2SPanopticAnswerPacket(buf.readBlockPos(), buf.readVarIntArray(64));
    }

    public static void handle(C2SPanopticAnswerPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(msg.controller.getX() + 0.5, msg.controller.getY() + 0.5,
                    msg.controller.getZ() + 0.5) > 64 * 64) {
                return;
            }
            PanopticEngineMachine engine = PanopticEngineMachine.at(player.serverLevel(), msg.controller);
            if (engine == null || !engine.isFormed()) return;

            String lacking = engine.pay(PanopticService.offering(player, msg.values));
            if (lacking != null) {
                S2CPanopticStatePacket.send(player,
                        S2CPanopticStatePacket.withMessage(player, msg.controller, lacking));
                return;
            }

            PanopticService.Outcome outcome = PanopticService.answer(player, msg.values);
            S2CPanopticStatePacket.send(player, S2CPanopticStatePacket.of(player, msg.controller, false,
                    outcome));
        });
    }
}
