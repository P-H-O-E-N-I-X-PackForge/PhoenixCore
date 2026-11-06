package net.phoenix.core.integration.conflux.dimension.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.dimension.client.ClientDisciplineProgressionCache;

public class S2CDisciplineProgressionSyncPacket implements CustomPacketPayload {

    public static final Type<S2CDisciplineProgressionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_discipline_progression_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CDisciplineProgressionSyncPacket> STREAM_CODEC = StreamCodec
            .of((buf, pkt) -> buf.writeNbt(pkt.data), buf -> new S2CDisciplineProgressionSyncPacket(buf.readNbt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final CompoundTag data;

    public S2CDisciplineProgressionSyncPacket(CompoundTag data) {
        this.data = data;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                ClientDisciplineProgressionCache.deserializeFromNBT(data);
            }
        });
    }

    public static void send(ServerPlayer player, CompoundTag data) {
        PacketDistributor.sendToPlayer(player, new S2CDisciplineProgressionSyncPacket(data));
    }
}
