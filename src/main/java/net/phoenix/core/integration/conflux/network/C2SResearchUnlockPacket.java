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
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.integration.conflux.terminal.ResearchTerminalBlockEntity;

import java.util.UUID;

public class C2SResearchUnlockPacket implements CustomPacketPayload {

    public static final Type<C2SResearchUnlockPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_research_unlock_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SResearchUnlockPacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> C2SResearchUnlockPacket.encode(msg, buf), C2SResearchUnlockPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ResourceLocation nodeId;
    private final BlockPos terminalPos;

    public C2SResearchUnlockPacket(ResourceLocation nodeId, BlockPos terminalPos) {
        this.nodeId = nodeId;
        this.terminalPos = terminalPos;
    }

    public static void encode(C2SResearchUnlockPacket pkt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(pkt.nodeId);
        buf.writeBlockPos(pkt.terminalPos);
    }

    public static C2SResearchUnlockPacket decode(FriendlyByteBuf buf) {
        return new C2SResearchUnlockPacket(buf.readResourceLocation(), buf.readBlockPos());
    }

    public static void handle(C2SResearchUnlockPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            if (!(level.getBlockEntity(pkt.terminalPos) instanceof ResearchTerminalBlockEntity terminal)) return;
            if (player.distanceToSqr(pkt.terminalPos.getX() + 0.5, pkt.terminalPos.getY() + 0.5,
                    pkt.terminalPos.getZ() + 0.5) > 64)
                return;

            ResearchNode node = ResearchTreeRegistry.INSTANCE.getNode(pkt.nodeId).orElse(null);
            if (node == null) return;

            UUID teamId = ResearchTeamHelper.getTeamId(player);
            WorldResearchData researchData = WorldResearchData.get(level);
            String disciplineBefore = researchData.getDiscipline(teamId);

            boolean success = researchData.tryUnlock(teamId, node, terminal, ResearchTreeRegistry.INSTANCE, player);
            if (success) {
                ConfluxNetwork.syncResearchToPlayer(player);

                String disciplineAfter = researchData.getDiscipline(teamId);
                if (disciplineAfter != null && !disciplineAfter.equals(disciplineBefore)) {
                    net.phoenix.core.integration.conflux.dimension.ConfluxDimensionFactory
                            .enterDisciplineDimension(player, teamId, disciplineAfter);
                }

                net.phoenix.core.integration.conflux.dimension.ConfluxProgressionEvents
                        .onResearchUnlock(player, node, teamId);
            }
        });
    }
}
