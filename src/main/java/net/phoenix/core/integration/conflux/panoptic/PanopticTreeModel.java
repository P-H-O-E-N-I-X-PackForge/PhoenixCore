package net.phoenix.core.integration.conflux.panoptic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PanopticTreeModel {

    private PanopticTreeModel() {}

    public static final int STAGES = 7;

    public record Params(float growth, float height, float trunkRadius, int depth, float leafDensity, float leafSize,
                         float leafFallPerSecond, int trunkColor, int leafColorA, int leafColorB, int glowColor,
                         float glow) {}

    public record Palette(int trunk, int leafA, int leafB, int glow) {}

    public record Segment(float x0, float y0, float z0, float x1, float y1, float z1, float radius0, float radius1,
                          int depth) {}

    public record Leaf(float x, float y, float z, float size, float phase) {}

    public static final Palette PHOENIX = new Palette(0xFF8A4A24, 0xFFFF8A1F, 0xFFFFD25A, 0xFFFFB347);
    public static final Palette SCULK = new Palette(0xFF226B70, 0xFF2FB5A8, 0xFF7DE8D6, 0xFF2FB5A8);
    public static final Palette VOID = new Palette(0xFF4A3A8C, 0xFF8C6CFF, 0xFFE0D4FF, 0xFF8C6CFF);
    public static final Palette SEALED_A = new Palette(0xFF5A6678, 0xFF9FB4D6, 0xFFE3ECFF, 0xFFA9C2F0);
    public static final Palette SEALED_B = new Palette(0xFF4E5A52, 0xFF9FB8A8, 0xFFE0F0E6, 0xFFB0D0BC);
    public static final Palette SEALED = new Palette(0xFF5C636C, 0xFF8A8F99, 0xFFD6D9DE, 0xFF8A8F99);

    public static Palette palette(String discipline) {
        if (discipline == null) return SEALED;
        return switch (discipline) {
            case "phoenix" -> PHOENIX;
            case "sculk" -> SCULK;
            case "void" -> VOID;
            case "sealed_a" -> SEALED_A;
            case "sealed_b" -> SEALED_B;
            default -> SEALED;
        };
    }

    public static float growth(int stage, float progress) {
        float g = (Math.max(0, stage) + clamp01(progress)) / STAGES;
        return clamp01(g);
    }

    public static Params params(String discipline, float growth) {
        float g = clamp01(growth);
        Palette p = palette(discipline);
        float eased = g * g * (3f - 2f * g);
        float height = 3f + 45f * eased;
        float trunk = 0.25f + 2.6f * eased;
        int depth = 2 + Math.round(4f * eased);
        float density = 0.05f + 0.95f * eased;
        float leafSize = 0.5f + 1.6f * eased;

        float fall = g < 0.55f ? 0f : (g - 0.55f) / 0.45f * 6f;

        int dim = 0xFF1A1A1A;
        int leafA = lerpColor(dim, p.leafA(), 0.35f + 0.65f * eased);
        int leafB = lerpColor(p.leafA(), p.leafB(), eased);
        int trunkColor = lerpColor(dim, p.trunk(), 0.5f + 0.5f * eased);
        return new Params(g, height, trunk, depth, density, leafSize, fall, trunkColor, leafA, leafB, p.glow(),
                eased);
    }

    private static final int SEGMENT_CAP = 2600;

    public static List<Segment> branches(long seed, Params params) {
        List<Segment> out = new ArrayList<>();
        Random rng = new Random(seed);
        float g = params.growth();
        float lean = 0.06f * params.height();
        float tx = (rng.nextFloat() - 0.5f) * lean;
        float tz = (rng.nextFloat() - 0.5f) * lean;
        float trunkTop = params.height() * 0.5f;
        float r = params.trunkRadius();

        int pieces = 6;
        float[] cx = new float[pieces + 1];
        float[] cy = new float[pieces + 1];
        float[] cz = new float[pieces + 1];
        float[] cr = new float[pieces + 1];
        float sway = 0.04f * params.height();
        for (int i = 0; i <= pieces; i++) {
            float t = i / (float) pieces;
            cx[i] = tx * t * t + (i == 0 || i == pieces ? 0f : (rng.nextFloat() - 0.5f) * sway);
            cy[i] = trunkTop * t;
            cz[i] = tz * t * t + (i == 0 || i == pieces ? 0f : (rng.nextFloat() - 0.5f) * sway);
            cr[i] = r * (1f - 0.42f * t);
        }
        for (int i = 0; i < pieces; i++) {
            out.add(new Segment(cx[i], cy[i], cz[i], cx[i + 1], cy[i + 1], cz[i + 1], cr[i], cr[i + 1], 0));
        }

        if (g > 0.2f) {
            int roots = 5 + Math.round(g * 4f);
            float reach = r * (2.8f + 2.2f * g);
            for (int i = 0; i < roots; i++) {
                double angle = Math.PI * 2.0 * i / roots + rng.nextFloat() * 0.5;
                float len = reach * (0.7f + 0.6f * rng.nextFloat());
                float rx = (float) Math.cos(angle) * len;
                float rz = (float) Math.sin(angle) * len;
                out.add(new Segment(0, r * 1.6f, 0, rx, 0f, rz, r * 0.85f, r * 0.18f, 0));
            }
        }

        int limbs = 3 + Math.round(6f * g);
        for (int k = 0; k < limbs; k++) {
            float t = limbs == 1 ? 1f : 0.38f + 0.62f * (k / (float) (limbs - 1));
            float at = t * pieces;
            int i0 = Math.min(pieces - 1, (int) at);
            float f = at - i0;
            float ox = cx[i0] + (cx[i0 + 1] - cx[i0]) * f;
            float oy = cy[i0] + (cy[i0 + 1] - cy[i0]) * f;
            float oz = cz[i0] + (cz[i0 + 1] - cz[i0]) * f;
            float oradius = cr[i0] + (cr[i0 + 1] - cr[i0]) * f;

            double azimuth = k * 2.39996 + rng.nextFloat() * 0.5;
            float climb = 0.28f + 0.55f * t;
            float flat = 1f - climb * 0.7f;
            float dx = (float) Math.cos(azimuth) * flat;
            float dz = (float) Math.sin(azimuth) * flat;
            float len = (float) Math.sqrt(dx * dx + climb * climb + dz * dz);
            dx /= len;
            float dy = climb / len;
            dz /= len;

            float length = params.height() * 0.36f * (1.2f - 0.6f * t) * (0.85f + 0.3f * rng.nextFloat());
            float limbRadius = Math.max(0.08f, oradius * (0.62f - 0.18f * t));
            float nx = ox + dx * length;
            float ny = oy + dy * length;
            float nz = oz + dz * length;
            float tip = Math.max(0.05f, limbRadius * 0.7f);
            out.add(new Segment(ox, oy, oz, nx, ny, nz, limbRadius, tip, 1));
            grow(out, rng, nx, ny, nz, dx, dy, dz, length, tip, 2, params.depth());
        }

        float lx = cx[pieces];
        float ly = cy[pieces];
        float lz = cz[pieces];
        float leaderLen = params.height() * 0.3f;
        float tipRadius = Math.max(0.06f, cr[pieces] * 0.8f);
        out.add(new Segment(lx, ly, lz, lx + (rng.nextFloat() - 0.5f) * 0.1f * leaderLen, ly + leaderLen,
                lz + (rng.nextFloat() - 0.5f) * 0.1f * leaderLen, cr[pieces], tipRadius, 1));
        grow(out, rng, lx, ly + leaderLen, lz, 0f, 1f, 0f, leaderLen, tipRadius, 2, params.depth());
        return out;
    }

    private static void grow(List<Segment> out, Random rng, float x, float y, float z, float dx, float dy, float dz,
                             float length, float radius, int depth, int maxDepth) {
        if (depth > maxDepth || length < 0.3f || out.size() > SEGMENT_CAP) return;
        int kids = depth <= 2 ? 3 : 2;
        if (depth >= 3 && rng.nextInt(4) == 0) kids = 3;
        if (out.size() > SEGMENT_CAP * 0.8) kids = 1;
        for (int i = 0; i < kids; i++) {
            float spread = 0.42f + 0.34f * rng.nextFloat();
            float ax = dx + (rng.nextFloat() - 0.5f) * 2f * spread;
            float az = dz + (rng.nextFloat() - 0.5f) * 2f * spread;

            float ay = dy + (rng.nextFloat() - 0.5f) * spread + (depth < 4 ? 0.18f : -0.06f);
            float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            ax /= len;
            ay /= len;
            az /= len;
            float segLen = length * (0.62f + 0.2f * rng.nextFloat());
            float nx = x + ax * segLen;
            float ny = y + ay * segLen;
            float nz = z + az * segLen;
            float childRadius = Math.max(0.04f, radius * 0.66f);
            out.add(new Segment(x, y, z, nx, ny, nz, radius, childRadius, depth));
            grow(out, rng, nx, ny, nz, ax, ay, az, segLen, childRadius, depth + 1, maxDepth);
        }
    }

    public static List<Leaf> leaves(long seed, List<Segment> branches, Params params) {
        List<Leaf> out = new ArrayList<>();
        Random rng = new Random(seed ^ 0x5DEECE66DL);
        int maxDepth = 0;
        for (Segment s : branches) maxDepth = Math.max(maxDepth, s.depth());
        for (Segment s : branches) {
            if (s.depth() < Math.max(1, maxDepth - 1)) continue;
            if (rng.nextFloat() > params.leafDensity()) continue;
            out.add(new Leaf(s.x1(), s.y1(), s.z1(), params.leafSize() * (0.7f + 0.6f * rng.nextFloat()),
                    rng.nextFloat()));

            if (s.depth() == maxDepth && rng.nextFloat() < params.leafDensity()) {
                float mx = (s.x0() + s.x1()) * 0.5f + (rng.nextFloat() - 0.5f) * params.leafSize();
                float my = (s.y0() + s.y1()) * 0.5f + (rng.nextFloat() - 0.2f) * params.leafSize();
                float mz = (s.z0() + s.z1()) * 0.5f + (rng.nextFloat() - 0.5f) * params.leafSize();
                out.add(new Leaf(mx, my, mz, params.leafSize() * (0.6f + 0.5f * rng.nextFloat()), rng.nextFloat()));
            }
        }
        return out;
    }

    public static float[] leafFall(Leaf leaf, Params params, float seconds) {
        if (params.leafFallPerSecond() <= 0f) return null;
        float cycle = 14f;
        float t = (seconds * (0.3f + params.leafFallPerSecond() * 0.1f) + leaf.phase() * cycle) % cycle;
        float loose = 4f;
        if (t > loose) return null;
        float u = t / loose;
        float drop = u * u * params.height() * 0.5f;
        float sway = (float) Math.sin((u + leaf.phase()) * 9.0) * 1.4f * u;
        return new float[] { sway, -drop, sway * 0.6f };
    }

    public static int lerpColor(int a, int b, float t) {
        float k = clamp01(t);
        int aa = (int) (((a >>> 24) & 0xFF) + (((b >>> 24) & 0xFF) - ((a >>> 24) & 0xFF)) * k);
        int ar = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * k);
        int ag = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * k);
        int ab = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * k);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
