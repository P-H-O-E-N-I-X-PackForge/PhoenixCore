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

    // Used to be `static void send(CompoundTag data) {}` - an empty no-op with no target and no
    // caller anywhere in the codebase, so nothing ever actually reached the client even though this
    // packet type was fully registered and its handle() correctly wired to
    // ClientDisciplineProgressionCache.
    public static void send(ServerPlayer player, CompoundTag data) {
        ConfluxNetwork.CHANNEL.sendTo(new S2CDisciplineProgressionSyncPacket(data), player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
    }
}
