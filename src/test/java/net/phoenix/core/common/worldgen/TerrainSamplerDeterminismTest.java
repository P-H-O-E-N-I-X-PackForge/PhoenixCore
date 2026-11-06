package net.phoenix.core.common.worldgen;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainSamplerDeterminismTest {

    private static TerrainProfile phoenix() {
        return TerrainProfile.builder("phoenix").seed(12345L).baseY(72).amplitude(100).frequency(0.004).octaves(6)
                .style(TerrainProfile.Style.RIDGED).flatShare(0.40).seaLevel(28).caves(true).build();
    }

    private static TerrainProfile sculk() {
        return TerrainProfile.builder("sculk").seed(12345L).baseY(66).amplitude(45).frequency(0.0035).octaves(5)
                .style(TerrainProfile.Style.HEIGHTMAP).seaLevel(57).caves(true).build();
    }

    @Test
    void callOrderDoesNotChangeTheAnswer() {
        for (TerrainProfile p : new TerrainProfile[] { phoenix(), sculk() }) {
            Random rnd = new Random(1);
            int columns = 150;
            int steps = 96;
            for (int i = 0; i < columns; i++) {
                int x = rnd.nextInt(2000) - 1000;
                int z = rnd.nextInt(2000) - 1000;
                double[] up = new double[steps];
                for (int k = 0; k < steps; k++) up[k] = p.sampler().sample(x, p.minY() + k * 4, z);
                double[] down = new double[steps];
                for (int k = steps - 1; k >= 0; k--) down[k] = p.sampler().sample(x, p.minY() + k * 4, z);
                p.sampler().sample(x + 4, 200, z + 4);
                for (int k = 0; k < steps; k++) {
                    assertEquals(up[k], down[k], 1e-9, p.name() + " " + x + "," + z + " step " + k);
                    assertEquals(up[k], p.sampler().sample(x, p.minY() + k * 4, z), 1e-9);
                }
            }
        }
    }
}
