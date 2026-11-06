package net.phoenix.core.integration.conflux.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class SculkFamiliarity extends SavedData {

    public static final int[] THRESHOLDS = { 0, 6, 20, 50, 120, 260 };
    public static final int MAX_LEVEL = THRESHOLDS.length - 1;

    private final Map<Long, int[]> chunks = new HashMap<>();

    public static SculkFamiliarity get(ServerLevel level) {
        String name = "phoenixcore_familiarity_" + level.dimension().location().getPath().replace('/', '_');
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(SculkFamiliarity::new, SculkFamiliarity::load), name);
    }

    public static int levelOf(int points) {
        int level = 0;
        for (int i = 1; i < THRESHOLDS.length; i++) if (points >= THRESHOLDS[i]) level = i;
        return level;
    }

    public int points(ChunkPos pos) {
        int[] c = chunks.get(pos.toLong());
        return c == null ? 0 : c[0];
    }

    public int level(ChunkPos pos) {
        return levelOf(points(pos));
    }

    public int grown(ChunkPos pos) {
        int[] c = chunks.get(pos.toLong());
        return c == null ? 0 : c[1];
    }

    public void setGrown(ChunkPos pos, int level) {
        int[] c = chunks.computeIfAbsent(pos.toLong(), k -> new int[2]);
        if (c[1] != level) {
            c[1] = level;
            setDirty();
        }
    }

    public int[] add(ChunkPos pos, int points) {
        int[] c = chunks.computeIfAbsent(pos.toLong(), k -> new int[2]);
        int before = levelOf(c[0]);
        c[0] += points;
        setDirty();
        return new int[] { before, levelOf(c[0]) };
    }

    public void setPoints(ChunkPos pos, int points) {
        int[] c = chunks.computeIfAbsent(pos.toLong(), k -> new int[2]);
        c[0] = points;
        setDirty();
    }

    public int countAtLeast(int level) {
        int n = 0;
        for (int[] c : chunks.values()) if (levelOf(c[0]) >= level) n++;
        return n;
    }

    private static SculkFamiliarity load(CompoundTag tag, HolderLookup.Provider registries) {
        SculkFamiliarity data = new SculkFamiliarity();
        long[] keys = tag.getLongArray("Keys");
        int[] points = tag.getIntArray("Points");
        int[] grown = tag.getIntArray("Grown");
        for (int i = 0; i < keys.length && i < points.length && i < grown.length; i++) {
            data.chunks.put(keys[i], new int[] { points[i], grown[i] });
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        int n = chunks.size();
        long[] keys = new long[n];
        int[] points = new int[n];
        int[] grown = new int[n];
        int i = 0;
        for (Map.Entry<Long, int[]> e : chunks.entrySet()) {
            keys[i] = e.getKey();
            points[i] = e.getValue()[0];
            grown[i] = e.getValue()[1];
            i++;
        }
        tag.putLongArray("Keys", keys);
        tag.putIntArray("Points", points);
        tag.putIntArray("Grown", grown);
        return tag;
    }
}
