package net.phoenix.core.integration.conflux.dimension.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.conflux.dimension.client.ClientDisciplineProgressionCache;
import net.phoenix.core.integration.conflux.network.ConfluxNetwork;

import java.util.function.Supplier;

public class S2CDisciplineProgressionSyncPacket {

    private final CompoundTag data;

    public S2CDisciplineProgressionSyncPacket(CompoundTag data) {
        this.data = data;
    }

    public S2CDisciplineProgressionSyncPacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientDisciplineProgressionCache.deserializeFromNBT(data);
            });
        });

        return true;
    }

    public static void send(ServerPlayer player, CompoundTag data) {
        ConfluxNetwork.CHANNEL.sendTo(new S2CDisciplineProgressionSyncPacket(data), player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
    }
}
