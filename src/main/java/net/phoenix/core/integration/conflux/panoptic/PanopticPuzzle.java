package net.phoenix.core.integration.conflux.panoptic;

import java.util.List;

public interface PanopticPuzzle {

    String discipline();

    Round generate(Testimony testimony, int round, long seed);

    Verdict check(Round round, Answer answer);

    default int[] solution(Round round) {
        return round.truth().clone();
    }

    default java.util.List<String> describeSolution(Round round, int[] solution) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < solution.length; i++) {
            int v = solution[i];
            lines.add((i + 1) + ". " + (v >= 0 && v < round.choices().size() ? round.choices().get(v) : "?"));
        }
        return lines;
    }

    default PanopticOffering offering(Round round, Answer answer) {
        return PanopticOffering.NONE;
    }

    default String costHint(Round round) {
        return "";
    }

    record Round(int index, String title, List<String> clues, List<String> choices, int slots, int maxValue,
                 int[] truth) {}

    record Answer(int[] values) {}

    record Verdict(boolean correct, float closeness, String message) {}
}
