package net.phoenix.core.rebirth.rite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContinuumRiteTest {

    private static int findWindow(ContinuumRite r) {
        for (int t = 0; t < ContinuumRite.PERIOD; t++) if (r.spread(t) <= ContinuumRite.TOLERANCE) return t;
        return -1;
    }

    @Test
    void everyRiteHasAWindowAndItOpensStageTwo() {
        for (long seed = 0; seed < 100; seed++) {
            ContinuumRite r = new ContinuumRite(seed);
            int t = findWindow(r);
            assertTrue(t >= 0, "seed " + seed);
            assertEquals(Rite.Outcome.CONTINUE, r.act(ContinuumRite.LOCK, new int[] { t }).outcome());
            assertEquals(2, r.data()[0]);
        }
    }

    @Test
    void aClosedWindowStaysShut() {
        ContinuumRite r = new ContinuumRite(1);
        int t = findWindow(r);
        r.act(ContinuumRite.LOCK, new int[] { Math.floorMod(t + 60, ContinuumRite.PERIOD) });
        assertEquals(1, r.data()[0]);
    }

    @Test
    void probesReportHopsAndTheTrueRingWins() {
        for (long seed = 0; seed < 50; seed++) {
            ContinuumRite r = new ContinuumRite(seed);
            r.act(ContinuumRite.LOCK, new int[] { findWindow(r) });
            int[] ring = r.relayRing();
            int[] order = new int[ContinuumRite.RELAYS];
            for (int node = 0; node < ContinuumRite.RELAYS; node++) order[ring[node]] = node;
            int[] edges = new int[ContinuumRite.RELAYS * 2];
            for (int i = 0; i < ContinuumRite.RELAYS; i++) {
                edges[i * 2] = order[i];
                edges[i * 2 + 1] = order[(i + 1) % ContinuumRite.RELAYS];
            }
            var probe = r.act(ContinuumRite.PROBE, new int[] { edges[0], edges[1] });
            assertEquals(1, probe.extra()[2]);
            assertEquals(Rite.Outcome.SUCCESS, r.act(ContinuumRite.SUBMIT, edges).outcome());
        }
    }

    @Test
    void wrongSubmissionsRunOut() {
        ContinuumRite r = new ContinuumRite(2);
        r.act(ContinuumRite.LOCK, new int[] { findWindow(r) });
        int[] bad = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Rite.Outcome out = Rite.Outcome.CONTINUE;
        for (int i = 0; i < ContinuumRite.SUBMITS; i++) out = r.act(ContinuumRite.SUBMIT, bad).outcome();
        assertEquals(Rite.Outcome.FAIL, out);
    }
}
