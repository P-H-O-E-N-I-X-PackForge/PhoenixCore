package net.phoenix.core.client.stellar.ui;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.phoenix.core.client.gui.hud.GuiDraw;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

public final class Stellar2D {

    private Stellar2D() {}

    private static final int SEGS = 48;

    public static float time() {
        return Util.getMillis() / 50.0f;
    }

    public static void draw(GuiGraphics g, StellarForgeState.Stage stage, int rgb, float cx, float cy, float r,
                            float time, float tilt, int holeRgb, float vigor) {
        rgb &= 0xFFFFFF;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = g.pose().last().pose();
        switch (stage) {
            case GAS_CLOUD, IDLE -> cloud(m, cx, cy, r, rgb, time);
            case PROTOSTAR -> protostar(m, cx, cy, r, rgb, time);
            case STAR -> star(g, m, cx, cy, r, rgb, time);
            case BLACK_HOLE -> blackHole(m, cx, cy, r, time, tilt, holeRgb, vigor);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void cloud(Matrix4f m, float cx, float cy, float r, int rgb, float t) {
        additive();
        for (int i = 0; i < 16; i++) {
            float a = i * 2.399f + t * (0.004f + 0.003f * (i % 3));
            float d = r * (0.12f + 0.5f * frac(i * 0.618f));
            float x = cx + Mth.cos(a) * d;
            float y = cy + Mth.sin(a) * d * 0.8f;
            float size = r * (0.32f + 0.3f * frac(i * 0.37f)) * (0.9f + 0.1f * Mth.sin(t * 0.03f + i));
            int c = i % 4 == 0 ? GuiDraw.mix(0xFF000000 | rgb, 0xFFFFFFFF, 0.35f) & 0xFFFFFF : rgb;
            glow(m, x, y, size, size * 0.85f, alpha(c, 0x34), alpha(c, 0));
        }
        glow(m, cx, cy, r * 0.5f, r * 0.45f, alpha(rgb, 0x40), alpha(rgb, 0));
        normal();
    }

    private static void protostar(Matrix4f m, float cx, float cy, float r, int rgb, float t) {
        int dust = GuiDraw.mix(0xFF000000 | rgb, 0xFFFF9A40, 0.5f) & 0xFFFFFF;
        additive();
        glow(m, cx, cy, r, r, alpha(rgb, 0x30), alpha(rgb, 0));

        float flick = 0.85f + 0.15f * Mth.sin(t * 0.2f);
        spike(m, cx, cy, -Mth.HALF_PI, r * 1.0f * flick, r * 0.09f, alpha(0xCFE8FF, 0x70));
        spike(m, cx, cy, Mth.HALF_PI, r * 1.0f * flick, r * 0.09f, alpha(0xCFE8FF, 0x70));

        for (int i = 0; i < 7; i++) {
            float rr = r * (0.38f + 0.075f * i);
            ring(m, cx, cy, rr, rr + r * 0.08f, 0.26f, 0.0f, Mth.TWO_PI,
                    alpha(dust, 0x50 - i * 6), alpha(dust, 0x50 - i * 6));
        }
        glow(m, cx, cy, r * 0.42f, r * 0.42f, alpha(0xFFF4E0, 0xE0), alpha(rgb, 0));
        normal();
    }

    private static void star(GuiGraphics g, Matrix4f m, float cx, float cy, float r, int rgb, float t) {
        float body = r * 0.55f;
        int white = GuiDraw.mix(0xFF000000 | rgb, 0xFFFFFFFF, 0.65f) & 0xFFFFFF;
        additive();
        glow(m, cx, cy, r, r, alpha(rgb, 0x3C), alpha(rgb, 0));
        for (int i = 0; i < 10; i++) {
            float a = i * Mth.TWO_PI / 10.0f + t * 0.004f;
            float len = r * (0.82f + 0.18f * Mth.sin(t * 0.05f + i * 1.7f));
            spike(m, cx, cy, a, len, r * 0.05f, alpha(rgb, 0x50));
        }
        glow(m, cx, cy, r * 0.75f, r * 0.75f, alpha(rgb, 0x70), alpha(rgb, 0));
        normal();

        disc(m, cx, cy, body, 0xFF000000 | white, 0xFF000000 | GuiDraw.mix(0xFF000000 | rgb, 0xFF000000, 0.15f));

        for (int i = 0; i < 26; i++) {
            float a = i * 2.399f + t * 0.006f;
            float d = body * 0.88f * (float) Math.sqrt(frac(i * 0.754f + 0.1f));
            float x = cx + Mth.cos(a) * d;
            float y = cy + Mth.sin(a) * d * 0.95f;
            boolean light = i % 2 == 0;
            int c = light ? alpha(0xFFFFFF, 0x30) : alpha(rgb & 0x7F7F7F, 0x40);
            int s = i % 3 == 0 ? 2 : 1;
            g.fill((int) x, (int) y, (int) x + s, (int) y + s, c);
        }
    }

    private static void blackHole(Matrix4f m, float cx, float cy, float r, float t, float tilt, int holeRgb,
                                  float vigor) {
        vigor = Mth.clamp(vigor, 0.0f, 1.0f);
        float open = 0.16f + 0.5f * Mth.clamp(tilt, 0.0f, 1.0f);
        float shadow = r * 0.25f;
        float inner = shadow * 1.18f;
        float outer = r * (0.62f + 0.38f * vigor);
        int hot = holeRgb >= 0 ? holeRgb : GuiDraw.mix(0xFFFFDB99, 0xFFDCEAFF, vigor) & 0xFFFFFF;
        int cool = holeRgb >= 0 ? darken(holeRgb, 0.5f) : GuiDraw.mix(0xFFF25214, 0xFFFF8A3A, vigor) & 0xFFFFFF;

        additive();
        glow(m, cx, cy, outer * 1.05f, outer * 1.05f * (0.5f + open), alpha(cool, 0x30), alpha(cool, 0));
        normal();
        disk(m, cx, cy, inner, outer, open, t, hot, cool, vigor, 0.0f, Mth.TWO_PI);
        disc(m, cx, cy, shadow, 0xFF000000, 0xFF000000);

        additive();
        ring(m, cx, cy, shadow * 1.0f, shadow * 1.55f, 1.0f, Mth.PI, Mth.TWO_PI,
                alpha(hot, 0xE0), alpha(cool, 0x00));
        ring(m, cx, cy, shadow * 1.0f, shadow * 1.35f, 1.0f, 0.0f, Mth.PI, alpha(hot, 0x90), alpha(cool, 0x00));
        ring(m, cx, cy, shadow * 0.97f, shadow * 1.06f, 1.0f, 0.0f, Mth.TWO_PI, alpha(0xFFFFFF, 0xC0),
                alpha(hot, 0x20));
        normal();

        disk(m, cx, cy, inner, outer, open, t, hot, cool, vigor, 0.0f, Mth.PI);
    }

    private static void disk(Matrix4f m, float cx, float cy, float inner, float outer, float open, float t, int hot,
                             int cool, float vigor, float from, float to) {
        int rings = 18;
        float width = (outer - inner) / rings;
        int steps = Math.max(12, (int) (SEGS * 2 * (to - from) / Mth.TWO_PI));
        for (int ring = 0; ring < rings; ring++) {
            float r0 = inner + ring * width;
            float r1 = r0 + width * 1.25f;
            float f0 = ring / (float) rings;
            float f1 = (ring + 1) / (float) rings;
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP,
                    DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i <= steps; i++) {
                float a = from + (to - from) * i / steps;
                float ca = Mth.cos(a);
                float sa = Mth.sin(a);
                v(b, m, cx + ca * r1, cy + sa * r1 * open, diskColor(a, f1, t, hot, cool, vigor));
                v(b, m, cx + ca * r0, cy + sa * r0 * open, diskColor(a, f0, t, hot, cool, vigor));
            }
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
    }

    private static int diskColor(float a, float out, float t, int hot, int cool, float vigor) {
        float arm = 0.5f + 0.5f * Mth.sin(2.0f * a - 10.0f * out + t * 0.09f);
        arm *= arm;
        float doppler = 0.5f + 0.5f * Mth.cos(a + t * 0.04f);
        float inten = Mth.clamp(0.3f + 0.4f * arm + 0.35f * doppler * doppler, 0.0f, 1.0f);
        float heat = (float) Math.pow(1.0f - out, 1.4f);
        int rgb = GuiDraw.mix(0xFF000000 | cool, 0xFF000000 | hot,
                Mth.clamp(heat * (0.55f + 0.7f * inten), 0.0f, 1.0f));
        rgb = GuiDraw.mix(0xFF000000 | rgb, 0xFFFFFFFF, heat * heat * 0.5f * vigor) & 0xFFFFFF;
        float fade = (1.0f - 0.8f * out) * (0.45f + 0.55f * inten) * (0.7f + 0.3f * vigor);
        return alpha(rgb, (int) Mth.clamp(255 * fade, 0.0f, 255.0f));
    }

    private static void glow(Matrix4f m, float cx, float cy, float rx, float ry, int center, int edge) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN,
                DefaultVertexFormat.POSITION_COLOR);
        v(b, m, cx, cy, center);
        for (int i = 0; i <= SEGS; i++) {
            float a = Mth.TWO_PI * i / SEGS;
            v(b, m, cx + Mth.cos(a) * rx, cy + Mth.sin(a) * ry, edge);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void disc(Matrix4f m, float cx, float cy, float r, int center, int edge) {
        glow(m, cx, cy, r, r, center, edge);
    }

    private static void ring(Matrix4f m, float cx, float cy, float r0, float r1, float open, float from, float to,
                             int inside, int outside) {
        int steps = Math.max(8, (int) (SEGS * (to - from) / Mth.TWO_PI));
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP,
                DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            float a = from + (to - from) * i / steps;
            float ca = Mth.cos(a);
            float sa = Mth.sin(a);
            v(b, m, cx + ca * r1, cy + sa * r1 * open, outside);
            v(b, m, cx + ca * r0, cy + sa * r0 * open, inside);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void spike(Matrix4f m, float cx, float cy, float angle, float len, float width, int base) {
        float ca = Mth.cos(angle);
        float sa = Mth.sin(angle);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        v(b, m, cx - sa * width, cy + ca * width, base);
        v(b, m, cx + sa * width, cy - ca * width, base);
        v(b, m, cx + ca * len, cy + sa * len, base & 0xFFFFFF);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void v(BufferBuilder b, Matrix4f m, float x, float y, int argb) {
        b.addVertex(m, x, y, 0.0f).setColor(argb);
    }

    private static void additive() {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    private static void normal() {
        RenderSystem.defaultBlendFunc();
    }

    private static int alpha(int rgb, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int darken(int rgb, float keep) {
        return ((int) (((rgb >> 16) & 0xFF) * keep) << 16) | ((int) (((rgb >> 8) & 0xFF) * keep) << 8) |
                (int) ((rgb & 0xFF) * keep);
    }

    private static float frac(float x) {
        return x - (float) Math.floor(x);
    }
}
