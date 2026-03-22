package net.phoenix.core.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;

import java.util.UUID;

public final class TeamUtils {

    private TeamUtils() {}

    public static UUID getTeamIdOrPlayerFallback(UUID playerUUID) {
        if (playerUUID == null) return null;

        return FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerUUID)
                .filter(Team::isPartyTeam)
                .map(Team::getTeamId)
                .orElse(playerUUID);
    }

    public static UUID getTeamIdOrPlayerFallback(ServerPlayer player) {
        if (player == null) return null;

        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .filter(Team::isPartyTeam)
                .map(Team::getTeamId)
                .orElse(player.getUUID());
    }

    public static String getTeamName(UUID teamId) {
        if (teamId == null) return "Unknown";

        return FTBTeamsAPI.api().getManager().getTeamByID(teamId)
                .map(team -> team.getShortName())
                .orElse("Player: " + teamId.toString().substring(0, 8));
    }

    public static boolean isPlayerOnTeam(Player player, UUID teamUUID) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getTeamIdOrPlayerFallback(serverPlayer).equals(teamUUID);
        }
        return false;
    }
}
