package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

public final class NullWellRite extends Rite {

    public static final int ACT = 0;
    public static final int TURNS = 12;
    public static final int MID = 50;
    public static final int STEP = 3;

    public static final int CHOICES = 7;
    private static final int PULL = 9;

    private final int[] pull = new int[TURNS + 2];
    private int mass = MID;
    private int turn;
    private int lastPull;
    private int lastMove;

    public NullWellRite(long seed) {
        super(Capstone.NULL_WELL, seed);
        for (int i = 0; i < pull.length; i++) pull[i] = rng.nextInt(2 * PULL + 1) - PULL;
    }

    public static int band(int t) {
        return Math.max(4, 14 - t);
    }

    @Override
    public int[] data() {
        int next = Math.min(turn, TURNS - 1);
        int a = turn < TURNS ? pull[turn] : 0;
        int b = turn + 1 < TURNS ? pull[turn + 1] : 0;
        return new int[] { mass, turn, TURNS, band(next), a, b, lastPull, lastMove };
    }

    @Override
    public Reply act(int action, int[] args) {
        if (action != ACT || args.length < 1) return Reply.go("");
        int choice = args[0];
        if (choice < 0 || choice >= CHOICES) return Reply.go("");
        int move = (choice - 3) * STEP;
        int p = pull[turn];
        mass += move + p;
        lastMove = move;
        lastPull = p;
        int allowed = band(turn);
        turn++;
        if (Math.abs(mass - MID) > allowed) {
            return Reply.lose("The hole swells past the field. It slips, and the Well lets go.");
        }
        if (turn >= TURNS) return Reply.win("The hole settles. The Well is held.");
        return Reply.go("");
    }
}
