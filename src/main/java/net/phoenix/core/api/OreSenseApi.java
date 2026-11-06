package net.phoenix.core.api;

import net.minecraft.server.MinecraftServer;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.solaris.OreMemoryData;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OreSenseApi {

    public enum Persistence {
        PERSISTENT,
        LIVE
    }

    private static final Map<String, Persistence> OVERRIDES = new ConcurrentHashMap<>();

    private OreSenseApi() {}

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

    public static void forget(MinecraftServer server, UUID team) {
        OreMemoryData.get(server.overworld()).forget(team);
    }

    public static void forgetDiscipline(MinecraftServer server, String discipline) {
        OreMemoryData.get(server.overworld()).forgetDimension("phoenixcore:conflux/" +
                discipline.toLowerCase(Locale.ROOT));
    }

    public static void forgetAll(MinecraftServer server) {
        OreMemoryData.get(server.overworld()).forgetAll();
    }
}
