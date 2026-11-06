package net.phoenix.core.integration.solaris;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientFamiliarity {

    private static final Map<String, Map<Long, Byte>> BY_DIMENSION = new ConcurrentHashMap<>();

    private ClientFamiliarity() {}

    public static void apply(String dimension, int centerX, int centerZ, int radius, byte[] levels) {
        Map<Long, Byte> map = BY_DIMENSION.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>());
        int size = radius * 2 + 1;
        for (int i = 0; i < levels.length && i < size * size; i++) {
            int cx = centerX - radius + i % size;
            int cz = centerZ - radius + i / size;
            long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
            if (levels[i] > 0) map.put(key, levels[i]);
            else map.remove(key);
        }
    }

    public static int level(String dimension, int chunkX, int chunkZ) {
        Map<Long, Byte> map = BY_DIMENSION.get(dimension);
        if (map == null) return 0;
        Byte b = map.get(((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL));
        return b == null ? 0 : b;
    }

    public static void clear() {
        BY_DIMENSION.clear();
    }
}
