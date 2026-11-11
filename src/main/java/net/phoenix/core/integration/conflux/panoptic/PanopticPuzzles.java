package net.phoenix.core.integration.conflux.panoptic;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class PanopticPuzzles {

    private PanopticPuzzles() {}

    private static final Map<String, PanopticPuzzle> BY_DISCIPLINE = new HashMap<>();

    static {
        register(new PhoenixPuzzle());
        register(new SculkPuzzle());
        register(new VoidPuzzle());
        register(new SealedAPuzzle());
        register(new SealedBPuzzle());
    }

    public static void register(PanopticPuzzle puzzle) {
        BY_DISCIPLINE.put(puzzle.discipline(), puzzle);
    }

    public static @Nullable PanopticPuzzle forDiscipline(@Nullable String discipline) {
        return discipline == null ? null : BY_DISCIPLINE.get(discipline);
    }
}
