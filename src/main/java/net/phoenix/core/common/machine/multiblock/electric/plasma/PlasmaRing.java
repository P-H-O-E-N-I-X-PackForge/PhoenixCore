package net.phoenix.core.common.machine.multiblock.electric.plasma;

import java.util.ArrayDeque;
import java.util.Random;

public final class PlasmaRing {

    public static final int CELLS = 6;
    public static final int PHASES = 4;
    public static final int STATES = 1 << (CELLS * 2);

    private PlasmaRing() {}

    public record Pattern(int state, int par) {}

    public static int phase(int state, int cell) {
        return (state >> (cell * 2)) & 3;
    }

    private static int with(int state, int cell, int phase) {
        return (state & ~(3 << (cell * 2))) | ((phase & 3) << (cell * 2));
    }

    public static int tune(int state, int cell) {
        int left = (cell + CELLS - 1) % CELLS;
        int right = (cell + 1) % CELLS;
        state = with(state, cell, phase(state, cell) + 1);
        state = with(state, left, phase(state, left) + 3);
        return with(state, right, phase(state, right) + 3);
    }

    public static boolean locked(int state) {
        return state == 0;
    }

    public static int par(int start) {
        int[] dist = new int[STATES];
        java.util.Arrays.fill(dist, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        dist[start] = 0;
        queue.add(start);
        while (!queue.isEmpty()) {
            int s = queue.poll();
            if (s == 0) return dist[s];
            for (int c = 0; c < CELLS; c++) {
                int n = tune(s, c);
                if (dist[n] < 0) {
                    dist[n] = dist[s] + 1;
                    queue.add(n);
                }
            }
        }
        return -1;
    }

    public static Pattern generate(long seed, int solves) {
        Random rng = new Random(seed);
        for (int attempt = 0; attempt < 64; attempt++) {
            int state = 0;
            int presses = 5 + Math.min(solves, 6) + rng.nextInt(3);
            for (int i = 0; i < presses; i++) state = tune(state, rng.nextInt(CELLS));
            int par = par(state);
            if (par >= 4) return new Pattern(state, par);
        }
        int state = tune(tune(tune(tune(0, 0), 2), 4), 1);
        return new Pattern(state, Math.max(1, par(state)));
    }

    public static int drifting(int state) {
        int n = 0;
        for (int i = 0; i < CELLS; i++) if (phase(state, i) != 0) n++;
        return n;
    }

    public static String grade(int moves, int par) {
        if (moves <= par) return "Perfect lock";
        if (moves <= par + 2) return "Clean lock";
        return "Rough lock";
    }

    public static int shieldPercent(int moves, int par) {
        if (moves <= par + 2) return 85;
        return Math.max(70, 85 - (moves - par - 2) * 3);
    }
}
