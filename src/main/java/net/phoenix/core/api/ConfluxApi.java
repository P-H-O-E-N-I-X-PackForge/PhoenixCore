package net.phoenix.core.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.integration.conflux.network.ConfluxNetwork;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import org.jetbrains.annotations.Nullable;

/**
 * Stable entry point for the rest of the suite (Chronicles, Archives, Phantasia, Guilds...) to read and write a
 * team's Conflux progression without depending on its internals. Every method resolves the player's team the same way
 * the research terminal does, so callers never deal with team ids.
 */
public final class ConfluxApi {

    private ConfluxApi() {}

    public static boolean hasFlag(ServerPlayer player, String flag) {
        return data(player).hasFlag(ResearchTeamHelper.getTeamId(player), flag);
    }

    public static boolean isUnlocked(ServerPlayer player, ResourceLocation nodeId) {
        return data(player).isUnlocked(ResearchTeamHelper.getTeamId(player), nodeId);
    }

    /** The discipline the player's team chose, or null if none yet. */
    public static @Nullable String discipline(ServerPlayer player) {
        return data(player).getDiscipline(ResearchTeamHelper.getTeamId(player));
    }

    public static void grantFlag(ServerPlayer player, String flag) {
        data(player).grantFlag(ResearchTeamHelper.getTeamId(player), flag);
        ConfluxNetwork.syncResearchToPlayer(player);
    }

    public static void grantUnlock(ServerPlayer player, ResourceLocation nodeId) {
        data(player).grantUnlock(ResearchTeamHelper.getTeamId(player), nodeId);
        ConfluxNetwork.syncResearchToPlayer(player);
    }

    private static WorldResearchData data(ServerPlayer player) {
        return WorldResearchData.get(player.serverLevel());
    }
}
