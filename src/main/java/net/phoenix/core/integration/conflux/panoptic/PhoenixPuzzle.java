package net.phoenix.core.integration.conflux.panoptic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class PhoenixPuzzle implements PanopticPuzzle {

    @Override
    public String discipline() {
        return "phoenix";
    }

    @Override
    public Round generate(Testimony testimony, int round, long seed) {
        Random rng = new Random(seed * 31L + round * 7919L);
        int count = Math.min(3 + round, 9);

        List<Testimony.Fact> pool = new ArrayList<>(testimony.facts());
        Collections.shuffle(pool, rng);
        List<String> names = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (i < pool.size()) {
                names.add(pool.get(i).title());
                depths.add(pool.get(i).depth());
            } else {

                names.add("An ember you never lit (" + (i + 1) + ")");
                depths.add(rng.nextInt(4));
            }
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) order.add(i);
        Collections.shuffle(order, rng);
        List<String> choices = new ArrayList<>();
        int[] truth = new int[count];
        for (int i = 0; i < count; i++) {
            choices.add(names.get(order.get(i)));
            truth[i] = depths.get(order.get(i));
        }

        List<String> clues = new ArrayList<>();
        clues.add("Light the embers in the order you earned them.");
        clues.add("The deeper the research, the later it burned.");
        if (round >= 3) clues.add("Embers of equal depth may burn in either order.");
        return new Round(round, "Ledger of Ash " + roman(round + 1), clues, choices, count, 0, truth);
    }

    @Override
    public Verdict check(Round round, Answer answer) {
        int n = round.slots();
        int[] v = answer.values();
        if (v.length != n) return new Verdict(false, 0f, "The ledger wants " + n + " embers.");
        boolean[] seen = new boolean[n];
        for (int x : v) {
            if (x < 0 || x >= n || seen[x]) return new Verdict(false, 0f, "Each ember is lit exactly once.");
            seen[x] = true;
        }
        int good = 0;
        for (int i = 0; i + 1 < n; i++) {
            if (round.truth()[v[i]] <= round.truth()[v[i + 1]]) good++;
        }
        boolean correct = good == n - 1;
        float closeness = n <= 1 ? 1f : good / (float) (n - 1);
        return new Verdict(correct, closeness, correct ? "The ledger balances." : "The ash disagrees.");
    }

    @Override
    public int[] solution(Round round) {
        Integer[] order = new Integer[round.truth().length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (x, y) -> Integer.compare(round.truth()[x], round.truth()[y]));
        int[] out = new int[order.length];
        for (int i = 0; i < out.length; i++) out[i] = order[i];
        return out;
    }

    @Override
    public List<String> describeSolution(Round round, int[] solution) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < solution.length; i++) {
            lines.add(
                    (i + 1) + ". " + round.choices().get(solution[i]) + "  (depth " + round.truth()[solution[i]] + ")");
        }
        return lines;
    }

    static final int LAVA_PER_EMBER = 250;

    @Override
    public PanopticOffering offering(Round round, Answer answer) {
        return PanopticOffering.fluid("minecraft:lava", LAVA_PER_EMBER * answer.values().length);
    }

    @Override
    public String costHint(Round round) {
        return LAVA_PER_EMBER + " mB of lava for every ember you light (" + LAVA_PER_EMBER * round.slots() +
                " mB for this ledger). Feed it into a fluid hatch.";
    }

    static String roman(int n) {
        String[] r = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" };
        return n >= 0 && n < r.length ? r[n] : Integer.toString(n);
    }
}
