package net.phoenix.core.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;

import com.mojang.authlib.GameProfile;

import java.util.Optional;
import java.util.UUID;

public final class TeamUtils {

    private static final boolean FTB_TEAMS_LOADED = ModList.get().isLoaded("ftbteams");

    private TeamUtils() {}

    public static UUID getTeamIdOrPlayerFallback(UUID playerUUID) {
        if (playerUUID == null) return null;
        if (!FTB_TEAMS_LOADED) return playerUUID;

        return FTBTeamsCompat.getTeamIdOrPlayerFallback(playerUUID);
    }

    public static String resolvePlayerName(UUID playerUUID) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(playerUUID);
            if (online != null) return online.getGameProfile().getName();

            if (server.getProfileCache() != null) {
                Optional<GameProfile> cached = server.getProfileCache().get(playerUUID);
                if (cached.isPresent()) return cached.get().getName();
            }
        }
        return "Player: " + playerUUID.toString().substring(0, 8);
    }

    public static String getTeamName(UUID teamId) {
        if (teamId == null) return "Unknown";
        if (!FTB_TEAMS_LOADED) return "Player: " + teamId.toString().substring(0, 8);

        return FTBTeamsCompat.getTeamName(teamId);
    }

    public static boolean isPlayerOnTeam(Player player, UUID teamUUID) {
        if (!(player instanceof ServerPlayer)) return false;
        if (!FTB_TEAMS_LOADED) return player.getUUID().equals(teamUUID);

        return FTBTeamsCompat.isPlayerOnTeam(player, teamUUID);
    }
}
