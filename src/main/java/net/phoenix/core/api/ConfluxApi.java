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

    public static @Nullable String discipline(ServerPlayer player) {
        return data(player).getDiscipline(ResearchTeamHelper.getTeamId(player));
    }

    public static void grantFlag(ServerPlayer player, String flag) {
        data(player).grantFlag(ResearchTeamHelper.getTeamId(player), flag);
        ConfluxNetwork.syncResearchToPlayer(player);
    }

    public static void grantUnlock(ServerPlayer player, ResourceLocation nodeId) {
        var team = ResearchTeamHelper.getTeamId(player);
        var before = net.phoenix.core.integration.continuum.common.ContinuumDiscoveryNotifier.snapshot(player.server,
                team);
        data(player).grantUnlock(team, nodeId);
        net.phoenix.core.integration.continuum.common.ContinuumDiscoveryNotifier.announce(player.server, team, before);
        ConfluxNetwork.syncResearchToPlayer(player);
    }

    private static WorldResearchData data(ServerPlayer player) {
        return WorldResearchData.get(player.serverLevel());
    }
}
