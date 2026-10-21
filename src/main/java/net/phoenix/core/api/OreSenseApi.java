package net.phoenix.core.api;

import net.minecraft.server.MinecraftServer;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.solaris.OreMemoryData;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls whether ore sense (the Solaris map's ore markers inside Conflux dimensions) remembers what a research team
 * has
 * seen. Persistent: ore a team has sensed stays on their map, and is refreshed when they come back to it. Live: only
 * the ore around the player right now is shown.
 *
 * <p>
 * The default is the {@code oreSenseMemory} config option, with {@code oreSenseLiveDisciplines} naming disciplines that
 * should be live regardless. {@link #setPersistence} overrides both at runtime (not saved, so call it on server start),
 * and {@link OreSenseEvent.Remember} lets a pack veto individual readings.
 */
public final class OreSenseApi {

    public enum Persistence {
        PERSISTENT,
        LIVE
    }

    private static final Map<String, Persistence> OVERRIDES = new ConcurrentHashMap<>();

    private OreSenseApi() {}

    /** Overrides the persistence for one discipline ({@code phoenix}, {@code sculk}...). Null removes the override. */
    public static void setPersistence(String discipline, @Nullable Persistence persistence) {
        String key = discipline.toLowerCase(Locale.ROOT);
        if (persistence == null) OVERRIDES.remove(key);
        else OVERRIDES.put(key, persistence);
    }

    public static Persistence getPersistence(String discipline) {
        String key = discipline.toLowerCase(Locale.ROOT);
        Persistence override = OVERRIDES.get(key);
        if (override != null) return override;

        var config = PhoenixConfigs.INSTANCE.confluxSolaris;
        for (String live : config.oreSenseLiveDisciplines.split(",")) {
            if (live.trim().toLowerCase(Locale.ROOT).equals(key)) return Persistence.LIVE;
        }
        return config.oreSenseMemory;
    }

    /** Forgets everything one team has remembered. */
    public static void forget(MinecraftServer server, UUID team) {
        OreMemoryData.get(server.overworld()).forget(team);
    }

    /** Forgets everything remembered in one discipline's dimension, for every team. */
    public static void forgetDiscipline(MinecraftServer server, String discipline) {
        OreMemoryData.get(server.overworld()).forgetDimension("phoenixcore:conflux/" +
                discipline.toLowerCase(Locale.ROOT));
    }

    /** Forgets everything every team has remembered. */
    public static void forgetAll(MinecraftServer server) {
        OreMemoryData.get(server.overworld()).forgetAll();
    }
}
