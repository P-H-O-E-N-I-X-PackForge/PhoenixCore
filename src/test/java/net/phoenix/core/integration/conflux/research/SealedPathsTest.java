package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SealedPathsTest {

    private static ResearchTree load(String name) throws Exception {
        try (InputStream in = SealedPathsTest.class.getResourceAsStream("/data/phoenixcore/conflux/research/" + name +
                ".json")) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return ResearchTree.fromJson(ResourceLocation.fromNamespaceAndPath("phoenixcore", name), json);
        }
    }

    private static Set<String> milestonesOf(ResearchTree tree) {
        Set<String> out = new HashSet<>();
        tree.getNodes().forEach(n -> {
            if (n.milestone != null) out.add(n.milestone);
        });
        return out;
    }

    @Test
    void theSealedPathsAreClosedUntilTheirMilestoneIsReached() throws Exception {
        ResearchTree a = load("discipline_sealed_a");
        ResearchTree b = load("discipline_sealed_b");
        assertFalse(a.isOpenTo(m -> false));
        assertFalse(b.isOpenTo(m -> false));
        assertTrue(a.isOpenTo("phoenix_committed"::equals));
        assertTrue(a.isOpenTo("sculk_committed"::equals));
        assertFalse(a.isOpenTo("void_apex"::equals));
        assertTrue(b.isOpenTo("void_apex"::equals));
        assertFalse(b.isOpenTo("phoenix_committed"::equals));
    }

    @Test
    void theOrdinaryPathsAreAlwaysOpen() throws Exception {
        for (String name : new String[] { "discipline_phoenix", "discipline_sculk", "discipline_void" }) {
            assertTrue(load(name).isOpenTo(m -> false), name);
        }
    }

    @Test
    void everyMilestoneASealedPathAsksForIsGivenBySomeNode() throws Exception {
        Set<String> given = new HashSet<>();
        for (String name : new String[] { "discipline_phoenix", "discipline_sculk", "discipline_void" }) {
            given.addAll(milestonesOf(load(name)));
        }
        for (String sealed : new String[] { "discipline_sealed_a", "discipline_sealed_b" }) {
            for (String needed : load(sealed).requiresMilestones) {
                assertTrue(given.contains(needed), sealed + " asks for " + needed + " but no node gives it");
            }
        }
        assertEquals(Set.of("phoenix_committed", "sculk_committed", "void_apex"), given);
    }

    @Test
    void eachPathHasItsOwnSwitchPrice() throws Exception {
        Set<String> prices = new HashSet<>();
        for (String name : new String[] { "discipline_phoenix", "discipline_sculk", "discipline_void",
                "discipline_sealed_a", "discipline_sealed_b" }) {
            prices.add(load(name).switchCost.toString());
        }
        assertEquals(5, prices.size(), "two paths share the same switch price");
    }
}
