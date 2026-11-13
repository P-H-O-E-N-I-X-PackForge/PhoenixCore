package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

import java.util.List;

public final class ColliderRite extends Rite {

    public static final int TURN = 0;
    public static final int SIZE = 7;
    private static final int DECOYS = 5;

    private final int[] cells = new int[SIZE * SIZE];
    private int src;
    private int tgt;

    public ColliderRite(long seed) {
        super(Capstone.ANTIMATTER_COLLIDER, seed);
        int y0 = rng.nextInt(SIZE);
        int x1 = 1 + rng.nextInt(3);
        int x2 = x1 + 1 + rng.nextInt(Math.max(1, SIZE - 2 - x1));
        int y1;
        do {
            y1 = rng.nextInt(SIZE);
        } while (y1 == y0);
        int y2;
        do {
            y2 = rng.nextInt(SIZE);
        } while (y2 == y1);
        src = y0;
        tgt = y2;
        boolean[] onPath = new boolean[cells.length];
        for (int x = 0; x <= x1; x++) onPath[y0 * SIZE + x] = true;
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) onPath[y * SIZE + x1] = true;
        for (int x = x1; x <= x2; x++) onPath[y1 * SIZE + x] = true;
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) onPath[y * SIZE + x2] = true;
        for (int x = x2; x < SIZE; x++) onPath[y2 * SIZE + x] = true;

        int first = y1 < y0 ? 1 : 2;
        int second = y2 < y1 ? 1 : 2;
        cells[y0 * SIZE + x1] = first;
        cells[y1 * SIZE + x1] = first;
        cells[y1 * SIZE + x2] = second;
        cells[y2 * SIZE + x2] = second;
        for (int placed = 0; placed < DECOYS;) {
            int i = rng.nextInt(cells.length);
            if (onPath[i] || cells[i] != 0) continue;
            cells[i] = 1 + rng.nextInt(2);
            placed++;
        }

        do {
            for (int i = 0; i < cells.length; i++) if (cells[i] != 0) cells[i] = 1 + rng.nextInt(2);
        } while (solved(cells, src, tgt));
    }

    public static boolean trace(int[] cells, int src, int tgt, List<Integer> path) {
        int x = -1;
        int y = src;
        int dx = 1;
        int dy = 0;
        for (int step = 0; step < 400; step++) {
            x += dx;
            y += dy;
            if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return x >= SIZE && dx == 1 && y == tgt;
            if (path != null) path.add(y * SIZE + x);
            int c = cells[y * SIZE + x];
            if (c == 1) {
                int ndx = -dy;
                dy = -dx;
                dx = ndx;
            } else if (c == 2) {
                int ndx = dy;
                dy = dx;
                dx = ndx;
            }
        }
        return false;
    }

    public static boolean solved(int[] cells, int src, int tgt) {
        return trace(cells, src, tgt, null);
    }

    @Override
    public int[] data() {
        int[] out = new int[3 + cells.length];
        out[0] = SIZE;
        out[1] = src;
        out[2] = tgt;
        System.arraycopy(cells, 0, out, 3, cells.length);
        return out;
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != TURN || args.length < 1) return Reply.go("");
        int i = args[0];
        if (i < 0 || i >= cells.length || cells[i] == 0) return Reply.go("");
        cells[i] = cells[i] == 1 ? 2 : 1;
        if (solved(cells, src, tgt)) return Reply.win("The beams meet in the detector. The Collider is satisfied.");
        return Reply.go("");
    }
}
