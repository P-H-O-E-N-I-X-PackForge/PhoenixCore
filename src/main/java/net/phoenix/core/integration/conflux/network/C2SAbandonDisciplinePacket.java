package net.phoenix.core.integration.conflux.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.integration.conflux.terminal.ResearchTerminalBlockEntity;

import java.util.UUID;

public class C2SAbandonDisciplinePacket implements CustomPacketPayload {

    public static final Type<C2SAbandonDisciplinePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_abandon_discipline_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SAbandonDisciplinePacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> C2SAbandonDisciplinePacket.encode(msg, buf), C2SAbandonDisciplinePacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos terminalPos;

    public C2SAbandonDisciplinePacket(BlockPos terminalPos) {
        this.terminalPos = terminalPos;
    }

    public static void encode(C2SAbandonDisciplinePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.terminalPos);
    }

    public static C2SAbandonDisciplinePacket decode(FriendlyByteBuf buf) {
        return new C2SAbandonDisciplinePacket(buf.readBlockPos());
    }

    public static void handle(C2SAbandonDisciplinePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            if (!(level.getBlockEntity(pkt.terminalPos) instanceof ResearchTerminalBlockEntity terminal)) return;
            if (player.distanceToSqr(pkt.terminalPos.getX() + 0.5, pkt.terminalPos.getY() + 0.5,
                    pkt.terminalPos.getZ() + 0.5) > 64)
                return;

            UUID teamId = ResearchTeamHelper.getTeamId(player);
            boolean success = WorldResearchData.get(level).abandonDiscipline(teamId, terminal,
                    ResearchTreeRegistry.INSTANCE);
            if (success) {
                ConfluxNetwork.syncResearchToPlayer(player);
                net.phoenix.core.integration.continuum.common.ContinuumServerEvents.sendStateToTeam(player.server,
                        teamId);
            }
        });
    }
}
