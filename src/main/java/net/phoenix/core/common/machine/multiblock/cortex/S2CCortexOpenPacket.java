package net.phoenix.core.common.machine.multiblock.cortex;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record S2CCortexOpenPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<S2CCortexOpenPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_cortex_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCortexOpenPacket> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> buf.writeBlockPos(m.pos), buf -> new S2CCortexOpenPacket(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CCortexOpenPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.client.cortex.CortexClientHandler.open(msg.pos()));
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, S2CCortexOpenPacket::handle);
    }
}
