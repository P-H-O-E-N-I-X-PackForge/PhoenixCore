package net.phoenix.core.integration.drone.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.drone.client.DroneAlertClient;

public record S2CDroneAlertPacket(BlockPos pos, String title, String message,
                                  boolean severe)
        implements CustomPacketPayload {

    public static final Type<S2CDroneAlertPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_drone_alert"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CDroneAlertPacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.pos);
                buf.writeUtf(msg.title, 128);
                buf.writeUtf(msg.message, 256);
                buf.writeBoolean(msg.severe);
            },
            buf -> new S2CDroneAlertPacket(buf.readBlockPos(), buf.readUtf(128), buf.readUtf(256),
                    buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CDroneAlertPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) DroneAlertClient.show(msg);
        });
    }
}
