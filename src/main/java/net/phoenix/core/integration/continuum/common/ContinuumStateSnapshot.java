package net.phoenix.core.integration.continuum.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import java.util.*;

public record ContinuumStateSnapshot(long serverNow, Map<ResourceLocation, DiscoveryStage> stages,
                                     List<MissionView> missions, List<OutpostView> outposts,
                                     Map<ResourceLocation, float[]> deposits,
                                     java.util.Set<ResourceLocation> hidden,
                                     Map<ResourceLocation, Integer> stations, int missionSlots,
                                     Map<ResourceLocation, AffinityView> affinities) {

    public record AffinityView(float trip, float wear, float yield, String label) {}

    public record OutpostView(ResourceLocation body, int probes, int readyCycles, long nextCycleAt, long cycleMillis,
                              int maxReady, long upkeepPerCycle, boolean powered, int lostCycles, boolean damaged,
                              int brokenCycles) {

        public int readyAt(long now) {
            int ready = readyCycles;
            if (ready < maxReady && cycleMillis > 0 && now >= nextCycleAt) {
                ready += 1 + (int) ((now - nextCycleAt) / cycleMillis);
            }
            return Math.min(ready, maxReady);
        }
    }

    public record MissionView(UUID id, ResourceLocation destination, Mission.Type type, int probes,
                              long startMillis, long durationMillis, Mission.State state, float wearAfter,
                              String launcherName, List<String> events, long distressUntil, boolean rescuing) {

        public boolean stranded() {
            return distressUntil > 0;
        }

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

            float wearAfter = mission.state.finished() ? RocketStats.wear(mission.rocket) :
                    Math.min(1.0f, RocketStats.wear(mission.rocket) + mission.wearCost);
            views.add(new MissionView(mission.id, mission.destination, mission.type, mission.probes,
                    mission.startMillis, mission.durationMillis, mission.state, wearAfter, mission.launcherName,
                    mission.state.finished() ? List.copyOf(mission.events) : List.of(), mission.distressUntil,
                    mission.rescuing));
        }
        List<OutpostView> outposts = new ArrayList<>();
        data.outposts(team).forEach((body, outpost) -> outposts.add(new OutpostView(body, outpost.probes(),
                outpost.readyCycles(), outpost.cycleStartMillis() + cycleMillis, cycleMillis, maxReady,
                OutpostPower.upkeepPerCycle(outpost.probes()), outpost.powered(), outpost.lostCycles(),
                outpost.damaged(), outpost.brokenCycles())));

        Map<ResourceLocation, float[]> deposits = new LinkedHashMap<>();
        for (ContinuumBody body : ContinuumData.allBodies()) {
            int depth = data.depth(team, body.id());
            if (depth <= 0 || body.yields().isEmpty()) continue;
            float[] values = new float[body.yields().size()];
            for (int i = 0; i < values.length && i < depth; i++) {
                values[i] = Deposits.richness(team, body.id(), body.yields().get(i).item());
            }
            deposits.put(body.id(), values);
        }
        java.util.Set<ResourceLocation> hidden = new java.util.HashSet<>();
        for (ContinuumBody body : ContinuumData.allBodies()) {
            if (data.isHidden(server, team, body)) hidden.add(body.id());
        }
        Map<ResourceLocation, AffinityView> affinities = new LinkedHashMap<>();
        for (ContinuumBody body : ContinuumData.allBodies()) {
            Affinities.Factors factors = Affinities.of(server, team, body);
            if (!factors.none()) {
                affinities.put(body.id(), new AffinityView(factors.trip(), factors.wear(), factors.yield(),
                        factors.label()));
            }
        }
        return new ContinuumStateSnapshot(now, stages, views, outposts, deposits, hidden,
                new LinkedHashMap<>(data.stations(team)),
                net.phoenix.core.configs.PhoenixConfigs.INSTANCE.continuum.maxMissionsPerTeam +
                        data.stationCount(team),
                affinities);
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
            buf.writeVarInt(mission.events().size());
            for (String event : mission.events()) buf.writeUtf(event, 32);
            buf.writeLong(mission.distressUntil());
            buf.writeBoolean(mission.rescuing());
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
            buf.writeBoolean(outpost.damaged());
            buf.writeVarInt(outpost.brokenCycles());
        }
        buf.writeVarInt(deposits.size());
        deposits.forEach((body, values) -> {
            buf.writeResourceLocation(body);
            buf.writeVarInt(values.length);
            for (float value : values) buf.writeFloat(value);
        });
        buf.writeVarInt(stations.size());
        stations.forEach((body, level) -> {
            buf.writeResourceLocation(body);
            buf.writeVarInt(level);
        });
        buf.writeVarInt(missionSlots);
        buf.writeVarInt(affinities.size());
        affinities.forEach((body, view) -> {
            buf.writeResourceLocation(body);
            buf.writeFloat(view.trip());
            buf.writeFloat(view.wear());
            buf.writeFloat(view.yield());
            buf.writeUtf(view.label(), 256);
        });
        buf.writeVarInt(hidden.size());
        for (ResourceLocation id : hidden) buf.writeResourceLocation(id);
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
            List<String> events = new ArrayList<>();
            int eventCount = buf.readVarInt();
            for (int e = 0; e < eventCount; e++) events.add(buf.readUtf(32));
            long distressUntil = buf.readLong();
            boolean rescuing = buf.readBoolean();
            missions.add(new MissionView(id, destination, Mission.Type.values()[typeOrdinal], probes, start, duration,
                    Mission.State.values()[ordinal], wearAfter, launcher, events, distressUntil, rescuing));
        }
        List<OutpostView> outposts = new ArrayList<>();
        int outpostCount = buf.readVarInt();
        for (int i = 0; i < outpostCount; i++) {
            outposts.add(new OutpostView(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(),
                    buf.readLong(), buf.readLong(), buf.readVarInt(), buf.readVarLong(), buf.readBoolean(),
                    buf.readVarInt(), buf.readBoolean(), buf.readVarInt()));
        }
        Map<ResourceLocation, float[]> deposits = new LinkedHashMap<>();
        int depositCount = buf.readVarInt();
        for (int i = 0; i < depositCount; i++) {
            ResourceLocation body = buf.readResourceLocation();
            float[] values = new float[buf.readVarInt()];
            for (int v = 0; v < values.length; v++) values[v] = buf.readFloat();
            deposits.put(body, values);
        }
        Map<ResourceLocation, Integer> stations = new LinkedHashMap<>();
        int stationCount = buf.readVarInt();
        for (int i = 0; i < stationCount; i++) stations.put(buf.readResourceLocation(), buf.readVarInt());
        int missionSlots = buf.readVarInt();
        Map<ResourceLocation, AffinityView> affinities = new LinkedHashMap<>();
        int affinityCount = buf.readVarInt();
        for (int i = 0; i < affinityCount; i++) {
            affinities.put(buf.readResourceLocation(),
                    new AffinityView(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readUtf(256)));
        }
        java.util.Set<ResourceLocation> hidden = new java.util.HashSet<>();
        int hiddenCount = buf.readVarInt();
        for (int i = 0; i < hiddenCount; i++) hidden.add(buf.readResourceLocation());
        return new ContinuumStateSnapshot(serverNow, stages, missions, outposts, deposits, hidden, stations,
                missionSlots, affinities);
    }
}
