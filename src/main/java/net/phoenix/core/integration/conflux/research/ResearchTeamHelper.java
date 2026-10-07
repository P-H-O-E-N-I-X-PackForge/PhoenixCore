package net.phoenix.core.integration.conflux.research;

import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.utils.TeamUtils;

import java.util.UUID;

public final class ResearchTeamHelper {

    /**
     * Delegates to the same team resolution the Tesla network uses (Phoenix Guilds, then FTB Teams,
     * then a solo-player fallback) - this used to maintain its own separate, FTB-only compat here,
     * which is exactly the kind of duplication that let {@code ConfluxProgressionEvents} grow an
     * entirely different (and broken) team lookup instead of reusing this one.
     */
    public static UUID getTeamId(ServerPlayer player) {
        return TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
    }

    private ResearchTeamHelper() {}
}
