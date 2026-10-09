package net.phoenix.core.integration.continuum.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ContinuumTeamData extends SavedData {

    private static final String ID = "continuum_state";

    private final Map<UUID, Map<ResourceLocation, DiscoveryStage>> stages = new HashMap<>();
    private final Map<UUID, List<Mission>> missions = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Outpost>> outposts = new HashMap<>();

    private final Map<UUID, Map<ResourceLocation, Integer>> depths = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Integer>> stations = new HashMap<>();

    public static ContinuumTeamData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ContinuumTeamData::load, ContinuumTeamData::new, ID);
    }

    private static DiscoveryStage initial(ResourceLocation id) {
        ContinuumBody body = ContinuumData.body(id);
        if (body != null) return body.initialStage();
        ContinuumSystem system = ContinuumData.system(id);
        return system != null ? system.initialStage() : DiscoveryStage.UNKNOWN;
    }

    private static DiscoveryStage max(DiscoveryStage a, DiscoveryStage b) {
        return a.atLeast(b) ? a : b;
    }

    public DiscoveryStage storedStage(UUID team, ResourceLocation id) {
        return max(stages.getOrDefault(team, Map.of()).getOrDefault(id, DiscoveryStage.UNKNOWN), initial(id));
    }

    public boolean isHidden(MinecraftServer server, UUID team, ContinuumBody body) {
        if (body.discipline() == null) return false;
        String chosen = WorldResearchData.get(server.overworld()).getDiscipline(team);
        return !body.discipline().equals(chosen == null ? "none" : chosen);
    }

    public DiscoveryStage effectiveStage(MinecraftServer server, UUID team, ResourceLocation id) {
        DiscoveryStage stage = max(stages.getOrDefault(team, Map.of()).getOrDefault(id, DiscoveryStage.UNKNOWN),
                initial(id));

        ContinuumBody body = ContinuumData.body(id);
        if (body != null && isHidden(server, team, body)) return DiscoveryStage.UNKNOWN;
        if (body != null) {
            WorldResearchData research = null;
            if (body.surveyedResearch() != null || body.detectedResearch() != null) {
                research = WorldResearchData.get(server.overworld());
            }
            if (research != null) {
                if (body.surveyedResearch() != null && research.isUnlocked(team, body.surveyedResearch())) {
                    stage = max(stage, DiscoveryStage.SURVEYED);
                } else if (body.detectedResearch() != null && research.isUnlocked(team, body.detectedResearch())) {
                    stage = max(stage, DiscoveryStage.DETECTED);
                }
            }
            return stage;
        }

        ContinuumSystem system = ContinuumData.system(id);
        if (system != null) {
            if (system.surveyedResearch() != null || system.detectedResearch() != null) {
                WorldResearchData research = WorldResearchData.get(server.overworld());
                if (system.surveyedResearch() != null && research.isUnlocked(team, system.surveyedResearch())) {
                    stage = max(stage, DiscoveryStage.SURVEYED);
                } else if (system.detectedResearch() != null &&
                        research.isUnlocked(team, system.detectedResearch())) {
                            stage = max(stage, DiscoveryStage.DETECTED);
                        }
            }
            for (ContinuumBody inSystem : ContinuumData.bodiesOf(id)) {
                DiscoveryStage bodyStage = effectiveStage(server, team, inSystem.id());
                if (bodyStage == DiscoveryStage.SURVEYED) return DiscoveryStage.SURVEYED;
                if (bodyStage == DiscoveryStage.DETECTED) stage = max(stage, DiscoveryStage.DETECTED);
            }
        }
        return stage;
    }

    public boolean raise(UUID team, ResourceLocation id, DiscoveryStage stage) {
        Map<ResourceLocation, DiscoveryStage> map = stages.computeIfAbsent(team, t -> new HashMap<>());
        DiscoveryStage current = map.getOrDefault(id, DiscoveryStage.UNKNOWN);
        if (current.atLeast(stage)) return false;
        map.put(id, stage);
        setDirty();
        return true;
    }

    public void force(UUID team, ResourceLocation id, DiscoveryStage stage) {
        stages.computeIfAbsent(team, t -> new HashMap<>()).put(id, stage);
        setDirty();
    }

    public void resetDiscovery(UUID team) {
        stages.remove(team);
        depths.remove(team);
        setDirty();
    }

    public int stationLevel(UUID team, ResourceLocation body) {
        return stations.getOrDefault(team, Map.of()).getOrDefault(body, 0);
    }

    public Map<ResourceLocation, Integer> stations(UUID team) {
        return stations.getOrDefault(team, Map.of());
    }

    public int stationCount(UUID team) {
        return stations.getOrDefault(team, Map.of()).size();
    }

    public void setStationLevel(UUID team, ResourceLocation body, int level) {
        if (level <= 0) {
            Map<ResourceLocation, Integer> mine = stations.get(team);
            if (mine != null) mine.remove(body);
        } else {
            stations.computeIfAbsent(team, t -> new HashMap<>()).put(body, Math.min(Stations.MAX_LEVEL, level));
        }
        setDirty();
    }

    public int depth(UUID team, ResourceLocation body) {
        return depths.getOrDefault(team, Map.of()).getOrDefault(body, 0);
    }

    public boolean revealDeposit(UUID team, ResourceLocation body, int total) {
        int current = depth(team, body);
        if (current >= total) return false;
        depths.computeIfAbsent(team, t -> new HashMap<>()).put(body, current + 1);
        setDirty();
        return true;
    }

    public void setDepth(UUID team, ResourceLocation body, int depth) {
        depths.computeIfAbsent(team, t -> new HashMap<>()).put(body, Math.max(0, depth));
        setDirty();
    }

    public List<Mission> missions(UUID team) {
        return missions.computeIfAbsent(team, t -> new ArrayList<>());
    }

    public Collection<UUID> teamsWithMissions() {
        return new ArrayList<>(missions.keySet());
    }

    public void addMission(Mission mission) {
        missions(mission.team).add(mission);
        setDirty();
    }

    public void removeMission(UUID team, UUID missionId) {
        missions(team).removeIf(m -> m.id.equals(missionId));
        setDirty();
    }

    public Optional<Mission> mission(UUID team, UUID missionId) {
        return missions(team).stream().filter(m -> m.id.equals(missionId)).findFirst();
    }

    public Map<ResourceLocation, Outpost> outposts(UUID team) {
        return outposts.computeIfAbsent(team, t -> new HashMap<>());
    }

    public @Nullable Outpost outpost(UUID team, ResourceLocation body) {
        Map<ResourceLocation, Outpost> map = outposts.get(team);
        return map == null ? null : map.get(body);
    }

    public Outpost deployProbes(UUID team, ResourceLocation body, int probes, long now) {
        Outpost outpost = outposts(team).get(body);
        if (outpost == null) {
            outpost = new Outpost(probes, 0, now);
            outposts(team).put(body, outpost);
        } else {
            outpost.addProbes(probes);
        }
        setDirty();
        return outpost;
    }

    public void clearOutposts(UUID team) {
        outposts.remove(team);
        setDirty();
    }

    public java.util.Set<UUID> settleOutposts(MinecraftServer server, long now) {
        long cycleMillis = RocketStats.outpostCycleMillis();
        int maxReady = RocketStats.outpostMaxReady();
        java.util.Set<UUID> newlyDamaged = new java.util.HashSet<>();

        for (var team : outposts.entrySet()) {
            Outpost.CyclePayer payer = OutpostPower.payerFor(server, team.getKey());
            for (var entry : team.getValue().entrySet()) {
                if (!entry.getValue().settle(now, cycleMillis, maxReady, payer)) continue;

                newlyDamaged.add(team.getKey());
                ContinuumBody body = ContinuumData.body(entry.getKey());
                String name = body != null ? body.name() : entry.getKey().getPath();
                ContinuumMissions.notifyTeam(server, team.getKey(), net.minecraft.network.chat.Component
                        .literal("The outpost on " + name + " has been damaged and stopped producing. " +
                                "Fly a repair run to fix it.")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
        setDirty();
        return newlyDamaged;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag stageTeams = new ListTag();
        for (var team : stages.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", team.getKey());
            ListTag list = new ListTag();
            for (var entry : team.getValue().entrySet()) {
                CompoundTag e = new CompoundTag();
                e.putString("Id", entry.getKey().toString());
                e.putByte("Stage", (byte) entry.getValue().ordinal());
                list.add(e);
            }
            teamTag.put("Stages", list);
            stageTeams.add(teamTag);
        }
        tag.put("StageTeams", stageTeams);

        ListTag missionList = new ListTag();
        for (List<Mission> list : missions.values()) {
            for (Mission mission : list) missionList.add(mission.save());
        }
        tag.put("Missions", missionList);

        ListTag outpostTeams = new ListTag();
        for (var team : outposts.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", team.getKey());
            ListTag list = new ListTag();
            for (var entry : team.getValue().entrySet()) {
                CompoundTag e = entry.getValue().save();
                e.putString("Body", entry.getKey().toString());
                list.add(e);
            }
            teamTag.put("Outposts", list);
            outpostTeams.add(teamTag);
        }
        tag.put("OutpostTeams", outpostTeams);

        ListTag depthTeams = new ListTag();
        for (var team : depths.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", team.getKey());
            ListTag list = new ListTag();
            for (var entry : team.getValue().entrySet()) {
                CompoundTag e = new CompoundTag();
                e.putString("Body", entry.getKey().toString());
                e.putInt("Depth", entry.getValue());
                list.add(e);
            }
            teamTag.put("Depths", list);
            depthTeams.add(teamTag);
        }
        tag.put("DepthTeams", depthTeams);

        ListTag stationTeams = new ListTag();
        for (var team : stations.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", team.getKey());
            ListTag list = new ListTag();
            for (var entry : team.getValue().entrySet()) {
                CompoundTag e = new CompoundTag();
                e.putString("Body", entry.getKey().toString());
                e.putInt("Level", entry.getValue());
                list.add(e);
            }
            teamTag.put("Stations", list);
            stationTeams.add(teamTag);
        }
        tag.put("StationTeams", stationTeams);
        return tag;
    }

    public static ContinuumTeamData load(CompoundTag tag) {
        ContinuumTeamData data = new ContinuumTeamData();

        for (Tag t : tag.getList("StageTeams", Tag.TAG_COMPOUND)) {
            CompoundTag teamTag = (CompoundTag) t;
            Map<ResourceLocation, DiscoveryStage> map = new HashMap<>();
            for (Tag s : teamTag.getList("Stages", Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) s;
                int ordinal = Math.max(0, Math.min(e.getByte("Stage"), DiscoveryStage.values().length - 1));
                map.put(new ResourceLocation(e.getString("Id")), DiscoveryStage.values()[ordinal]);
            }
            data.stages.put(teamTag.getUUID("Team"), map);
        }

        for (Tag t : tag.getList("Missions", Tag.TAG_COMPOUND)) {
            Mission mission = Mission.load((CompoundTag) t);
            data.missions.computeIfAbsent(mission.team, k -> new ArrayList<>()).add(mission);
        }

        for (Tag t : tag.getList("OutpostTeams", Tag.TAG_COMPOUND)) {
            CompoundTag teamTag = (CompoundTag) t;
            Map<ResourceLocation, Outpost> map = new HashMap<>();
            for (Tag o : teamTag.getList("Outposts", Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) o;
                map.put(new ResourceLocation(e.getString("Body")), Outpost.load(e));
            }
            data.outposts.put(teamTag.getUUID("Team"), map);
        }

        for (Tag t : tag.getList("DepthTeams", Tag.TAG_COMPOUND)) {
            CompoundTag teamTag = (CompoundTag) t;
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (Tag d : teamTag.getList("Depths", Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) d;
                map.put(new ResourceLocation(e.getString("Body")), e.getInt("Depth"));
            }
            data.depths.put(teamTag.getUUID("Team"), map);
        }

        for (Tag t : tag.getList("StationTeams", Tag.TAG_COMPOUND)) {
            CompoundTag teamTag = (CompoundTag) t;
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (Tag s : teamTag.getList("Stations", Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) s;
                map.put(new ResourceLocation(e.getString("Body")), e.getInt("Level"));
            }
            data.stations.put(teamTag.getUUID("Team"), map);
        }
        return data;
    }
}
