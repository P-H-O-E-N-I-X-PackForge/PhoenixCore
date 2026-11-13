package net.phoenix.core.client.fx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FxThemes {

    private static final Map<String, FxTheme> THEMES = new LinkedHashMap<>();
    private static final Map<String, FxTheme> BUILT_IN = new LinkedHashMap<>();

    static {

        add(FxTheme.of("miku", "Miku", 0x1FCCC2, 0xCCFFFF, 0xFF73BF, 2f, 0.12f)
                .withExtras(Map.of("strands", 4f, "twist", 6f, "radius", 1.05f)));

        add(FxTheme.of("teto", "Teto", 0xC70A2E, 0xFF6B85, 0xFFBFCC, 3f, 0.09f)
                .withExtras(Map.of("strands", 2f, "twist", 15f, "radius", 1.5f)));

        add(FxTheme.of("pft", "PFT logo", 0x5C14EB, 0xF538F5, 0xFFE6FF, 1f, 0.05f)
                .withExtras(Map.of("strands", 6f, "twist", 5f, "radius", 1.1f, "comet", 1f)));

        add(FxTheme.of("rin", "Rin", 0xFFC21F, 0xFF8A1F, 0xFFF3B0, 2f, 0.1f));
        add(FxTheme.of("luka", "Luka", 0xFF8FB8, 0xFFD27A, 0xFFF0D0, 2f, 0.08f));
        add(FxTheme.of("kaito", "Kaito", 0x2A5CFF, 0xBFD9FF, 0xFFFFFF, 2f, 0.09f));
        add(FxTheme.of("meiko", "Meiko", 0xB5121B, 0x7A3B22, 0xFFC9A0, 2f, 0.08f));
        add(FxTheme.of("gumi", "Gumi", 0x4FD34F, 0xFFA83F, 0xE8FFD0, 2f, 0.1f));

        add(FxTheme.of("ocean", "Ocean", 0x0A3FB5, 0x18D6C8, 0xDFFFFF, 2f, 0.07f));
        add(FxTheme.of("toxic", "Toxic", 0x2BD92B, 0xE0F52A, 0xF4FFC0, 2f, 0.14f));
        add(FxTheme.of("ice", "Ice", 0x9FD8FF, 0xFFFFFF, 0xFFFFFF, 3f, 0.06f));
        add(FxTheme.of("void", "Void", 0x2A0E7A, 0x8A2BE2, 0xD9B8FF, 1f, 0.04f));
        add(FxTheme.of("blood", "Blood", 0x5E0008, 0xE01028, 0xFFB0B8, 2f, 0.11f));
        add(FxTheme.stops("gold", "Gold", 0xFFFFFF, 3f, 0.08f, 0xB8860B, 0xFFD700, 0xFFF1A8));
        add(FxTheme.stops("ember", "Ember", 0xFFF0C0, 2f, 0.16f, 0x8A1100, 0xFF4A00, 0xFFB000));
        add(FxTheme.stops("sunset", "Sunset", 0xFFE0B0, 1f, 0.05f, 0xFF7A1A, 0xFF3D7F, 0x7B2FBE));
        add(FxTheme.stops("aurora", "Aurora", 0xE0FFF4, 1f, 0.06f, 0x1FE08A, 0x18B8D6, 0x8B5CF6));
        add(FxTheme.stops("rainbow", "Rainbow", 0xFFFFFF, 1f, 0.08f, 0xFF2D2D, 0xFF9A1F, 0xFFE81F, 0x2DDB4F, 0x2D8CFF,
                0x7A3DFF));
        add(FxTheme.stops("trans", "Trans", 0xFFFFFF, 1f, 0.06f, 0x5BCEFA, 0xF5A9B8, 0xFFFFFF, 0xF5A9B8));
    }

    private FxThemes() {}

    private static void add(FxTheme theme) {
        BUILT_IN.put(theme.id(), theme);
        THEMES.put(theme.id(), theme);
    }

    public static synchronized void register(FxTheme theme) {
        THEMES.put(theme.id(), theme);
    }

    public static synchronized FxTheme get(String id) {
        return id == null ? null : THEMES.get(id);
    }

    public static synchronized List<String> ids() {
        return new ArrayList<>(THEMES.keySet());
    }

    static synchronized void resetToBuiltIn() {
        THEMES.clear();
        THEMES.putAll(BUILT_IN);
    }
}
