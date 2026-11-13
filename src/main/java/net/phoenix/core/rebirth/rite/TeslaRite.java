package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class TeslaRite extends Rite {

    public static final int PROBE = 0;
    public static final int LOCK = 1;
    public static final int RANGE = 100;

    public static final int LOCK_SIGNAL = 960;

    private final int[] target = new int[3];

    public TeslaRite(long seed) {
        super(Capstone.TESLA, seed);
        for (int i = 0; i < 3; i++) target[i] = 12 + rng.nextInt(RANGE - 24);
    }

    public int signal(int a, int b, int c) {
        double ea = a - target[0] + 0.35 * (b - target[1]);
        double eb = b - target[1] - 0.35 * (c - target[2]);
        double ec = c - target[2] + 0.35 * (a - target[0]);
        double dist = Math.sqrt(ea * ea + eb * eb + ec * ec);
        return (int) Math.round(1000 * Math.exp(-dist / 40.0));
    }

    @Override
    public int[] data() {
        return new int[] { RANGE, LOCK_SIGNAL };
    }

    @Override
    public Reply act(int action, int[] args) {
        if (args.length < 3) return Reply.go("");
        int a = Math.max(0, Math.min(RANGE, args[0]));
        int b = Math.max(0, Math.min(RANGE, args[1]));
        int c = Math.max(0, Math.min(RANGE, args[2]));
        int signal = signal(a, b, c);
        if (action == LOCK) {
            if (signal >= LOCK_SIGNAL) return Reply.win("The tower rings true.");
            return Reply.go("The lock slips: the signal is only " + signal / 10 + "%.", signal);
        }
        return Reply.go("", signal);
    }
}
