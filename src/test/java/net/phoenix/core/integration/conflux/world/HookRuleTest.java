package net.phoenix.core.integration.conflux.world;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HookRuleTest {

    private static HookRule rule(String json) {
        return HookRule.fromJson("test:rule", JsonParser.parseString(json).getAsJsonObject());
    }

    private static HookRule.Context ctx(String type, String discipline, double y, Map<String, Object> data) {
        return new HookRule.Context(type, "phoenixcore:conflux/" + discipline, discipline, y, data);
    }

    @Test
    void parsesEverything() {
        HookRule r = rule("""
                { "on": ["region_entered", "familiarity_up"],
                  "conditions": { "discipline": "sculk", "min_level": "3" },
                  "once": "team", "cooldown_ticks": 40,
                  "actions": [ { "type": "flag", "value": "f" },
                               { "type": "command", "value": "say hi", "as": "server" },
                               { "type": "chronicles", "value": "t", "data": { "k": "v" } } ] }
                """);
        assertTrue(r.enabled());
        assertEquals(HookRule.Once.TEAM, r.once());
        assertEquals(40, r.cooldownTicks());
        assertEquals(3, r.actions().size());
        assertEquals("server", r.actions().get(1).as());
        assertEquals("v", r.actions().get(2).data().get("k"));
        assertTrue(r.listensTo("familiarity_up"));
        assertFalse(r.listensTo("void_edge"));
    }

    @Test
    void aSingleOnAndTheWildcardWork() {
        assertTrue(rule("{ \"on\": \"void_edge\" }").listensTo("void_edge"));
        assertTrue(rule("{ \"on\": \"*\" }").listensTo("anything"));
    }

    @Test
    void conditionsLookAtThePlaceAndTheEventData() {
        HookRule r = rule("""
                { "on": "familiarity_up",
                  "conditions": { "discipline": "sculk", "min_level": "3", "max_level": "4", "region": "sculk_marsh",
                                  "y_min": "40", "y_max": "120" } }
                """);
        Map<String, Object> ok = Map.of("level", 3, "region", "sculk_marsh");
        assertTrue(r.matches(ctx("familiarity_up", "sculk", 60, ok)));
        assertFalse(r.matches(ctx("familiarity_up", "void", 60, ok)));
        assertFalse(r.matches(ctx("familiarity_up", "sculk", 30, ok)));
        assertFalse(r.matches(ctx("familiarity_up", "sculk", 130, ok)));
        assertFalse(r.matches(ctx("familiarity_up", "sculk", 60, Map.of("level", 2, "region", "sculk_marsh"))));
        assertFalse(r.matches(ctx("familiarity_up", "sculk", 60, Map.of("level", 5, "region", "sculk_marsh"))));
        assertFalse(r.matches(ctx("familiarity_up", "sculk", 60, Map.of("level", 3, "region", "sculk_forest"))));
        assertFalse(r.matches(ctx("familiarity_up", "sculk", 60, Map.of("level", 3))));
    }

    @Test
    void disabledAndDefaults() {
        assertFalse(rule("{ \"enabled\": false, \"on\": \"x\" }").enabled());
        HookRule r = rule("{ \"on\": \"x\" }");
        assertTrue(r.enabled());
        assertEquals(HookRule.Once.NEVER, r.once());
        assertTrue(r.matches(ctx("x", "phoenix", 0, Map.of())));
    }
}
