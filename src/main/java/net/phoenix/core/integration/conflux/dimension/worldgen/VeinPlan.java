package net.phoenix.core.integration.conflux.dimension.worldgen;

import java.util.Random;

public final class VeinPlan {

    public static final int BLOCK = 3;

    private VeinPlan() {}

    private static long mix(long seed, int a, int b, long salt) {
        long h = seed ^ salt;
        h = h * 0x9E3779B97F4A7C15L + a;
        h = h * 0xC2B2AE3D27D4EB4FL + b;
        h ^= h >>> 31;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return h;
    }

    public static boolean exists(long seed, int cellX, int cellZ, float chance, boolean forced) {
        if (forced) return true;
        long h = mix(seed, cellX, cellZ, 0x50484F454E4958L);
        return ((h >>> 11) & 0xFFFFFF) / (double) 0x1000000 < chance;
    }

    public static int[] origin(long seed, int cellX, int cellZ, int cellSize) {
        long h = mix(seed, cellX, cellZ, 0x0123456789ABCDEFL);
        int x = cellX * cellSize + (int) Math.floorMod(h >>> 8, (long) cellSize);
        int z = cellZ * cellSize + (int) Math.floorMod(h >>> 36, (long) cellSize);
        return new int[] { x, z };
    }

    public static int kind(long seed, int cellX, int cellZ, int kinds) {
        if (kinds <= 1) return 0;
        int blockX = Math.floorDiv(cellX, BLOCK);
        int blockZ = Math.floorDiv(cellZ, BLOCK);
        Random random = new Random(mix(seed, blockX, blockZ, 0x7A11DEC0DEL));
        int slots = BLOCK * BLOCK;
        int[] deal = new int[Math.max(slots, kinds)];
        for (int i = 0; i < deal.length; i++) deal[i] = i % kinds;
        for (int i = deal.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = deal[i];
            deal[i] = deal[j];
            deal[j] = t;
        }
        int slot = Math.floorMod(cellX, BLOCK) * BLOCK + Math.floorMod(cellZ, BLOCK);
        return deal[slot];
    }

    public static boolean stays(long seed, int cellX, int cellZ, int cellSize, int kinds, float chance, double gap,
                                java.util.function.BiPredicate<Integer, Integer> forced) {
        if (!exists(seed, cellX, cellZ, chance, forced.test(cellX, cellZ))) return false;
        int mine = kind(seed, cellX, cellZ, kinds);
        int[] origin = origin(seed, cellX, cellZ, cellSize);
        int reach = (int) Math.ceil(gap / cellSize);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int nx = cellX + dx;
                int nz = cellZ + dz;
                if (nx > cellX || (nx == cellX && nz >= cellZ)) continue;
                if (!exists(seed, nx, nz, chance, forced.test(nx, nz))) continue;
                if (kind(seed, nx, nz, kinds) != mine) continue;
                int[] other = origin(seed, nx, nz, cellSize);
                if (Math.hypot(other[0] - origin[0], other[1] - origin[1]) < gap) return false;
            }
        }
        return true;
    }
}
