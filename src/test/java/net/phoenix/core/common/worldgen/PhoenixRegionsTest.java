package net.phoenix.core.common.worldgen;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoenixRegionsTest {

    @Test
    void sevenRegionsAllAppear() {
        List<TerrainRegions.Spec> specs = List.of(
                new TerrainRegions.Spec("plains", TerrainProfile.Style.HEIGHTMAP, 70, 22, 0.004, 4, 0, 0.3),
                new TerrainRegions.Spec("peaks", TerrainProfile.Style.RIDGED, 96, 112, 0.005, 6, 0, 0.0),
                new TerrainRegions.Spec("crimson", TerrainProfile.Style.TERRACED, 78, 40, 0.0035, 4, 9.0, 0.05),
                new TerrainRegions.Spec("ashlands", TerrainProfile.Style.HEIGHTMAP, 60, 14, 0.003, 3, 0, 0.15),
                new TerrainRegions.Spec("sulfur", TerrainProfile.Style.WARPED, 68, 34, 0.005, 4, 24.0, 0.0),
                new TerrainRegions.Spec("basalt", TerrainProfile.Style.TERRACED, 58, 12, 0.003, 3, 4.0, 0.05),
                new TerrainRegions.Spec("grove", TerrainProfile.Style.HEIGHTMAP, 76, 36, 0.0045, 5, 0, 0.0));
        TerrainRegions r = new TerrainRegions(123L, specs);
        Map<String, Integer> seen = new HashMap<>();
        for (int x = -8000; x < 8000; x += 32) {
            for (int z = -8000; z < 8000; z += 32) seen.merge(r.biomeAt(x, z), 1, Integer::sum);
        }
        System.out.println("PHOENIX REGIONS " + seen);
        for (var s : specs) assertTrue(seen.getOrDefault(s.biomeId(), 0) > 150, s.biomeId() + " is rare: " + seen);
    }
}
