package net.phoenix.core.integration.conflux.panoptic;

import net.phoenix.core.integration.conflux.ConfluxDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SealedAPuzzle implements PanopticPuzzle {

    static final long DATA_PER_TUMBLER = 500L;

    @Override
    public String discipline() {
        return "sealed_a";
    }

    @Override
    public Round generate(Testimony testimony, int round, long seed) {
        Random rng = new Random(seed * 19L + round * 32452843L);
        ConfluxDataType[] types = ConfluxDataType.values();
        int length = Math.min(8, 4 + (round + 1) / 2);
        ConfluxDataType least = testimony.ranking().get(types.length - 1);

        int total = 0;
        for (ConfluxDataType t : types) total += testimony.affinity()[t.ordinal()] + 1;

        int[] truth = new int[length];
        int[] counts = new int[types.length];
        for (int i = 0; i < length; i++) {
            int pick = -1;
            for (int attempt = 0; attempt < 40 && pick < 0; attempt++) {
                int roll = rng.nextInt(total);
                int candidate = 0;
                for (ConfluxDataType t : types) {
                    roll -= testimony.affinity()[t.ordinal()] + 1;
                    if (roll < 0) {
                        candidate = t.ordinal();
                        break;
                    }
                }
                if (counts[candidate] >= 2) continue;
                if (i == 0 && candidate == least.ordinal()) continue;
                pick = candidate;
            }
            if (pick < 0) {
                for (int t = 0; t < types.length; t++) {
                    if (counts[t] < 2 && !(i == 0 && t == least.ordinal())) {
                        pick = t;
                        break;
                    }
                }
            }
            truth[i] = pick;
            counts[pick]++;
        }

        List<String> clues = new ArrayList<>();
        clues.add("A lock of " + length + " tumblers, each cut from one kind of data.");
        clues.add("No kind is used more than twice.");
        clues.add("It never opens on " + least.displayName + ".");
        clues.add("It tells you how many tumblers are set, and how many turn.");
        List<String> choices = new ArrayList<>();
        for (ConfluxDataType t : types) choices.add(t.displayName);
        return new Round(round, "The First Lock " + PhoenixPuzzle.roman(round + 1), clues, choices, length, 0, truth);
    }

    @Override
    public Verdict check(Round round, Answer answer) {
        int[] truth = round.truth();
        int[] v = answer.values();
        if (v.length != truth.length) {
            return new Verdict(false, 0f, "The lock has " + truth.length + " tumblers.");
        }
        int kinds = round.choices().size();
        int[] inTruth = new int[kinds];
        int[] inAnswer = new int[kinds];
        int set = 0;
        for (int i = 0; i < truth.length; i++) {
            if (v[i] < 0 || v[i] >= kinds) return new Verdict(false, 0f, "That is not a tumbler.");
            inTruth[truth[i]]++;
            inAnswer[v[i]]++;
            if (v[i] == truth[i]) set++;
        }
        int common = 0;
        for (int t = 0; t < kinds; t++) common += Math.min(inTruth[t], inAnswer[t]);
        int turning = common - set;
        boolean correct = set == truth.length;
        return new Verdict(correct, set / (float) truth.length,
                correct ? "The lock gives." : set + " set, " + turning + " turning.");
    }

    @Override
    public PanopticOffering offering(Round round, Answer answer) {
        long[] data = new long[ConfluxDataType.values().length];
        for (int v : answer.values()) {
            if (v >= 0 && v < data.length) data[v] += DATA_PER_TUMBLER;
        }
        return PanopticOffering.data(data);
    }

    @Override
    public String costHint(Round round) {
        return DATA_PER_TUMBLER + " data of a kind for every tumbler of it, spent on every attempt, right or wrong. " +
                "Pipe the data into the Conflux data input hatch.";
    }
}
