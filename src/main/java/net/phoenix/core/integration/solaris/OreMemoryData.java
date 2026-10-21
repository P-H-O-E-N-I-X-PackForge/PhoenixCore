package net.phoenix.core.integration.solaris;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The ore each research team has sensed, per chunk, so it can stay on their map. */
public class OreMemoryData extends SavedData {

    private static final String DATA_NAME = "phoenixcore_ore_memory";

    /** Ore of one kind in one chunk. */
    public record Memory(String dimension, int chunkX, int chunkZ, String block, int count, int x, int y, int z,
                         int minY, int maxY) {}

    // team -> "dimension|chunkX|chunkZ" -> block id -> memory
    private final Map<UUID, Map<String, Map<String, Memory>>> byTeam = new HashMap<>();

    public static OreMemoryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(OreMemoryData::new, OreMemoryData::load), DATA_NAME);
    }

    private static String chunkKey(String dimension, int chunkX, int chunkZ) {
        return dimension + "|" + chunkX + "|" + chunkZ;
    }

    /** Replaces what a team remembers about one chunk (an empty collection forgets it, e.g. once mined out). */
    public void replaceChunk(UUID team, String dimension, int chunkX, int chunkZ, Collection<Memory> memories) {
        Map<String, Map<String, Memory>> chunks = byTeam.computeIfAbsent(team, id -> new LinkedHashMap<>());
        String key = chunkKey(dimension, chunkX, chunkZ);

        Map<String, Memory> updated = new LinkedHashMap<>();
        for (Memory memory : memories) updated.put(memory.block(), memory);

        Map<String, Memory> previous = updated.isEmpty() ? chunks.remove(key) : chunks.put(key, updated);
        if (!updated.equals(previous == null ? Map.of() : previous)) setDirty();
    }

    public List<Memory> forTeam(UUID team, String dimension) {
        List<Memory> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, Memory>> chunk : byTeam.getOrDefault(team, Map.of()).entrySet()) {
            if (!chunk.getKey().startsWith(dimension + "|")) continue;
            out.addAll(chunk.getValue().values());
        }
        return out;
    }

    public void forget(UUID team) {
        if (byTeam.remove(team) != null) setDirty();
    }

    public void forgetDimension(String dimension) {
        boolean changed = false;
        for (Map<String, Map<String, Memory>> chunks : byTeam.values()) {
            changed |= chunks.keySet().removeIf(key -> key.startsWith(dimension + "|"));
        }
        if (changed) setDirty();
    }

    public void forgetAll() {
        if (byTeam.isEmpty()) return;
        byTeam.clear();
        setDirty();
    }

    public static OreMemoryData load(CompoundTag tag, HolderLookup.Provider registries) {
        OreMemoryData data = new OreMemoryData();
        ListTag teams = tag.getList("Teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag teamTag = teams.getCompound(i);
            if (!teamTag.hasUUID("Team")) continue;
            Map<String, Map<String, Memory>> chunks = new LinkedHashMap<>();
            ListTag list = teamTag.getList("Ore", Tag.TAG_COMPOUND);
            for (int j = 0; j < list.size(); j++) {
                CompoundTag m = list.getCompound(j);
                Memory memory = new Memory(m.getString("Dimension"), m.getInt("ChunkX"), m.getInt("ChunkZ"),
                        m.getString("Block"), m.getInt("Count"), m.getInt("X"), m.getInt("Y"), m.getInt("Z"),
                        m.getInt("MinY"), m.getInt("MaxY"));
                chunks.computeIfAbsent(chunkKey(memory.dimension(), memory.chunkX(), memory.chunkZ()),
                        k -> new LinkedHashMap<>()).put(memory.block(), memory);
            }
            data.byTeam.put(teamTag.getUUID("Team"), chunks);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        for (Map.Entry<UUID, Map<String, Map<String, Memory>>> team : byTeam.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", team.getKey());
            ListTag list = new ListTag();
            for (Map<String, Memory> chunk : team.getValue().values()) {
                for (Memory memory : chunk.values()) {
                    CompoundTag m = new CompoundTag();
                    m.putString("Dimension", memory.dimension());
                    m.putInt("ChunkX", memory.chunkX());
                    m.putInt("ChunkZ", memory.chunkZ());
                    m.putString("Block", memory.block());
                    m.putInt("Count", memory.count());
                    m.putInt("X", memory.x());
                    m.putInt("Y", memory.y());
                    m.putInt("Z", memory.z());
                    m.putInt("MinY", memory.minY());
                    m.putInt("MaxY", memory.maxY());
                    list.add(m);
                }
            }
            teamTag.put("Ore", list);
            teams.add(teamTag);
        }
        tag.put("Teams", teams);
        return tag;
    }
}
