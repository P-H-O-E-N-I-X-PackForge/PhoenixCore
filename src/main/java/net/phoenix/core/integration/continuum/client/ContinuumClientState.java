package net.phoenix.core.integration.continuum.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ContinuumClientState {

    private ContinuumClientState() {}

    private static Map<ResourceLocation, DiscoveryStage> stages = Map.of();
    private static List<ContinuumStateSnapshot.MissionView> missions = List.of();
    private static Map<ResourceLocation, Integer> stations = Map.of();
    private static int missionSlots;
    private static Map<ResourceLocation, ContinuumStateSnapshot.AffinityView> affinities = Map.of();
    private static int stockRockets;
    private static Map<ResourceLocation, ContinuumStateSnapshot.OutpostView> outposts = Map.of();
    private static Map<ResourceLocation, float[]> deposits = Map.of();
    private static java.util.Set<ResourceLocation> hidden = java.util.Set.of();
    private static long clockOffset;

    private static Map<UUID, Mission.State> lastStates = Map.of();

    private static boolean haveStages;

    public static void accept(ContinuumStateSnapshot snapshot) {
        Map<ResourceLocation, DiscoveryStage> before = stages;
        boolean hadStages = haveStages;
        stages = new HashMap<>(snapshot.stages());
        haveStages = true;

        if (hadStages) {
            for (Map.Entry<ResourceLocation, DiscoveryStage> entry : stages.entrySet()) {
                DiscoveryStage old = before.getOrDefault(entry.getKey(), DiscoveryStage.UNKNOWN);
                if (entry.getValue().ordinal() > old.ordinal() && entry.getValue() != DiscoveryStage.UNKNOWN) {
                    ContinuumClientHooks.loreUnlocked(entry.getKey(), entry.getValue());
                }
            }
        }
        missions = List.copyOf(snapshot.missions());

        Map<UUID, Mission.State> states = new HashMap<>();
        for (ContinuumStateSnapshot.MissionView mission : missions) {
            states.put(mission.id(), mission.state());
            Mission.State previousState = lastStates.get(mission.id());
            if (previousState != null && !previousState.finished() && mission.state().finished()) {
                ContinuumClientHooks.missionLanded(mission);
            }
        }
        lastStates = states;
        Map<ResourceLocation, ContinuumStateSnapshot.OutpostView> byBody = new HashMap<>();
        for (ContinuumStateSnapshot.OutpostView outpost : snapshot.outposts()) byBody.put(outpost.body(), outpost);

        for (ContinuumStateSnapshot.OutpostView outpost : byBody.values()) {
            ContinuumStateSnapshot.OutpostView old = outposts.get(outpost.body());
            if (old != null && !old.damaged() && outpost.damaged()) {
                ContinuumClientHooks.outpostDamaged(outpost.body());
            }
        }
        outposts = byBody;
        deposits = new HashMap<>(snapshot.deposits());
        hidden = java.util.Set.copyOf(snapshot.hidden());
        stations = new HashMap<>(snapshot.stations());
        missionSlots = snapshot.missionSlots();
        affinities = new HashMap<>(snapshot.affinities());

        clockOffset = snapshot.serverNow() - System.currentTimeMillis();
    }

    private static @Nullable BlockPos stockPad;
    private static boolean stockFromBuses;
    private static ItemStack stockRocket = ItemStack.EMPTY;
    private static int stockProbes;
    private static int stockKits;

    public static void acceptPadStock(BlockPos pad, boolean buses, ItemStack rocket, int probes, int kits,
                                      int rockets) {
        stockRockets = rockets;
        stockPad = pad;
        stockFromBuses = buses;
        stockRocket = rocket;
        stockProbes = probes;
        stockKits = kits;
    }

    public static boolean padUsesBuses(@Nullable BlockPos pad) {
        return pad != null && pad.equals(stockPad) && stockFromBuses;
    }

    public static ItemStack padRocket() {
        return stockRocket;
    }

    public static int padProbes() {
        return stockProbes;
    }

    public static int padKits() {
        return stockKits;
    }

    public static int padRockets() {
        return stockRockets;
    }

    public static @Nullable ContinuumStateSnapshot.AffinityView affinity(ResourceLocation body) {
        return affinities.get(body);
    }

    public static int stationLevel(ResourceLocation body) {
        return stations.getOrDefault(body, 0);
    }

    public static Map<ResourceLocation, Integer> stations() {
        return stations;
    }

    public static int missionSlots() {
        return missionSlots;
    }

    public static boolean isHidden(ResourceLocation body) {
        return hidden.contains(body);
    }

    public static float richness(ResourceLocation body, int index) {
        float[] values = deposits.get(body);
        return values != null && index >= 0 && index < values.length ? values[index] : 0.0f;
    }

    public static int mappedDeposits(ResourceLocation body) {
        float[] values = deposits.get(body);
        if (values == null) return 0;
        int count = 0;
        for (float value : values) {
            if (value > 0.0f) count++;
        }
        return count;
    }

    public static DiscoveryStage stage(ResourceLocation id) {
        return stages.getOrDefault(id, DiscoveryStage.UNKNOWN);
    }

    public static List<ContinuumStateSnapshot.MissionView> missions() {
        return missions;
    }

    public static @Nullable ContinuumStateSnapshot.OutpostView outpost(ResourceLocation body) {
        return outposts.get(body);
    }

    public static @Nullable ContinuumStateSnapshot.MissionView mission(UUID id) {
        for (ContinuumStateSnapshot.MissionView mission : missions) {
            if (mission.id().equals(id)) return mission;
        }
        return null;
    }

    public static long now() {
        return System.currentTimeMillis() + clockOffset;
    }
}
