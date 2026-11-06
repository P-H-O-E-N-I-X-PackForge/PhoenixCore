package net.phoenix.core.integration.conflux.dimension;

import net.phoenix.core.common.worldgen.TerrainProfile;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class WorldgenBenchTest {

    private static final String[] DISCIPLINES = { "phoenix", "sculk", "void", "sealed_a", "sealed_b" };

    private static java.util.List<net.phoenix.core.common.worldgen.TerrainRegions.Spec> sculkRegions() {
        return java.util.List.of(

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_forest",
                        TerrainProfile.Style.HEIGHTMAP, 68, 40, 0.004, 5, 0, 0.3),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_ridge", TerrainProfile.Style.RIDGED,
                        86, 82, 0.006, 5, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_depths", TerrainProfile.Style.WARPED,
                        56, 32, 0.005, 4, 20.0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_shriek",
                        TerrainProfile.Style.HEIGHTMAP, 74, 58, 0.0055, 5, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_hollow",
                        TerrainProfile.Style.HEIGHTMAP, 63, 28, 0.0045, 4, 0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_marsh",
                        TerrainProfile.Style.HEIGHTMAP, 56, 10, 0.003, 3, 0, 0.1),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("sculk_thicket", TerrainProfile.Style.WARPED,
                        71, 38, 0.0048, 4, 18.0, 0.0));
    }

    private static java.util.List<net.phoenix.core.common.worldgen.TerrainRegions.Spec> phoenixRegions() {
        return java.util.List.of(

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_plains",
                        TerrainProfile.Style.HEIGHTMAP, 70, 36, 0.004, 5, 0, 0.3),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_peaks", TerrainProfile.Style.RIDGED,
                        96, 112, 0.005, 6, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_crimson",
                        TerrainProfile.Style.TERRACED, 78, 40, 0.0035, 4, 9.0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_ashlands",
                        TerrainProfile.Style.HEIGHTMAP, 60, 28, 0.0035, 4, 0, 0.15),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_sulfur_fields",
                        TerrainProfile.Style.WARPED, 68, 34, 0.005, 4, 24.0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_basalt_flats",
                        TerrainProfile.Style.TERRACED, 58, 12, 0.003, 3, 4.0, 0.05),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("phoenix_scorched_grove",
                        TerrainProfile.Style.HEIGHTMAP, 76, 36, 0.0045, 5, 0, 0.0));
    }

    private static java.util.List<net.phoenix.core.common.worldgen.TerrainRegions.Spec> voidRegions() {
        return java.util.List.of(

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_island", TerrainProfile.Style.HEIGHTMAP,
                        70, 38, 0.004, 5, 0, 0.45),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_crystal_field",
                        TerrainProfile.Style.RIDGED, 76, 78, 0.006, 5, 0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_amethyst_grove",
                        TerrainProfile.Style.WARPED, 64, 42, 0.005, 4, 26.0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_starlit_meadow",
                        TerrainProfile.Style.TERRACED, 88, 32, 0.003, 4, 7.0, 0.0),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_lavender_fields",
                        TerrainProfile.Style.HEIGHTMAP, 60, 9, 0.003, 3, 0, 0.1),

                new net.phoenix.core.common.worldgen.TerrainRegions.Spec("void_drift", TerrainProfile.Style.RIDGED,
                        68, 34, 0.008, 4, 0, -0.05));
    }

    private static TerrainProfile createTerrainForDiscipline(String disciplineId, long seed) {
        return switch (disciplineId) {
            case "phoenix" -> TerrainProfile.builder("phoenix")
                    .seed(seed)
                    .baseY(72).amplitude(100).frequency(0.004).octaves(6)
                    .style(TerrainProfile.Style.RIDGED)
                    .flatShare(0.40)
                    .seaLevel(34)
                    .regions(phoenixRegions())
                    .basins(0.0032, 0.85, 14)
                    .caves(true).build();

            case "sculk" -> TerrainProfile.builder("sculk")
                    .seed(seed).baseY(66).amplitude(45)
                    .frequency(0.0035).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .seaLevel(57)
                    .regions(sculkRegions())
                    .caves(true).build();

            case "void" -> TerrainProfile.builder("void")
                    .seed(seed).baseY(68).amplitude(55)
                    .frequency(0.003).octaves(5)
                    .style(TerrainProfile.Style.HEIGHTMAP)
                    .seaLevel(55)
                    .regions(voidRegions())
                    .chasms(0.0075, 0.53)
                    .caves(true).build();

            case "sealed_a" -> TerrainProfile.builder("sealed_a")
                    .seed(seed).baseY(68).amplitude(40)
                    .frequency(0.003).octaves(4)
                    .style(TerrainProfile.Style.TERRACED).terraceStep(6.0)
                    .seaLevel(52)
                    .caves(true).build();

            case "sealed_b" -> TerrainProfile.builder("sealed_b")
                    .seed(seed).baseY(65).amplitude(75)
                    .frequency(0.0032).octaves(6)
                    .style(TerrainProfile.Style.WARPED).warpStrength(20.0)
                    .seaLevel(47)
                    .caves(true).build();

            default -> TerrainProfile.builder("default").seed(seed).caves(true).build();
        };
    }

    private static TerrainProfile profile(String id) {
        return createTerrainForDiscipline(id, 987654321L);
    }

    private static long terrainGrid(TerrainProfile p, int chunkX, int chunkZ) {
        int minX = chunkX * 16, minZ = chunkZ * 16, minY = p.minY(), maxY = p.maxY();
        int cell = 4;
        int gridY = (maxY - minY + cell - 1) / cell + 1;
        long h = 1125899906842597L;
        for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
                for (int gy = 0; gy < gridY; gy++) {
                    double v = p.sampler().sample(minX + gx * cell, Math.min(minY + gy * cell, maxY - 1),
                            minZ + gz * cell);
                    h = h * 31 + Double.doubleToLongBits(v);
                }
            }
        }
        return h;
    }

    private static int groundHeight(TerrainProfile profile, int x, int z, int minY, int maxY) {
        int result = minY;
        int top = maxY - 1;
        for (int y = top; y >= minY; y -= 4) {
            if (profile.sampler().sample(x, y, z) > 0) {
                result = y + 1;
                for (int above = Math.min(top, y + 4 - 1); above > y; above--) {
                    if (profile.sampler().sample(x, above, z) > 0) {
                        result = above + 1;
                        break;
                    }
                }
                break;
            }
        }
        if (result == minY && profile.sampler().sample(x, minY, z) > 0) result = minY + 1;
        return result;
    }

    private static int groundHeightFast(TerrainProfile profile, int x, int z, int minY, int maxY) {
        int top = maxY - 1;
        int start = Math.min(top, profile.groundCeiling());
        if (start < top && profile.sampler().sample(x, start, z) > 0) start = top;
        int result = minY;
        for (int y = start; y >= minY; y -= 4) {
            if (profile.sampler().sample(x, y, z) > 0) {
                result = y + 1;
                for (int above = Math.min(top, y + 4 - 1); above > y; above--) {
                    if (profile.sampler().sample(x, above, z) > 0) {
                        result = above + 1;
                        break;
                    }
                }
                break;
            }
        }
        if (result == minY && profile.sampler().sample(x, minY, z) > 0) result = minY + 1;
        return result;
    }

    @Test
    void theCeilingSearchFindsTheSameGroundAsTheFullSearch() {
        for (String id : DISCIPLINES) {
            int checked = 0, mismatches = 0, fellBack = 0, highest = Integer.MIN_VALUE;
            for (long seed : new long[] { 1L, 987654321L, -55555L, 20261104L, 424242L }) {
                TerrainProfile p = createTerrainForDiscipline(id, seed);
                int ceiling = p.groundCeiling();
                java.util.Random r = new java.util.Random(seed);
                for (int i = 0; i < 40_000; i++) {
                    int x = (int) (r.nextGaussian() * 2500);
                    int z = (int) (r.nextGaussian() * 2500);
                    int slow = groundHeight(p, x, z, p.minY(), p.maxY());
                    int fast = groundHeightFast(p, x, z, p.minY(), p.maxY());
                    checked++;
                    if (slow != fast) mismatches++;
                    highest = Math.max(highest, slow);
                    if (p.sampler().sample(x, Math.min(p.maxY() - 1, ceiling), z) > 0) fellBack++;
                }
                org.junit.jupiter.api.Assertions.assertTrue(highest <= ceiling,
                        id + ": ground at " + highest + " reaches the ceiling " + ceiling);
            }
            System.out.println(id + ": " + checked + " columns, " + mismatches + " mismatches, " + fellBack +
                    " reached the ceiling, highest ground " + highest);
            org.junit.jupiter.api.Assertions.assertEquals(0, mismatches, id);
        }
    }

    private static final int DEEP_LAVA_TOP = 9;

    private static byte[] fillColumns(TerrainProfile profile, int chunkX, int chunkZ, boolean earlyExit) {
        int minX = chunkX * 16, minZ = chunkZ * 16, minY = profile.minY(), maxY = profile.maxY();
        int seaLevel = profile.seaLevel();
        int depth = maxY - minY;
        byte[] out = new byte[16 * 16 * depth];
        final int cell = 4;
        int cellsY = (maxY - minY + cell - 1) / cell;
        int gridY = cellsY + 1;
        double[] grid = new double[5 * 5 * gridY];
        for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
                int base = (gx * 5 + gz) * gridY;
                for (int gy = 0; gy < gridY; gy++) {
                    grid[base + gy] = profile.sampler().sample(minX + gx * cell, Math.min(minY + gy * cell, maxY - 1),
                            minZ + gz * cell);
                }
            }
        }
        int[] topLayer = new int[16];
        for (int cx = 0; cx < 4; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                int c00 = (cx * 5 + cz) * gridY, c01 = (cx * 5 + cz + 1) * gridY;
                int c10 = ((cx + 1) * 5 + cz) * gridY, c11 = ((cx + 1) * 5 + cz + 1) * gridY;
                int top = gridY - 1;
                while (top > 0 && grid[c00 + top] <= 0 && grid[c01 + top] <= 0 && grid[c10 + top] <= 0 &&
                        grid[c11 + top] <= 0) {
                    top--;
                }
                topLayer[cx * 4 + cz] = top;
            }
        }
        for (int x = minX; x < minX + 16; x++) {
            double fx = (x - minX) / (double) cell;
            int cx = (int) fx;
            fx -= cx;
            for (int z = minZ; z < minZ + 16; z++) {
                double fz = (z - minZ) / (double) cell;
                int cz = (int) fz;
                fz -= cz;
                int b00 = (cx * 5 + cz) * gridY, b01 = (cx * 5 + cz + 1) * gridY;
                int b10 = ((cx + 1) * 5 + cz) * gridY, b11 = ((cx + 1) * 5 + cz + 1) * gridY;
                int ground = minY - 1;
                boolean open = profile.chasms() != null &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 1, cell, gridY) < 0 &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 40, cell, gridY) < 0 &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 100, cell, gridY) < 0;
                boolean deepOpen = profile.chasms() != null &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 1, cell, gridY) < 0 &&
                        interp(grid, b00, b01, b10, b11, fx, fz, minY, minY + 20, cell, gridY) < 0;
                int columnTop = topLayer[cx * 4 + cz];
                int col = ((x - minX) * 16 + (z - minZ)) * depth;
                for (int y = minY; y < maxY; y++) {
                    if (open) break;
                    int gy = (y - minY) / cell;
                    if (earlyExit && gy > columnTop && y > minY + DEEP_LAVA_TOP) break;
                    double fy = (y - minY) / (double) cell - gy;
                    int g1 = Math.min(gy + 1, gridY - 1);
                    double d00 = grid[b00 + gy] + (grid[b00 + g1] - grid[b00 + gy]) * fy;
                    double d01 = grid[b01 + gy] + (grid[b01 + g1] - grid[b01 + gy]) * fy;
                    double d10 = grid[b10 + gy] + (grid[b10 + g1] - grid[b10 + gy]) * fy;
                    double d11 = grid[b11 + gy] + (grid[b11 + g1] - grid[b11 + gy]) * fy;
                    double d0 = d00 + (d01 - d00) * fz;
                    double d1 = d10 + (d11 - d10) * fz;
                    boolean solid = d0 + (d1 - d0) * fx > 0;
                    int state;
                    if (y == minY) {
                        if (deepOpen) continue;
                        state = 3;
                    } else if (!solid) {
                        if (deepOpen || y > minY + DEEP_LAVA_TOP) continue;
                        state = 2;
                    } else {
                        state = 1;
                    }
                    out[col + (y - minY)] = (byte) state;
                    ground = y;
                }
                for (int y = open ? ground : Math.min(seaLevel, maxY - 1); y > ground; y--) {
                    out[col + (y - minY)] = 4;
                }
            }
        }
        return out;
    }

    private static double interp(double[] grid, int b00, int b01, int b10, int b11, double fx, double fz, int minY,
                                 int y, int cell, int gridY) {
        int gy = (y - minY) / cell;
        double fy = (y - minY) / (double) cell - gy;
        int g1 = Math.min(gy + 1, gridY - 1);
        gy = Math.min(gy, gridY - 1);
        double d00 = grid[b00 + gy] + (grid[b00 + g1] - grid[b00 + gy]) * fy;
        double d01 = grid[b01 + gy] + (grid[b01 + g1] - grid[b01 + gy]) * fy;
        double d10 = grid[b10 + gy] + (grid[b10 + g1] - grid[b10 + gy]) * fy;
        double d11 = grid[b11 + gy] + (grid[b11 + g1] - grid[b11 + gy]) * fy;
        double d0 = d00 + (d01 - d00) * fz;
        double d1 = d10 + (d11 - d10) * fz;
        return d0 + (d1 - d0) * fx;
    }

    @Test
    void stoppingAtTheTopOfTheGroundPlacesExactlyTheSameBlocks() {
        for (String id : DISCIPLINES) {
            int chunks = 0, differing = 0;
            long slowNs = 0, fastNs = 0;
            for (long seed : new long[] { 1L, 987654321L, -55555L }) {
                TerrainProfile p = createTerrainForDiscipline(id, seed);
                java.util.Random r = new java.util.Random(seed);
                for (int i = 0; i < 120; i++) {
                    int cx = (int) (r.nextGaussian() * 120), cz = (int) (r.nextGaussian() * 120);
                    long a = System.nanoTime();
                    byte[] full = fillColumns(p, cx, cz, false);
                    long b = System.nanoTime();
                    byte[] early = fillColumns(p, cx, cz, true);
                    long c = System.nanoTime();
                    slowNs += b - a;
                    fastNs += c - b;
                    chunks++;
                    if (!java.util.Arrays.equals(full, early)) differing++;
                }
            }
            System.out.println(id + ": " + chunks + " chunks, " + differing + " differ; fill loop " +
                    String.format("%.2f", slowNs / 1e6 / chunks) + " ms -> " +
                    String.format("%.2f", fastNs / 1e6 / chunks) + " ms per chunk");
            org.junit.jupiter.api.Assertions.assertEquals(0, differing, id);
        }
    }

    @Test
    void measure() throws Exception {
        String out = System.getenv("BENCH_OUT");
        List<String> lines = new ArrayList<>();
        for (String id : DISCIPLINES) {
            TerrainProfile p = profile(id);
            int chunks = 48;

            int[] offsets = { 0, 40, 200, 900, -700, 3000 };

            for (int warm = 0; warm < 2; warm++) {
                for (int i = 0; i < 16; i++) terrainGrid(p, offsets[i % offsets.length] + i, 5 + i);
            }
            long gridHash = 0;
            long t0 = System.nanoTime();
            for (int i = 0; i < chunks; i++) {
                gridHash = gridHash * 131 + terrainGrid(p, offsets[i % offsets.length] + i * 3, 11 + i * 5);
            }
            double gridMs = (System.nanoTime() - t0) / 1e6 / chunks;

            long groundHash = 0;
            long samples = 0;
            int cols = 0;
            long t1 = System.nanoTime();
            for (int c = 0; c < 12; c++) {
                int bx = (offsets[c % offsets.length] + c * 7) * 16;
                int bz = (20 + c * 9) * 16;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        groundHash = groundHash * 31 + groundHeight(p, bx + x, bz + z, p.minY(), p.maxY());
                        cols++;
                    }
                }
            }
            double groundMs = (System.nanoTime() - t1) / 1e6 / (cols / 256.0);

            long fastHash = 0;
            int fcols = 0;
            long t2 = System.nanoTime();
            for (int c = 0; c < 12; c++) {
                int bx = (offsets[c % offsets.length] + c * 7) * 16;
                int bz = (20 + c * 9) * 16;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        fastHash = fastHash * 31 + groundHeightFast(p, bx + x, bz + z, p.minY(), p.maxY());
                        fcols++;
                    }
                }
            }
            double fastMs = (System.nanoTime() - t2) / 1e6 / (fcols / 256.0);

            lines.add(String.format(
                    "%-9s grid %.3f ms/chunk  ground %.3f ms (full) -> %.3f ms (ceiling) per 256 cols  gridHash=%d groundHash=%d same=%b ceiling=%d",
                    id, gridMs, groundMs, fastMs, gridHash, groundHash, groundHash == fastHash, p.groundCeiling()));
        }
        String text = String.join("\n", lines) + "\n";
        System.out.print(text);
        if (out != null) Files.writeString(Path.of(out), text);
    }
}
