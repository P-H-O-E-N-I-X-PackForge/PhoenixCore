package net.phoenix.core.integration.drone.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.drone.client.DroneFleetCache;

import java.util.ArrayList;
import java.util.List;

public record S2CDroneFleetPacket(BlockPos controller, int drones, List<Target> targets)
        implements CustomPacketPayload {

    public record Target(BlockPos pos, int colour) {}

    public static final Type<S2CDroneFleetPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_drone_fleet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CDroneFleetPacket> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.controller);
                buf.writeVarInt(msg.drones);
                buf.writeVarInt(msg.targets.size());
                for (Target t : msg.targets) {
                    buf.writeBlockPos(t.pos());
                    buf.writeInt(t.colour());
                }
            },
            buf -> {
                BlockPos controller = buf.readBlockPos();
                int drones = buf.readVarInt();
                int n = buf.readVarInt();
                List<Target> targets = new ArrayList<>(n);
                for (int i = 0; i < n; i++) targets.add(new Target(buf.readBlockPos(), buf.readInt()));
                return new S2CDroneFleetPacket(controller, drones, targets);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CDroneFleetPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) DroneFleetCache.accept(msg);
        });
    }
}
