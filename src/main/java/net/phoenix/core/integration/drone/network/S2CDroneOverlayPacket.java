package net.phoenix.core.integration.drone.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.drone.client.DroneOverlayCache;

import java.util.List;

public record S2CDroneOverlayPacket(BlockPos controller, int range,
                                    List<DroneTargetView> targets)
        implements CustomPacketPayload {

    public static final Type<S2CDroneOverlayPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_drone_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CDroneOverlayPacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.controller);
                buf.writeVarInt(msg.range);
                DroneTargetView.encodeList(msg.targets, buf);
            },
            buf -> new S2CDroneOverlayPacket(buf.readBlockPos(), buf.readVarInt(), DroneTargetView.decodeList(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CDroneOverlayPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) DroneOverlayCache.accept(msg);
        });
    }
}
