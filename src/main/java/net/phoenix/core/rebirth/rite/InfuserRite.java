package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class InfuserRite extends Rite {

    public static final int MOVE = 0;
    public static final int DISKS = 4;
    public static final int PEGS = 3;
    public static final int BUDGET = 22;

    private final int[] peg = new int[DISKS];
    private int moves;

    public InfuserRite(long seed) {
        super(Capstone.PHOENIX_INFUSER, seed);
    }

    @Override
    public int[] data() {
        int[] out = new int[3 + DISKS];
        out[0] = moves;
        out[1] = BUDGET;
        out[2] = DISKS;
        System.arraycopy(peg, 0, out, 3, DISKS);
        return out;
    }

    private int top(int p) {
        for (int d = 0; d < DISKS; d++) if (peg[d] == p) return d;
        return -1;
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != MOVE || args.length < 2) return Reply.go("");
        int from = args[0];
        int to = args[1];
        if (from < 0 || from >= PEGS || to < 0 || to >= PEGS || from == to) return Reply.go("");
        int disk = top(from);
        if (disk < 0) return Reply.go("That peg is bare.");
        int landing = top(to);
        if (landing >= 0 && landing < disk) return Reply.go("A larger disk cannot rest on a smaller one.");
        peg[disk] = to;
        moves++;
        boolean done = true;
        for (int d = 0; d < DISKS; d++) if (peg[d] != PEGS - 1) done = false;
        if (done) return Reply.win("The tower stands again, whole. The Infuser takes the flame.");
        if (moves >= BUDGET) return Reply.lose("The fire gutters out before the tower is done. It is rebuilt.");
        return Reply.go("");
    }
}
