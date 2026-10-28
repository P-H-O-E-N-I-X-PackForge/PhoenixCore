package net.phoenix.core.integration.chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * PhoenixCore's way into Phoenix Chronicles, safe to call with Chronicles not installed (every call does nothing then).
 * This is the extension point for quests: fire a trigger and any quest with an "External Trigger" task of that id counts
 * it. Triggers registered here show up in the quest editor. Addons use it the same way:
 *
 * <pre>{@code
 * ChroniclesLink.registerTrigger("mymod:my_machine/done", "A player's My Machine finished a run.");
 * ChroniclesLink.fire(player, "mymod:my_machine/done", data);
 * }</pre>
 *
 * This class never touches a Chronicles class itself, so it loads without Chronicles; the work is in
 * {@link ChroniclesImpl}, which is only loaded when the mod is there.
 */
public final class ChroniclesLink {

    public static final String MOD_ID = "phoenix_chronicles";

    private ChroniclesLink() {}

    private static Boolean present;

    public static boolean present() {
        if (present == null) present = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
        return present;
    }

    /** Make a trigger id known to the quest editor (it can be used without this, but is not listed). */
    public static void registerTrigger(String id, @Nullable String description) {
        if (present()) ChroniclesImpl.registerTrigger(id, description);
    }

    /** One player did the thing: every quest of theirs waiting on this trigger counts it. Server side only. */
    public static void fire(@Nullable Player player, String id, @Nullable CompoundTag data) {
        if (player instanceof ServerPlayer && present()) ChroniclesImpl.fire(player, id, data);
    }

    /** As {@link #fire}, for a player who may be offline (a machine's owner): does nothing if they are not on. */
    public static void fire(@Nullable MinecraftServer server, @Nullable UUID player, String id,
                            @Nullable CompoundTag data) {
        if (server == null || player == null || !present()) return;
        fire(server.getPlayerList().getPlayer(player), id, data);
    }

    /** Every online member of a Conflux research team did the thing. */
    public static void fireTeam(MinecraftServer server, UUID team, String id, @Nullable CompoundTag data) {
        if (!present()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (team.equals(ResearchTeamHelper.getTeamId(player))) ChroniclesImpl.fire(player, id, data);
        }
    }

    /** A small data tag with one text value, the usual payload of a trigger. */
    public static CompoundTag data(String key, String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }

    /** Called once at startup: registers PhoenixCore's own triggers and task types. */
    public static void setup() {
        if (present()) ChroniclesImpl.setup();
    }
}
