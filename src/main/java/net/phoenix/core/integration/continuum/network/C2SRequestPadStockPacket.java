package net.phoenix.core.integration.continuum.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.common.ContinuumMissions;
import net.phoenix.core.network.PhoenixNetwork;

import java.util.function.Supplier;

public class C2SRequestPadStockPacket implements CustomPacketPayload {
    public static final Type<C2SRequestPadStockPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_request_pad_stock_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestPadStockPacket> STREAM_CODEC = StreamCodec.ofMember(C2SRequestPadStockPacket::encode, C2SRequestPadStockPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


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

    public static void handle(C2SRequestPadStockPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null || !player.level().isLoaded(msg.pad)) return;

            double range = PhoenixConfigs.INSTANCE.continuum.launchRangeBlocks + 8.0;
            if (player.distanceToSqr(msg.pad.getX() + 0.5, msg.pad.getY() + 0.5, msg.pad.getZ() + 0.5) >
                    range * range) {
                return;
            }

            var stock = ContinuumMissions.padStock(player, msg.pad);
            PacketDistributor.sendToPlayer(player, new S2CPadStockPacket(msg.pad, stock.buses(), stock.rocket(), stock.probes(), stock.kits(),
                            stock.rockets()));
        });
        }
}
