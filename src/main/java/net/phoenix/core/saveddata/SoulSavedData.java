package net.phoenix.core.saveddata;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.integration.ars_nouveau.common.data.worldgen.soul.SoulBalance;
import net.phoenix.core.integration.ars_nouveau.common.data.worldgen.soul.SoulChunkEntry;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SoulSavedData extends SavedData {

    public record BiomeRule(float base, java.util.List<String> words) {}

    public static final java.util.List<BiomeRule> BIOME_RULES = java.util.List.of(
            new BiomeRule(1.5f, java.util.List.of("forest", "grove")),
            new BiomeRule(2.5f, java.util.List.of("magical", "old_growth")),
            new BiomeRule(0.5f, java.util.List.of("wasteland", "desert")),
            new BiomeRule(0.1f, java.util.List.of("dead", "corruption")));

    public static final float DEFAULT_BIOME_BASE = 1.0f;

    public static final float REGEN_PER_MINUTE = 0.01f;
    public static final long REGEN_TICKS = 1200;

    private final Map<ChunkPos, SoulChunkEntry> soulMap = new HashMap<>();
    private final ServerLevel level;

    public SoulSavedData(ServerLevel level) {
        this.level = level;
    }

    public static SoulSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new SoulSavedData(level),
                        (tag, provider) -> new SoulSavedData(level, tag), null),
                "ars_soul_data");
    }

    public SoulSavedData(ServerLevel level, CompoundTag nbt) {
        this.level = level;
        ListTag list = nbt.getList("SoulData", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            soulMap.put(new ChunkPos(entryTag.getLong("pos")), SoulChunkEntry.load(entryTag));
        }
    }

    public void modifySoul(ChunkPos pos, float amount, boolean isPermanent) {
        getMultiplier(pos);

        SoulChunkEntry entry = soulMap.get(pos);
        if (entry != null) {
            if (isPermanent) {

                entry.maxCapacity += amount;
                entry.currentSoul += amount;
            } else {

                entry.currentSoul = Math.min(entry.maxCapacity, entry.currentSoul + amount);
            }
            this.setDirty();
        }
    }

    private float calculateBiomeBase(ChunkPos pos) {
        var biomeHolder = level.getBiome(pos.getMiddleBlockPosition(level.getSeaLevel()));
        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biomeHolder.value());

        float base = DEFAULT_BIOME_BASE;
        if (biomeId != null) {
            String path = biomeId.getPath();
            for (BiomeRule rule : BIOME_RULES) {
                if (rule.words().stream().anyMatch(path::contains)) {
                    base = rule.base();
                    break;
                }
            }
        }

        float noise = ((Math.abs(pos.x * 31 + pos.z * 17) % 100) / 333.0f) - 0.15f;

        return Math.max(0.05f, base * SoulBalance.dimensionBase(level) + noise);
    }

    private void applyNaturalRegen(SoulChunkEntry entry, long currentTime) {
        if (entry.currentSoul < entry.maxCapacity) {
            long timePassed = currentTime - entry.lastUpdateTime;

            if (timePassed >= REGEN_TICKS) {
                float regenAmount = (timePassed / (float) REGEN_TICKS) * REGEN_PER_MINUTE;
                entry.currentSoul = Math.min(entry.maxCapacity, entry.currentSoul + regenAmount);
                entry.lastUpdateTime = currentTime;
                this.setDirty();
            }
        } else {

            entry.lastUpdateTime = currentTime;
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        soulMap.forEach((pos, entry) -> {
            CompoundTag chunkTag = entry.save();
            chunkTag.putLong("pos", pos.toLong());
            list.add(chunkTag);
        });
        tag.put("SoulData", list);
        return tag;
    }

    public static SoulSavedData load(CompoundTag tag, ServerLevel level) {
        SoulSavedData data = new SoulSavedData(level);

        ListTag list = tag.getList("SoulData", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag chunkTag = list.getCompound(i);
            data.soulMap.put(new ChunkPos(chunkTag.getLong("pos")), SoulChunkEntry.load(chunkTag));
        }
        return data;
    }

    public float getMultiplier(ChunkPos pos) {
        if (soulMap.containsKey(pos)) {
            SoulChunkEntry entry = soulMap.get(pos);

            applyNaturalRegen(entry, level.getGameTime());

            return entry.currentSoul;
        }

        float baseSoul = calculateBiomeBase(pos);
        soulMap.put(pos, new SoulChunkEntry(baseSoul, baseSoul, level.getGameTime()));
        this.setDirty();
        return baseSoul;
    }

    private void tickRegen(ChunkPos pos, SoulChunkEntry entry) {
        long ticksPassed = level.getGameTime() - entry.lastUpdateTime;
        if (ticksPassed <= 0) return;

        Holder<Biome> biomeHolder = level.getBiome(pos.getMiddleBlockPosition(64));
        SoulBalance.SoulProfile profile = SoulBalance.get(biomeHolder, level);

        float updatedSoul = entry.currentSoul + (ticksPassed * profile.regenPerTick());
        entry.currentSoul = Math.min(entry.maxCapacity, updatedSoul);

        entry.lastUpdateTime = level.getGameTime();
        setDirty();
    }

    public Map<ChunkPos, SoulChunkEntry> getSoulMap() {
        return soulMap;
    }
}
