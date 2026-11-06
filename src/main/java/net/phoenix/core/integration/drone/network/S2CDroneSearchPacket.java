package net.phoenix.core.integration.drone.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.drone.client.DroneSearchRenderer;

import java.util.ArrayList;
import java.util.List;

public record S2CDroneSearchPacket(BlockPos controller, String query,
                                   List<Hit> hits)
        implements CustomPacketPayload {

    public record Hit(BlockPos pos, String label) {}

    public static final Type<S2CDroneSearchPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_drone_search"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CDroneSearchPacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.controller);
                buf.writeUtf(msg.query, 64);
                buf.writeVarInt(msg.hits.size());
                for (Hit h : msg.hits) {
                    buf.writeBlockPos(h.pos());
                    buf.writeUtf(h.label(), 96);
                }
            },
            buf -> {
                BlockPos controller = buf.readBlockPos();
                String query = buf.readUtf(64);
                int n = buf.readVarInt();
                List<Hit> hits = new ArrayList<>(n);
                for (int i = 0; i < n; i++) hits.add(new Hit(buf.readBlockPos(), buf.readUtf(96)));
                return new S2CDroneSearchPacket(controller, query, hits);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CDroneSearchPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) DroneSearchRenderer.accept(msg);
        });
    }
}
