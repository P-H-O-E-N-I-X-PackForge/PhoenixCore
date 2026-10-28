package net.phoenix.core.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.integration.conflux.network.ConfluxNetwork;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import org.jetbrains.annotations.Nullable;

public final class ConfluxApi {

    private ConfluxApi() {}

    public static volatile java.util.function.BiConsumer<ServerPlayer, String> archiveUnlocker;

    public static boolean hasFlag(ServerPlayer player, String flag) {
        return data(player).hasFlag(ResearchTeamHelper.getTeamId(player), flag);
    }

    public static boolean isUnlocked(ServerPlayer player, ResourceLocation nodeId) {
        return data(player).isUnlocked(ResearchTeamHelper.getTeamId(player), nodeId);
    }

    /** What this client has been told of its team's research (call on the client only). */
    public static boolean clientHasFlag(String flag) {
        return net.phoenix.core.integration.conflux.client.ClientResearchCache.flags.contains(flag);
    }

    public static boolean clientIsUnlocked(ResourceLocation nodeId) {
        return net.phoenix.core.integration.conflux.client.ClientResearchCache.isUnlocked(nodeId);
    }

    public static @Nullable String clientDiscipline() {
        return net.phoenix.core.integration.conflux.client.ClientResearchCache.getDisciplineInfo().disciplineId();
    }

    public static @Nullable String discipline(ServerPlayer player) {
        return data(player).getDiscipline(ResearchTeamHelper.getTeamId(player));
    }

    public static void grantFlag(ServerPlayer player, String flag) {
        data(player).grantFlag(ResearchTeamHelper.getTeamId(player), flag);
        ConfluxNetwork.syncResearchToPlayer(player);
        net.phoenix.core.integration.chronicles.ChroniclesLink.fireTeam(player.server,
                ResearchTeamHelper.getTeamId(player),
                net.phoenix.core.integration.chronicles.PhoenixChroniclesTriggers.CONFLUX_FLAG,
                net.phoenix.core.integration.chronicles.ChroniclesLink.data("flag", flag));
    }

    public static void grantUnlock(ServerPlayer player, ResourceLocation nodeId) {
        var team = ResearchTeamHelper.getTeamId(player);
        var context = net.phoenix.core.api.event.ConfluxEvents.preResearch(player.server, team, nodeId);
        data(player).grantUnlock(team, nodeId);
        net.phoenix.core.api.event.ConfluxEvents.postResearch(player.server, team, nodeId, context);
        ConfluxNetwork.syncResearchToPlayer(player);
    }

    private static WorldResearchData data(ServerPlayer player) {
        return WorldResearchData.get(player.serverLevel());
    }
}
