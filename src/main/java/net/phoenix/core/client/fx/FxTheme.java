package net.phoenix.core.client.fx;

import java.util.Map;

public record FxTheme(String id, String name, float[] a, float[] b, float[] accent, float cycles, float speed,
                      Map<String, Float> extras, float[][] stops) {

    public FxTheme {
        extras = extras == null ? Map.of() : Map.copyOf(extras);
    }

    public FxTheme(String id, String name, float[] a, float[] b, float[] accent, float cycles, float speed,
                   Map<String, Float> extras) {
        this(id, name, a, b, accent, cycles, speed, extras, null);
    }

    public static FxTheme stops(String id, String name, int accent, float cycles, float speed, int... colours) {
        if (colours.length < 2) throw new IllegalArgumentException("a theme needs two colours or more");
        float[][] stops = new float[colours.length][];
        for (int i = 0; i < colours.length; i++) stops[i] = rgb(colours[i]);
        return new FxTheme(id, name, stops[0], stops[1], rgb(accent), cycles, speed, Map.of(), stops);
    }

    public static FxTheme of(String id, String name, int a, int b, int accent, float cycles, float speed) {
        return new FxTheme(id, name, rgb(a), rgb(b), rgb(accent), cycles, speed, Map.of());
    }

    public FxTheme withExtras(Map<String, Float> extras) {
        return new FxTheme(id, name, a, b, accent, cycles, speed, extras, stops);
    }

    public float extra(String key, float fallback) {
        Float v = extras.get(key);
        return v == null ? fallback : v;
    }

    public float[] colourAt(float s, float t) {
        if (stops != null && stops.length > 2) {
            float p = s * cycles - t * speed * 2f;
            p = (p - (float) Math.floor(p)) * stops.length;
            int i = Math.min(stops.length - 1, (int) p);
            float f = p - i;
            f = f * f * (3f - 2f * f);
            float[] from = stops[i];
            float[] to = stops[(i + 1) % stops.length];
            return new float[] { from[0] + (to[0] - from[0]) * f, from[1] + (to[1] - from[1]) * f,
                    from[2] + (to[2] - from[2]) * f };
        }
        float g = 0.5f + 0.5f * (float) Math.sin(2.0 * Math.PI * (s * cycles - t * speed * 2f));
        return new float[] { a[0] + (b[0] - a[0]) * g, a[1] + (b[1] - a[1]) * g, a[2] + (b[2] - a[2]) * g };
    }

    public static float[] rgb(int rgb) {
        return new float[] { (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f };
    }

    public static float[] parse(String text) {
        if (text == null) return null;
        String h = text.startsWith("#") ? text.substring(1) : text;
        if (h.length() != 6) return null;
        try {
            return rgb(Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
