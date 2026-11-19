package net.phoenix.core.common.machine.multiblock.nullwell;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Tells a player's client to open the Null Well's full screen. */
public record S2CNullWellOpenPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<S2CNullWellOpenPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_null_well_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CNullWellOpenPacket> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> buf.writeBlockPos(m.pos), buf -> new S2CNullWellOpenPacket(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CNullWellOpenPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.client.nullwell.NullWellClientHandler.open(msg.pos()));
    }
}
