package net.phoenix.core.integration.conflux.dimension.worldgen;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VeinPlanTest {

    private record Vein(int x, int z, int kind) {}

    private static List<Vein> world(long seed, int kinds, int cell, float chance, double gap, int half) {
        List<Vein> out = new ArrayList<>();
        for (int cx = -half; cx < half; cx++) {
            for (int cz = -half; cz < half; cz++) {
                if (!VeinPlan.stays(seed, cx, cz, cell, kinds, chance, gap,
                        (a, b) -> a >= -1 && a <= 0 && b >= -1 && b <= 0)) {
                    continue;
                }
                int[] o = VeinPlan.origin(seed, cx, cz, cell);
                out.add(new Vein(o[0], o[1], VeinPlan.kind(seed, cx, cz, kinds)));
            }
        }
        return out;
    }

    @Test
    void kindsAreDealtSoNeighboursDiffer() {
        for (long seed : new long[] { 1L, 77L, 424242L }) {
            for (int kinds : new int[] { 6, 7, 8 }) {
                List<Vein> veins = world(seed, kinds, 96, 0.85f, 200.0, 40);
                int total = 0, repeats = 0, minGapOk = 0;
                double worstSame = Double.MAX_VALUE;
                for (Vein v : veins) {
                    List<Vein> near = new ArrayList<>(veins);
                    near.remove(v);
                    near.sort(Comparator.comparingDouble(o -> Math.hypot(o.x() - v.x(), o.z() - v.z())));

                    total++;
                    boolean same = false;
                    for (int i = 0; i < 3 && i < near.size(); i++) same |= near.get(i).kind() == v.kind();
                    if (same) repeats++;
                    for (Vein o : near) {
                        if (o.kind() == v.kind()) {
                            worstSame = Math.min(worstSame, Math.hypot(o.x() - v.x(), o.z() - v.z()));
                            break;
                        }
                    }
                }
                double perCell = veins.size() / (80.0 * 80.0);
                System.out.println("VEINS seed " + seed + " kinds " + kinds + ": " + veins.size() + " veins (" +
                        (100.0 * perCell / 0.85) + "% of the possible), nearest same kind " + worstSame +
                        ", own kind among 3 nearest " + (100.0 * repeats / total) + "%");
                assertTrue(worstSame >= 200.0 - 1, "same kind too close: " + worstSame);
                assertTrue(100.0 * repeats / total < 5.0,
                        "own kind among the 3 nearest " + (100.0 * repeats / total) + "%");
                assertTrue(perCell / 0.85 > 0.4, "too few veins kept: " + (100.0 * perCell / 0.85) + "%");
            }
        }
    }
}
