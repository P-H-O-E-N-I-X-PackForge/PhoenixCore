package net.phoenix.core.rebirth.rite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SRitePacket(BlockPos pos, int action, int[] args) implements CustomPacketPayload {

    public static final Type<C2SRitePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_rite"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRitePacket> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeBlockPos(m.pos);
                buf.writeVarInt(m.action);
                buf.writeVarIntArray(m.args);
            }, buf -> new C2SRitePacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarIntArray(32)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SRitePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) RebirthRites.act(player, msg.pos, msg.action, msg.args);
        });
    }
}
