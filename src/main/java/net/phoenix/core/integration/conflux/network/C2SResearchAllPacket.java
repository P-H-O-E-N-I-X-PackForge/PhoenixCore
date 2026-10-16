package net.phoenix.core.integration.conflux.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.integration.conflux.terminal.ResearchTerminalBlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Researches everything in one tree that the team can currently unlock and afford, repeating until nothing more
 * opens up. Commitment nodes are left alone: they lock the team into a discipline and send it to its dimension, so
 * that choice has to be made on purpose.
 */
public class C2SResearchAllPacket {

    private static final int MAX_PASSES = 64;

    private final ResourceLocation treeId;
    private final BlockPos terminalPos;

    public C2SResearchAllPacket(ResourceLocation treeId, BlockPos terminalPos) {
        this.treeId = treeId;
        this.terminalPos = terminalPos;
    }

    public static void encode(C2SResearchAllPacket pkt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(pkt.treeId);
        buf.writeBlockPos(pkt.terminalPos);
    }

    public static C2SResearchAllPacket decode(FriendlyByteBuf buf) {
        return new C2SResearchAllPacket(buf.readResourceLocation(), buf.readBlockPos());
    }

    public static void handle(C2SResearchAllPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) return;
            if (!(level.getBlockEntity(pkt.terminalPos) instanceof ResearchTerminalBlockEntity terminal)) return;
            if (player.distanceToSqr(pkt.terminalPos.getX() + 0.5, pkt.terminalPos.getY() + 0.5,
                    pkt.terminalPos.getZ() + 0.5) > 64)
                return;

            ResearchTree tree = ResearchTreeRegistry.INSTANCE.getTree(pkt.treeId).orElse(null);
            if (tree == null) return;

            UUID teamId = ResearchTeamHelper.getTeamId(player);
            WorldResearchData data = WorldResearchData.get(level);

            int unlockedCount = 0;
            for (int pass = 0; pass < MAX_PASSES; pass++) {
                boolean progressed = false;
                for (ResearchNode node : tree.getNodes()) {
                    if (node.isCommitmentNode) continue;
                    if (data.getUnlocked(teamId).contains(node.id)) continue;
                    if (node.hidden && !node.isVisible(data.getUnlocked(teamId))) continue;
                    if (data.tryUnlock(teamId, node, terminal, ResearchTreeRegistry.INSTANCE)) {
                        progressed = true;
                        unlockedCount++;
                        net.phoenix.core.integration.conflux.dimension.ConfluxProgressionEvents
                                .onResearchUnlock(player, node, teamId);
                    }
                }
                if (!progressed) break;
            }

            if (unlockedCount > 0) {
                ConfluxNetwork.syncResearchToPlayer(player);
                net.phoenix.core.integration.continuum.common.ContinuumServerEvents
                        .sendStateToTeam(player.server, teamId);
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .literal("Researched " + unlockedCount + " node" + (unlockedCount == 1 ? "" : "s") + "."));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .literal("Nothing in this tree can be researched right now."));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
