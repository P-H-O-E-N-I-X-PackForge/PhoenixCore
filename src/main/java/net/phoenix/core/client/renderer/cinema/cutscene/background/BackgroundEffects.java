package net.phoenix.core.client.renderer.cinema.cutscene.background;

import net.phoenix.core.client.renderer.cinema.cutscene.background.BackgroundEffect.Point;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Built-in {@link BackgroundEffect}s, usable both from Java (the static factories) and from JSON
 * (the {@code "type"} of a layer). Speeds are in cycles per second, colours are ARGB
 * ({@code "#RRGGBB"}, {@code "#AARRGGBB"} or an int in JSON).
 *
 * <p>
 * Register extra JSON effect types with {@link #register(String, MapCodec)}.
 */
public final class BackgroundEffects {

    private BackgroundEffects() {}

    private static final float TWO_PI = (float) (Math.PI * 2);

    private static final Map<String, MapCodec<? extends BackgroundEffect.Data>> TYPES = new LinkedHashMap<>();

    public static final Codec<Integer> COLOR = Codec.either(Codec.INT, Codec.STRING).comapFlatMap(
            either -> either.map(DataResult::success, BackgroundEffects::parseColor),
            color -> Either.right(String.format("#%08X", color)));

    /** Inline form: the effect's fields sit next to {@code "type"} (and the layer's opacity/blend). */
    public static final MapCodec<BackgroundEffect.Data> MAP_CODEC = ExtraCodecs.validate(Codec.STRING,
            type -> TYPES.containsKey(type) ? DataResult.success(type) :
                    DataResult.error(() -> "Unknown background effect '" + type + "', known: " + TYPES.keySet()))
            .dispatchMap("type", BackgroundEffect.Data::type, type -> TYPES.get(type).codec());

    public static void register(String type, MapCodec<? extends BackgroundEffect.Data> codec) {
        TYPES.put(type, codec);
    }

    public static Set<String> types() {
        return TYPES.keySet();
    }

    static {
        register("solid", Solid.CODEC);
        register("hue_cycle", HueCycle.CODEC);
        register("linear_gradient", LinearGradient.CODEC);
        register("radial_gradient", RadialGradient.CODEC);
        register("color_cycle", ColorCycle.CODEC);
        register("ring", Ring.CODEC);
        register("pulse", Pulse.CODEC);
        register("rays", Rays.CODEC);
        register("sparkle", Sparkle.CODEC);
        register("noise", Noise.CODEC);
        register("glitch_shear", GlitchShear.CODEC);
    }

    // ---------------------------------------------------------------- factories

    public static Solid solid(int argb) {
        return new Solid(argb, 0);
    }

    public static Solid solid(int argb, float pulseSpeed) {
        return new Solid(argb, pulseSpeed);
    }

    /** Rotates through every hue. {@code spread} is how much of the colour wheel is visible across the screen. */
    public static HueCycle hueCycle(float speed, float saturation, float brightness, float spread) {
        return new HueCycle(speed, saturation, brightness, spread, 90);
    }

    public static LinearGradient linearGradient(int from, int to, float angleDegrees) {
        return new LinearGradient(from, to, angleDegrees);
    }

    public static RadialGradient radialGradient(int inner, int outer) {
        return new RadialGradient(inner, outer, 1, 0);
    }

    public static RadialGradient radialGradient(int inner, int outer, float radius, float pulseSpeed) {
        return new RadialGradient(inner, outer, radius, pulseSpeed);
    }

    /** Dark edges fading to transparent in the middle. */
    public static RadialGradient vignette(float strength) {
        return new RadialGradient(0x00000000, (Math.round(clamp01(strength) * 255) << 24), 1.6f, 0);
    }

    public static ColorCycle colorCycle(float speed, int... colors) {
        return new ColorCycle(java.util.Arrays.stream(colors).boxed().toList(), speed);
    }

    public static Ring ring(int color, float radius, float thickness, float rotationSpeed) {
        return new Ring(color, radius, thickness, rotationSpeed);
    }

    public static Pulse pulse(int inner, int outer, float speed) {
        return new Pulse(inner, outer, speed);
    }

    public static Rays rays(int color, int count, float speed, float sharpness) {
        return new Rays(color, count, speed, sharpness, 1.5f);
    }

    public static Sparkle sparkle(int color, float density, float speed) {
        return new Sparkle(color, density, speed);
    }

    public static Noise noise(int color, float scale, float speed) {
        return new Noise(color, scale, speed, 1);
    }

    public static GlitchShear glitchShear(int colorA, int colorB, float speed, float intensity) {
        return new GlitchShear(colorA, colorB, speed, intensity);
    }

    // ---------------------------------------------------------------- effects

    public record Solid(int color, float pulseSpeed) implements BackgroundEffect.Data {

        public static final MapCodec<Solid> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("color").forGetter(Solid::color),
                Codec.FLOAT.optionalFieldOf("pulse_speed", 0f).forGetter(Solid::pulseSpeed))
                .apply(i, Solid::new));

        @Override
        public int colorAt(Point p, float time) {
            if (pulseSpeed == 0) return color;
            return withAlphaScale(color, 0.6f + 0.4f * wave(time * pulseSpeed));
        }

        @Override
        public String type() {
            return "solid";
        }
    }

    public record HueCycle(float speed, float saturation, float brightness, float spread, float angle)
            implements BackgroundEffect.Data {

        public static final MapCodec<HueCycle> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("speed", 0.03f).forGetter(HueCycle::speed),
                Codec.FLOAT.optionalFieldOf("saturation", 0.6f).forGetter(HueCycle::saturation),
                Codec.FLOAT.optionalFieldOf("brightness", 0.3f).forGetter(HueCycle::brightness),
                Codec.FLOAT.optionalFieldOf("spread", 0.12f).forGetter(HueCycle::spread),
                Codec.FLOAT.optionalFieldOf("angle", 90f).forGetter(HueCycle::angle))
                .apply(i, HueCycle::new));

        @Override
        public int colorAt(Point p, float time) {
            float hue = frac(time * speed + along(p, angle) * spread);
            return 0xFF000000 | (Mth.hsvToRgb(hue, saturation, brightness) & 0xFFFFFF);
        }

        @Override
        public String type() {
            return "hue_cycle";
        }
    }

    /** {@code angle} in degrees: 0 = left to right, 90 = top to bottom. */
    public record LinearGradient(int from, int to, float angle) implements BackgroundEffect.Data {

        public static final MapCodec<LinearGradient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("from").forGetter(LinearGradient::from),
                COLOR.fieldOf("to").forGetter(LinearGradient::to),
                Codec.FLOAT.optionalFieldOf("angle", 90f).forGetter(LinearGradient::angle))
                .apply(i, LinearGradient::new));

        @Override
        public int colorAt(Point p, float time) {
            return mix(from, to, along(p, angle));
        }

        @Override
        public String type() {
            return "linear_gradient";
        }
    }

    public record RadialGradient(int inner, int outer, float radius, float pulseSpeed)
            implements BackgroundEffect.Data {

        public static final MapCodec<RadialGradient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("inner").forGetter(RadialGradient::inner),
                COLOR.fieldOf("outer").forGetter(RadialGradient::outer),
                Codec.FLOAT.optionalFieldOf("radius", 1f).forGetter(RadialGradient::radius),
                Codec.FLOAT.optionalFieldOf("pulse_speed", 0f).forGetter(RadialGradient::pulseSpeed))
                .apply(i, RadialGradient::new));

        @Override
        public int colorAt(Point p, float time) {
            float scale = pulseSpeed == 0 ? 1 : 0.8f + 0.2f * wave(time * pulseSpeed);
            return mix(inner, outer, clamp01(p.dist / (radius * scale)));
        }

        @Override
        public String type() {
            return "radial_gradient";
        }
    }

    public record ColorCycle(List<Integer> colors, float speed) implements BackgroundEffect.Data {

        public static final MapCodec<ColorCycle> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.listOf().fieldOf("colors").forGetter(ColorCycle::colors),
                Codec.FLOAT.optionalFieldOf("speed", 0.1f).forGetter(ColorCycle::speed))
                .apply(i, ColorCycle::new));

        @Override
        public int colorAt(Point p, float time) {
            if (colors.isEmpty()) return 0;
            if (colors.size() == 1) return colors.get(0);
            float scaled = frac(time * speed) * colors.size();
            int a = (int) scaled % colors.size();
            return mix(colors.get(a), colors.get((a + 1) % colors.size()), scaled - (float) Math.floor(scaled));
        }

        @Override
        public String type() {
            return "color_cycle";
        }
    }

    public record Ring(int color, float radius, float thickness, float rotationSpeed)
            implements BackgroundEffect.Data {

        public static final MapCodec<Ring> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("color").forGetter(Ring::color),
                Codec.FLOAT.optionalFieldOf("radius", 0.6f).forGetter(Ring::radius),
                Codec.FLOAT.optionalFieldOf("thickness", 0.1f).forGetter(Ring::thickness),
                Codec.FLOAT.optionalFieldOf("rotation_speed", 0f).forGetter(Ring::rotationSpeed))
                .apply(i, Ring::new));

        @Override
        public int colorAt(Point p, float time) {
            float half = thickness / 2f;
            float a = 1f - smoothstep(half * 0.5f, half, Math.abs(p.dist - radius));
            if (rotationSpeed != 0) {
                a *= 0.35f + 0.65f * (0.5f + 0.5f * Mth.cos(p.angle - time * rotationSpeed * TWO_PI));
            }
            return withAlphaScale(color, a);
        }

        @Override
        public String type() {
            return "ring";
        }
    }

    public record Pulse(int inner, int outer, float speed) implements BackgroundEffect.Data {

        public static final MapCodec<Pulse> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("inner").forGetter(Pulse::inner),
                COLOR.fieldOf("outer").forGetter(Pulse::outer),
                Codec.FLOAT.optionalFieldOf("speed", 0.5f).forGetter(Pulse::speed))
                .apply(i, Pulse::new));

        @Override
        public int colorAt(Point p, float time) {
            float radius = 0.35f + wave(time * speed) * 0.08f;
            float core = 1f - smoothstep(0f, radius, p.dist);
            return withAlphaScale(mix(outer, inner, core), core);
        }

        @Override
        public String type() {
            return "pulse";
        }
    }

    public record Rays(int color, int count, float speed, float sharpness, float reach)
            implements BackgroundEffect.Data {

        public static final MapCodec<Rays> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("color").forGetter(Rays::color),
                Codec.INT.optionalFieldOf("count", 8).forGetter(Rays::count),
                Codec.FLOAT.optionalFieldOf("speed", 0.05f).forGetter(Rays::speed),
                Codec.FLOAT.optionalFieldOf("sharpness", 4f).forGetter(Rays::sharpness),
                Codec.FLOAT.optionalFieldOf("reach", 1.5f).forGetter(Rays::reach))
                .apply(i, Rays::new));

        @Override
        public int colorAt(Point p, float time) {
            float raw = Mth.cos((p.angle + time * speed * TWO_PI) * count);
            float rays = (float) Math.pow(Math.max(0f, raw), sharpness);
            return withAlphaScale(color, rays * smoothstep(reach, 0.15f, p.dist));
        }

        @Override
        public String type() {
            return "rays";
        }
    }

    public record Sparkle(int color, float density, float speed) implements BackgroundEffect.Data {

        public static final MapCodec<Sparkle> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Sparkle::color),
                Codec.FLOAT.optionalFieldOf("density", 0.5f).forGetter(Sparkle::density),
                Codec.FLOAT.optionalFieldOf("speed", 2f).forGetter(Sparkle::speed))
                .apply(i, Sparkle::new));

        @Override
        public int colorAt(Point p, float time) {
            float cells = Math.max(2f, 24f * clamp01(density));
            float step = (float) Math.floor(time * speed);
            float cx = (float) Math.floor(p.nx * 0.5f * cells);
            float cy = (float) Math.floor(p.ny * 0.5f * cells);
            float h = hash(cx * 13.1f + cy * 7.7f + step * 31.7f);
            float threshold = 1f - 0.35f * density;
            return withAlphaScale(color, h > threshold ? (h - threshold) / (1f - threshold) : 0f);
        }

        @Override
        public String type() {
            return "sparkle";
        }
    }

    /** Drifting fog: the colour's alpha is scaled by animated value noise. */
    public record Noise(int color, float scale, float speed, float contrast) implements BackgroundEffect.Data {

        public static final MapCodec<Noise> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("color").forGetter(Noise::color),
                Codec.FLOAT.optionalFieldOf("scale", 2f).forGetter(Noise::scale),
                Codec.FLOAT.optionalFieldOf("speed", 0.1f).forGetter(Noise::speed),
                Codec.FLOAT.optionalFieldOf("contrast", 1f).forGetter(Noise::contrast))
                .apply(i, Noise::new));

        @Override
        public int colorAt(Point p, float time) {
            float x = p.nx * scale + time * speed, y = p.ny * scale - time * speed * 0.7f;
            float n = 0, amplitude = 0.5f;
            for (int octave = 0; octave < 3; octave++) {
                n += valueNoise(x, y) * amplitude;
                x *= 2;
                y *= 2;
                amplitude *= 0.5f;
            }
            return withAlphaScale(color, clamp01((n / 0.875f - 0.5f) * contrast + 0.5f));
        }

        @Override
        public String type() {
            return "noise";
        }
    }

    public record GlitchShear(int colorA, int colorB, float speed, float intensity) implements BackgroundEffect.Data {

        public static final MapCodec<GlitchShear> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                COLOR.fieldOf("color_a").forGetter(GlitchShear::colorA),
                COLOR.fieldOf("color_b").forGetter(GlitchShear::colorB),
                Codec.FLOAT.optionalFieldOf("speed", 1f).forGetter(GlitchShear::speed),
                Codec.FLOAT.optionalFieldOf("intensity", 0.3f).forGetter(GlitchShear::intensity))
                .apply(i, GlitchShear::new));

        @Override
        public int colorAt(Point p, float time) {
            float t = time * speed;
            float half = p.ny >= 0f ? 1f : 0f;
            float shift = (hash((float) Math.floor(t * 4f) + half * 13f) - 0.5f) * intensity * 2f;
            float shiftedNx = p.nx + (half > 0.5f ? shift : -shift);
            boolean band = Math.floorMod((int) Math.floor((shiftedNx + 1f) * 4f), 2) == 0;
            float seam = smoothstep(0.3f, 0f, Math.abs(p.ny)) * (0.5f + 0.5f * Mth.sin(t * 20f));
            int base = band ? colorA : colorB;
            return ((base >>> 24) << 24) | (mix(base, 0xFFFFFFFF, seam * 0.5f) & 0xFFFFFF);
        }

        @Override
        public String type() {
            return "glitch_shear";
        }
    }

    // ---------------------------------------------------------------- math

    /** Position of the point along a gradient direction, 0..1 from one screen edge to the opposite one. */
    public static float along(Point p, float angleDegrees) {
        float rad = angleDegrees * Mth.DEG_TO_RAD;
        float c = Mth.cos(rad), s = Mth.sin(rad);
        float extent = Math.abs(c) + Math.abs(s);
        return clamp01(0.5f + ((p.u - 0.5f) * c + (p.v - 0.5f) * s) / extent);
    }

    public static float clamp01(float v) {
        return Mth.clamp(v, 0f, 1f);
    }

    public static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /** 0..1 sine wave with the given number of cycles elapsed. */
    public static float wave(float cycles) {
        return 0.5f + 0.5f * Mth.sin(cycles * TWO_PI);
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp01(edge1 == edge0 ? (x < edge0 ? 0f : 1f) : (x - edge0) / (edge1 - edge0));
        return t * t * (3f - 2f * t);
    }

    public static float hash(float n) {
        return frac((float) Math.sin(n) * 43758.5453f);
    }

    private static float valueNoise(float x, float y) {
        float ix = (float) Math.floor(x), iy = (float) Math.floor(y);
        float fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float a = hash(ix * 127.1f + iy * 311.7f);
        float b = hash((ix + 1) * 127.1f + iy * 311.7f);
        float c = hash(ix * 127.1f + (iy + 1) * 311.7f);
        float d = hash((ix + 1) * 127.1f + (iy + 1) * 311.7f);
        return Mth.lerp(fy, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }

    public static int mix(int colorA, int colorB, float t) {
        t = clamp01(t);
        int a = Math.round(((colorA >>> 24) & 0xFF) + (((colorB >>> 24) & 0xFF) - ((colorA >>> 24) & 0xFF)) * t);
        int r = Math.round(((colorA >> 16) & 0xFF) + (((colorB >> 16) & 0xFF) - ((colorA >> 16) & 0xFF)) * t);
        int g = Math.round(((colorA >> 8) & 0xFF) + (((colorB >> 8) & 0xFF) - ((colorA >> 8) & 0xFF)) * t);
        int b = Math.round((colorA & 0xFF) + ((colorB & 0xFF) - (colorA & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlphaScale(int argb, float scale) {
        return (Math.round(((argb >>> 24) & 0xFF) * clamp01(scale)) << 24) | (argb & 0x00FFFFFF);
    }

    private static DataResult<Integer> parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) :
                value.startsWith("0x") || value.startsWith("0X") ? value.substring(2) : value;
        try {
            if (hex.length() == 6) return DataResult.success(0xFF000000 | Integer.parseInt(hex, 16));
            if (hex.length() == 8) return DataResult.success((int) Long.parseLong(hex, 16));
        } catch (NumberFormatException ignored) {}
        return DataResult.error(() -> "Invalid colour '" + value + "', expected #RRGGBB or #AARRGGBB");
    }
}
