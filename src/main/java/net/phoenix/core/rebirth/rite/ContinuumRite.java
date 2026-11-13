package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class ContinuumRite extends Rite {

    public static final int LOCK = 0;
    public static final int PROBE = 1;
    public static final int SUBMIT = 2;

    public static final int PERIOD = 240;
    public static final int SATELLITES = 4;
    public static final int RELAYS = 6;
    public static final int PROBES = 9;
    public static final int SUBMITS = 3;

    public static final int TOLERANCE = 6;

    private static final int[] RATES = { 1, 2, 3, 5 };

    private int stage = 1;
    private final int[] phase = new int[SATELLITES];
    private final int[] ringIndex = new int[RELAYS];
    private int probesLeft = PROBES;
    private int submitsLeft = SUBMITS;

    public ContinuumRite(long seed) {
        super(Capstone.CONTINUUM, seed);
        int window = 20 + rng.nextInt(PERIOD - 40);
        int base = rng.nextInt(PERIOD);
        for (int i = 0; i < SATELLITES; i++) phase[i] = Math.floorMod(base - RATES[i] * window, PERIOD);

        java.util.List<Integer> order = new java.util.ArrayList<>();
        for (int i = 0; i < RELAYS; i++) order.add(i);
        java.util.Collections.shuffle(order, rng);
        for (int i = 0; i < RELAYS; i++) ringIndex[order.get(i)] = i;
    }

    public static int position(int rate, int phase, int time) {
        return Math.floorMod(phase + rate * time, PERIOD);
    }

    public int spread(int time) {
        int worst = 0;
        for (int i = 0; i < SATELLITES; i++) {
            for (int j = i + 1; j < SATELLITES; j++) {
                int d = Math.abs(position(RATES[i], phase[i], time) - position(RATES[j], phase[j], time));
                d = Math.min(d, PERIOD - d);
                worst = Math.max(worst, d);
            }
        }
        return worst;
    }

    public int hops(int a, int b) {
        int d = Math.abs(ringIndex[a] - ringIndex[b]);
        return Math.min(d, RELAYS - d);
    }

    @Override
    public int[] data() {
        int[] out = new int[2 + SATELLITES * 2 + 2];
        out[0] = stage;
        out[1] = PERIOD;
        for (int i = 0; i < SATELLITES; i++) {
            out[2 + i] = RATES[i];
            out[2 + SATELLITES + i] = phase[i];
        }
        out[2 + SATELLITES * 2] = probesLeft;
        out[3 + SATELLITES * 2] = submitsLeft;
        return out;
    }

    @Override
    public Reply act(int action, int[] args) {
        if (stage == 1) {
            if (action != LOCK || args.length < 1) return Reply.go("");
            int time = Math.floorMod(args[0], PERIOD);
            if (spread(time) > TOLERANCE) return Reply.go("The window is not open: the satellites are still apart.");
            stage = 2;
            return Reply.go("The window opens. Now tie the relays into a ring.");
        }
        if (action == PROBE) {
            if (args.length < 2 || probesLeft <= 0) return Reply.go("The relays have nothing left to say.");
            int a = args[0];
            int b = args[1];
            if (a < 0 || b < 0 || a >= RELAYS || b >= RELAYS || a == b) return Reply.go("");
            probesLeft--;
            return Reply.go("", a, b, hops(a, b));
        }
        if (action == SUBMIT) {
            if (args.length < RELAYS * 2 || submitsLeft <= 0) return Reply.go("");
            submitsLeft--;
            int right = 0;
            boolean[][] claimed = new boolean[RELAYS][RELAYS];
            int links = 0;
            for (int i = 0; i < RELAYS; i++) {
                int a = args[i * 2];
                int b = args[i * 2 + 1];
                if (a < 0 || b < 0 || a >= RELAYS || b >= RELAYS || a == b || claimed[a][b]) continue;
                claimed[a][b] = true;
                claimed[b][a] = true;
                links++;
                if (hops(a, b) == 1) right++;
            }
            if (right == RELAYS && links == RELAYS) return Reply.win("Every relay answers. The ring holds.");
            if (submitsLeft <= 0) return Reply.lose("The relays fall silent. The launch window closes. Begin again.");
            return Reply.go(right + " of " + RELAYS + " links hold.", right);
        }
        return Reply.go("");
    }

    int[] relayRing() {
        return ringIndex.clone();
    }
}
