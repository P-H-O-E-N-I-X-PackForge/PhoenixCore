package net.phoenix.core.integration.conflux.panoptic;

import net.phoenix.core.integration.conflux.ConfluxDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class VoidPuzzle implements PanopticPuzzle {

    @Override
    public String discipline() {
        return "void";
    }

    @Override
    public Round generate(Testimony testimony, int round, long seed) {
        Random rng = new Random(seed * 13L + round * 15485863L);
        ConfluxDataType[] types = ConfluxDataType.values();
        int n = types.length;

        int scale = 4 + 2 * round;
        int[] truth = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) {
            truth[i] = Math.max(0, (testimony.affinity()[i] + 1) * scale / 3 + rng.nextInt(scale + 1));
            total += truth[i];
        }

        List<String> clues = new ArrayList<>();
        for (int attempt = 0; attempt < 24; attempt++) {
            clues.clear();

            int sign0 = 1;
            int[] coef = new int[n];
            coef[0] = 1;
            boolean sumTies = round >= 2 && attempt < 20;
            for (int i = 1; i < n; i++) {
                int j = rng.nextInt(i);
                boolean sum = sumTies && rng.nextBoolean();
                if (sum) {
                    coef[i] = -coef[j];
                    clues.add(types[i].displayName + " and " + types[j].displayName + " together make " +
                            (truth[i] + truth[j]) + ".");
                } else {
                    coef[i] = coef[j];
                    clues.add(compare(types[i], truth[i], types[j], truth[j]));
                }
            }
            int sumCoef = 0;
            for (int c : coef) sumCoef += c;
            if (sumCoef * sign0 != 0) break;
        }
        clues.add("Between them the reserves hold " + total + ".");

        List<String> choices = new ArrayList<>();
        for (ConfluxDataType t : types) choices.add(t.displayName);
        return new Round(round, "Null Ledger " + PhoenixPuzzle.roman(round + 1), clues, choices, n, total, truth);
    }

    @Override
    public java.util.List<String> describeSolution(Round round, int[] solution) {
        java.util.List<String> lines = new ArrayList<>();
        for (int i = 0; i < solution.length; i++) lines.add(round.choices().get(i) + ": " + solution[i]);
        return lines;
    }

    static final long DATA_PER_UNIT = 40L;

    @Override
    public PanopticOffering offering(Round round, Answer answer) {
        long[] data = new long[ConfluxDataType.values().length];
        int[] v = answer.values();
        for (int i = 0; i < data.length && i < v.length; i++) data[i] = Math.max(0, v[i]) * DATA_PER_UNIT;
        return PanopticOffering.data(data);
    }

    @Override
    public String costHint(Round round) {
        return DATA_PER_UNIT + " data of a type for every unit in its reserve. The reserves are drawn from the " +
                "Conflux data input hatch.";
    }

    private static String compare(ConfluxDataType a, int av, ConfluxDataType b, int bv) {
        if (av == bv) return a.displayName + " equals " + b.displayName + ".";
        if (av > bv) return a.displayName + " exceeds " + b.displayName + " by " + (av - bv) + ".";
        return b.displayName + " exceeds " + a.displayName + " by " + (bv - av) + ".";
    }

    @Override
    public Verdict check(Round round, Answer answer) {
        int[] truth = round.truth();
        int[] v = answer.values();
        if (v.length != truth.length) return new Verdict(false, 0f, "Five reserves, five amounts.");
        int off = 0;
        int total = 0;
        for (int i = 0; i < truth.length; i++) {
            off += Math.abs(v[i] - truth[i]);
            total += truth[i];
        }
        boolean correct = off == 0;
        float closeness = total <= 0 ? (correct ? 1f : 0f) : Math.max(0f, 1f - off / (2f * total));
        return new Verdict(correct, closeness, correct ? "The ledger closes." : "The reserves do not agree.");
    }
}
