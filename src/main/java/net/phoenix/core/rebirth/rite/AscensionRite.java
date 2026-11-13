package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

import java.util.HashSet;
import java.util.Set;

public final class AscensionRite extends Rite {

    public static final int ACT = 0;
    public static final int TURNS = 12;
    public static final int LO = 35;
    public static final int HI = 65;
    public static final int MOVES = 7;
    public static final String[] NAMES = { "RODS IN", "RODS OUT", "COOLANT UP", "COOLANT DOWN", "COILS UP",
            "COILS DOWN", "HOLD" };

    public static final int[][] EFFECT = {
            { -8, -3, 0 }, { 8, 4, 2 }, { 1, -9, -2 }, { -1, 7, 1 }, { -2, 3, 8 }, { 1, -2, -8 }, { 0, 0, 0 } };
    private static final int PULL = 8;

    private final int[][] pull = new int[TURNS + 2][3];
    private final int[] gauge = { 50, 50, 50 };
    private int turn;
    private int last = -1;
    private final int[] lastPull = new int[3];

    public AscensionRite(long seed) {
        super(Capstone.ASCENSION_REACTOR, seed);
        for (int attempt = 0; attempt < 300; attempt++) {
            for (int[] p : pull) for (int k = 0; k < 3; k++) p[k] = rng.nextInt(2 * PULL + 1) - PULL;
            if (solvable(0, 50, 50, 50, new HashSet<>())) return;
        }
        for (int[] p : pull) java.util.Arrays.fill(p, 0);
    }

    boolean canBeWon() {
        return solvable(0, 50, 50, 50, new HashSet<>());
    }

    private boolean solvable(int t, int f, int h, int p, Set<Long> dead) {
        if (t >= TURNS) return true;
        long key = (((long) t * 128 + f) * 128 + h) * 128 + p;
        if (dead.contains(key)) return false;
        for (int[] e : EFFECT) {
            int nf = f + e[0] + pull[t][0];
            int nh = h + e[1] + pull[t][1];
            int np = p + e[2] + pull[t][2];
            if (nf < LO || nf > HI || nh < LO || nh > HI || np < LO || np > HI) continue;
            if (solvable(t + 1, nf, nh, np, dead)) return true;
        }
        dead.add(key);
        return false;
    }

    @Override
    public int[] data() {
        int[] a = turn < TURNS ? pull[turn] : new int[3];
        int[] b = turn + 1 < TURNS ? pull[turn + 1] : new int[3];
        return new int[] { turn, TURNS, gauge[0], gauge[1], gauge[2], LO, HI, a[0], a[1], a[2], b[0], b[1], b[2], last,
                lastPull[0], lastPull[1], lastPull[2] };
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != ACT || args.length < 1) return Reply.go("");
        int move = args[0];
        if (move < 0 || move >= MOVES) return Reply.go("");
        last = move;
        boolean out = false;
        for (int k = 0; k < 3; k++) {
            lastPull[k] = pull[turn][k];
            gauge[k] += EFFECT[move][k] + pull[turn][k];
            if (gauge[k] < LO || gauge[k] > HI) out = true;
        }
        turn++;
        if (out) return Reply.lose("A gauge left its band. The field fails and the reactor scrams.");
        if (turn >= TURNS) return Reply.win("Twelve turns, all three in band. The reactor ascends.");
        return Reply.go("");
    }
}
