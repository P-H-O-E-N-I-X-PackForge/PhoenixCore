package net.phoenix.core.rebirth.rite;

import net.phoenix.core.rebirth.Capstone;

import java.util.Random;

public abstract class Rite {

    public enum Outcome {
        CONTINUE,
        SUCCESS,
        FAIL
    }

    public record Reply(String message, Outcome outcome, int[] extra) {

        public static Reply go(String message) {
            return new Reply(message, Outcome.CONTINUE, new int[0]);
        }

        public static Reply go(String message, int... extra) {
            return new Reply(message, Outcome.CONTINUE, extra);
        }

        public static Reply win(String message) {
            return new Reply(message, Outcome.SUCCESS, new int[0]);
        }

        public static Reply lose(String message) {
            return new Reply(message, Outcome.FAIL, new int[0]);
        }
    }

    public final Capstone capstone;
    public final long seed;
    protected final Random rng;

    protected Rite(Capstone capstone, long seed) {
        this.capstone = capstone;
        this.seed = seed;
        this.rng = new Random(seed);
    }

    public abstract int[] data();

    public abstract Reply act(int action, int[] args);

    public static Rite create(Capstone capstone, long seed) {
        return switch (capstone) {
            case TESLA -> new TeslaRite(seed);
            case HONEY -> new HoneyRite(seed);
            case PLASMA -> new PlasmaRite(seed);
            case ENGINE -> new EngineRite(seed);
            case CONTINUUM -> new ContinuumRite(seed);
            case NULL_WELL -> new NullWellRite(seed);
            case TOKAMAK -> new TokamakRite(seed);
            case STELLAR_FORGE -> new StellarRite(seed);
            case AETHER_CRUCIBLE -> new CrucibleRite(seed);
            case SCULK_CORTEX -> new CortexRite(seed);
            case PHOENIX_INFUSER -> new InfuserRite(seed);
            case ANTIMATTER_COLLIDER -> new ColliderRite(seed);
            case ASCENSION_REACTOR -> new AscensionRite(seed);
        };
    }
}
