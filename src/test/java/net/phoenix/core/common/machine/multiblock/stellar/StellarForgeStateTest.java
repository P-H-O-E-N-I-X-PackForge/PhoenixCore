package net.phoenix.core.common.machine.multiblock.stellar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StellarForgeStateTest {

    /** A player who keeps every setting in the middle of its band. */
    private static void play(StellarForgeState s, double massPerTick, int ticks) {
        for (int i = 0; i < ticks; i++) {
            var f = s.fieldBand();
            var p = s.spinBand();
            s.field = Math.round((f.low() + f.high()) / 2.0f);
            s.spin = Math.round((p.low() + p.high()) / 2.0f);
            s.feed = 50;
            s.tick(massPerTick, true, false);
        }
    }

    @Test
    void feedingStartsACloudAndHoldingTheBandsMakesAStar() {
        StellarForgeState s = new StellarForgeState(42L);
        assertEquals(StellarForgeState.Stage.IDLE, s.stage);
        play(s, 0.2, 1);
        assertEquals(StellarForgeState.Stage.GAS_CLOUD, s.stage);
        play(s, 0.2, 4000);
        assertTrue(s.stage.ordinal() >= StellarForgeState.Stage.PROTOSTAR.ordinal(),
                "a well kept cloud becomes a protostar, is " + s.stage + " at " + s.mass);
    }

    @Test
    void neglectedSettingsLoseStabilityThenMass() {
        StellarForgeState s = new StellarForgeState(7L);
        s.field = 0;
        s.spin = 100;
        s.feed = 100;
        s.tick(5.0, true, false);
        double start = s.mass;
        for (int i = 0; i < 1500; i++) s.tick(0.0, true, false);
        assertTrue(s.stability < 30.0f, "stability should fall, is " + s.stability);
        assertTrue(s.mass < start || s.stage == StellarForgeState.Stage.IDLE, "and mass is lost");
    }

    @Test
    void enoughMassAndFieldGoesStraightToABlackHole() {
        StellarForgeState s = new StellarForgeState(1L);
        s.field = 90;
        s.spin = 50;
        s.feed = 100;
        s.tick(StellarForgeState.DIRECT_COLLAPSE + 1.0, true, false);
        assertEquals(StellarForgeState.Stage.BLACK_HOLE, s.stage);
        assertTrue(s.radius() > 0.0f);
    }

    @Test
    void bandsDifferBetweenForgesAndSizesFitTheChamber() {
        var a = new StellarForgeState(1L);
        var b = new StellarForgeState(99999L);
        a.stage = b.stage = StellarForgeState.Stage.PROTOSTAR;
        assertTrue(a.fieldBand().low() != b.fieldBand().low(), "each forge is its own puzzle");
        for (var stage : StellarForgeState.Stage.values()) {
            a.stage = stage;
            a.mass = 1.0e6;
            assertTrue(a.radius() <= 1.25f, stage + " must fit in a three block room: " + a.radius());
        }
    }
}
