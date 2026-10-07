package net.phoenix.core.integration.continuum.common;

import net.minecraft.server.MinecraftServer;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;

import java.math.BigInteger;
import java.util.UUID;

/**
 * What outposts cost to run. Upkeep is paid in EU from the owning team's Tesla Network pool: every probe costs
 * {@code outpostUpkeepEUPerProbe} for each production cycle. The pool is only ever read, never created, so a team that
 * has built no Tesla Network simply cannot pay and its outposts idle.
 */
public final class OutpostPower {

    private OutpostPower() {}

    /** True when outposts need power at all (upkeep is on and costs something). */
    public static boolean enabled() {
        var cfg = PhoenixConfigs.INSTANCE.continuum;
        return cfg.outpostRequiresPower && cfg.outpostUpkeepEUPerProbe > 0.0;
    }

    /** EU one cycle costs an outpost of {@code probes} probes. */
    public static long upkeepPerCycle(int probes) {
        if (!enabled()) return 0L;
        return Math.round(PhoenixConfigs.INSTANCE.continuum.outpostUpkeepEUPerProbe) * Math.max(0, probes);
    }

    /** A payer that draws on the team's Tesla Network pool. */
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

    /** Whether the team's pool currently holds enough for one more cycle of an outpost this size. */
    public static boolean canAffordCycle(MinecraftServer server, UUID team, int probes) {
        long perCycle = upkeepPerCycle(probes);
        if (perCycle <= 0) return true;

        var pool = TeslaTeamEnergyData.get(server.overworld()).getNetworksView().get(team);
        return pool != null && pool.stored.compareTo(BigInteger.valueOf(perCycle)) >= 0;
    }
}
