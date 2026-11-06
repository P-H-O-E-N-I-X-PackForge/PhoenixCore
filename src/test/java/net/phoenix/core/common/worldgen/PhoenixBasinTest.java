package net.phoenix.core.common.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoenixBasinTest {

    private static java.util.List<TerrainRegions.Spec> specs() {
        return java.util.List.of(
                new TerrainRegions.Spec("plains", TerrainProfile.Style.HEIGHTMAP, 70, 22, 0.004, 4, 0, 0.3),
                new TerrainRegions.Spec("peaks", TerrainProfile.Style.RIDGED, 96, 112, 0.005, 6, 0, 0.0),
                new TerrainRegions.Spec("crimson", TerrainProfile.Style.TERRACED, 78, 40, 0.0035, 4, 9.0, 0.05),
                new TerrainRegions.Spec("ashlands", TerrainProfile.Style.HEIGHTMAP, 60, 14, 0.003, 3, 0, 0.15),
                new TerrainRegions.Spec("sulfur", TerrainProfile.Style.WARPED, 68, 34, 0.005, 4, 24.0, 0.0),
                new TerrainRegions.Spec("basalt", TerrainProfile.Style.TERRACED, 58, 12, 0.003, 3, 4.0, 0.05),
                new TerrainRegions.Spec("grove", TerrainProfile.Style.HEIGHTMAP, 76, 36, 0.0045, 5, 0, 0.0));
    }

    private static TerrainProfile phoenix(long seed) {
        return TerrainProfile.builder("phoenix").seed(seed).baseY(72).amplitude(100).frequency(0.004).octaves(6)
                .style(TerrainProfile.Style.RIDGED).seaLevel(34).regions(specs()).basins(0.0032, 0.85, 14).caves(true)
                .build();
    }

    private static TerrainProfile withoutBasins(long seed) {
        return TerrainProfile.builder("phoenix").seed(seed).baseY(72).amplitude(100).frequency(0.004).octaves(6)
                .style(TerrainProfile.Style.RIDGED).seaLevel(34).regions(specs()).caves(true).build();
    }

    private static int top(TerrainProfile p, int x, int z) {
        for (int y = p.maxY() - 1; y >= p.minY(); y--) if (p.sampler().sample(x, y, z) > 0) return y;
        return p.minY();
    }

    @Test
    void seasExistButSpawnStaysDry() {
        for (long seed : new long[] { 3L, 777L, 99L }) {
            TerrainProfile p = phoenix(seed);
            for (int x = -200; x <= 200; x += 20) {
                for (int z = -200; z <= 200; z += 20) {
                    assertTrue(top(p, x, z) == top(withoutBasins(seed), x, z),
                            "basin near spawn at " + x + "," + z + " seed " + seed);
                }
            }
            int n = 0, wet = 0;
            for (int x = -4000; x < 4000; x += 40) {
                for (int z = -4000; z < 4000; z += 40) {
                    n++;
                    if (top(p, x, z) < p.seaLevel()) wet++;
                }
            }
            double share = 100.0 * wet / n;
            System.out.println("LAVASEA seed " + seed + ": " + share + "% below sea level");
            assertTrue(share > 3 && share < 25, "sea share " + share);
        }
    }
}
