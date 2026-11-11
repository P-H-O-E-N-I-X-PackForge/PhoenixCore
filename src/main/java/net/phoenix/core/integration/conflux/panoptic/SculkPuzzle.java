package net.phoenix.core.integration.conflux.panoptic;

import net.phoenix.core.integration.conflux.ConfluxDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SculkPuzzle implements PanopticPuzzle {

    @Override
    public String discipline() {
        return "sculk";
    }

    @Override
    public Round generate(Testimony testimony, int round, long seed) {
        Random rng = new Random(seed * 17L + round * 104729L);
        int length = Math.min(3 + round, 9);
        ConfluxDataType[] types = ConfluxDataType.values();

        int total = 0;
        for (ConfluxDataType t : types) total += testimony.affinity()[t.ordinal()] + 1;

        int[] truth = new int[length];
        for (int i = 0; i < length; i++) {
            int pick = 0;

            for (int attempt = 0; attempt < 8; attempt++) {
                pick = weightedPick(testimony, total, rng);
                if (i < 2 || !(truth[i - 1] == pick && truth[i - 2] == pick)) break;
                pick = (pick + 1) % types.length;
            }
            truth[i] = pick;
        }

        double kept = Math.max(0.0, 0.75 - 0.11 * round);
        StringBuilder echo = new StringBuilder("Echo:");
        for (int i = 0; i < length; i++) {
            boolean shown = rng.nextDouble() < kept;
            echo.append(i == 0 ? " " : " - ").append(shown ? types[truth[i]].displayName : "?");
        }

        List<String> clues = new ArrayList<>();
        clues.add(echo.toString());
        clues.add("What you leaned on most is what the echo remembers best: " + testimony.ranking().get(0).displayName +
                ".");
        if (round >= 2) clues.add("It never repeats a pulse more than twice in a row.");
        List<String> choices = new ArrayList<>();
        for (ConfluxDataType t : types) choices.add(t.displayName);
        return new Round(round, "Echo Recall " + PhoenixPuzzle.roman(round + 1), clues, choices, length, 0, truth);
    }

    static final long DATA_PER_PULSE = 250L;

    @Override
    public PanopticOffering offering(Round round, Answer answer) {
        long[] data = new long[ConfluxDataType.values().length];
        for (int v : answer.values()) {
            if (v >= 0 && v < data.length) data[v] += DATA_PER_PULSE;
        }
        return PanopticOffering.data(data);
    }

    @Override
    public String costHint(Round round) {
        return DATA_PER_PULSE +
                " data of a type for every pulse of it. Pipe the data into the Conflux data input hatch.";
    }

    private static int weightedPick(Testimony testimony, int total, Random rng) {
        int roll = rng.nextInt(total);
        for (ConfluxDataType t : ConfluxDataType.values()) {
            roll -= testimony.affinity()[t.ordinal()] + 1;
            if (roll < 0) return t.ordinal();
        }
        return 0;
    }

    @Override
    public Verdict check(Round round, Answer answer) {
        int[] truth = round.truth();
        int[] v = answer.values();
        if (v.length != truth.length) return new Verdict(false, 0f, "The echo is " + truth.length + " pulses long.");
        int hit = 0;
        for (int i = 0; i < truth.length; i++) if (v[i] == truth[i]) hit++;
        boolean correct = hit == truth.length;
        return new Verdict(correct, hit / (float) truth.length,
                correct ? "The echo settles." : "The echo comes back wrong.");
    }
}
