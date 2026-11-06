package net.phoenix.core.common.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChasmMaskTest {

    private static TerrainProfile profile(long seed) {
        return TerrainProfile.builder("void").seed(seed).baseY(68).amplitude(55).frequency(0.003).octaves(5)
                .style(TerrainProfile.Style.HEIGHTMAP).seaLevel(55).chasms(0.0075, 0.53).caves(true).build();
    }

    @Test
    void spawnStaysSolid() {
        for (long seed : new long[] { 1L, 777L, 424242L }) {
            var mask = profile(seed).chasms();
            for (int x = -200; x <= 200; x += 8) {
                for (int z = -200; z <= 200; z += 8) {
                    assertFalse(mask.isVoid(x, z), "void near spawn at " + x + "," + z + " seed " + seed);
                }
            }
        }
    }

    @Test
    void aFairShareOfTheMapIsVoid() {
        for (long seed : new long[] { 1L, 777L, 424242L }) {
            var mask = profile(seed).chasms();
            int n = 0, hit = 0;
            for (int x = -6000; x < 6000; x += 24) {
                for (int z = -6000; z < 6000; z += 24) {
                    n++;
                    if (mask.isVoid(x, z)) hit++;
                }
            }
            double share = 100.0 * hit / n;
            System.out.println("CHASM seed " + seed + ": " + share + "% void");
            assertTrue(share > 4 && share < 45, "void share " + share);
        }
    }

    @Test
    void voidColumnsHaveNoGroundAtAnyHeight() {
        var p = profile(777L);
        int checked = 0;
        for (int x = -4000; x < 4000 && checked < 50; x += 17) {
            for (int z = -4000; z < 4000 && checked < 50; z += 17) {
                if (!p.chasms().isVoid(x, z)) continue;
                checked++;
                for (int y = p.minY(); y < p.maxY(); y += 7) assertTrue(p.sampler().sample(x, y, z) < 0);
            }
        }
        assertEquals(50, checked);
    }

    @Test
    void landJustInsideTheEdgeHangsOverNothing() {
        var p = profile(777L);
        int checked = 0;
        for (int x = -4000; x < 4000 && checked < 40; x += 13) {
            for (int z = -4000; z < 4000 && checked < 40; z += 13) {
                double m = p.chasms().margin(x, z);
                if (m < 0.0 || m > 0.01) continue;
                checked++;
                assertTrue(p.sampler().sample(x, p.minY() + 2, z) < 0, "rock at the bottom of the cliff edge");
                boolean ground = false;
                for (int y = 0; y < 140 && !ground; y++) ground = p.sampler().sample(x, y, z) > 0;
                assertTrue(ground, "no ground at the cliff edge");
            }
        }
        assertTrue(checked >= 20, "found " + checked + " edge columns");
    }

    @Test
    void cracksOnlyNearTheEdgeAndDeeperCloser() {
        var mask = profile(777L).chasms();
        long near = 0, nearCount = 0, far = 0, farCount = 0;
        int cracked = 0, total = 0;
        for (int x = -6000; x < 6000; x += 3) {
            for (int z = -6000; z < 6000; z += 3) {
                double m = mask.margin(x, z);
                int d = mask.crackDepth(x, z);
                if (m <= 0.0 || m >= 0.55) {
                    assertTrue(d == 0, "crack outside the band");
                    continue;
                }
                total++;
                if (d > 0) cracked++;
                if (m < 0.2) {
                    nearCount++;
                    if (d > 0) near += d;
                } else if (m > 0.35) {
                    farCount++;
                    if (d > 0) far += d;
                }
            }
        }
        System.out.println("CRACKS " + (100.0 * cracked / total) + "% of the band columns");
        assertTrue(cracked > 0 && 100.0 * cracked / total < 25.0, "cracks cover " + (100.0 * cracked / total) + "%");
        assertTrue((double) near / Math.max(1, nearCount) > (double) far / Math.max(1, farCount), "not deeper near");
    }
}
