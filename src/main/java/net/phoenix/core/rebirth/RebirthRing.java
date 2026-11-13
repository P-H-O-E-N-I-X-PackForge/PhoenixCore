package net.phoenix.core.rebirth;

public final class RebirthRing {

    private RebirthRing() {}

    public static int[] secret(long seed) {
        int[] out = new int[Capstone.COUNT];
        int n = 0;
        for (Capstone c : Capstone.PROGRESSION) out[n++] = c.ordinal();

        boolean[] used = new boolean[Capstone.COUNT];
        for (int i = 0; i < n; i++) used[out[i]] = true;
        for (int i = 0; i < Capstone.COUNT && n < out.length; i++) if (!used[i]) out[n++] = i;
        return out;
    }

    public static boolean isRing(int[] ring) {
        if (ring == null || ring.length != Capstone.COUNT) return false;
        boolean[] seen = new boolean[Capstone.COUNT];
        for (int v : ring) {
            if (v < 0 || v >= Capstone.COUNT || seen[v]) return false;
            seen[v] = true;
        }
        return true;
    }

    public static int aligned(int[] secret, int[] ring) {
        int n = 0;
        for (int i = 0; i < Capstone.COUNT; i++) if (secret[i] == ring[i]) n++;
        return n;
    }
}
