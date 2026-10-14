package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public final class AcousticStateMachine {

    public enum State {
        IDLE,
        LISTENING,
        SOLVED,
        FAILED
    }

    private State state = State.IDLE;
    private int step = 0;

    public State state() {
        return state;
    }

    public int step() {
        return step;
    }

    public void load(int stateOrdinal, int step) {
        State[] values = State.values();
        this.state = stateOrdinal >= 0 && stateOrdinal < values.length ? values[stateOrdinal] : State.IDLE;
        this.step = Math.max(0, step);
    }

    public boolean wouldAccept(@Nullable AcousticPuzzle puzzle, @Nullable ResourceLocation sound) {
        if (puzzle == null || puzzle.steps().isEmpty() || sound == null) return false;
        int at = state == State.SOLVED || state == State.FAILED ? 0 : step;
        AcousticSoundEntry entry = AcousticData.sound(sound);
        return at < puzzle.steps().size() && entry != null && puzzle.steps().get(at).matches(entry);
    }

    public void reset() {
        state = State.IDLE;
        step = 0;
    }

    public boolean onSound(@Nullable AcousticPuzzle puzzle, @Nullable ResourceLocation sound) {
        if (puzzle == null || puzzle.steps().isEmpty() || sound == null) return false;
        if (state == State.SOLVED || state == State.FAILED) {
            reset();
        }
        AcousticSoundEntry entry = AcousticData.sound(sound);
        if (step < puzzle.steps().size() && entry != null && puzzle.steps().get(step).matches(entry)) {
            step++;
            state = step >= puzzle.steps().size() ? State.SOLVED : State.LISTENING;
        } else {
            state = State.FAILED;
            step = 0;
        }
        return true;
    }
}
