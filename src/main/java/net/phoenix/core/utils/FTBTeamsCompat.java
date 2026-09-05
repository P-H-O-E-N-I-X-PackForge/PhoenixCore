package net.phoenix.core.utils;

import net.minecraft.world.entity.player.Player;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.UUID;

final class FTBTeamsCompat {

    private FTBTeamsCompat() {}

    static UUID getTeamIdOrPlayerFallback(UUID playerUUID) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) return playerUUID;

        return FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerUUID)
                .map(team -> {
                    if (team.isPartyTeam() || team.isServerTeam()) {
                        return team.getTeamId();
                    }
                    return playerUUID;
                })
                .orElse(playerUUID);
    }

    static String getTeamName(UUID teamId) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) return "Player: " + teamId.toString().substring(0, 8);

        return FTBTeamsAPI.api().getManager().getTeamByID(teamId)
                .map(team -> team.getShortName())
                .orElse("Player: " + teamId.toString().substring(0, 8));
    }

    static boolean isPlayerOnTeam(Player player, UUID teamUUID) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) return player.getUUID().equals(teamUUID);

        return FTBTeamsAPI.api().getManager().getTeamByID(teamUUID)
                .map(team -> team.getMembers().contains(player.getUUID()))
                .orElse(player.getUUID().equals(teamUUID));
    }
}
