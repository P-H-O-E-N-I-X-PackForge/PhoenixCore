package net.phoenix.core.integration.solaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Places that belong to a research team (their Research Terminals), kept so they can be shown on a map
 * without loading the chunks they are in. Entries are checked against the world before being shown, so a
 * terminal that was blown up does not linger.
 */
public class TeamSiteData extends SavedData {

    private static final String DATA_NAME = "phoenixcore_team_sites";

    public record Site(String dimension, int x, int y, int z) {

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private final Map<UUID, Map<String, Site>> byTeam = new HashMap<>();

    public static TeamSiteData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TeamSiteData::new, TeamSiteData::load), DATA_NAME);
    }

    /** Called when a terminal gets an owner or loads with one; safe to call repeatedly. */
    public static void registerTerminal(ServerLevel level, UUID team, BlockPos pos) {
        TeamSiteData data = get(level);
        String dimension = level.dimension().location().toString();
        String key = "terminal/" + dimension + "/" + pos.asLong();
        Map<String, Site> sites = data.byTeam.computeIfAbsent(team, id -> new LinkedHashMap<>());
        if (sites.put(key, new Site(dimension, pos.getX(), pos.getY(), pos.getZ())) == null) data.setDirty();
    }

    /** Remembers a site of another kind (for example a Continuum launch complex) for a team. */
    public static void registerSite(ServerLevel level, UUID team, String kind, BlockPos pos) {
        TeamSiteData data = get(level);
        String dimension = level.dimension().location().toString();
        String key = kind + "/" + dimension + "/" + pos.asLong();
        Map<String, Site> sites = data.byTeam.computeIfAbsent(team, id -> new LinkedHashMap<>());
        if (sites.put(key, new Site(dimension, pos.getX(), pos.getY(), pos.getZ())) == null) data.setDirty();
    }

    public Map<String, Site> forTeam(UUID team) {
        return byTeam.getOrDefault(team, Map.of());
    }

    public void remove(UUID team, String key) {
        Map<String, Site> sites = byTeam.get(team);
        if (sites != null && sites.remove(key) != null) setDirty();
    }

    public static TeamSiteData load(CompoundTag tag, HolderLookup.Provider registries) {
        TeamSiteData data = new TeamSiteData();
        ListTag teams = tag.getList("Teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag teamTag = teams.getCompound(i);
            if (!teamTag.hasUUID("Team")) continue;
            Map<String, Site> sites = new LinkedHashMap<>();
            ListTag list = teamTag.getList("Sites", Tag.TAG_COMPOUND);
            for (int j = 0; j < list.size(); j++) {
                CompoundTag s = list.getCompound(j);
                sites.put(s.getString("Key"),
                        new Site(s.getString("Dimension"), s.getInt("X"), s.getInt("Y"), s.getInt("Z")));
            }
            data.byTeam.put(teamTag.getUUID("Team"), sites);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        for (Map.Entry<UUID, Map<String, Site>> team : byTeam.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", team.getKey());
            ListTag list = new ListTag();
            for (Map.Entry<String, Site> site : team.getValue().entrySet()) {
                CompoundTag s = new CompoundTag();
                s.putString("Key", site.getKey());
                s.putString("Dimension", site.getValue().dimension());
                s.putInt("X", site.getValue().x());
                s.putInt("Y", site.getValue().y());
                s.putInt("Z", site.getValue().z());
                list.add(s);
            }
            teamTag.put("Sites", list);
            teams.add(teamTag);
        }
        tag.put("Teams", teams);
        return tag;
    }
}
