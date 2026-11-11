package net.phoenix.core.integration.conflux.panoptic;

import net.phoenix.core.integration.conflux.ConfluxDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SealedBPuzzle implements PanopticPuzzle {

    static final long ENERGETIC_PER_BEAT = 100L;
    static final long COMPUTATIONAL_PER_STEP = 120L;

    @Override
    public String discipline() {
        return "sealed_b";
    }

    static int tier(int depth) {
        return depth <= 0 ? 0 : depth <= 2 ? 1 : 2;
    }

    @Override
    public Round generate(Testimony testimony, int round, long seed) {
        Random rng = new Random(seed * 23L + round * 49979687L);
        int count = Math.min(10, 4 + round);

        List<Testimony.Fact> pool = new ArrayList<>(testimony.facts());
        Collections.shuffle(pool, rng);
        List<String> names = new ArrayList<>();
        int[] truth = new int[count];
        for (int i = 0; i < count; i++) {
            if (i < pool.size()) {
                names.add(pool.get(i).title());
                truth[i] = tier(pool.get(i).depth());
            } else {

                names.add("A beat you never rang (" + (i + 1) + ")");
                truth[i] = tier(rng.nextInt(5));
            }
        }

        List<String> clues = new ArrayList<>();
        clues.add("Each beat is a piece of your own research. Say how loud it rings.");
        clues.add("Silent: you began here. Soft: one or two steps in. Loud: three steps or more.");
        clues.add("A wrong answer says only how many beats rang true.");
        return new Round(round, "The Vigil " + PhoenixPuzzle.roman(round + 1), clues, names, count, 2, truth);
    }

    @Override
    public List<String> describeSolution(Round round, int[] solution) {
        String[] levels = { "silent", "soft", "loud" };
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < solution.length; i++) {
            lines.add(round.choices().get(i) + ": " + levels[Math.max(0, Math.min(2, solution[i]))]);
        }
        return lines;
    }

    @Override
    public Verdict check(Round round, Answer answer) {
        int[] truth = round.truth();
        int[] v = answer.values();
        if (v.length != truth.length) return new Verdict(false, 0f, "The vigil is " + truth.length + " beats.");
        int true_ = 0;
        for (int i = 0; i < truth.length; i++) {
            if (v[i] < 0 || v[i] > 2) return new Verdict(false, 0f, "A beat is silent, soft or loud.");
            if (v[i] == truth[i]) true_++;
        }
        boolean correct = true_ == truth.length;
        return new Verdict(correct, true_ / (float) truth.length,
                correct ? "The vigil holds." : true_ + " of " + truth.length + " beats rang true.");
    }

    @Override
    public PanopticOffering offering(Round round, Answer answer) {
        long[] data = new long[ConfluxDataType.values().length];
        int[] v = answer.values();
        data[ConfluxDataType.ENERGETIC.ordinal()] = ENERGETIC_PER_BEAT * v.length;
        long steps = 0;
        for (int x : v) steps += Math.max(0, Math.min(2, x));
        data[ConfluxDataType.COMPUTATIONAL.ordinal()] = COMPUTATIONAL_PER_STEP * steps;
        return PanopticOffering.data(data);
    }

    @Override
    public String costHint(Round round) {
        return ENERGETIC_PER_BEAT + " Energetic for every beat, and " + COMPUTATIONAL_PER_STEP +
                " Computational for every step of loudness. Pipe it into the Conflux data input hatch.";
    }
}
