package net.phoenix.core.rebirth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RebirthRingTest {

    @Test
    void secretIsAPermutationAndStable() {
        for (long seed = 0; seed < 200; seed++) {
            int[] s = RebirthRing.secret(seed);
            assertTrue(RebirthRing.isRing(s));
            assertArrayEquals(s, RebirthRing.secret(seed));
        }
    }

    @Test
    void alignedCountsMatches() {
        int[] s = RebirthRing.secret(7);
        assertEquals(Capstone.COUNT, RebirthRing.aligned(s, s.clone()));
        int[] swapped = s.clone();
        swapped[0] = s[1];
        swapped[1] = s[0];
        assertTrue(RebirthRing.aligned(s, swapped) <= Capstone.COUNT - 2);
    }
}
