package net.phoenix.core.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.UUID;

public final class TeamUtils {

    private static final boolean PHOENIX_GUILDS_LOADED = ModList.get().isLoaded("phoenix_guilds");
    private static final boolean FTB_TEAMS_LOADED = ModList.get().isLoaded("ftbteams");

    private TeamUtils() {}

    public static UUID getTeamIdOrPlayerFallback(UUID playerUUID) {
        if (playerUUID == null) return null;
        if (PHOENIX_GUILDS_LOADED) return PhoenixGuildsCompat.getTeamIdOrPlayerFallback(playerUUID);
        if (FTB_TEAMS_LOADED) return FTBTeamsCompat.getTeamIdOrPlayerFallback(playerUUID);

        return playerUUID;
    }

    public static String getTeamName(UUID teamId) {
        if (teamId == null) return "Unknown";
        if (PHOENIX_GUILDS_LOADED) return PhoenixGuildsCompat.getTeamName(teamId);
        if (FTB_TEAMS_LOADED) return FTBTeamsCompat.getTeamName(teamId);

        return resolvePlayerName(teamId);
    }

    public static String resolvePlayerName(UUID playerUUID) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(playerUUID);
            if (online != null) return online.getGameProfile().getName();

            if (server.getProfileCache() != null) {
                Optional<com.mojang.authlib.GameProfile> cached = server.getProfileCache().get(playerUUID);
                if (cached.isPresent()) return cached.get().getName();
            }
        }
        return "Player: " + playerUUID.toString().substring(0, 8);
    }

    public static boolean isPlayerOnTeam(Player player, UUID teamUUID) {
        if (!(player instanceof ServerPlayer)) return false;
        if (PHOENIX_GUILDS_LOADED) return PhoenixGuildsCompat.isPlayerOnTeam(player, teamUUID);
        if (FTB_TEAMS_LOADED) return FTBTeamsCompat.isPlayerOnTeam(player, teamUUID);

        return player.getUUID().equals(teamUUID);
    }
}
