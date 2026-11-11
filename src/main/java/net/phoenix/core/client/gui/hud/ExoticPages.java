package net.phoenix.core.client.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class ExoticPages {

    private ExoticPages() {}

    private static void dot(GuiGraphics g, float x, float y, int r, int color) {
        g.fill(Math.round(x) - r, Math.round(y) - r, Math.round(x) + r + 1, Math.round(y) + r + 1, color);
    }

    private static void badge(GuiGraphics g, Font font, int w, String text, int color) {
        int bw = font.width(text) + 10;
        g.fill(w - 8 - bw, 6, w - 8, 18, GuiDraw.withAlpha(color, 0x30));
        GuiDraw.drawBorder(g, w - 8 - bw, 6, bw, 12, color, 1);
        g.drawString(font, text, w - 8 - bw + 5, 8, color, false);
    }

    private static void big(GuiGraphics g, Font font, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static String num(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    private static void chevron(GuiGraphics g, int x, int y, int size, boolean right, int color) {
        int tip = right ? x + size : x;
        int back = right ? x : x + size;
        GuiDraw.drawLine(g, back, y - size, tip, y, color);
        GuiDraw.drawLine(g, back, y + size, tip, y, color);
    }

    public static CanvasPainter sourceHatch(String title, boolean input, IntSupplier stored, IntSupplier capacity,
                                            IntSupplier rate, IntSupplier flow, int radius, int accent,
                                            Supplier<String> status) {
        Supplier<Float> level = Heroes.eased(() -> stored.getAsInt() / (float) Math.max(1, capacity.getAsInt()));
        Heroes.Hero orb = ExoticHeroes.sourceOrb(level, input, accent);
        return (g, w, h, mx, my) -> {
            Font font = Minecraft.getInstance().font;
            float t = Heroes.now();

            g.fillGradient(0, 0, w, h, 0xFF0A050F, 0xFF050208);
            for (int gx = 16; gx < w; gx += 16) g.fill(gx, 0, gx + 1, h, 0x0AFFFFFF);
            for (int gy = 16; gy < h; gy += 16) g.fill(0, gy, w, gy + 1, 0x0AFFFFFF);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            long ms = System.currentTimeMillis();
            int ox = (int) ((ms / 50) & 255);
            int oy = (int) ((ms / 70) & 255);
            for (int py = 0; py < h; py += 4) {
                for (int px = 0; px < w; px += 4) {
                    int nx = (px + ox) & 255;
                    int ny = (py + oy) & 255;
                    int v = ((nx * 734287 + ny * 912271) ^ (nx * 31 + ny * 17)) & 255;
                    g.fill(px, py, px + 4, py + 4, ((v * 35) / 255 << 24) | 0x8F00FF);
                }
            }
            RenderSystem.disableBlend();
            GuiDraw.drawBorder(g, 0, 0, w, h, GuiDraw.withAlpha(accent, 0xAA), 1);

            g.drawString(font, title.toUpperCase(Locale.ROOT), 8, 9, accent, false);
            badge(g, font, w, input ? "INPUT" : "OUTPUT", input ? HudModel.PURPLE : HudModel.CYAN);
            g.fill(8, 22, w - 8, 23, GuiDraw.withAlpha(accent, 0x60));

            int orbSize = Math.min(w / 2 - 6, h - 130);
            orb.draw(g, 8, 28, orbSize, orbSize, t);

            int tubeX = 8 + orbSize + 8;
            int tubeY = 32;
            int tubeH = orbSize - 8;
            g.fill(tubeX, tubeY, tubeX + 8, tubeY + tubeH, 0x55000000);
            GuiDraw.drawBorder(g, tubeX, tubeY, 8, tubeH, GuiDraw.withAlpha(accent, 0x90), 1);
            int filled = (int) (tubeH * Mth.clamp(level.get(), 0f, 1f));
            g.fill(tubeX + 1, tubeY + tubeH - filled, tubeX + 7, tubeY + tubeH - 1, GuiDraw.withAlpha(accent, 0xC0));
            for (int i = 0; i <= 10; i++) {
                int ty = tubeY + tubeH - Math.round(tubeH * i / 10f);
                g.fill(tubeX + 9, ty, tubeX + (i % 5 == 0 ? 15 : 12), ty + 1, GuiDraw.withAlpha(accent, 0x90));
            }

            int colX = tubeX + 22;
            int colW = w - 8 - colX;
            int cur = stored.getAsInt();
            int max = capacity.getAsInt();
            int r = rate.getAsInt();
            int fl = flow.getAsInt();
            String[] labels = { "SOURCE", "RATE", "FLOW", "REACH" };
            String[] values = { num(cur), num(r) + " /t", num(fl), radius + " blocks" };
            String[] subs = { "of " + num(max), "most it moves", "lately", "jar search" };
            int[] colors = { HudModel.PURPLE, HudModel.CYAN, HudModel.GREEN, HudModel.GOLD };
            float[] fills = { cur / (float) Math.max(1, max), 1f, Mth.clamp(fl / (float) Math.max(1, r * 20), 0f, 1f),
                    1f };
            int vy = 30;
            for (int i = 0; i < 4; i++) {
                g.drawString(font, labels[i], colX, vy, HudModel.DIM, false);
                big(g, font, font.plainSubstrByWidth(values[i], (int) (colW / 1.4f)), colX, vy + 10, 1.4f, colors[i]);
                g.drawString(font, font.plainSubstrByWidth(subs[i], colW), colX, vy + 24, HudModel.MID, false);

                g.fill(colX, vy + 35, colX + colW, vy + 37, 0x55000000);
                g.fill(colX, vy + 35, colX + Math.round(colW * fills[i]), vy + 37, GuiDraw.withAlpha(colors[i], 0xE0));
                vy += 44;
            }

            int fy = Math.max(28 + orbSize + 14, 30 + 4 * 44 + 8);
            boolean flowing = fl > 0 || (input ? cur < max : cur > 0);
            int spacing = 14;
            float shift = (t * 22f) % spacing;
            for (int cx = -spacing; cx < w + spacing; cx += spacing) {
                float px = cx + (input ? -shift : shift) + (input ? spacing : 0);
                float edge = Math.min(Math.max(0f, px), w - 1f) / (w - 1f);
                float fade = (float) Math.sin(edge * Math.PI);
                int a = (int) ((flowing ? 0xB0 : 0x30) * fade);
                if (a > 4) chevron(g, Math.round(px), fy, 4, !input, GuiDraw.withAlpha(accent, a));
            }

            String line = status.get();
            int ty = fy + 12;
            for (var part : font.split(net.minecraft.network.chat.Component.literal(line), w - 16)) {
                g.drawString(font, part, 8, ty, HudModel.MID, false);
                ty += 10;
            }
            for (var part : font.split(net.minecraft.network.chat.Component
                    .literal("Source pipes and relays can feed it too."), w - 16)) {
                g.drawString(font, part, 8, ty + 2, HudModel.DIM, false);
                ty += 10;
            }
        };
    }

    public static CanvasPainter threadHatch(String title, boolean output, IntSupplier stored, IntSupplier capacity,
                                            IntSupplier rate, int accent, int star, Supplier<String> status) {
        Supplier<Float> level = Heroes.eased(() -> stored.getAsInt() / (float) Math.max(1, capacity.getAsInt()));
        Heroes.Hero loom = ExoticHeroes.threadLoom(level, !output, accent, star);
        return (g, w, h, mx, my) -> {
            Font font = Minecraft.getInstance().font;
            float t = Heroes.now();

            g.fillGradient(0, 0, w, h, 0xFF050818, 0xFF010208);
            for (int i = 0; i < 46; i++) {
                float speed = 2.0f + (i % 5) * 2.5f;
                float sx = (((i * 61) % 211) / 211.0f * w + t * speed * (output ? 1 : -1)) % w;
                if (sx < 0) sx += w;
                float sy = ((i * 37) % 173) / 173.0f * h;
                float twinkle = 0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(t * 1.3f + i));
                dot(g, sx, sy, i % 7 == 0 ? 1 : 0, GuiDraw.withAlpha(star, (int) (0x80 * twinkle)));
            }
            GuiDraw.drawBorder(g, 0, 0, w, h, GuiDraw.withAlpha(accent, 0x90), 1);

            g.drawString(font, title.toUpperCase(Locale.ROOT), 8, 9, star, false);
            badge(g, font, w, output ? "OUTPUT" : "INPUT", output ? HudModel.CYAN : HudModel.PURPLE);
            g.fill(8, 22, w - 8, 23, GuiDraw.withAlpha(star, 0x50));

            int loomH = Math.min(120, h / 2 - 20);
            GuiDraw.enableScissor(g, 8, 28, w - 8, 28 + loomH);
            loom.draw(g, 8, 28, w - 16, loomH, t);
            g.disableScissor();
            GuiDraw.drawBorder(g, 8, 28, w - 16, loomH, GuiDraw.withAlpha(star, 0x60), 1);

            int sy = 28 + loomH + 8;
            int pw = (w - 16 - 8) / 3;
            int cur = stored.getAsInt();
            int max = capacity.getAsInt();
            int[] px = { 8, 8 + pw + 4, 8 + 2 * (pw + 4) };
            String[] labels = { "RATE", "THREAD", "DIRECTION" };
            String[] values = { num(rate.getAsInt()) + " /t", num(cur), output ? "OUT" : "IN" };
            String[] subs = { "most it moves", "of " + num(max), output ? "holds what is spun" : "feeds the machine" };
            int[] colors = { HudModel.CYAN, star, HudModel.PURPLE };

            int subLines = 1;
            for (String sub : subs) {
                subLines = Math.max(subLines, font.split(net.minecraft.network.chat.Component.literal(sub), pw - 10)
                        .size());
            }
            int boxH = 36 + subLines * 10;
            for (int i = 0; i < 3; i++) {
                g.fill(px[i], sy, px[i] + pw, sy + boxH, GuiDraw.withAlpha(0xFF000000, 0x70));
                GuiDraw.drawBorder(g, px[i], sy, pw, boxH, GuiDraw.withAlpha(colors[i], 0x80), 1);
                g.fill(px[i], sy, px[i] + pw, sy + 2, colors[i]);
                g.drawString(font, labels[i], px[i] + 5, sy + 6, HudModel.DIM, false);
                big(g, font, font.plainSubstrByWidth(values[i], (int) ((pw - 10) / 1.3f)), px[i] + 5, sy + 17, 1.3f,
                        colors[i]);
                int ly = sy + 34;
                for (var part : font.split(net.minecraft.network.chat.Component.literal(subs[i]), pw - 10)) {
                    g.drawString(font, part, px[i] + 5, ly, HudModel.MID, false);
                    ly += 10;
                }
            }

            int beads = 24;
            int by = sy + boxH + 14;
            float fillBeads = level.get() * beads;
            int step = (w - 24) / beads;
            for (int i = 0; i < beads; i++) {
                float bx = 12 + i * step + step / 2.0f;
                boolean lit = i < Math.floor(fillBeads);
                boolean edge = i == (int) Math.floor(fillBeads) && fillBeads < beads;
                float pulse = edge ? 0.5f + 0.5f * (float) Math.sin(t * 5f) : 1f;
                int c = lit ? GuiDraw.withAlpha(accent, 0xE0) : edge ? GuiDraw.withAlpha(accent, (int) (0x40 + 0x80 *
                        pulse)) : GuiDraw.withAlpha(accent, 0x28);
                dot(g, bx, by, 2, c);
                if (lit) dot(g, bx, by, 0, GuiDraw.withAlpha(0xFF_FFFFFF, 0xC0));
            }
            int ty = by + 12;
            for (var part : font.split(net.minecraft.network.chat.Component.literal(status.get()), w - 16)) {
                g.drawString(font, part, 8, ty, HudModel.MID, false);
                ty += 10;
            }
        };
    }
}
