package net.phoenix.core.integration.continuum.common;

import net.minecraft.server.MinecraftServer;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;

import java.math.BigInteger;
import java.util.UUID;

public final class OutpostPower {

    private OutpostPower() {}

    public static boolean enabled() {
        var cfg = PhoenixConfigs.INSTANCE.continuum;
        return cfg.outpostRequiresPower && cfg.outpostUpkeepEUPerProbe > 0.0;
    }

    public static long upkeepPerCycle(int probes) {
        if (!enabled()) return 0L;
        return Math.round(PhoenixConfigs.INSTANCE.continuum.outpostUpkeepEUPerProbe) * Math.max(0, probes);
    }

    public static Outpost.CyclePayer payerFor(MinecraftServer server, UUID team) {
        if (!enabled()) return Outpost.FREE;

        return (cycles, probes) -> {
            long perCycle = upkeepPerCycle(probes);
            if (perCycle <= 0 || cycles <= 0) return cycles;

            var pool = TeslaTeamEnergyData.get(server.overworld()).getNetworksView().get(team);
            if (pool == null) return 0;

            BigInteger affordable = pool.stored.divide(BigInteger.valueOf(perCycle));
            int paid = affordable.min(BigInteger.valueOf(cycles)).intValue();
            if (paid > 0) pool.drain(BigInteger.valueOf(perCycle).multiply(BigInteger.valueOf(paid)));
            return paid;
        };
    }

    public static boolean canAffordCycle(MinecraftServer server, UUID team, int probes) {
        long perCycle = upkeepPerCycle(probes);
        if (perCycle <= 0) return true;

        var pool = TeslaTeamEnergyData.get(server.overworld()).getNetworksView().get(team);
        return pool != null && pool.stored.compareTo(BigInteger.valueOf(perCycle)) >= 0;
    }
}
