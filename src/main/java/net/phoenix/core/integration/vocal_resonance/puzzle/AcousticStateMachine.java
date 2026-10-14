package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * The jukebox puzzle as a small state machine. It starts IDLE, moves to LISTENING when a step is played right, and to
 * SOLVED when the whole sequence has been played. A wrong sound sends it to FAILED, and the next sound starts over.
 * It only ever sees the server sound table, so what a client hears cannot change the result.
 */
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

    public void reset() {
        state = State.IDLE;
        step = 0;
    }

    /**
     * Feed it a sound. Returns true when the state or the step changed. A sound missing from the table counts as a wrong
     * sound.
     */
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
