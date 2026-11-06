package net.phoenix.core.common.worldgen;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainRegionsTest {

    private static List<TerrainRegions.Spec> specs() {
        return List.of(
                new TerrainRegions.Spec("meadow", TerrainProfile.Style.HEIGHTMAP, 70, 26, 0.004, 4, 0, 0.45),
                new TerrainRegions.Spec("ridges", TerrainProfile.Style.RIDGED, 76, 78, 0.006, 5, 0, 0.0),
                new TerrainRegions.Spec("mesa", TerrainProfile.Style.TERRACED, 88, 24, 0.003, 3, 7.0, 0.0),
                new TerrainRegions.Spec("plains", TerrainProfile.Style.HEIGHTMAP, 60, 9, 0.003, 3, 0, 0.1));
    }

    @Test
    void everyRegionAppearsAndTheyDifferInRelief() {
        TerrainRegions r = new TerrainRegions(777L, specs());
        TerrainSampler s = r.sampler();
        Map<String, Integer> seen = new HashMap<>();
        Map<String, double[]> range = new HashMap<>();
        for (int x = -8000; x < 8000; x += 32) {
            for (int z = -8000; z < 8000; z += 32) {
                String id = r.biomeAt(x, z);
                seen.merge(id, 1, Integer::sum);
                double h = s.sample(x, 0, z);
                double[] mm = range.computeIfAbsent(id, k -> new double[] { 1e9, -1e9 });
                mm[0] = Math.min(mm[0], h);
                mm[1] = Math.max(mm[1], h);
            }
        }
        System.out.println("REGIONS " + seen);
        for (var spec : specs()) assertTrue(seen.getOrDefault(spec.biomeId(), 0) > 200, spec.biomeId() + " is rare");
        double ridgeSpan = range.get("ridges")[1] - range.get("ridges")[0];
        double plainsSpan = range.get("plains")[1] - range.get("plains")[0];
        assertTrue(ridgeSpan > plainsSpan * 2, "ridges " + ridgeSpan + " vs plains " + plainsSpan);
    }

    @Test
    void borderColumnsBlendSmoothly() {
        TerrainRegions r = new TerrainRegions(5L, specs());
        TerrainSampler s = r.sampler();
        double worst = 0;
        for (int z = -3000; z < 3000; z++) {
            worst = Math.max(worst, Math.abs(s.sample(900, 0, z + 1) - s.sample(900, 0, z)));
        }
        assertTrue(worst < 25, "step between neighbouring columns " + worst);
    }
}
