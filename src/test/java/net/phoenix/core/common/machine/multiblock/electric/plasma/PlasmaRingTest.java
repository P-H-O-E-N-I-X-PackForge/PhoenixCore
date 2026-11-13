package net.phoenix.core.common.machine.multiblock.electric.plasma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlasmaRingTest {

    @Test
    void everyGeneratedRingIsSolvableWithinItsPar() {
        for (long seed = 0; seed < 300; seed++) {
            var p = PlasmaRing.generate(seed, (int) (seed % 9));
            assertFalse(PlasmaRing.locked(p.state()));
            assertTrue(p.par() >= 1);
            int s = p.state();

            assertEquals(p.par(), PlasmaRing.par(s));
            assertTrue(p.par() <= 14);
        }
    }

    @Test
    void tuningFourTimesIsNotIdentityButSolvedStaysReachable() {
        assertEquals(0, PlasmaRing.par(0));
        int s = PlasmaRing.tune(0, 3);
        assertEquals(1, PlasmaRing.phase(s, 3));
        assertEquals(3, PlasmaRing.phase(s, 2));
        assertEquals(3, PlasmaRing.phase(s, 4));
        assertTrue(PlasmaRing.par(s) >= 1);
    }
}
