package net.phoenix.core.saveddata;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.common.data.worldgen.soul.SoulBalance;
import net.phoenix.core.common.data.worldgen.soul.SoulChunkEntry;

import java.util.HashMap;
import java.util.Map;

public class SoulSavedData extends SavedData {

    private final Map<ChunkPos, SoulChunkEntry> soulMap = new HashMap<>();
    private final ServerLevel level;

    public SoulSavedData(ServerLevel level) {
        this.level = level;
    }

    public static SoulSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> new SoulSavedData(level, tag),
                () -> new SoulSavedData(level),
                "ars_soul_data");
    }

    public SoulSavedData(ServerLevel level, CompoundTag nbt) {
        this.level = level;
        ListTag list = nbt.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            soulMap.put(new ChunkPos(entryTag.getLong("pos")), SoulChunkEntry.load(entryTag.getCompound("data")));
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        soulMap.forEach((pos, entry) -> {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", pos.toLong());
            entryTag.put("data", entry.save());
            list.add(entryTag);
        });
        nbt.put("Entries", list);
        return nbt;
    }

    /**
     * Gets the current multiplier for a chunk.
     * If the chunk is new, it rolls a unique max capacity based on the biome's range.
     */
    public float getMultiplier(ChunkPos pos) {
        SoulChunkEntry entry = soulMap.computeIfAbsent(pos, p -> {
            // Get the Biome Holder (needed for Tag checks)
            Holder<Biome> biomeHolder = level.getBiome(pos.getMiddleBlockPosition(64));

            // Get the profile (Manual -> Tag -> Default)
            SoulBalance.SoulProfile profile = SoulBalance.get(biomeHolder);

            // Roll a unique "Hotspot" value for this specific chunk
            float min = profile.minSoul();
            float max = profile.maxSoul();
            float rolledMax = min + (level.random.nextFloat() * (max - min));

            // New chunks start at full capacity
            return new SoulChunkEntry(rolledMax, rolledMax, level.getGameTime());
        });

        tickRegen(pos, entry);
        return entry.currentSoul;
    }

    private void tickRegen(ChunkPos pos, SoulChunkEntry entry) {
        long ticksPassed = level.getGameTime() - entry.lastUpdateTime;
        if (ticksPassed <= 0) return;

        // We still need the profile to know the REGEN RATE for this biome
        Holder<Biome> biomeHolder = level.getBiome(pos.getMiddleBlockPosition(64));
        SoulBalance.SoulProfile profile = SoulBalance.get(biomeHolder);

        // Update soul: Add (regen * time), capped at the chunk's unique maxCapacity
        float updatedSoul = entry.currentSoul + (ticksPassed * profile.regenPerTick());
        entry.currentSoul = Math.min(entry.maxCapacity, updatedSoul);

        entry.lastUpdateTime = level.getGameTime();
        setDirty();
    }

    public Map<ChunkPos, SoulChunkEntry> getSoulMap() {
        return soulMap;
    }
}
