package net.phoenix.core.integration.continuum.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.continuum.common.ContinuumMissions;
import net.phoenix.core.integration.continuum.common.ContinuumServerEvents;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.utils.TeamUtils;

public class C2SLaunchMissionPacket implements CustomPacketPayload {

    public static final Type<C2SLaunchMissionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_launch_mission_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SLaunchMissionPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SLaunchMissionPacket::encode, C2SLaunchMissionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ResourceLocation destination;
    private final BlockPos pad;
    private final Mission.Type type;
    private final int probes;
    private final int count;

    public C2SLaunchMissionPacket(ResourceLocation destination, BlockPos pad, Mission.Type type, int probes,
                                  int count) {
        this.destination = destination;
        this.pad = pad;
        this.type = type;
        this.probes = probes;
        this.count = count;
    }

    public C2SLaunchMissionPacket(FriendlyByteBuf buf) {
        this.destination = buf.readResourceLocation();
        this.pad = buf.readBlockPos();
        this.type = Mission.Type.values()[Math.max(0, Math.min(buf.readByte(), Mission.Type.values().length - 1))];
        this.probes = buf.readVarInt();
        this.count = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(destination);
        buf.writeBlockPos(pad);
        buf.writeByte(type.ordinal());
        buf.writeVarInt(probes);
        buf.writeVarInt(count);
    }

    public static void handle(C2SLaunchMissionPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            String error = ContinuumMissions.launchMany(player, msg.destination, msg.pad, msg.type, msg.probes,
                    msg.count);
            PacketDistributor.sendToPlayer(player,
                    new S2CMissionLaunchedPacket(error == null, msg.destination, msg.pad, error == null ? "" : error));
            if (error == null) {
                ContinuumServerEvents.sendStateToTeam(player.server,
                        TeamUtils.getTeamIdOrPlayerFallback(player.getUUID()));
            }
        });
    }
}
