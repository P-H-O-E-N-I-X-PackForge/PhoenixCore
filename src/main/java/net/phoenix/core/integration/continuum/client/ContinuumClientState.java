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

/**
 * What this client currently knows about its team: the stage of every system and body, and the team's missions. It is
 * only ever replaced wholesale by a snapshot from the server; nothing on the client changes it.
 */
public final class ContinuumClientState {

    private ContinuumClientState() {}

    private static Map<ResourceLocation, DiscoveryStage> stages = Map.of();
    private static List<ContinuumStateSnapshot.MissionView> missions = List.of();
    private static Map<ResourceLocation, ContinuumStateSnapshot.OutpostView> outposts = Map.of();
    private static Map<ResourceLocation, float[]> deposits = Map.of();
    private static long clockOffset;

    private static Map<UUID, Mission.State> lastStates = Map.of();

    private static boolean haveStages;

    public static void accept(ContinuumStateSnapshot snapshot) {
        Map<ResourceLocation, DiscoveryStage> before = stages;
        boolean hadStages = haveStages;
        stages = new HashMap<>(snapshot.stages());
        haveStages = true;

        // a body that has just risen a stage has just written a new Archive entry (not on the first snapshot)
        if (hadStages) {
            for (Map.Entry<ResourceLocation, DiscoveryStage> entry : stages.entrySet()) {
                DiscoveryStage old = before.getOrDefault(entry.getKey(), DiscoveryStage.UNKNOWN);
                if (entry.getValue().ordinal() > old.ordinal() && entry.getValue() != DiscoveryStage.UNKNOWN) {
                    ContinuumClientHooks.loreUnlocked(entry.getKey(), entry.getValue());
                }
            }
        }
        missions = List.copyOf(snapshot.missions());

        // a mission we saw running that has now finished has just landed
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

        // an outpost we saw working that is now broken has just had an incident
        for (ContinuumStateSnapshot.OutpostView outpost : byBody.values()) {
            ContinuumStateSnapshot.OutpostView old = outposts.get(outpost.body());
            if (old != null && !old.damaged() && outpost.damaged()) {
                ContinuumClientHooks.outpostDamaged(outpost.body());
            }
        }
        outposts = byBody;
        deposits = new HashMap<>(snapshot.deposits());
        // the server's wall clock minus ours, so countdowns agree even when the two machines' clocks differ
        clockOffset = snapshot.serverNow() - System.currentTimeMillis();
    }

    private static @Nullable BlockPos stockPad;
    private static boolean stockFromBuses;
    private static ItemStack stockRocket = ItemStack.EMPTY;
    private static int stockProbes;
    private static int stockKits;

    public static void acceptPadStock(BlockPos pad, boolean buses, ItemStack rocket, int probes, int kits) {
        stockPad = pad;
        stockFromBuses = buses;
        stockRocket = rocket;
        stockProbes = probes;
        stockKits = kits;
    }

    /** Whether the pad's stock was counted in its item input buses (otherwise the player's inventory is used). */
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

    /** The mapped richness of deposit {@code index} on a body (in its yield order), or 0 if it is not mapped yet. */
    public static float richness(ResourceLocation body, int index) {
        float[] values = deposits.get(body);
        return values != null && index >= 0 && index < values.length ? values[index] : 0.0f;
    }

    /** How many of a body's deposits this team has mapped. */
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

    /** The team's outpost on a body, or null. */
    public static @Nullable ContinuumStateSnapshot.OutpostView outpost(ResourceLocation body) {
        return outposts.get(body);
    }

    public static @Nullable ContinuumStateSnapshot.MissionView mission(UUID id) {
        for (ContinuumStateSnapshot.MissionView mission : missions) {
            if (mission.id().equals(id)) return mission;
        }
        return null;
    }

    /** The current time on the server's clock. */
    public static long now() {
        return System.currentTimeMillis() + clockOffset;
    }
}
