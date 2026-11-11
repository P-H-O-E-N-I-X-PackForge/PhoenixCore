package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PanopticPuzzleTest {

    private static Testimony testimony(String discipline, int facts) {
        List<Testimony.Fact> list = new ArrayList<>();
        for (int i = 0; i < facts; i++) {
            list.add(new Testimony.Fact(ResourceLocation.parse("phoenixcore:node_" + i), "Node " + i, i % 4));
        }
        return new Testimony(discipline, 1234L, list, new int[] { 7, 2, 5, 9, 1 });
    }

    @Test
    void everyRoundOfEveryPuzzleIsSolvableAndDeterministic() {
        for (String discipline : List.of("phoenix", "sculk", "void", "sealed_a", "sealed_b")) {
            PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline(discipline);
            assertNotNull(puzzle, discipline);
            for (int facts : new int[] { 0, 2, 12 }) {
                Testimony t = testimony(discipline, facts);
                for (int round = 0; round < PanopticTreeModel.STAGES; round++) {
                    PanopticPuzzle.Round a = puzzle.generate(t, round, 99L);
                    PanopticPuzzle.Round b = puzzle.generate(t, round, 99L);
                    assertEquals(a.clues(), b.clues(), discipline + " round " + round);
                    assertArrayEquals(a.truth(), b.truth());

                    int[] answer = solution(discipline, a);
                    PanopticPuzzle.Verdict v = puzzle.check(a, new PanopticPuzzle.Answer(answer));
                    assertTrue(v.correct(), discipline + " round " + round + ": " + v.message());
                    assertEquals(1f, v.closeness(), 1e-6);
                }
            }
        }
    }

    private static int[] solution(String discipline, PanopticPuzzle.Round round) {
        int[] truth = round.truth();
        if (discipline.equals("phoenix")) {
            Integer[] order = new Integer[truth.length];
            for (int i = 0; i < order.length; i++) order[i] = i;
            java.util.Arrays.sort(order, (x, y) -> Integer.compare(truth[x], truth[y]));
            int[] out = new int[order.length];
            for (int i = 0; i < out.length; i++) out[i] = order[i];
            return out;
        }
        return truth.clone();
    }

    @Test
    void wrongAnswersAreRejectedAndNearMissesScorePartially() {
        Testimony t = testimony("sculk", 6);
        PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline("sculk");
        PanopticPuzzle.Round r = puzzle.generate(t, 3, 5L);
        int[] wrong = r.truth().clone();
        wrong[0] = (wrong[0] + 1) % 5;
        PanopticPuzzle.Verdict v = puzzle.check(r, new PanopticPuzzle.Answer(wrong));
        assertFalse(v.correct());
        assertTrue(v.closeness() > 0f && v.closeness() < 1f);
    }

    @Test
    void voidCluesPinDownOneAnswer() {
        PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline("void");
        for (long seed = 0; seed < 60; seed++) {
            Testimony t = testimony("void", 5);
            PanopticPuzzle.Round r = puzzle.generate(t, (int) (seed % 7), seed);
            int total = 0;
            for (int x : r.truth()) total += x;

            int solutions = 0;
            for (int a0 = 0; a0 <= total; a0++) {
                if (solve(r, a0, total)) solutions++;
            }
            assertEquals(1, solutions, "seed " + seed + " " + r.clues());
        }
    }

    private static boolean solve(PanopticPuzzle.Round r, int a0, int total) {
        String[] names = { "Material", "Biological", "Energetic", "Computational", "Arcane" };
        int[] a = new int[5];
        Boolean[] known = new Boolean[5];
        a[0] = a0;
        known[0] = true;
        boolean progress = true;
        while (progress) {
            progress = false;
            for (String clue : r.clues()) {
                if (clue.startsWith("Between")) continue;
                int[] ij = pair(clue, names);
                int i = ij[0];
                int j = ij[1];
                int k = ij[2];
                boolean sum = clue.contains("together make");
                boolean eq = clue.contains(" equals ");

                if (known[j] == Boolean.TRUE && known[i] == null) {
                    a[i] = sum ? k - a[j] : eq ? a[j] : a[j] + k;
                    known[i] = true;
                    progress = true;
                } else if (known[i] == Boolean.TRUE && known[j] == null) {
                    a[j] = sum ? k - a[i] : eq ? a[i] : a[i] - k;
                    known[j] = true;
                    progress = true;
                }
            }
        }
        int sumAll = 0;
        for (int i = 0; i < 5; i++) {
            if (known[i] == null || a[i] < 0) return false;
            sumAll += a[i];
        }
        return sumAll == total;
    }

    private static int[] pair(String clue, String[] names) {
        int first = -1;
        int second = -1;
        int firstAt = Integer.MAX_VALUE;
        int secondAt = Integer.MAX_VALUE;
        for (int i = 0; i < names.length; i++) {
            int at = clue.indexOf(names[i]);
            if (at < 0) continue;
            if (at < firstAt) {
                second = first;
                secondAt = firstAt;
                first = i;
                firstAt = at;
            } else if (at < secondAt) {
                second = i;
                secondAt = at;
            }
        }
        int k = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(clue);
        if (m.find()) k = Integer.parseInt(m.group(1));
        return new int[] { first, second, k };
    }

    @Test
    void treeIsDeterministicAndGrowsWithProgress() {
        PanopticTreeModel.Params young = PanopticTreeModel.params("phoenix", 0.1f);
        PanopticTreeModel.Params old = PanopticTreeModel.params("phoenix", 1f);
        assertTrue(old.height() > young.height());
        assertTrue(old.trunkRadius() > young.trunkRadius());
        assertEquals(0f, young.leafFallPerSecond());
        assertTrue(old.leafFallPerSecond() > 0f);

        var a = PanopticTreeModel.branches(42L, old);
        var b = PanopticTreeModel.branches(42L, old);
        assertEquals(a, b);
        assertTrue(a.size() > PanopticTreeModel.branches(42L, young).size());
        assertFalse(PanopticTreeModel.leaves(42L, a, old).isEmpty());
        assertNotEquals(a, PanopticTreeModel.branches(43L, old));
        assertEquals(0f, PanopticTreeModel.growth(0, 0f));
        assertEquals(1f, PanopticTreeModel.growth(PanopticTreeModel.STAGES, 0f));
    }

    @Test
    void offeringsFollowTheAnswerAndGuessingIsNeverFree() {
        Testimony t = testimony("sculk", 4);
        PanopticPuzzle sculk = PanopticPuzzles.forDiscipline("sculk");
        PanopticPuzzle.Round r = sculk.generate(t, 2, 1L);
        PanopticOffering o = sculk.offering(r, new PanopticPuzzle.Answer(new int[] { 0, 0, 3 }));
        assertEquals(2 * SculkPuzzle.DATA_PER_PULSE, o.data()[0]);
        assertEquals(SculkPuzzle.DATA_PER_PULSE, o.data()[3]);
        assertEquals(0, o.data()[1]);

        PanopticPuzzle voidPuzzle = PanopticPuzzles.forDiscipline("void");
        PanopticOffering v = voidPuzzle.offering(voidPuzzle.generate(testimony("void", 4), 1, 1L),
                new PanopticPuzzle.Answer(new int[] { 3, 0, 5, 0, 1 }));
        assertEquals(3 * VoidPuzzle.DATA_PER_UNIT, v.data()[0]);
        assertEquals(5 * VoidPuzzle.DATA_PER_UNIT, v.data()[2]);

        PanopticPuzzle phoenix = PanopticPuzzles.forDiscipline("phoenix");
        PanopticOffering p = phoenix.offering(phoenix.generate(testimony("phoenix", 6), 0, 1L),
                new PanopticPuzzle.Answer(new int[] { 0, 1, 2 }));
        assertEquals(1, p.fluids().size());
        assertEquals(3 * PhoenixPuzzle.LAVA_PER_EMBER, p.fluids().get(0).millibuckets());
        assertFalse(p.isFree());
        assertTrue(PanopticOffering.NONE.isFree());
    }

    @Test
    void sealedLockGivesFeedbackAndFollowsItsOwnClues() {
        PanopticPuzzle lock = PanopticPuzzles.forDiscipline("sealed_a");
        for (long seed = 0; seed < 80; seed++) {
            for (int round = 0; round < PanopticTreeModel.STAGES; round++) {
                PanopticPuzzle.Round r = lock.generate(testimony("sealed_a", 5), round, seed);
                int[] counts = new int[5];
                for (int x : r.truth()) counts[x]++;
                for (int c : counts) assertTrue(c <= 2, "a kind used more than twice: " + seed + "/" + round);

                assertNotEquals(4, r.truth()[0], "opened on the forbidden kind");
            }
        }
        PanopticPuzzle.Round r = lock.generate(testimony("sealed_a", 5), 2, 7L);
        int[] swapped = r.truth().clone();
        int tmp = swapped[0];
        swapped[0] = swapped[1];
        swapped[1] = tmp;
        PanopticPuzzle.Verdict v = lock.check(r, new PanopticPuzzle.Answer(swapped));
        if (swapped[0] != r.truth()[0]) {
            assertFalse(v.correct());
            assertTrue(v.message().contains("turning"), v.message());
        }
    }

    @Test
    void sealedVigilTiersFollowDepthAndCostsFollowLoudness() {
        assertEquals(0, SealedBPuzzle.tier(0));
        assertEquals(1, SealedBPuzzle.tier(1));
        assertEquals(1, SealedBPuzzle.tier(2));
        assertEquals(2, SealedBPuzzle.tier(3));
        PanopticPuzzle vigil = PanopticPuzzles.forDiscipline("sealed_b");
        PanopticPuzzle.Round r = vigil.generate(testimony("sealed_b", 12), 2, 3L);
        assertEquals(r.slots(), r.choices().size());
        PanopticOffering quiet = vigil.offering(r, new PanopticPuzzle.Answer(new int[r.slots()]));
        int[] loud = new int[r.slots()];
        java.util.Arrays.fill(loud, 2);
        PanopticOffering louder = vigil.offering(r, new PanopticPuzzle.Answer(loud));
        assertTrue(louder.data()[3] > quiet.data()[3]);
        assertFalse(quiet.isFree());
    }
}
