package net.phoenix.core.integration.continuum.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.continuum.client.ContinuumClientHooks;

public class S2COpenMapPacket implements CustomPacketPayload {

    public static final Type<S2COpenMapPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_open_map_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenMapPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2COpenMapPacket::encode, S2COpenMapPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pad;

    public S2COpenMapPacket(BlockPos pad) {
        this.pad = pad;
    }

    public S2COpenMapPacket(FriendlyByteBuf buf) {
        this.pad = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pad);
    }

    public static void handle(S2COpenMapPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ContinuumClientHooks.openMap(msg.pad);
        });
    }
}
