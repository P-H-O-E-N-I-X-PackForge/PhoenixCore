package net.phoenix.core.integration.conflux.research;

import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.utils.TeamUtils;

import java.util.UUID;

public final class ResearchTeamHelper {

    public static UUID getTeamId(ServerPlayer player) {
        return TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
    }

    private ResearchTeamHelper() {}
}
