package net.phoenix.core.rebirth.rite;

import net.phoenix.core.common.machine.multiblock.electric.plasma.PlasmaRing;
import net.phoenix.core.rebirth.Capstone;

public final class PlasmaRite extends Rite {

    public static final int TUNE = 0;
    public static final int SLACK = 3;

    private int state;
    private int par;
    private int moves;

    public PlasmaRite(long seed) {
        super(Capstone.PLASMA, seed);
        var p = PlasmaRing.generate(seed, 4);
        state = p.state();
        par = p.par();
    }

    public int budget() {
        return par + SLACK;
    }

    @Override
    public int[] data() {
        return new int[] { state, par, moves, budget() };
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != TUNE || args.length < 1) return Reply.go("");
        int cell = args[0];
        if (cell < 0 || cell >= PlasmaRing.CELLS) return Reply.go("");
        state = PlasmaRing.tune(state, cell);
        moves++;
        if (PlasmaRing.locked(state)) return Reply.win("The plasma locks.");
        if (moves >= budget()) return Reply.lose("The coils overheat. The ring scrambles.");
        return Reply.go("");
    }
}
