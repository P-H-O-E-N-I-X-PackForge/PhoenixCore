package net.phoenix.core.common.machine.multiblock.stellar;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StellarPlasmaTest {

    private static StellarPlasma.Recipe shipped(String name) throws Exception {
        try (var in = StellarPlasmaTest.class.getResourceAsStream(
                "/data/phoenixcore/phoenixcore/stellar_plasmas/" + name + ".json")) {
            assertNotNull(in, name + " should ship with the mod");
            return StellarPlasma.parse(ResourceLocation.parse("phoenixcore:" + name),
                    JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        }
    }

    @Test
    void everyShippedPlasmaIsValidAndHurtsTheBody() throws Exception {
        for (String name : List.of("helium", "nitrogen", "argon", "iron", "tin", "nickel", "americium")) {
            var r = shipped(name);
            assertTrue(r.rate() > 0.0, name + " gives something");
            assertTrue(r.damageStability() > 0.0f && r.damageMass() > 0.0, name + " costs the body something");
            assertEquals("gtceu:" + name + "_plasma", r.fluid().toString());
        }
    }

    @Test
    void eachPlasmaIsOnlyForItsKindOfBodyHeatAndMass() throws Exception {
        var iron = shipped("iron");
        var s = new StellarForgeState(1L);
        s.stage = StellarForgeState.Stage.STAR;
        s.mass = 2000.0;
        s.heat = 80.0f;
        assertTrue(iron.eligible(s));
        assertNull(iron.whyNot(s));

        s.heat = 40.0f;
        assertFalse(iron.eligible(s), "a cool star does not give iron plasma");
        assertTrue(iron.whyNot(s).contains("heat"));
        s.heat = 80.0f;
        s.mass = 500.0;
        assertTrue(iron.whyNot(s).contains("M"), "a light star is told how much it needs");
        s.mass = 2000.0;
        s.stage = StellarForgeState.Stage.BLACK_HOLE;
        assertFalse(iron.eligible(s), "a black hole gives its own kinds");
        assertTrue(shipped("nickel").body() == StellarForgeState.Stage.BLACK_HOLE);
    }

    @Test
    void thePickerGoesThroughEveryPlasmaAndBackToNone() throws Exception {
        for (String name : List.of("helium", "iron", "nickel")) {
            StellarPlasma.register(shipped(name));
        }
        String id = "";
        int seen = 0;
        do {
            id = StellarPlasma.next(id);
            if (!id.isEmpty()) seen++;
            assertTrue(seen <= 50, "it must come back round");
        } while (!id.isEmpty());
        assertTrue(seen >= 3, "every registered plasma is offered");
    }
}
