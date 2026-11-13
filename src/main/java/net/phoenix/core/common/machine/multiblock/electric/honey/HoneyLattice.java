package net.phoenix.core.common.machine.multiblock.electric.honey;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class HoneyLattice {

    private HoneyLattice() {}

    public static final int RADIUS = 2;
    public static final int CELLS = 19;

    private static final int[] Q = new int[CELLS];
    private static final int[] R = new int[CELLS];

    private static final int[] PRESS = new int[CELLS];

    private static final int[][] DIRECTIONS = { { 1, 0 }, { 1, -1 }, { 0, -1 }, { -1, 0 }, { -1, 1 }, { 0, 1 } };

    static {
        int n = 0;
        for (int r = -RADIUS; r <= RADIUS; r++) {
            for (int q = -RADIUS; q <= RADIUS; q++) {
                if (Math.abs(q + r) > RADIUS) continue;
                Q[n] = q;
                R[n] = r;
                n++;
            }
        }
        for (int i = 0; i < CELLS; i++) {
            int mask = 1 << i;
            for (int[] d : DIRECTIONS) {
                int other = indexOf(Q[i] + d[0], R[i] + d[1]);
                if (other >= 0) mask |= 1 << other;
            }
            PRESS[i] = mask;
        }
    }

    public static int q(int cell) {
        return Q[cell];
    }

    public static int r(int cell) {
        return R[cell];
    }

    public static int indexOf(int q, int r) {
        for (int i = 0; i < CELLS; i++) if (Q[i] == q && R[i] == r) return i;
        return -1;
    }

    public static int pressMask(int cell) {
        return PRESS[cell];
    }

    public static int press(int state, int cell) {
        if (cell < 0 || cell >= CELLS) return state;
        return state ^ PRESS[cell];
    }

    public static boolean isLit(int state, int cell) {
        return (state >> cell & 1) != 0;
    }

    public static int litCount(int state) {
        return Integer.bitCount(state & ((1 << CELLS) - 1));
    }

    public record Pattern(int target, int par) {}

    public static int parFor(int solves) {
        return Math.min(9, 5 + solves / 2);
    }

    public static Pattern generate(long seed, int solves) {
        Random rng = new Random(seed);
        int par = parFor(solves);
        for (int attempt = 0; attempt < 32; attempt++) {
            List<Integer> cells = new ArrayList<>();
            for (int i = 0; i < CELLS; i++) cells.add(i);
            java.util.Collections.shuffle(cells, rng);
            int target = 0;
            for (int i = 0; i < par; i++) target = press(target, cells.get(i));

            int lit = litCount(target);
            if (lit < 4 || lit > CELLS - 3) continue;
            return new Pattern(target, par);
        }

        int target = 0;
        for (int i : new int[] { 0, 5, 9, 13, 18 }) target = press(target, i);
        return new Pattern(target, 5);
    }

    public static int reward(int moves, int par) {
        if (moves <= par) return 10;
        if (moves <= par + 4) return 6;
        return 3;
    }

    public static String grade(int moves, int par) {
        if (moves <= par) return "Flawless";
        if (moves <= par + 4) return "Clean";
        return "Rough";
    }
}
