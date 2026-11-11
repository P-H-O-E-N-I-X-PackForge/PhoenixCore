package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SPanopticRevealPacket(BlockPos controller) implements CustomPacketPayload {

    public static final Type<C2SPanopticRevealPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_panoptic_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPanopticRevealPacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> buf.writeBlockPos(msg.controller),
                    buf -> new C2SPanopticRevealPacket(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPanopticRevealPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("The Engine does not show its answers to you."));
                return;
            }
            PanopticEngineMachine engine = PanopticEngineMachine.at(player.serverLevel(), msg.controller);
            if (engine == null || !engine.isFormed()) return;
            PanopticService.Solution solution = PanopticService.solution(player);
            if (solution == null) return;
            PacketDistributor.sendToPlayer(player,
                    new S2CPanopticSolutionPacket(msg.controller, solution.values(), solution.lines()));
        });
    }
}
