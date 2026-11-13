package net.phoenix.core.client.fx;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FxSystemTest {

    private static FxRender make(String id) {
        return FxRender.builder(id, "Test " + id).param("size", 0.2f, 12f, "how big")
                .param("speed", 0f, 10f, "how fast")
                .build();
    }

    @Test
    void settingsOverrideAndAreKeptInRange() {
        FxRender r = make("t_range");
        assertEquals(0.6f, r.get("size", 0.6f));
        r.set("size", 50f);
        assertEquals(12f, r.get("size", 0.6f));
        r.set("size", 0.01f);
        assertEquals(0.2f, r.get("size", 0.6f));
        r.set("size", null);
        assertEquals(0.6f, r.get("size", 0.6f));
        assertThrows(IllegalArgumentException.class, () -> r.set("nope", 1f));
    }

    @Test
    void resetClearsEverything() {
        FxRender r = make("t_reset");
        r.set("size", 3f);
        r.setTheme(FxThemes.get("miku"));
        r.setColour(new float[] { 1, 0, 0 });
        r.reset();
        assertNull(r.override("size"));
        assertNull(r.theme());
        assertNull(r.colour());
    }

    @Test
    void renderIsReachableByIdAndResetAllReachesIt() {
        FxRender r = make("t_registry");
        assertSame(r, FxRegistry.get("t_registry"));
        r.set("speed", 4f);
        FxRegistry.resetAll();
        assertNull(r.override("speed"));
    }

    @Test
    void builtInThemesAreThereAndMoreCanBeAdded() {
        assertNotNull(FxThemes.get("miku"));
        assertNotNull(FxThemes.get("teto"));
        assertNotNull(FxThemes.get("pft"));
        assertEquals(15f, FxThemes.get("teto").extra("twist", 0f));
        FxThemes.register(FxTheme.of("t_extra", "Extra", 0xFF0000, 0x0000FF, 0xFFFFFF, 1f, 0.1f)
                .withExtras(Map.of("strands", 3f)));
        assertEquals(3f, FxThemes.get("t_extra").extra("strands", 0f));
        assertEquals(9f, FxThemes.get("t_extra").extra("missing", 9f));
        FxThemes.resetToBuiltIn();
        assertNull(FxThemes.get("t_extra"));
        assertNotNull(FxThemes.get("miku"));
    }

    @Test
    void themeGradientStaysBetweenItsColours() {
        FxTheme t = FxThemes.get("pft");
        for (float s = 0; s < 1f; s += 0.1f) {
            float[] c = t.colourAt(s, 3.3f);
            for (int i = 0; i < 3; i++) {
                assertTrue(c[i] >= Math.min(t.a()[i], t.b()[i]) - 1e-4f);
                assertTrue(c[i] <= Math.max(t.a()[i], t.b()[i]) + 1e-4f);
            }
        }
    }

    @Test
    void colourParsing() {
        assertArrayEquals(new float[] { 1f, 0f, 0f }, FxTheme.parse("#ff0000"));
        assertNull(FxTheme.parse("ff00"));
        assertNull(FxTheme.parse("zzzzzz"));
    }

    @Test
    void clockRunsAtTheSpeedSetWithoutJumping() {
        FxClock c = new FxClock();
        c.now(0L);
        float a = c.now(1_000_000_000L / 10);
        c.speed(0f);
        float b = c.now(2_000_000_000L / 10);
        assertEquals(a, b, 1e-6f);
        c.speed(2f);
        float d = c.now(3_000_000_000L / 10);
        assertTrue(d > b);
    }

    @Test
    void multiColourThemesRunThroughAllTheirColoursAndBack() {
        FxTheme rainbow = FxThemes.get("rainbow");
        assertNotNull(rainbow);
        assertEquals(6, rainbow.stops().length);

        for (int i = 0; i < 6; i++) {
            float[] c = rainbow.colourAt(i / 6f, 0f);
            assertArrayEquals(rainbow.stops()[i], c, 1e-3f);
        }

        assertArrayEquals(rainbow.stops()[0], rainbow.colourAt(1f, 0f), 1e-3f);
        assertThrows(IllegalArgumentException.class, () -> FxTheme.stops("bad", "Bad", 0, 1f, 1f, 0xFF0000));
    }

    @Test
    void moreThemesAreThere() {
        for (String id : new String[] { "rin", "luka", "kaito", "meiko", "gumi", "ocean", "toxic", "ice", "void",
                "blood", "gold", "ember", "sunset", "aurora", "rainbow", "trans" }) {
            assertNotNull(FxThemes.get(id), id);
            float[] c = FxThemes.get(id).colourAt(0.37f, 1.2f);
            for (float v : c) assertTrue(v >= 0f && v <= 1f, id);
        }
    }
}
