package net.phoenix.core.integration.conflux.research;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfluxRecipeEffectsTest {

    @Test
    void readsMultipliersPercentsAndSpeed() {
        var duration = ConfluxRecipeEffects.parse("duration 0.8 gtceu:macerator");
        assertEquals(ConfluxRecipeEffects.Stat.DURATION, duration.stat());
        assertEquals(0.8, duration.multiplier(), 1e-9);
        assertEquals("gtceu:macerator", duration.selector());

        assertEquals(0.8, ConfluxRecipeEffects.parse("duration x0.8").multiplier(), 1e-9);
        assertEquals("*", ConfluxRecipeEffects.parse("duration 0.8").selector());
        assertEquals(0.8, ConfluxRecipeEffects.parse("time -20%").multiplier(), 1e-9);
        assertEquals(1.1, ConfluxRecipeEffects.parse("eut +10% *").multiplier(), 1e-9);
        // speed is the time the other way round
        var speed = ConfluxRecipeEffects.parse("speed 2 *");
        assertEquals(ConfluxRecipeEffects.Stat.DURATION, speed.stat());
        assertEquals(0.5, speed.multiplier(), 1e-9);
    }

    @Test
    void refusesNonsense() {
        assertNull(ConfluxRecipeEffects.parse(""));
        assertNull(ConfluxRecipeEffects.parse("duration"));
        assertNull(ConfluxRecipeEffects.parse("duration fast *"));
        assertNull(ConfluxRecipeEffects.parse("duration 0 *"));
        assertNull(ConfluxRecipeEffects.parse("duration -100% *"));
        assertNull(ConfluxRecipeEffects.parse("colour 0.8 *"));
    }

    @Test
    void describesInWords() {
        assertEquals("Macerator machines run 20% faster",
                ConfluxRecipeEffects.describe("duration 0.8 gtceu:macerator"));
        assertEquals("All machines use 10% less power", ConfluxRecipeEffects.describe("eut 0.9 *"));
        assertEquals("All machines run 25% slower", ConfluxRecipeEffects.describe("duration 1.25 *"));
    }

    @Test
    void selectorsMatchMachinesRecipeTypesAndNamespaces() {
        var byMachine = ConfluxRecipeEffects.parse("duration 0.8 gtceu:macerator");
        assertTrue(byMachine.matches("gtceu:macerator", "gtceu:other"));
        assertTrue(byMachine.matches("phoenixcore:x", "gtceu:macerator"));
        assertFalse(byMachine.matches("gtceu:lathe", "gtceu:lathe"));

        var byNamespace = ConfluxRecipeEffects.parse("duration 0.8 phoenixcore:*");
        assertTrue(byNamespace.matches("phoenixcore:greenhouse", "gtceu:x"));
        assertFalse(byNamespace.matches("gtceu:macerator", "gtceu:macerator"));

        assertTrue(ConfluxRecipeEffects.parse("duration 0.8 *").matches("anything:at_all", "x:y"));
    }
}
