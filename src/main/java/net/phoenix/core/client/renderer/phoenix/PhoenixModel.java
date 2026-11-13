package net.phoenix.core.client.renderer.phoenix;

public final class PhoenixModel {

    public interface Sink {

        void tri(float ax, float ay, float az, int ac, float bx, float by, float bz, int bc, float cx, float cy,
                 float cz, int cc);
    }

    public static final int WHITE_HOT = 0xFFF4D8;
    public static final int GOLD = 0xFFC24A;
    public static final int ORANGE = 0xFF7A1E;
    public static final int RED = 0xE02A10;

    private PhoenixModel() {}

    private static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24;
        int ba = b >>> 24;
        int al = (int) (aa + (ba - aa) * t);
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    private static void feather(Sink s, float rx, float ry, float rz, float dx, float dy, float dz, float len,
                                float width,
                                float bx, float by, float bz, int cRoot, int cMid, int cTip, int segs) {
        float nx = -dz;
        float ny = 0f;
        float nz = dx;
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-3f) {
            nx = 1f;
            ny = 0f;
            nz = 0f;
        } else {
            nx /= nl;
            ny /= nl;
            nz /= nl;
        }
        float px = 0, py = 0, pz = 0, pw = 0;
        int pc = 0;
        for (int i = 0; i <= segs; i++) {
            float u = i / (float) segs;
            float x = rx + dx * len * u + bx * u * u;
            float y = ry + dy * len * u + by * u * u;
            float z = rz + dz * len * u + bz * u * u;
            float w = width * (float) Math.sin(Math.PI * (0.18 + 0.82 * Math.pow(u, 0.8)));
            int c = u < 0.5f ? lerp(cRoot, cMid, u * 2f) : lerp(cMid, cTip, (u - 0.5f) * 2f);
            if (i > 0) {
                s.tri(px + nx * pw, py + ny * pw, pz + nz * pw, pc, px - nx * pw, py - ny * pw, pz - nz * pw, pc,
                        x + nx * w, y + ny * w, z + nz * w, c);
                s.tri(px - nx * pw, py - ny * pw, pz - nz * pw, pc, x - nx * w, y - ny * w, z - nz * w, c, x + nx * w,
                        y + ny * w, z + nz * w, c);
            }
            px = x;
            py = y;
            pz = z;
            pw = w;
            pc = c;
        }
    }

    public static void build(Sink s, float t, float alpha) {
        float phase = t * 2.2f;
        float flap = (float) Math.sin(phase);
        float vel = (float) Math.cos(phase);
        float beta = 0.2f + 0.55f * flap;
        float cosB = (float) Math.cos(beta);
        float sinB = (float) Math.sin(beta);

        wings(s, alpha, cosB, sinB, vel, t);
        tail(s, alpha, t, vel);
        body(s, alpha);
        head(s, alpha, t);
    }

    private static void wings(Sink s, float alpha, float cosB, float sinB, float vel, float t) {
        for (int side = -1; side <= 1; side += 2) {
            float shoulderX = side * 0.08f;
            float shoulderY = 0.07f;
            float shoulderZ = 0.15f;

            int primaries = 10;
            for (int i = 0; i < primaries; i++) {
                float f = i / (float) (primaries - 1);
                float a = (float) Math.toRadians(-6 + 72 * f);
                float len = 1.5f * (1f - 0.4f * (float) Math.pow(f, 1.4f));
                float dx0 = side * (float) Math.cos(a);
                float dz0 = -(float) Math.sin(a);
                float dx = dx0 * cosB;
                float dy = Math.abs(dx0) * sinB;
                float rx = shoulderX + side * 0.05f * (1f - f);
                float lag = -0.3f * len * vel;
                int root = argb(WHITE_HOT, 0.7f * alpha);
                int mid = argb(i % 2 == 0 ? GOLD : ORANGE, 0.5f * alpha);
                int tip = argb(i % 3 == 0 ? RED : ORANGE, 0f);
                feather(s, rx, shoulderY, shoulderZ - 0.02f * f, dx, dy, dz0, len, 0.095f, 0f, lag - 0.12f * len,
                        -0.1f * len * f, root, mid, tip, 6);
            }

            int secondaries = 8;
            for (int i = 0; i < secondaries; i++) {
                float f = i / (float) (secondaries - 1);
                float a = (float) Math.toRadians(32 + 52 * f);
                float len = 0.85f * (1f - 0.32f * f);
                float dx0 = side * (float) Math.cos(a);
                float dz0 = -(float) Math.sin(a);
                float dx = dx0 * cosB;
                float dy = Math.abs(dx0) * sinB;
                float lag = -0.34f * len * vel;
                feather(s, shoulderX, shoulderY - 0.01f, shoulderZ - 0.05f - 0.06f * f, dx, dy, dz0, len, 0.085f, 0f,
                        lag - 0.1f * len, -0.08f * len, argb(WHITE_HOT, 0.75f * alpha), argb(GOLD, 0.6f * alpha),
                        argb(ORANGE, 0f), 5);
            }

            for (int i = 0; i < 6; i++) {
                float f = i / 5f;
                float a = (float) Math.toRadians(-4 + 40 * f);
                float len = 0.5f * (1f - 0.35f * f);
                float dx0 = side * (float) Math.cos(a);
                float dz0 = -(float) Math.sin(a);
                feather(s, shoulderX, shoulderY + 0.012f, shoulderZ + 0.02f, dx0 * cosB, Math.abs(dx0) * sinB, dz0,
                        len * 1.4f,
                        0.06f, 0f, -0.2f * len * vel - 0.05f, 0f, argb(0xFFFFFF, 0.85f * alpha),
                        argb(WHITE_HOT, 0.6f * alpha), argb(GOLD, 0f), 4);
            }
        }
    }

    private static void tail(Sink s, float alpha, float t, float vel) {
        for (int i = 0; i < 9; i++) {
            float f = (i - 4) / 4f;
            float len = 1.05f + 0.7f * (1f - Math.abs(f)) + (i % 2 == 0 ? 0.12f : 0f);
            float sway = (float) Math.sin(t * 2f + i * 0.7f) * 0.18f;
            float dx = f * 0.42f;
            float dz = -1f;
            float dl = (float) Math.sqrt(dx * dx + dz * dz);
            int root = argb(WHITE_HOT, 0.65f * alpha);
            int mid = argb(i % 2 == 0 ? GOLD : ORANGE, 0.5f * alpha);
            int tip = argb(i % 3 == 0 ? RED : ORANGE, 0f);
            feather(s, f * 0.03f, -0.03f, -0.42f, dx / dl, -0.1f, dz / dl, len, 0.075f, sway + f * 0.2f,
                    -0.25f * len + 0.1f * vel, -0.1f * len, root, mid, tip, 6);
        }
    }

    private static void body(Sink s, float alpha) {
        int rings = 9;
        int sides = 6;
        float[] ry = new float[rings + 1];
        float[] rr = new float[rings + 1];
        float[] rz = new float[rings + 1];
        for (int k = 0; k <= rings; k++) {
            float v = k / (float) rings;
            rz[k] = -0.5f + 0.9f * v;
            rr[k] = 0.15f * (0.3f + 0.7f * (float) Math.sin(Math.PI * Math.pow(v, 0.75)));
            ry[k] = 0.02f * (float) Math.sin(v * Math.PI);
        }
        for (int k = 0; k < rings; k++) {
            for (int i = 0; i < sides; i++) {
                float a0 = (float) (Math.PI * 2 * i / sides);
                float a1 = (float) (Math.PI * 2 * (i + 1) / sides);
                float v0 = k / (float) rings;
                float v1 = (k + 1) / (float) rings;
                int c00 = bodyColor(a0, v0, alpha);
                int c01 = bodyColor(a1, v0, alpha);
                int c10 = bodyColor(a0, v1, alpha);
                int c11 = bodyColor(a1, v1, alpha);
                float x00 = (float) Math.cos(a0) * rr[k];
                float y00 = ry[k] + (float) Math.sin(a0) * rr[k];
                float x01 = (float) Math.cos(a1) * rr[k];
                float y01 = ry[k] + (float) Math.sin(a1) * rr[k];
                float x10 = (float) Math.cos(a0) * rr[k + 1];
                float y10 = ry[k + 1] + (float) Math.sin(a0) * rr[k + 1];
                float x11 = (float) Math.cos(a1) * rr[k + 1];
                float y11 = ry[k + 1] + (float) Math.sin(a1) * rr[k + 1];
                s.tri(x00, y00, rz[k], c00, x01, y01, rz[k], c01, x11, y11, rz[k + 1], c11);
                s.tri(x00, y00, rz[k], c00, x11, y11, rz[k + 1], c11, x10, y10, rz[k + 1], c10);
            }
        }
    }

    private static int bodyColor(float angle, float v, float alpha) {
        float belly = 0.5f + 0.5f * (float) Math.sin(angle - Math.PI / 2);
        int c = lerp(argb(ORANGE, 0.65f * alpha), argb(WHITE_HOT, 0.85f * alpha), Math.max(belly * 0.7f, v * 0.9f));
        return c;
    }

    private static void head(Sink s, float alpha, float t) {
        int sides = 5;
        float[][] path = { { 0f, 0.05f, 0.38f, 0.085f }, { 0f, 0.12f, 0.47f, 0.075f }, { 0f, 0.2f, 0.55f, 0.065f },
                { 0f, 0.27f, 0.61f, 0.06f } };
        for (int k = 0; k < path.length - 1; k++) {
            for (int i = 0; i < sides; i++) {
                float a0 = (float) (Math.PI * 2 * i / sides);
                float a1 = (float) (Math.PI * 2 * (i + 1) / sides);
                float[] p = path[k];
                float[] q = path[k + 1];
                int c = argb(WHITE_HOT, 0.85f * alpha);
                int d = argb(GOLD, 0.75f * alpha);
                s.tri(p[0] + (float) Math.cos(a0) * p[3], p[1] + (float) Math.sin(a0) * p[3], p[2], c,
                        p[0] + (float) Math.cos(a1) * p[3], p[1] + (float) Math.sin(a1) * p[3], p[2], c,
                        q[0] + (float) Math.cos(a1) * q[3], q[1] + (float) Math.sin(a1) * q[3], q[2], d);
                s.tri(p[0] + (float) Math.cos(a0) * p[3], p[1] + (float) Math.sin(a0) * p[3], p[2], c,
                        q[0] + (float) Math.cos(a1) * q[3], q[1] + (float) Math.sin(a1) * q[3], q[2], d,
                        q[0] + (float) Math.cos(a0) * q[3], q[1] + (float) Math.sin(a0) * q[3], q[2], d);
            }
        }

        int hot = argb(0xFFFFFF, 0.95f * alpha);
        int gold = argb(GOLD, 0.9f * alpha);
        float hz = 0.61f;
        float hy = 0.27f;
        s.tri(-0.055f, hy, hz, hot, 0.055f, hy, hz, hot, 0f, hy + 0.07f, hz + 0.02f, hot);
        s.tri(-0.05f, hy, hz, hot, 0.05f, hy, hz, hot, 0f, hy - 0.04f, hz + 0.1f, gold);
        s.tri(-0.04f, hy, hz + 0.03f, gold, 0.04f, hy, hz + 0.03f, gold, 0f, hy - 0.035f, hz + 0.2f,
                argb(0xFFE08A, 0.95f * alpha));

        for (int side = -1; side <= 1; side += 2) {
            float ex = side * 0.052f;
            s.tri(ex, hy + 0.012f, hz + 0.03f, hot, ex + side * 0.012f, hy + 0.012f, hz + 0.045f, hot, ex, hy + 0.03f,
                    hz + 0.04f, hot);
        }

        for (int i = 0; i < 5; i++) {
            float f = (i - 2) / 2f;
            float sway = (float) Math.sin(t * 3f + i) * 0.05f;
            float dx = f * 0.35f;
            float len = 0.42f - 0.05f * Math.abs(f);
            feather(s, f * 0.015f, hy + 0.06f, hz - 0.02f, dx, 0.55f, -0.8f, len, 0.03f, sway, -0.12f, -0.1f,
                    argb(0xFFFFFF, 0.9f * alpha), argb(GOLD, 0.75f * alpha), argb(RED, 0f), 5);
        }
    }
}
