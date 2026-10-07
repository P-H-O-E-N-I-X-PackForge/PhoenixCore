package net.phoenix.core.integration.continuum.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import java.util.*;

/**
 * What a player's client needs to know about their team: the stage of every system and body, and the team's missions.
 * {@code serverNow} lets the client turn the server's wall-clock timestamps into countdowns on its own clock.
 */
public record ContinuumStateSnapshot(long serverNow, Map<ResourceLocation, DiscoveryStage> stages,
                                     List<MissionView> missions, List<OutpostView> outposts) {

    /**
     * An outpost as the client sees it. {@code nextCycleAt} and {@code cycleMillis} let the client keep counting
     * finished cycles between snapshots; {@code maxReady} is the stockpile cap. {@code upkeepPerCycle} is the EU one
     * cycle costs, {@code powered} whether the latest cycles were paid for and {@code lostCycles} how many have been
     * lost to a power shortage since the last haul.
     */
    public record OutpostView(ResourceLocation body, int probes, int readyCycles, long nextCycleAt, long cycleMillis,
                              int maxReady, long upkeepPerCycle, boolean powered, int lostCycles) {

        /** Cycles ready at {@code now}, including ones finished since the snapshot was taken. */
        public int readyAt(long now) {
            int ready = readyCycles;
            if (ready < maxReady && cycleMillis > 0 && now >= nextCycleAt) {
                ready += 1 + (int) ((now - nextCycleAt) / cycleMillis);
            }
            return Math.min(ready, maxReady);
        }
    }

    /** The part of a {@link Mission} the client needs. */
    public record MissionView(UUID id, ResourceLocation destination, Mission.Type type, int probes,
                              long startMillis, long durationMillis, Mission.State state, float wearAfter,
                              String launcherName) {

        public float progress(long now) {
            if (state.finished() || durationMillis <= 0) return 1.0f;
            return Math.max(0.0f, Math.min(1.0f, (now - startMillis) / (float) durationMillis));
        }

        public long endMillis() {
            return startMillis + durationMillis;
        }
    }

    public static ContinuumStateSnapshot build(MinecraftServer server, UUID team) {
        ContinuumTeamData data = ContinuumTeamData.get(server);

        long now = System.currentTimeMillis();
        long cycleMillis = RocketStats.outpostCycleMillis();
        int maxReady = RocketStats.outpostMaxReady();
        data.settleOutposts(server, now);

        Map<ResourceLocation, DiscoveryStage> stages = new LinkedHashMap<>();
        for (ContinuumSystem system : ContinuumData.systems()) {
            stages.put(system.id(), data.effectiveStage(server, team, system.id()));
        }
        for (ContinuumBody body : ContinuumData.allBodies()) {
            stages.put(body.id(), data.effectiveStage(server, team, body.id()));
        }

        List<MissionView> views = new ArrayList<>();
        for (Mission mission : data.missions(team)) {
            // a landed rocket already carries its new wear; one still flying will pick up this trip's cost
            float wearAfter = mission.state.finished() ? RocketStats.wear(mission.rocket) :
                    Math.min(1.0f, RocketStats.wear(mission.rocket) + mission.wearCost);
            views.add(new MissionView(mission.id, mission.destination, mission.type, mission.probes,
                    mission.startMillis, mission.durationMillis, mission.state, wearAfter, mission.launcherName));
        }
        List<OutpostView> outposts = new ArrayList<>();
        data.outposts(team).forEach((body, outpost) -> outposts.add(new OutpostView(body, outpost.probes(),
                outpost.readyCycles(), outpost.cycleStartMillis() + cycleMillis, cycleMillis, maxReady,
                OutpostPower.upkeepPerCycle(outpost.probes()), outpost.powered(), outpost.lostCycles())));
        return new ContinuumStateSnapshot(now, stages, views, outposts);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeLong(serverNow);
        buf.writeVarInt(stages.size());
        stages.forEach((id, stage) -> {
            buf.writeResourceLocation(id);
            buf.writeByte(stage.ordinal());
        });
        buf.writeVarInt(missions.size());
        for (MissionView mission : missions) {
            buf.writeUUID(mission.id());
            buf.writeResourceLocation(mission.destination());
            buf.writeByte(mission.type().ordinal());
            buf.writeVarInt(mission.probes());
            buf.writeLong(mission.startMillis());
            buf.writeLong(mission.durationMillis());
            buf.writeByte(mission.state().ordinal());
            buf.writeFloat(mission.wearAfter());
            buf.writeUtf(mission.launcherName(), 64);
        }
        buf.writeVarInt(outposts.size());
        for (OutpostView outpost : outposts) {
            buf.writeResourceLocation(outpost.body());
            buf.writeVarInt(outpost.probes());
            buf.writeVarInt(outpost.readyCycles());
            buf.writeLong(outpost.nextCycleAt());
            buf.writeLong(outpost.cycleMillis());
            buf.writeVarInt(outpost.maxReady());
            buf.writeVarLong(outpost.upkeepPerCycle());
            buf.writeBoolean(outpost.powered());
            buf.writeVarInt(outpost.lostCycles());
        }
    }

    public static ContinuumStateSnapshot read(FriendlyByteBuf buf) {
        long serverNow = buf.readLong();
        Map<ResourceLocation, DiscoveryStage> stages = new LinkedHashMap<>();
        int stageCount = buf.readVarInt();
        for (int i = 0; i < stageCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int ordinal = Math.max(0, Math.min(buf.readByte(), DiscoveryStage.values().length - 1));
            stages.put(id, DiscoveryStage.values()[ordinal]);
        }
        List<MissionView> missions = new ArrayList<>();
        int missionCount = buf.readVarInt();
        for (int i = 0; i < missionCount; i++) {
            UUID id = buf.readUUID();
            ResourceLocation destination = buf.readResourceLocation();
            int typeOrdinal = Math.max(0, Math.min(buf.readByte(), Mission.Type.values().length - 1));
            int probes = buf.readVarInt();
            long start = buf.readLong();
            long duration = buf.readLong();
            int ordinal = Math.max(0, Math.min(buf.readByte(), Mission.State.values().length - 1));
            float wearAfter = buf.readFloat();
            String launcher = buf.readUtf(64);
            missions.add(new MissionView(id, destination, Mission.Type.values()[typeOrdinal], probes, start, duration,
                    Mission.State.values()[ordinal], wearAfter, launcher));
        }
        List<OutpostView> outposts = new ArrayList<>();
        int outpostCount = buf.readVarInt();
        for (int i = 0; i < outpostCount; i++) {
            outposts.add(new OutpostView(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(),
                    buf.readLong(), buf.readLong(), buf.readVarInt(), buf.readVarLong(), buf.readBoolean(),
                    buf.readVarInt()));
        }
        return new ContinuumStateSnapshot(serverNow, stages, missions, outposts);
    }
}
