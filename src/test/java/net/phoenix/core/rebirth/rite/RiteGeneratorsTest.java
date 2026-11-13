package net.phoenix.core.rebirth.rite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiteGeneratorsTest {

    @Test
    void crucibleBatchesAreSolvable() {
        for (long seed = 0; seed < 300; seed++) {
            int[] d = new CrucibleRite(seed).data();
            int n = d[1];
            assertEquals(CrucibleRite.LENGTH, n);
            long sum = 0;
            for (int i = 0; i < n; i++) sum += 1L << d[2 + i];
            assertEquals(1L << CrucibleRite.GOLD, sum);
            int[] row = new int[n];
            System.arraycopy(d, 2, row, 0, n);
            assertTrue(canMerge(row, n), "seed " + seed);
        }
    }

    private static boolean canMerge(int[] row, int n) {
        if (n == 1) return row[0] == CrucibleRite.GOLD;
        for (int i = 0; i + 1 < n; i++) {
            if (row[i] != row[i + 1]) continue;
            int[] next = new int[n - 1];
            for (int k = 0, j = 0; k < n; k++) {
                if (k == i + 1) continue;
                next[j++] = k == i ? row[k] + 1 : row[k];
            }
            if (canMerge(next, n - 1)) return true;
        }
        return false;
    }

    @Test
    void stellarRitesBuildForManySeeds() {
        for (long seed = 0; seed < 100; seed++) {
            int[] d = new StellarRite(seed).data();
            assertTrue(d[5] < d[6] && d[6] < d[7]);
            for (int s = 0; s < 3; s++) assertTrue(d[8 + 2 * s] <= d[9 + 2 * s]);
        }
    }

    @Test
    void tokamakRiteWinsOnTheExactMix() {
        TokamakRite rite = new TokamakRite(5);
        for (int a = 0; a <= 9; a++) {
            for (int b = 0; b <= 9; b++) {
                for (int c = 0; c <= 9; c++) {
                    TokamakRite r = new TokamakRite(5);
                    if (r.act(TokamakRite.INJECT, new int[] { a, b, c }).outcome() == Rite.Outcome.SUCCESS) return;
                }
            }
        }
        fail("no mix wins");
    }

    @Test
    void cortexRiteFitsThePacketAndCanBeWonByGreedyPlay() {
        for (long seed = 0; seed < 100; seed++) {
            CortexRite rite = new CortexRite(seed);
            int[] d = rite.data();
            assertTrue(d.length <= 64);
            int[] grid = new int[CortexRite.SIZE * CortexRite.SIZE];
            System.arraycopy(d, 3, grid, 0, grid.length);
            int budget = d[1];
            int used = 0;
            while (!CortexRite.uniform(grid) && used < budget) {
                int best = -1;
                int bestSize = -1;
                for (int c = 0; c < CortexRite.COLORS; c++) {
                    if (c == grid[0]) continue;
                    int[] copy = grid.clone();
                    CortexRite.flood(copy, c);
                    int size = CortexRite.covered(copy);
                    if (size > bestSize) {
                        bestSize = size;
                        best = c;
                    }
                }
                Rite.Reply r = rite.act(CortexRite.FLOOD, new int[] { best });
                CortexRite.flood(grid, best);
                used++;
                if (r.outcome() == Rite.Outcome.SUCCESS) break;
                assertNotEquals(Rite.Outcome.FAIL, r.outcome(), "seed " + seed);
            }
            assertTrue(CortexRite.uniform(grid), "seed " + seed);
        }
    }

    @Test
    void infuserRiteIsWonByTheStandardHanoiSolution() {
        InfuserRite rite = new InfuserRite(1);
        Rite.Reply last = Rite.Reply.go("");
        int[][] moves = hanoi(InfuserRite.DISKS, 0, 2, 1);
        assertEquals(15, moves.length);
        for (int[] m : moves) last = rite.act(InfuserRite.MOVE, m);
        assertEquals(Rite.Outcome.SUCCESS, last.outcome());
    }

    private static int[][] hanoi(int n, int from, int to, int via) {
        if (n == 0) return new int[0][];
        int[][] a = hanoi(n - 1, from, via, to);
        int[][] c = hanoi(n - 1, via, to, from);
        int[][] out = new int[a.length + 1 + c.length][];
        System.arraycopy(a, 0, out, 0, a.length);
        out[a.length] = new int[] { from, to };
        System.arraycopy(c, 0, out, a.length + 1, c.length);
        return out;
    }

    @Test
    void colliderRitesStartUnsolvedAndHaveASolution() {
        for (long seed = 0; seed < 150; seed++) {
            int[] d = new ColliderRite(seed).data();
            int n = d[0] * d[0];
            int[] cells = new int[n];
            System.arraycopy(d, 3, cells, 0, n);
            assertTrue(d.length <= 64);
            assertFalse(ColliderRite.solved(cells, d[1], d[2]), "seed " + seed);
            int[] at = new int[n];
            int k = 0;
            for (int i = 0; i < n; i++) if (cells[i] != 0) at[k++] = i;
            boolean found = false;
            for (int mask = 0; mask < (1 << k) && !found; mask++) {
                int[] trial = cells.clone();
                for (int j = 0; j < k; j++) trial[at[j]] = ((mask >> j) & 1) == 0 ? 1 : 2;
                found = ColliderRite.solved(trial, d[1], d[2]);
            }
            assertTrue(found, "seed " + seed);
        }
    }

    @Test
    void ascensionRitesCanAlwaysBeWon() {
        for (long seed = 0; seed < 100; seed++) {
            AscensionRite rite = new AscensionRite(seed);
            assertTrue(rite.canBeWon(), "seed " + seed);
            assertTrue(rite.data().length <= 64);
        }
    }
}
