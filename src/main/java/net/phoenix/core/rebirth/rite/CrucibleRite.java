package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

import java.util.ArrayList;
import java.util.List;

public final class CrucibleRite extends Rite {

    public static final int MERGE = 0;
    public static final int RESTART = 1;

    public static final int GOLD = 5;
    public static final int LENGTH = 9;

    private final List<Integer> start = new ArrayList<>();
    private final List<Integer> row = new ArrayList<>();

    public CrucibleRite(long seed) {
        super(Capstone.AETHER_CRUCIBLE, seed);

        List<Integer> r = new ArrayList<>();
        r.add(GOLD);
        while (r.size() < LENGTH) {
            List<Integer> splittable = new ArrayList<>();
            for (int i = 0; i < r.size(); i++) if (r.get(i) > 0) splittable.add(i);
            int i = splittable.get(rng.nextInt(splittable.size()));
            int v = r.get(i);
            r.set(i, v - 1);
            r.add(i + 1, v - 1);
        }
        start.addAll(r);
        row.addAll(r);
    }

    @Override
    public int[] data() {
        int[] out = new int[2 + row.size()];
        out[0] = GOLD;
        out[1] = row.size();
        for (int i = 0; i < row.size(); i++) out[2 + i] = row.get(i);
        return out;
    }

    private boolean anyMerge() {
        for (int i = 0; i + 1 < row.size(); i++) if (row.get(i).equals(row.get(i + 1))) return true;
        return false;
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action == RESTART) {
            row.clear();
            row.addAll(start);
            return Reply.go("The batch is poured back as it was.");
        }
        if (action != MERGE || args.length < 1) return Reply.go("");
        int i = args[0];
        if (i < 0 || i + 1 >= row.size() || !row.get(i).equals(row.get(i + 1))) return Reply.go("");
        row.set(i, row.get(i) + 1);
        row.remove(i + 1);
        if (row.size() == 1 && row.get(0) == GOLD) return Reply.win("One vessel, all gold. The Crucible is satisfied.");
        if (!anyMerge()) return Reply.go("Nothing left that can merge. Restart the batch and order it differently.");
        return Reply.go("");
    }
}
