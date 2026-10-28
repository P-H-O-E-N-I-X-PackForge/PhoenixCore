package net.phoenix.core.client.stellar;

import net.minecraft.Util;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import java.util.Map;
import java.util.WeakHashMap;

/** The Stellar Forges loaded on this client, and what each one is showing right now (eased, so it grows and flashes). */
public final class StellarForgeClient {

    private StellarForgeClient() {}

    public static final class Anim {

        public float radius;
        public float heat;
        public float flash;

        /** Flares to 1 when energy or a setting is pushed hard, and dies away. */
        public float surge;

        /** 0 while something is changing, rising to 1 once the body has been steady for a few seconds. */
        public float settle;
        private int lastEnergy;
        private int lastSettings = -1;
        private float steadyFor;
        public StellarForgeState.Stage stage = StellarForgeState.Stage.IDLE;

        /** Seconds into the collapse to a black hole, or -1 when there is none going on. */
        public float collapse = -1.0f;
        public StellarForgeState.Stage collapseFrom = StellarForgeState.Stage.STAR;
        public float collapseRadius;
        private boolean collapseFlashed;
        public static final float COLLAPSE_SECONDS = 5.5f;
        public static final float COLLAPSE_FLASH_AT = 0.58f;
        private long last = Util.getMillis();

        /** One reading a second for the last five minutes: mass, heat, stability, energy a tick. */
        public final java.util.ArrayDeque<float[]> history = new java.util.ArrayDeque<>();
        private long lastSample;
        public static final int HISTORY = 300;

        void step(StellarForgeMachine machine) {
            long now = Util.getMillis();
            float dt = Math.min(0.25f, (now - last) / 1000.0f);
            last = now;
            float target = StellarForgeState.radius(machine.stage(), machine.mass());
            radius += (target - radius) * Math.min(1.0f, dt * 2.5f);
            heat += (machine.heat() - heat) * Math.min(1.0f, dt * 1.5f);
            if (machine.stage() != stage) {
                var previous = stage;
                stage = machine.stage();
                boolean fell = stage == StellarForgeState.Stage.BLACK_HOLE &&
                        (previous == StellarForgeState.Stage.STAR || previous == StellarForgeState.Stage.PROTOSTAR ||
                                previous == StellarForgeState.Stage.GAS_CLOUD);
                if (fell) {
                    collapse = 0.0f;
                    collapseFrom = previous;
                    collapseRadius = radius;
                    collapseFlashed = false;
                } else {
                    flash = 1.0f;
                }
            }
            if (collapse >= 0.0f) {
                collapse += dt;
                if (!collapseFlashed && collapse >= COLLAPSE_SECONDS * COLLAPSE_FLASH_AT) {
                    collapseFlashed = true;
                    flash = 1.0f;
                    surge = 1.0f;
                }
                if (collapse >= COLLAPSE_SECONDS) collapse = -1.0f;
            }
            flash = Math.max(0.0f, flash - dt * 0.6f);

            // a big jump in the energy it draws, or a setting moved a long way, is a push: the light flares
            int energy = machine.energyUse();
            int jump = energy - lastEnergy;
            if (jump > 1500 && jump > energy * 0.25f) surge = Math.max(surge, Math.min(1.0f, jump / (energy * 0.6f + 1.0f)));
            lastEnergy = energy;
            int settings = machine.feedSetting() * 10000 + machine.fieldSetting() * 100 + machine.spinSetting();
            if (lastSettings >= 0 && settings != lastSettings) {
                int dFeed = Math.abs(machine.feedSetting() - lastSettings / 10000);
                int dField = Math.abs(machine.fieldSetting() - lastSettings / 100 % 100);
                if (dFeed + dField >= 8) surge = Math.max(surge, 0.8f);
            }
            lastSettings = settings;
            boolean steady = collapse < 0.0f && machine.stage() == stage && Math.abs(radius - target) < 0.02f && surge < 0.05f;
            steadyFor = steady ? steadyFor + dt : 0.0f;
            settle = Math.min(1.0f, Math.max(0.0f, (steadyFor - 2.0f) / 2.5f));
            surge = Math.max(0.0f, surge - dt * 0.4f);

            if (now - lastSample >= 1000L) {
                lastSample = now;
                history.addLast(new float[] { (float) machine.mass(), machine.heat(), machine.stability(),
                        machine.energyUse() });
                while (history.size() > HISTORY) history.removeFirst();
            }
        }
    }

    private static final Map<StellarForgeMachine, Anim> MACHINES = new WeakHashMap<>();

    public static synchronized void add(StellarForgeMachine machine) {
        MACHINES.putIfAbsent(machine, new Anim());
    }

    public static synchronized void remove(StellarForgeMachine machine) {
        MACHINES.remove(machine);
    }

    public static synchronized java.util.List<Map.Entry<StellarForgeMachine, Anim>> all() {
        java.util.List<Map.Entry<StellarForgeMachine, Anim>> list = new java.util.ArrayList<>();
        for (var entry : MACHINES.entrySet()) {
            entry.getValue().step(entry.getKey());
            list.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    public static synchronized Anim animOf(StellarForgeMachine machine) {
        return MACHINES.computeIfAbsent(machine, m -> new Anim());
    }

    public static synchronized void clear() {
        MACHINES.clear();
    }
}
