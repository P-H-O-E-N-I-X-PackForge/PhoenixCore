package net.phoenix.core.integration.chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ChroniclesLink {

    public static final String MOD_ID = "phoenix_chronicles";

    private ChroniclesLink() {}

    private static Boolean present;

    public static boolean present() {
        if (present == null) present = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
        return present;
    }

    public static void registerTrigger(String id, @Nullable String description) {
        if (present()) ChroniclesImpl.registerTrigger(id, description);
    }

    public static void fire(@Nullable Player player, String id, @Nullable CompoundTag data) {
        if (player instanceof ServerPlayer && present()) ChroniclesImpl.fire(player, id, data);
    }

    public static void fire(@Nullable MinecraftServer server, @Nullable UUID player, String id,
                            @Nullable CompoundTag data) {
        if (server == null || player == null || !present()) return;
        fire(server.getPlayerList().getPlayer(player), id, data);
    }

    public static void fireTeam(MinecraftServer server, UUID team, String id, @Nullable CompoundTag data) {
        if (!present()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (team.equals(ResearchTeamHelper.getTeamId(player))) ChroniclesImpl.fire(player, id, data);
        }
    }

    public static CompoundTag data(String key, String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }

    public static void setup() {
        if (present()) ChroniclesImpl.setup();
    }
}
