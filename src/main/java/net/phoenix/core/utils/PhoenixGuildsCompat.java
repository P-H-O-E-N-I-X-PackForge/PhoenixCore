package net.phoenix.core.utils;

import net.minecraft.world.entity.player.Player;
import net.phoenixvine.guilds.GuildAPI;

import java.util.UUID;

final class PhoenixGuildsCompat {

    private PhoenixGuildsCompat() {}

    static UUID getTeamIdOrPlayerFallback(UUID playerUUID) {
        return GuildAPI.getGuildIdOrPlayerFallback(playerUUID);
    }

    static String getTeamName(UUID teamId) {
        return GuildAPI.getDisplayName(teamId);
    }

    static boolean isPlayerOnTeam(Player player, UUID teamUUID) {
        return GuildAPI.isPlayerInGuildOrIs(player.getUUID(), teamUUID);
    }
}
