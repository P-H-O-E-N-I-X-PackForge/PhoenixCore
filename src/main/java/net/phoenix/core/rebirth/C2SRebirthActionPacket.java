package net.phoenix.core.rebirth;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.machine.multiblock.forge.PhoenixForgeMachine;

public record C2SRebirthActionPacket(BlockPos forge, int action, int[] ring) implements CustomPacketPayload {

    public static final int REFRESH = 0;
    public static final int SUBMIT_RING = 1;
    public static final int IGNITE = 2;

    public static final Type<C2SRebirthActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_rebirth_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRebirthActionPacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.forge);
                buf.writeVarInt(msg.action);
                buf.writeVarIntArray(msg.ring);
            }, buf -> new C2SRebirthActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarIntArray(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SRebirthActionPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(msg.forge.getX() + 0.5, msg.forge.getY() + 0.5, msg.forge.getZ() + 0.5) >
                    96 * 96) {
                return;
            }
            PhoenixForgeMachine forge = PhoenixForgeMachine.at(player.level(), msg.forge);
            if (forge == null || !forge.isFormed()) return;

            String message = "";
            boolean ignited = false;
            switch (msg.action) {
                case SUBMIT_RING -> message = RebirthNetwork.submitRing(player, player.serverLevel(), msg.ring)
                        .message();
                case IGNITE -> {
                    var result = RebirthNetwork.ignite(player, player.serverLevel());
                    message = result.message();
                    ignited = result.ok();
                }
                default -> {}
            }
            S2CRebirthStatePacket.send(player, S2CRebirthStatePacket.of(player, msg.forge, false, ignited, message));
        });
    }
}
