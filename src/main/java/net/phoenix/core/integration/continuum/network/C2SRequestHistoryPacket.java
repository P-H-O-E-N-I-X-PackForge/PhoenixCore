package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.integration.continuum.common.ContinuumTeamData;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.utils.TeamUtils;

import java.util.function.Supplier;

public class C2SRequestHistoryPacket {

    public C2SRequestHistoryPacket() {}

    public C2SRequestHistoryPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(C2SRequestHistoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
            PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new S2CHistoryPacket(ContinuumTeamData.get(player.server).history(team)));
        });
        ctx.get().setPacketHandled(true);
    }
}
