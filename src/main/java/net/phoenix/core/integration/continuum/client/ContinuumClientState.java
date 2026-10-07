package net.phoenix.core.integration.continuum.client;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
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
    private static long clockOffset;

    public static void accept(ContinuumStateSnapshot snapshot) {
        stages = new HashMap<>(snapshot.stages());
        missions = List.copyOf(snapshot.missions());
        Map<ResourceLocation, ContinuumStateSnapshot.OutpostView> byBody = new HashMap<>();
        for (ContinuumStateSnapshot.OutpostView outpost : snapshot.outposts()) byBody.put(outpost.body(), outpost);
        outposts = byBody;
        // the server's wall clock minus ours, so countdowns agree even when the two machines' clocks differ
        clockOffset = snapshot.serverNow() - System.currentTimeMillis();
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
