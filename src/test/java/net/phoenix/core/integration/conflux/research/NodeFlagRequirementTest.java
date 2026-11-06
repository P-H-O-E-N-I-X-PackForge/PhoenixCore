package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeFlagRequirementTest {

    private static final ResourceLocation ROOT = ResourceLocation.fromNamespaceAndPath("phoenixcore", "px_core");

    private static ResearchNode node(String json) {
        return ResearchNode.fromJson(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void aNodeListsTheFlagsItNeeds() {
        ResearchNode n = node("{\"id\":\"phoenixcore:x\",\"requires_flags\":[\"sig:ember:2\",\"other\"]}");
        assertEquals(java.util.List.of("sig:ember:2", "other"), n.requiresFlags);
    }

    @Test
    void aNodeWithNoFlagListNeedsNone() {
        assertTrue(node("{\"id\":\"phoenixcore:x\"}").requiresFlags.isEmpty());
    }

    @Test
    void theNodeStaysClosedUntilEveryFlagIsHeld() {
        ResearchNode n = node("{\"id\":\"phoenixcore:x\",\"requires_flags\":[\"a\",\"b\"]}");
        assertFalse(n.canUnlock(Set.of(), Set.of(), f -> false));
        assertFalse(n.canUnlock(Set.of(), Set.of(), Set.of("a")::contains));
        assertTrue(n.canUnlock(Set.of(), Set.of(), Set.of("a", "b")::contains));
    }

    @Test
    void theFlagsAreInAdditionToThePrerequisites() {
        ResearchNode n = node(
                "{\"id\":\"phoenixcore:x\",\"prerequisites\":[\"phoenixcore:px_core\"],\"requires_flags\":[\"a\"]}");
        assertFalse(n.canUnlock(Set.of(), Set.of(), f -> true));
        assertFalse(n.canUnlock(Set.of(ROOT), Set.of(), f -> false));
        assertTrue(n.canUnlock(Set.of(ROOT), Set.of(), f -> true));
    }

    @Test
    void theEditorWritesAndRemovesTheRequirements() {
        var json = JsonParser.parseString("{\"id\":\"phoenixcore:x\"}").getAsJsonObject();
        ResearchTreeEdits.setRequirements(json, java.util.List.of("sig:ember:2", "a"), " reached_x ");
        ResearchNode n = ResearchNode.fromJson(json);
        assertEquals(java.util.List.of("sig:ember:2", "a"), n.requiresFlags);
        assertEquals("reached_x", n.milestone);

        ResearchTreeEdits.setRequirements(json, java.util.List.of(), "  ");
        assertFalse(json.has("requires_flags"));
        assertFalse(json.has("milestone"));
    }
}
