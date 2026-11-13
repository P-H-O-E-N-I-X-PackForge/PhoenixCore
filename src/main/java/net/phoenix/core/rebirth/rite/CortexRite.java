package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class CortexRite extends Rite {

    public static final int FLOOD = 0;
    public static final int SIZE = 7;
    public static final int COLORS = 5;
    public static final int SLACK = 4;

    private final int[] grid = new int[SIZE * SIZE];
    private int moves;
    private int budget;

    public CortexRite(long seed) {
        super(Capstone.SCULK_CORTEX, seed);
        for (int attempt = 0; attempt < 80; attempt++) {
            for (int i = 0; i < grid.length; i++) grid[i] = rng.nextInt(COLORS);
            int g = greedy(grid.clone());
            budget = g + SLACK;
            if (g >= 8) break;
        }
    }

    public static boolean uniform(int[] g) {
        for (int v : g) if (v != g[0]) return false;
        return true;
    }

    public static int covered(int[] g) {
        return region(g, null);
    }

    private static int region(int[] g, boolean[] mark) {
        boolean[] seen = mark != null ? mark : new boolean[g.length];
        int[] stack = new int[g.length];
        int top = 0;
        stack[top++] = 0;
        seen[0] = true;
        int n = 0;
        int colour = g[0];
        while (top > 0) {
            int c = stack[--top];
            n++;
            int x = c % SIZE;
            int y = c / SIZE;
            int[][] d = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] dd : d) {
                int nx = x + dd[0];
                int ny = y + dd[1];
                if (nx < 0 || ny < 0 || nx >= SIZE || ny >= SIZE) continue;
                int ni = ny * SIZE + nx;
                if (seen[ni] || g[ni] != colour) continue;
                seen[ni] = true;
                stack[top++] = ni;
            }
        }
        return n;
    }

    public static void flood(int[] g, int colour) {
        boolean[] mark = new boolean[g.length];
        region(g, mark);
        for (int i = 0; i < g.length; i++) if (mark[i]) g[i] = colour;
    }

    private static int greedy(int[] g) {
        int moves = 0;
        while (!uniform(g) && moves < 80) {
            int best = -1;
            int bestSize = -1;
            for (int c = 0; c < COLORS; c++) {
                if (c == g[0]) continue;
                int[] copy = g.clone();
                flood(copy, c);
                int size = covered(copy);
                if (size > bestSize) {
                    bestSize = size;
                    best = c;
                }
            }
            flood(g, best);
            moves++;
        }
        return moves;
    }

    @Override
    public int[] data() {
        int[] out = new int[3 + grid.length];
        out[0] = moves;
        out[1] = budget;
        out[2] = SIZE;
        System.arraycopy(grid, 0, out, 3, grid.length);
        return out;
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != FLOOD || args.length < 1) return Reply.go("");
        int colour = args[0];
        if (colour < 0 || colour >= COLORS || colour == grid[0]) return Reply.go("");
        flood(grid, colour);
        moves++;
        if (uniform(grid)) return Reply.win("The sculk covers it all. The Cortex is awake.");
        if (moves >= budget)
            return Reply.lose("The sculk stalls with ground left. The Cortex withers and begins again.");
        return Reply.go("");
    }
}
