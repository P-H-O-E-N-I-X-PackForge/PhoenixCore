package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class HoneyRite extends Rite {

    public static final int SHOW = 0;
    public static final int PICK = 1;
    public static final int CELLS = 7;
    public static final int LENGTH = 6;
    public static final int SHOWS = 3;

    private final int[] sequence = new int[LENGTH];
    private int progress;
    private int showsLeft = SHOWS;

    public HoneyRite(long seed) {
        super(Capstone.HONEY, seed);
        int prev = -1;
        for (int i = 0; i < LENGTH; i++) {
            int c;
            do {
                c = rng.nextInt(CELLS);
            } while (c == prev);
            sequence[i] = c;
            prev = c;
        }
    }

    @Override
    public int[] data() {
        return new int[] { showsLeft, progress, LENGTH, CELLS };
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action == SHOW) {
            if (showsLeft <= 0) return Reply.go("The comb will not show it again.");
            showsLeft--;
            progress = 0;
            return Reply.go("Watch the comb.", sequence.clone());
        }
        if (action != PICK || args.length < 1) return Reply.go("");
        int cell = args[0];
        if (cell < 0 || cell >= CELLS) return Reply.go("");
        if (cell != sequence[progress]) {
            progress = 0;
            if (showsLeft <= 0) return Reply.lose("The comb crumbles for good. Begin again.");
            return Reply.go("Wrong cell. The comb crumbles back to the start.");
        }
        progress++;
        if (progress >= LENGTH) return Reply.win("The comb sets, gold and whole.");
        return Reply.go("");
    }
}
