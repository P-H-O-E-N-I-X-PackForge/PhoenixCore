package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class EngineRite extends Rite {

    public static final int TAKE = 0;

    private final int[] heaps = new int[3];
    private int lastBranch = -1;
    private int lastTake;

    public EngineRite(long seed) {
        super(Capstone.ENGINE, seed);
        do {
            for (int i = 0; i < 3; i++) heaps[i] = 3 + rng.nextInt(6);
        } while ((heaps[0] ^ heaps[1] ^ heaps[2]) == 0);
    }

    @Override
    public int[] data() {
        return new int[] { heaps[0], heaps[1], heaps[2], lastBranch, lastTake };
    }

    public int[] heaps() {
        return heaps.clone();
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != TAKE || args.length < 2) return Reply.go("");
        int branch = args[0];
        int take = args[1];
        if (branch < 0 || branch > 2 || take < 1 || take > heaps[branch]) return Reply.go("");
        heaps[branch] -= take;
        lastBranch = -1;
        lastTake = 0;
        if (heaps[0] + heaps[1] + heaps[2] == 0) return Reply.win("You took the last leaf. The Engine yields.");
        engineMove();
        if (heaps[0] + heaps[1] + heaps[2] == 0) {
            return Reply.lose("The Engine took the last leaf. Study the branches and begin again.");
        }
        return Reply.go("");
    }

    private void engineMove() {
        int x = heaps[0] ^ heaps[1] ^ heaps[2];
        if (x != 0) {
            for (int i = 0; i < 3; i++) {
                int want = heaps[i] ^ x;
                if (want < heaps[i]) {
                    lastBranch = i;
                    lastTake = heaps[i] - want;
                    heaps[i] = want;
                    return;
                }
            }
        }
        int big = 0;
        for (int i = 1; i < 3; i++) if (heaps[i] > heaps[big]) big = i;
        lastBranch = big;
        lastTake = 1;
        heaps[big]--;
    }
}
