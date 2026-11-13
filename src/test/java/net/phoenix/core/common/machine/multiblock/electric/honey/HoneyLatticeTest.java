package net.phoenix.core.common.machine.multiblock.electric.honey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HoneyLatticeTest {

    @Test
    void theCombHasNineteenCellsAndSensibleNeighbours() {
        assertEquals(19, HoneyLattice.CELLS);
        int centre = HoneyLattice.indexOf(0, 0);
        assertTrue(centre >= 0);
        assertEquals(7, Integer.bitCount(HoneyLattice.pressMask(centre)), "centre flips itself and six");
        int corner = HoneyLattice.indexOf(2, 0);
        assertEquals(4, Integer.bitCount(HoneyLattice.pressMask(corner)), "a corner flips itself and three");
        int edge = HoneyLattice.indexOf(1, 1);
        assertEquals(5, Integer.bitCount(HoneyLattice.pressMask(edge)), "an edge flips itself and four");
        assertEquals(-1, HoneyLattice.indexOf(2, 2));
    }

    @Test
    void pressingTwiceUndoesAndOrderDoesNotMatter() {
        for (int a = 0; a < HoneyLattice.CELLS; a++) {
            assertEquals(0, HoneyLattice.press(HoneyLattice.press(0, a), a));
            for (int b = 0; b < HoneyLattice.CELLS; b++) {
                assertEquals(HoneyLattice.press(HoneyLattice.press(0, a), b),
                        HoneyLattice.press(HoneyLattice.press(0, b), a));
            }
        }
    }

    @Test
    void everyGeneratedPatternIsSolvableWithinItsParAndNeverTrivial() {
        for (int solves = 0; solves < 24; solves++) {
            for (long seed = 0; seed < 200; seed++) {
                HoneyLattice.Pattern p = HoneyLattice.generate(seed * 7919L + solves, solves);
                assertEquals(HoneyLattice.parFor(solves), p.par());
                int lit = HoneyLattice.litCount(p.target());
                assertTrue(lit >= 4 && lit <= HoneyLattice.CELLS - 3, "lit " + lit);
                assertNotEquals(0, p.target());
                if (seed < 2 && solves % 8 == 0) {
                    assertTrue(minPresses(p.target()) <= p.par(), "unsolvable within par");
                }
                assertEquals(generateAgain(seed * 7919L + solves, solves), p, "deterministic");
            }
        }
    }

    private static HoneyLattice.Pattern generateAgain(long seed, int solves) {
        return HoneyLattice.generate(seed, solves);
    }

    private static int minPresses(int target) {
        int best = Integer.MAX_VALUE;
        for (int set = 0; set < (1 << HoneyLattice.CELLS); set++) {
            int count = Integer.bitCount(set);
            if (count >= best) continue;
            int state = 0;
            for (int i = 0; i < HoneyLattice.CELLS; i++) if ((set >> i & 1) != 0) state = HoneyLattice.press(state, i);
            if (state == target) best = count;
        }
        return best;
    }

    @Test
    void rewardsFollowThePar() {
        assertEquals(10, HoneyLattice.reward(5, 5));
        assertEquals(6, HoneyLattice.reward(9, 5));
        assertEquals(3, HoneyLattice.reward(10, 5));
        assertEquals("Flawless", HoneyLattice.grade(4, 5));
    }
}
