package net.phoenix.core.common.worldgen;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SculkRegionsTest {

    @Test
    void everyRegionAppearsAndTheMarshIsTheFlatOne() {
        List<TerrainRegions.Spec> specs = List.of(
                new TerrainRegions.Spec("forest", TerrainProfile.Style.HEIGHTMAP, 68, 40, 0.004, 5, 0, 0.3),
                new TerrainRegions.Spec("ridge", TerrainProfile.Style.RIDGED, 86, 82, 0.006, 5, 0, 0.0),
                new TerrainRegions.Spec("depths", TerrainProfile.Style.WARPED, 56, 32, 0.005, 4, 20.0, 0.05),
                new TerrainRegions.Spec("shriek", TerrainProfile.Style.HEIGHTMAP, 74, 58, 0.0055, 5, 0, 0.0),
                new TerrainRegions.Spec("hollow", TerrainProfile.Style.HEIGHTMAP, 63, 28, 0.0045, 4, 0, 0.05),
                new TerrainRegions.Spec("marsh", TerrainProfile.Style.HEIGHTMAP, 56, 10, 0.003, 3, 0, 0.1),
                new TerrainRegions.Spec("thicket", TerrainProfile.Style.WARPED, 71, 38, 0.0048, 4, 18.0, 0.0));
        TerrainRegions r = new TerrainRegions(31L, specs);
        TerrainSampler s = r.sampler();
        Map<String, Integer> seen = new HashMap<>();
        Map<String, double[]> rough = new HashMap<>();
        for (int x = -8000; x < 8000; x += 32) {
            for (int z = -8000; z < 8000; z += 32) {
                String id = r.biomeAt(x, z);
                seen.merge(id, 1, Integer::sum);
                double step = Math.abs(s.sample(x + 24, 0, z) - s.sample(x, 0, z));
                double[] acc = rough.computeIfAbsent(id, k -> new double[2]);
                acc[0] += step;
                acc[1] += 1;
            }
        }
        System.out.println("SCULK REGIONS " + seen);
        for (var spec : specs) assertTrue(seen.getOrDefault(spec.biomeId(), 0) > 150, spec.biomeId() + " is rare");
        double marsh = rough.get("marsh")[0] / rough.get("marsh")[1];
        double shriek = rough.get("shriek")[0] / rough.get("shriek")[1];
        System.out.println("SCULK roughness marsh " + marsh + " domes " + shriek);
        assertTrue(shriek > marsh * 1.8, "domes " + shriek + " vs marsh " + marsh);
    }
}
