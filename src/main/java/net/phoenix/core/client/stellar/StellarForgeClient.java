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
        public StellarForgeState.Stage stage = StellarForgeState.Stage.IDLE;
        private long last = Util.getMillis();

        void step(StellarForgeMachine machine) {
            long now = Util.getMillis();
            float dt = Math.min(0.25f, (now - last) / 1000.0f);
            last = now;
            float target = StellarForgeState.radius(machine.stage(), machine.mass());
            radius += (target - radius) * Math.min(1.0f, dt * 2.5f);
            heat += (machine.heat() - heat) * Math.min(1.0f, dt * 1.5f);
            if (machine.stage() != stage) {
                stage = machine.stage();
                flash = 1.0f;
            }
            flash = Math.max(0.0f, flash - dt * 0.6f);
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

    public static synchronized void clear() {
        MACHINES.clear();
    }
}
