package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class StellarRite extends Rite {

    public static final int ACT = 0;
    public static final int TURNS = 12;
    public static final int HMAX = 9;
    public static final int STAGES = 3;

    public static final int[] DM = { 2, 3, 6, 0 };
    public static final int[] DH = { 2, 3, 5, -3 };
    public static final String[] STAGE_NAMES = { "protostar", "star", "black hole" };

    private final int[] threshold = new int[STAGES];
    private final int[] lo = new int[STAGES];
    private final int[] hi = new int[STAGES];
    private int mass;
    private int heat;
    private int turn;
    private int stage;
    private int last = -1;

    public StellarRite(long seed) {
        super(Capstone.STELLAR_FORGE, seed);
        for (int attempt = 0; attempt < 600; attempt++) {
            threshold[0] = 8 + rng.nextInt(4);
            threshold[1] = threshold[0] + 7 + rng.nextInt(4);
            threshold[2] = threshold[1] + 9 + rng.nextInt(5);
            for (int s = 0; s < STAGES; s++) {
                lo[s] = 1 + s + rng.nextInt(3);
                hi[s] = Math.min(HMAX, lo[s] + 2 + rng.nextInt(2));
            }
            if (solvable()) return;
        }

        threshold[0] = 8;
        threshold[1] = 16;
        threshold[2] = 28;
        for (int s = 0; s < STAGES; s++) {
            lo[s] = 2 + s;
            hi[s] = 6 + s;
        }
    }

    private boolean solvable() {
        byte[][][][] memo = new byte[TURNS + 1][96][HMAX + 1][STAGES + 1];
        return solve(0, 0, 0, 0, memo);
    }

    private boolean solve(int t, int m, int h, int st, byte[][][][] memo) {
        if (t >= TURNS) return false;
        byte seen = memo[t][Math.min(m, 95)][h][st];
        if (seen != 0) return seen == 2;
        boolean found = false;
        for (int a = 0; a < 4 && !found; a++) {
            int nm = m + DM[a];
            int nh = Math.max(0, h + DH[a] - 1);
            if (nh > HMAX) continue;
            int ns = st;
            boolean ok = true;
            while (ns < STAGES && nm >= threshold[ns]) {
                if (nh < lo[ns] || nh > hi[ns]) {
                    ok = false;
                    break;
                }
                ns++;
            }
            if (!ok) continue;
            found = ns == STAGES || solve(t + 1, nm, nh, ns, memo);
        }
        memo[t][Math.min(m, 95)][h][st] = (byte) (found ? 2 : 1);
        return found;
    }

    @Override
    public int[] data() {
        return new int[] { mass, heat, turn, TURNS, stage, threshold[0], threshold[1], threshold[2], lo[0], hi[0],
                lo[1], hi[1], lo[2], hi[2], HMAX, last };
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != ACT || args.length < 1) return Reply.go("");
        int a = args[0];
        if (a < 0 || a > 3) return Reply.go("");
        last = a;
        mass += DM[a];
        heat = Math.max(0, heat + DH[a] - 1);
        turn++;
        if (heat > HMAX) return Reply.lose("The core flares past the field and blows itself apart.");
        while (stage < STAGES && mass >= threshold[stage]) {
            if (heat < lo[stage] || heat > hi[stage]) {
                return Reply.lose("The core was " + (heat < lo[stage] ? "too cold" : "too hot") + " as it became a " +
                        STAGE_NAMES[stage] + ". It scatters.");
            }
            stage++;
        }
        if (stage >= STAGES) return Reply.win("It falls in on itself. The Forge has made its black hole.");
        if (turn >= TURNS) return Reply.lose("The nursery runs out of time before the star is done.");
        return Reply.go("");
    }
}
