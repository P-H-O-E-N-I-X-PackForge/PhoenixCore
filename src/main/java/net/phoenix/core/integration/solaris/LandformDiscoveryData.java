package net.phoenix.core.integration.solaris;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.integration.conflux.dimension.worldgen.Landforms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LandformDiscoveryData extends SavedData {

    private static final String DATA_NAME = "phoenixcore_landform_discovery";

    public record Entry(String dimension, String label, String color, String icon, int x, int z, int y,
                        List<Landforms.Zone> zones) {}

    private final Map<UUID, Map<String, Entry>> byTeam = new HashMap<>();

    public static LandformDiscoveryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LandformDiscoveryData::new, LandformDiscoveryData::load), DATA_NAME);
    }

    public boolean discover(UUID team, String key, Entry entry) {
        Map<String, Entry> known = byTeam.computeIfAbsent(team, id -> new LinkedHashMap<>());
        Entry existing = known.get(key);
        if (existing == null) {
            known.put(key, entry);
            setDirty();
            return true;
        }

        if (existing.zones().isEmpty() && !entry.zones().isEmpty()) {
            known.put(key, entry);
            setDirty();
        }
        return false;
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
                List<Landforms.Zone> zones = new ArrayList<>();
                ListTag zoneList = e.getList("Zones", Tag.TAG_COMPOUND);
                for (int k = 0; k < zoneList.size(); k++) {
                    CompoundTag z = zoneList.getCompound(k);
                    zones.add(new Landforms.Zone(Landforms.ZoneType.values()[z.getByte("Type")],
                            Landforms.ZoneShape.values()[z.getByte("Shape")], z.getDouble("X"), z.getDouble("Z"),
                            z.getDouble("A"), z.getDouble("B"), z.getDouble("Theta"), z.getLong("Phase")));
                }
                known.put(e.getString("Key"), new Entry(e.getString("Dimension"), e.getString("Label"),
                        e.getString("Color"), e.getString("Icon"), e.getInt("X"), e.getInt("Z"), e.getInt("Y"),
                        zones));
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
                ListTag zoneList = new ListTag();
                for (Landforms.Zone zone : value.zones()) {
                    CompoundTag z = new CompoundTag();
                    z.putByte("Type", (byte) zone.type().ordinal());
                    z.putByte("Shape", (byte) zone.shape().ordinal());
                    z.putDouble("X", zone.x());
                    z.putDouble("Z", zone.z());
                    z.putDouble("A", zone.a());
                    z.putDouble("B", zone.b());
                    z.putDouble("Theta", zone.theta());
                    z.putLong("Phase", zone.phase());
                    zoneList.add(z);
                }
                e.put("Zones", zoneList);
                entries.add(e);
            }
            teamTag.put("Landforms", entries);
            teams.add(teamTag);
        }
        tag.put("Teams", teams);
        return tag;
    }
}
