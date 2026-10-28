package net.phoenix.core.common.machine.multiblock.stellar;

/**
 * What a Stellar Forge is doing, and the rules it follows: a puzzle of three settings (feed, containment field, spin)
 * that must be kept inside bands that move with the mass gathered and are shifted a little differently in every forge.
 * Hold them and the cloud of gas gathers into a protostar, lights as a star, and, fed something heavy enough, falls in
 * as a black hole. Let them drift and it loses stability, and then mass.
 *
 * <p>
 * This is only numbers, no Minecraft: the machine feeds it what it drew from its hatches and reads back what to show.
 */
public final class StellarForgeState {

    public enum Stage {

        IDLE("Idle"),
        GAS_CLOUD("Gas cloud"),
        PROTOSTAR("Protostar"),
        STAR("Star"),
        BLACK_HOLE("Black hole");

        public final String label;

        Stage(String label) {
            this.label = label;
        }

        public static Stage of(int ordinal) {
            Stage[] all = values();
            return all[Math.max(0, Math.min(ordinal, all.length - 1))];
        }
    }

    /** A band a setting must sit inside, in percent. */
    public record Band(float low, float high) {

        public boolean contains(float value) {
            return value >= low && value <= high;
        }

        /** 0 when inside, rising to 1 as far as {@code reach} outside. */
        public float miss(float value, float reach) {
            if (contains(value)) return 0.0f;
            float distance = value < low ? low - value : value - high;
            return Math.min(1.0f, distance / reach);
        }
    }

    /** Mass (in M) that a stage needs before the next. */
    public static final double CLOUD_TO_PROTOSTAR = 40.0;
    public static final double PROTOSTAR_TO_STAR = 400.0;
    public static final double STAR_TO_HOLE = 4000.0;
    /** Enough mass, fast enough, and the cloud skips straight to a black hole. */
    public static final double DIRECT_COLLAPSE = 3000.0;

    public static final int HOLD_TICKS_TO_ADVANCE = 100;
    public static final int FAIL_TICKS_TO_REGRESS = 200;

    /** Fluid each stage can take in at full feed, in mB a tick. */
    public static final double[] MAX_FEED_PER_TICK = { 0.0, 20.0, 40.0, 80.0, 160.0 };

    public Stage stage = Stage.IDLE;
    public double mass;
    public float heat;
    public float stability;

    /** The player's three settings, 0 to 100. */
    public int feed;
    public int field;
    public int spin;

    public boolean collapseArmed;
    public int holdTicks;
    public int failTicks;

    /** Shifts the bands a little, differently in every forge. */
    public final long seed;

    public StellarForgeState(long seed) {
        this.seed = seed;
    }

    private float shift(int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        return ((h & 0xFFFF) / 65535.0f - 0.5f) * 14.0f;
    }

    /** Where the containment field should sit now. */
    public Band fieldBand() {
        float base = switch (stage) {
            case GAS_CLOUD -> 25.0f + (float) (massFraction() * 20.0);
            case PROTOSTAR -> 45.0f + (float) (massFraction() * 20.0);
            case STAR -> 55.0f + (float) (massFraction() * 20.0);
            case BLACK_HOLE -> 75.0f;
            default -> 0.0f;
        };
        float centre = base + shift(1);
        return new Band(Math.max(0.0f, centre - 12.0f), Math.min(100.0f, centre + 12.0f));
    }

    public Band spinBand() {
        float base = switch (stage) {
            case GAS_CLOUD -> 15.0f + (float) (massFraction() * 20.0);
            case PROTOSTAR -> 35.0f + (float) (massFraction() * 25.0);
            case STAR -> 40.0f - (float) (massFraction() * 15.0);
            case BLACK_HOLE -> 60.0f;
            default -> 0.0f;
        };
        float centre = base + shift(2);
        return new Band(Math.max(0.0f, centre - 14.0f), Math.min(100.0f, centre + 14.0f));
    }

    /** Where the heat should sit: a cloud wants to stay cool, a star to burn. */
    public Band heatBand() {
        return switch (stage) {
            case GAS_CLOUD -> new Band(0.0f, 30.0f);
            case PROTOSTAR -> new Band(30.0f, 70.0f);
            case STAR -> new Band(50.0f, 85.0f);
            case BLACK_HOLE -> new Band(60.0f, 100.0f);
            default -> new Band(0.0f, 100.0f);
        };
    }

    /** Mass now as a share of what this stage needs to move on. */
    public double massFraction() {
        return Math.min(1.0, mass / nextMass());
    }

    public double nextMass() {
        return switch (stage) {
            case IDLE, GAS_CLOUD -> CLOUD_TO_PROTOSTAR;
            case PROTOSTAR -> PROTOSTAR_TO_STAR;
            case STAR -> STAR_TO_HOLE;
            case BLACK_HOLE -> Double.MAX_VALUE;
        };
    }

    /** What the forge draws to run this stage, in EU a tick. */
    public long energyPerTick() {
        if (stage == Stage.IDLE) return 0L;
        long base = 256L + 40L * field + 20L * spin + 12L * feed;
        return base * (1L << Math.max(0, stage.ordinal() - 1));
    }

    public double maxFeedPerTick() {
        return MAX_FEED_PER_TICK[stage.ordinal()];
    }

    /** mB a tick the settings ask for. */
    public double feedPerTick() {
        return maxFeedPerTick() * feed / 100.0;
    }

    /** What a star or black hole gives, in mB of plasma a tick. */
    public double plasmaPerTick() {
        if (stage != Stage.STAR && stage != Stage.BLACK_HOLE) return 0.0;
        double size = Math.max(1.0, Math.min(64.0, mass / 50.0));
        return size * (stability / 100.0) * (stage == Stage.BLACK_HOLE ? 2.0 : 1.0);
    }

    /** The picture's size, in blocks, for the chamber (it has to fit inside a three block room). */
    public float radius() {
        return radius(stage, mass);
    }

    public static float radius(Stage stage, double mass) {
        double m = Math.max(0.0, mass);
        return switch (stage) {
            case IDLE -> 0.0f;
            case GAS_CLOUD -> (float) (0.3 + 0.9 * Math.min(1.0, m / CLOUD_TO_PROTOSTAR));
            case PROTOSTAR -> (float) (0.25 + 0.3 * Math.min(1.0, m / PROTOSTAR_TO_STAR));
            case STAR -> (float) (0.3 + 0.35 * Math.min(1.0, Math.log10(1.0 + m) / Math.log10(1.0 + STAR_TO_HOLE)));
            case BLACK_HOLE -> (float) (0.15 + 0.15 * Math.min(1.0, m / (STAR_TO_HOLE * 4.0)));
        };
    }

    /** One tick. {@code fed} is mass that came in this tick, {@code powered} whether the forge got its energy. */
    public void tick(double fed, boolean powered, boolean outputBlocked) {
        if (stage == Stage.IDLE) {
            if (fed > 0.0) {
                stage = Stage.GAS_CLOUD;
            } else {
                heat = Math.max(0.0f, heat - 0.5f);
                stability = Math.max(0.0f, stability - 1.0f);
                return;
            }
        }
        mass += fed;

        heat += (heatTarget(powered) - heat) * 0.02f;
        stability += (stabilityTarget(powered, outputBlocked) - stability) * 0.05f;

        advance();
    }

    public float effectiveField(boolean powered) {
        return powered ? field : field * 0.3f;
    }

    /** Where the heat settles: the middle of its band, up with feed and spin, down with the field, lower while young. */
    public float heatTarget(boolean powered) {
        var hb = heatBand();
        var fb = fieldBand();
        var sb = spinBand();
        float centre = (hb.low() + hb.high()) / 2.0f * (float) (0.5 + 0.5 * massFraction());
        float target = centre + 0.4f * (feed - 50) + 0.3f * (spin - (sb.low() + sb.high()) / 2.0f) -
                0.4f * (effectiveField(powered) - (fb.low() + fb.high()) / 2.0f);
        return Math.max(0.0f, Math.min(100.0f, target));
    }

    public float missField(boolean powered) {
        return fieldBand().miss(effectiveField(powered), 25.0f);
    }

    public float missSpin() {
        return spinBand().miss(spin, 25.0f);
    }

    public float missHeat() {
        return heatBand().miss(heat, 25.0f);
    }

    /** Where stability is heading with the settings and heat as they are now. */
    public float stabilityTarget(boolean powered, boolean outputBlocked) {
        float target = 100.0f * Math.max(0.0f, 1.0f - (missField(powered) + missSpin() + missHeat()) / 2.0f);
        if (!powered) target = Math.min(target, 30.0f);
        if (outputBlocked) target = Math.min(target, 60.0f);
        return target;
    }

    /** The stability it would settle at (heat included) if the three settings were held at these values. */
    public float equilibriumStability(int atFeed, int atField, int atSpin) {
        StellarForgeState probe = new StellarForgeState(seed);
        probe.stage = stage;
        probe.mass = mass;
        probe.feed = atFeed;
        probe.field = atField;
        probe.spin = atSpin;
        probe.heat = probe.heatTarget(true);
        return probe.stabilityTarget(true, false);
    }

    /** A temperature to show, in kelvin, for the stage and how hot it is running. */
    public double kelvin() {
        return switch (stage) {
            case IDLE -> 3.0;
            case GAS_CLOUD -> 10.0 + heat * 9.0;
            case PROTOSTAR -> 2000.0 + heat * 45.0;
            case STAR -> 3000.0 + heat * 130.0;
            case BLACK_HOLE -> 1.0e6 + heat * 4.0e5;
        };
    }

    private void advance() {
        // enough mass and energy, all at once, and a cloud or protostar falls in as a black hole
        if ((stage == Stage.GAS_CLOUD || stage == Stage.PROTOSTAR) && mass >= DIRECT_COLLAPSE && field >= 85 &&
                feed >= 80) {
            collapse();
            return;
        }

        boolean holding = stability >= 70.0f && mass >= nextMass() && heatBand().miss(heat, 25.0f) < 0.2f;
        holdTicks = holding ? holdTicks + 1 : 0;
        if (holdTicks >= HOLD_TICKS_TO_ADVANCE) {
            holdTicks = 0;
            switch (stage) {
                case GAS_CLOUD -> stage = Stage.PROTOSTAR;
                case PROTOSTAR -> stage = Stage.STAR;
                case STAR -> {
                    if (collapseArmed && field >= 70) collapse();
                }
                default -> {}
            }
        }

        if (stability < 25.0f) {
            failTicks++;
            if (failTicks >= FAIL_TICKS_TO_REGRESS) {
                failTicks = 0;
                regress();
            }
        } else {
            failTicks = 0;
        }
    }

    private void collapse() {
        stage = Stage.BLACK_HOLE;
        collapseArmed = false;
        heat = 80.0f;
        stability = 60.0f;
        holdTicks = 0;
    }

    /** A cloud that cannot hold itself scatters, a star loses its hold and a black hole stays a black hole. */
    private void regress() {
        mass *= 0.6;
        switch (stage) {
            case GAS_CLOUD -> {
                if (mass < 1.0) disperse();
            }
            case PROTOSTAR -> stage = Stage.GAS_CLOUD;
            case STAR -> stage = Stage.PROTOSTAR;
            default -> {}
        }
    }

    public void disperse() {
        stage = Stage.IDLE;
        mass = 0.0;
        heat = 0.0f;
        stability = 0.0f;
        holdTicks = 0;
        failTicks = 0;
        collapseArmed = false;
    }

    /** A colour for the body from how hot it burns: red, orange, yellow, white, blue. */
    public int color() {
        return colorOf(heat);
    }

    public static int colorOf(float heat) {
        float t = Math.max(0.0f, Math.min(1.0f, heat / 100.0f));
        int[] stops = { 0xff5a2a, 0xff9a3a, 0xffd88a, 0xfff4e8, 0x9cc4ff };
        float scaled = t * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) scaled);
        float f = scaled - i;
        int a = stops[i];
        int b = stops[i + 1];
        int r = (int) (((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
        int g = (int) (((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
        int bl = (int) ((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return (r << 16) | (g << 8) | bl;
    }
}
