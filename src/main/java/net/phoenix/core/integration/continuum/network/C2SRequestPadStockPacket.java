package net.phoenix.core.integration.continuum.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.common.ContinuumMissions;
import net.phoenix.core.network.PhoenixNetwork;

import java.util.function.Supplier;

/** The open map asks what its launch pad currently has loaded (rocket, probes, repair kits). */
public class C2SRequestPadStockPacket {

    private final BlockPos pad;

    public C2SRequestPadStockPacket(BlockPos pad) {
        this.pad = pad;
    }

    public C2SRequestPadStockPacket(FriendlyByteBuf buf) {
        this.pad = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pad);
    }

    public static void handle(C2SRequestPadStockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.level().isLoaded(msg.pad)) return;

            double range = PhoenixConfigs.INSTANCE.continuum.launchRangeBlocks + 8.0;
            if (player.distanceToSqr(msg.pad.getX() + 0.5, msg.pad.getY() + 0.5, msg.pad.getZ() + 0.5) > range * range) {
                return;
            }

            var stock = ContinuumMissions.padStock(player, msg.pad);
            PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new S2CPadStockPacket(msg.pad, stock.buses(), stock.rocket(), stock.probes(), stock.kits()));
        });
        ctx.get().setPacketHandled(true);
    }
}
