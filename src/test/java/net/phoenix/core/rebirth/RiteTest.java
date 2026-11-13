package net.phoenix.core.rebirth;

import net.phoenix.core.rebirth.rite.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiteTest {

    @Test
    void teslaClimbsToALock() {
        for (long seed = 0; seed < 50; seed++) {
            TeslaRite r = new TeslaRite(seed);
            int[] v = { 50, 50, 50 };
            int best = r.signal(v[0], v[1], v[2]);
            for (int pass = 0; pass < 300; pass++) {
                boolean moved = false;
                for (int d = 0; d < 3; d++) {
                    for (int step : new int[] { -1, 1 }) {
                        v[d] += step;
                        int s = r.signal(v[0], v[1], v[2]);
                        if (s > best) {
                            best = s;
                            moved = true;
                        } else {
                            v[d] -= step;
                        }
                    }
                }
                if (!moved) break;
            }
            assertEquals(Rite.Outcome.SUCCESS, r.act(TeslaRite.LOCK, v.clone()).outcome(), "seed " + seed);
        }
    }

    @Test
    void honeyAcceptsTheShownRun() {
        HoneyRite r = new HoneyRite(5);
        int[] seq = r.act(HoneyRite.SHOW, new int[0]).extra();
        assertEquals(HoneyRite.LENGTH, seq.length);
        Rite.Reply last = null;
        for (int c : seq) last = r.act(HoneyRite.PICK, new int[] { c });
        assertEquals(Rite.Outcome.SUCCESS, last.outcome());
    }

    @Test
    void honeyCrumblesOnAMistake() {
        HoneyRite r = new HoneyRite(5);
        int[] seq = r.act(HoneyRite.SHOW, new int[0]).extra();
        int wrong = (seq[0] + 1) % HoneyRite.CELLS;
        assertEquals(Rite.Outcome.CONTINUE, r.act(HoneyRite.PICK, new int[] { wrong }).outcome());
        assertEquals(0, r.data()[1]);
    }

    @Test
    void engineIsBeatenByPerfectPlay() {
        for (long seed = 0; seed < 100; seed++) {
            EngineRite r = new EngineRite(seed);
            Rite.Outcome out = Rite.Outcome.CONTINUE;
            int guard = 0;
            while (out == Rite.Outcome.CONTINUE && guard++ < 50) {
                int[] h = r.heaps();
                int x = h[0] ^ h[1] ^ h[2];
                int branch = -1;
                int take = 0;
                for (int i = 0; i < 3; i++) {
                    if ((h[i] ^ x) < h[i]) {
                        branch = i;
                        take = h[i] - (h[i] ^ x);
                        break;
                    }
                }
                if (branch < 0) {
                    for (int i = 0; i < 3; i++) {
                        if (h[i] > 0) {
                            branch = i;
                            take = 1;
                            break;
                        }
                    }
                }
                out = r.act(EngineRite.TAKE, new int[] { branch, take }).outcome();
            }
            assertEquals(Rite.Outcome.SUCCESS, out, "seed " + seed);
        }
    }

    @Test
    void plasmaOverheatsPastItsBudget() {
        PlasmaRite r = new PlasmaRite(3);
        Rite.Outcome out = Rite.Outcome.CONTINUE;
        for (int i = 0; i < r.budget() + 2 && out == Rite.Outcome.CONTINUE; i++) {
            out = r.act(PlasmaRite.TUNE, new int[] { i % 6 }).outcome();
        }
        assertNotEquals(Rite.Outcome.CONTINUE, out);
    }
}
