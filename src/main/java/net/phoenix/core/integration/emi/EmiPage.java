package net.phoenix.core.integration.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

final class EmiPage {

    record Line(Component text, int color) {

        static Line of(String text, int color) {
            return new Line(Component.literal(text), color);
        }
    }

    static final int LINE_H = 10;
    /** The smallest a line is shrunk to before it is cut. */
    static final float MIN_SCALE = 0.72f;
    static final int ROW_H = 11;

    static final int TEXT = 0xFFE4E8F4;
    static final int DIM = 0xFF8C96B0;
    static final int GOOD = 0xFF6CE08C;
    static final int BAD = 0xFFE08A8A;
    static final int WARM = 0xFFFFD866;

    private EmiPage() {}

    static int measure(List<Line> lines, int width) {
        var font = Minecraft.getInstance().font;
        int h = 0;
        for (Line line : lines) h += font.split(line.text(), width).size() * LINE_H + 1;
        return h;
    }

    static int draw(WidgetHolder widgets, List<Line> lines, int x, int y, int width) {
        var font = Minecraft.getInstance().font;
        for (Line line : lines) {
            for (FormattedCharSequence part : font.split(line.text(), width)) {
                widgets.addText(part, x, y, line.color(), false);
                y += LINE_H;
            }
            y += 1;
        }
        return y;
    }

    static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    static void rect(WidgetHolder widgets, int x, int y, int w, int h, int argb) {
        widgets.addDrawable(x, y, w, h, (g, mx, my, delta) -> g.fill(0, 0, w, h, argb));
    }

    static void frame(WidgetHolder widgets, int w, int h, int accent) {
        widgets.addDrawable(0, 0, w, h, (g, mx, my, delta) -> {
            g.fill(-5, -4, w + 4, h + 4, 0xFF10131C);
            g.fill(-5, -4, w + 4, h + 4, withAlpha(accent, 0x12));
            g.renderOutline(-5, -4, w + 9, h + 8, 0xFF30344A);
            g.fill(-5, -4, -2, h + 4, accent);
        });
    }

    static int section(WidgetHolder widgets, String label, int x, int y, int w, int accent) {
        Font font = Minecraft.getInstance().font;
        String text = label.toUpperCase(java.util.Locale.ROOT);
        widgets.addText(Component.literal(text).getVisualOrderText(), x, y, accent, false);
        int after = x + font.width(text) + 4;
        rect(widgets, after, y + 4, Math.max(0, x + w - after), 1, withAlpha(accent, 0x60));
        return y + ROW_H + 1;
    }

    static void row(WidgetHolder widgets, Component text, int x, int y, int maxW, int color, List<Component> tip) {
        Font font = Minecraft.getInstance().font;
        String plain = text.getString();
        int width = font.width(plain);
        List<Component> lines = tip == null ? List.of() : tip;
        if (width <= maxW) {
            widgets.addText(text.getVisualOrderText(), x, y, color, false);
        } else {
            // a line that is too wide shrinks to fit first; only when that is not enough is it cut, and its full text
            // is the tooltip either way
            float scale = Math.max(MIN_SCALE, maxW / (float) width);
            FormattedCharSequence shown = text.getVisualOrderText();
            if (width * scale > maxW + 0.5f) {
                int room = Math.round(maxW / scale) - font.width("…");
                shown = Component.literal(font.plainSubstrByWidth(plain, Math.max(0, room)) + "…")
                        .withStyle(text.getStyle()).getVisualOrderText();
            }
            final FormattedCharSequence drawn = shown;
            widgets.addDrawable(x, y, maxW, font.lineHeight, (g, mx, my, delta) -> {
                g.pose().pushPose();
                g.pose().scale(scale, scale, 1f);
                g.drawString(font, drawn, 0, 0, color, false);
                g.pose().popPose();
            });
            if (lines.isEmpty()) lines = List.of(text);
        }
        if (!lines.isEmpty()) widgets.addTooltipText(lines, x, y - 1, maxW, ROW_H);
    }

    /** Text that wraps onto as many lines as it needs; returns the y below it. */
    static int wrap(WidgetHolder widgets, String text, int x, int y, int maxW, int color) {
        Font font = Minecraft.getInstance().font;
        for (FormattedCharSequence part : font.split(Component.literal(text), maxW)) {
            widgets.addText(part, x, y, color, false);
            y += LINE_H;
        }
        return y;
    }

    /** How tall {@link #wrap} makes the text. */
    static int wrapHeight(String text, int maxW) {
        return Minecraft.getInstance().font.split(Component.literal(text), maxW).size() * LINE_H;
    }

    static void row(WidgetHolder widgets, String text, int x, int y, int maxW, int color, List<Component> tip) {
        row(widgets, Component.literal(text), x, y, maxW, color, tip);
    }

    static void title(WidgetHolder widgets, String text, int x, int y, int maxW, int color) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        float scale = width <= maxW ? 1.0f : Math.max(0.6f, maxW / (float) width);
        widgets.addDrawable(x, y, maxW, font.lineHeight, (g, mx, my, delta) -> {
            g.pose().pushPose();
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, text, 0, 0, color, false);
            g.pose().popPose();
        });
        if (scale < 1.0f) widgets.addTooltipText(List.of(Component.literal(text)), x, y, maxW, font.lineHeight);
    }

    static void bar(WidgetHolder widgets, int x, int y, int w, int h, double share, int color) {
        int filled = (int) Math.round(w * Math.max(0.0, Math.min(1.0, share)));
        widgets.addDrawable(x, y, w, h, (g, mx, my, delta) -> {
            g.fill(0, 0, w, h, 0xFF0C0C18);
            if (filled > 0) g.fill(0, 0, filled, h, color);
        });
    }

    static int pill(WidgetHolder widgets, String text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(text) + 8;
        widgets.addDrawable(x, y, w, 11, (g, mx, my, delta) -> {
            g.fill(0, 0, w, 11, withAlpha(color, 0x38));
            g.renderOutline(0, 0, w, 11, withAlpha(color, 0xA0));
            g.drawString(font, text, 4, 2, color, false);
        });
        return w;
    }
}
