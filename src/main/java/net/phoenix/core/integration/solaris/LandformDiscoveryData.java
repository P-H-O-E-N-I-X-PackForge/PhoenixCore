package net.phoenix.core.integration.solaris;

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
 * Which Conflux landforms each research team has discovered. Stored with everything needed to put the
 * landform back on a map, so markers can be rebuilt without regenerating or reloading terrain.
 */
public class LandformDiscoveryData extends SavedData {

    private static final String DATA_NAME = "phoenixcore_landform_discovery";

    public record Entry(String dimension, String label, String color, String icon, int x, int z, int y) {}

    private final Map<UUID, Map<String, Entry>> byTeam = new HashMap<>();

    public static LandformDiscoveryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LandformDiscoveryData::new, LandformDiscoveryData::load), DATA_NAME);
    }

    /** Returns true if this landform was not known to the team before. */
    public boolean discover(UUID team, String key, Entry entry) {
        Map<String, Entry> known = byTeam.computeIfAbsent(team, id -> new LinkedHashMap<>());
        if (known.putIfAbsent(key, entry) != null) return false;
        setDirty();
        return true;
    }

    public Map<String, Entry> forTeam(UUID team) {
        return byTeam.getOrDefault(team, Map.of());
    }

    public static LandformDiscoveryData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandformDiscoveryData data = new LandformDiscoveryData();
        ListTag teams = tag.getList("Teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag teamTag = teams.getCompound(i);
            if (!teamTag.hasUUID("Team")) continue;
            Map<String, Entry> known = new LinkedHashMap<>();
            ListTag entries = teamTag.getList("Landforms", Tag.TAG_COMPOUND);
            for (int j = 0; j < entries.size(); j++) {
                CompoundTag e = entries.getCompound(j);
                known.put(e.getString("Key"), new Entry(e.getString("Dimension"), e.getString("Label"),
                        e.getString("Color"), e.getString("Icon"), e.getInt("X"), e.getInt("Z"), e.getInt("Y")));
            }
            data.byTeam.put(teamTag.getUUID("Team"), known);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        for (Map.Entry<UUID, Map<String, Entry>> team : byTeam.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", team.getKey());
            ListTag entries = new ListTag();
            for (Map.Entry<String, Entry> known : team.getValue().entrySet()) {
                Entry value = known.getValue();
                CompoundTag e = new CompoundTag();
                e.putString("Key", known.getKey());
                e.putString("Dimension", value.dimension());
                e.putString("Label", value.label());
                e.putString("Color", value.color());
                e.putString("Icon", value.icon());
                e.putInt("X", value.x());
                e.putInt("Z", value.z());
                e.putInt("Y", value.y());
                entries.add(e);
            }
            teamTag.put("Landforms", entries);
            teams.add(teamTag);
        }
        tag.put("Teams", teams);
        return tag;
    }
}
