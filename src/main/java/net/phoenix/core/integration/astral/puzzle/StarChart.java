package net.phoenix.core.integration.astral.puzzle;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The constellation an Astral machine has to be calibrated to. It is made from the machine's own seed, so every machine
 * has its own chart. A chart is a handful of stars on a small grid and the lines between them:
 *
 * <ul>
 * <li>{@link Kind#CHAIN} (the Spinning Wheel): one line from star to star in a fixed order. Every line has a direction, and
 * the chart is solved when you draw the same lines the same way round.</li>
 * <li>{@link Kind#WEB} (the Loom): a closed loop with some cross-links. Lines have no direction and no order, only the set of
 * them matters.</li>
 * </ul>
 *
 * A line is a pair of star indices. Stars and lines are packed into ints for NBT and packets: a star is {@code x * 16 + y},
 * a line is {@code from * 16 + to}.
 */
public final class StarChart {

    public enum Kind {
        CHAIN,
        WEB
    }

    public static final int GRID = 8;
    public static final int MAX_LINES = 32;

    private final Kind kind;
    private final int[] starX;
    private final int[] starY;
    /** Chain: in drawing order, directed. Web: normalised so the first star index is the lower one. */
    private final List<int[]> lines;

    private StarChart(Kind kind, int[] starX, int[] starY, List<int[]> lines) {
        this.kind = kind;
        this.starX = starX;
        this.starY = starY;
        this.lines = lines;
    }

    public static StarChart generate(long seed, Kind kind) {
        RandomSource random = RandomSource.create(seed ^ (kind == Kind.CHAIN ? 0x5DEECE66DL : 0x2545F4914F6CDD1DL));
        int count = (kind == Kind.CHAIN ? 6 : 7) + random.nextInt(3);

        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < GRID * GRID; i++) cells.add(i);
        Collections.shuffle(cells, new java.util.Random(random.nextLong()));
        int[] x = new int[count];
        int[] y = new int[count];
        for (int i = 0; i < count; i++) {
            x[i] = cells.get(i) / GRID;
            y[i] = cells.get(i) % GRID;
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) order.add(i);
        Collections.shuffle(order, new java.util.Random(random.nextLong()));

        List<int[]> lines = new ArrayList<>();
        if (kind == Kind.CHAIN) {
            for (int i = 0; i + 1 < count; i++) lines.add(new int[] { order.get(i), order.get(i + 1) });
        } else {
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < count; i++) addWeb(lines, seen, order.get(i), order.get((i + 1) % count));
            int extra = Math.max(2, count / 2);
            int guard = 0;
            while (extra > 0 && guard++ < 100) {
                int a = random.nextInt(count);
                int b = random.nextInt(count);
                if (a != b && addWeb(lines, seen, a, b)) extra--;
            }
        }
        return new StarChart(kind, x, y, List.copyOf(lines));
    }

    private static boolean addWeb(List<int[]> lines, Set<Integer> seen, int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        if (!seen.add(lo * 16 + hi)) return false;
        lines.add(new int[] { lo, hi });
        return true;
    }

    public Kind kind() {
        return kind;
    }

    public int starCount() {
        return starX.length;
    }

    public int starX(int i) {
        return starX[i];
    }

    public int starY(int i) {
        return starY[i];
    }

    public int lineCount() {
        return lines.size();
    }

    public int[] line(int i) {
        return lines.get(i);
    }

    public int[] packedStars() {
        int[] out = new int[starX.length];
        for (int i = 0; i < out.length; i++) out[i] = starX[i] * 16 + starY[i];
        return out;
    }

    /** One line, packed as {@code index * 256 + from * 16 + to}; the index is its place in the chain. */
    public int packedLine(int i) {
        int[] l = lines.get(i);
        return i * 256 + l[0] * 16 + l[1];
    }

    private int key(int from, int to) {
        if (kind == Kind.WEB) return Math.min(from, to) * 16 + Math.max(from, to);
        return from * 16 + to;
    }

    /** How many of the drawn lines are right. {@code drawn} holds pairs of star indices. */
    public int correct(List<int[]> drawn) {
        Set<Integer> right = new HashSet<>();
        for (int[] l : lines) right.add(key(l[0], l[1]));
        Set<Integer> counted = new HashSet<>();
        int score = 0;
        for (int[] l : drawn) {
            int k = key(l[0], l[1]);
            if (right.contains(k) && counted.add(k)) score++;
        }
        return score;
    }

    /** Right when every line is drawn and nothing extra is. */
    public boolean solvedBy(List<int[]> drawn) {
        Set<Integer> unique = new HashSet<>();
        for (int[] l : drawn) {
            if (l[0] == l[1] || l[0] < 0 || l[1] < 0 || l[0] >= starCount() || l[1] >= starCount()) return false;
            unique.add(key(l[0], l[1]));
        }
        return unique.size() == lines.size() && correct(drawn) == lines.size();
    }
}
