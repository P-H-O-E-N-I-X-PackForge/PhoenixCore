package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class TokamakRite extends Rite {

    public static final int INJECT = 0;
    public static final int FEEDS = 3;
    public static final int MAX = 9;
    public static final int BUDGET = 8;

    public static final int WORST = FEEDS * MAX;

    private final int[] target = new int[FEEDS];
    private final int[][] tried = new int[BUDGET][FEEDS + 1];
    private int tries;

    public TokamakRite(long seed) {
        super(Capstone.TOKAMAK, seed);
        for (int i = 0; i < FEEDS; i++) target[i] = rng.nextInt(MAX + 1);
    }

    @Override
    public int[] data() {
        int[] out = new int[2 + tries * (FEEDS + 1)];
        out[0] = tries;
        out[1] = BUDGET;
        for (int t = 0; t < tries; t++) {
            for (int k = 0; k <= FEEDS; k++) out[2 + t * (FEEDS + 1) + k] = tried[t][k];
        }
        return out;
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != INJECT || args.length < FEEDS) return Reply.go("");
        int loss = 0;
        for (int i = 0; i < FEEDS; i++) {
            if (args[i] < 0 || args[i] > MAX) return Reply.go("");
            loss += Math.abs(args[i] - target[i]);
        }
        for (int i = 0; i < FEEDS; i++) tried[tries][i] = args[i];
        tried[tries][FEEDS] = loss;
        tries++;
        if (loss == 0) return Reply.win("The mix is right. The plasma ignites and holds.");
        if (tries >= BUDGET) return Reply.lose("The field collapses. The reactor purges itself and begins again.");
        return Reply.go("");
    }
}
