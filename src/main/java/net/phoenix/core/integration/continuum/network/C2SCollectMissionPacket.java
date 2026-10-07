package net.phoenix.core.integration.continuum.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.common.ContinuumMissions;

import java.util.UUID;
import java.util.function.Supplier;

/** Takes the rocket back from a finished mission. */
public class C2SCollectMissionPacket {

    private final UUID mission;

    public C2SCollectMissionPacket(UUID mission) {
        this.mission = mission;
    }

    public C2SCollectMissionPacket(FriendlyByteBuf buf) {
        this.mission = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(mission);
    }

    public static void handle(C2SCollectMissionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            String error = ContinuumMissions.collect(player, msg.mission);
            if (error != null) {
                player.sendSystemMessage(Component.literal(error).withStyle(ChatFormatting.RED));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
