package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.phoenix.core.common.block.cinema.CinemaPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything on a cinema screen goes, in blocks from the middle of the screen (x to the viewer's right, y up): the
 * title, the wrapped body, the buttons and the page arrows. The renderer draws from it and a click is matched against it,
 * so what is drawn is what can be clicked.
 */
public final class CinemaLayout {

    public record Rect(float x0, float y0, float x1, float y1) {

        public boolean contains(float x, float y) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1;
        }
    }

    /** A clickable area: its action ({@code next}, {@code prev} or what the page's button names) and what it says. */
    public record Hit(Rect rect, String action, String label) {}

    /** A line of text at a position: {@code x} is its middle (centred) or its left edge, {@code y} its middle. */
    public record TextLine(FormattedCharSequence text, float x, float y, float scale, boolean centred) {}

    /** A picture and the square it fills. */
    public record Slot(CinemaPage.Picture picture, Rect rect) {}

    public static final float PAD = 0.2f;
    public final List<Slot> pictures = new ArrayList<>();

    public Rect plate;
    public final List<TextLine> titleLines = new ArrayList<>();
    public final List<TextLine> bodyLines = new ArrayList<>();
    public final List<Hit> hits = new ArrayList<>();
    public TextLine counter;
    public TextLine hint;

    private CinemaLayout() {}

    /** @param textScale blocks per font pixel (the screen's text scale) */
    public static CinemaLayout of(Font font, CinemaPage page, float textScale, float width, float height, int pageIndex,
                                  int pageCount, boolean centredText) {
        CinemaLayout l = new CinemaLayout();
        float w = width;
        float h = height;
        l.plate = new Rect(-w / 2 + 0.04f, -h / 2 + 0.04f, w / 2 - 0.04f, h / 2 - 0.04f);
        float innerW = w - 2 * PAD;
        float left = -w / 2 + PAD;
        float top = h / 2 - PAD;
        float bottom = -h / 2 + PAD;
        float lineH = (font.lineHeight + 2) * textScale;

        // the footer: arrows and the page count
        boolean paged = pageCount > 1;
        float navH = 0.3f;
        if (paged) {
            float navY0 = bottom;
            float navY1 = bottom + navH;
            l.hits.add(new Hit(new Rect(left, navY0, left + 0.7f, navY1), "prev", "<"));
            l.hits.add(new Hit(new Rect(left + innerW - 0.7f, navY0, left + innerW, navY1), "next", ">"));
            l.counter = new TextLine(Component.literal((pageIndex + 1) + " / " + pageCount).getVisualOrderText(), 0,
                    (navY0 + navY1) / 2, textScale, true);
            if (pageIndex == 0) {
                l.hint = new TextLine(Component.literal("right-click to turn the page").getVisualOrderText(), 0,
                        navY1 + lineH * 0.7f, textScale * 0.8f, true);
            }
            bottom = navY1 + 0.12f + (pageIndex == 0 ? lineH : 0.0f);
        }

        // buttons sit above the footer, in a row
        List<CinemaPage.Button> buttons = page.buttons();
        if (!buttons.isEmpty()) {
            float gap = 0.15f;
            float btnH = 0.36f;
            float btnW = Math.min(2.2f, (innerW - gap * (buttons.size() - 1)) / buttons.size());
            float rowW = btnW * buttons.size() + gap * (buttons.size() - 1);
            float x = -rowW / 2;
            for (CinemaPage.Button b : buttons) {
                l.hits.add(new Hit(new Rect(x, bottom, x + btnW, bottom + btnH), b.action(), b.label()));
                x += btnW + gap;
            }
            bottom += btnH + 0.15f;
        }

        // the title, then the body in what is left
        float y = top;
        if (!page.title().isEmpty()) {
            float ts = textScale * 1.4f;
            float th = (font.lineHeight + 3) * ts;
            for (FormattedCharSequence seq : font.split(Component.literal(page.title()), (int) (innerW / ts))) {
                l.titleLines.add(new TextLine(seq, 0, y - th / 2, ts, true));
                y -= th;
            }
            y -= 0.1f;
        }
        // a row of pictures, each with its caption under it
        if (!page.pictures().isEmpty()) {
            int n = page.pictures().size();
            float gap = 0.3f;
            float size = Math.min(1.0f, (innerW - gap * (n - 1)) / n);
            float capScale = textScale * 0.9f;
            float capH = (font.lineHeight + 2) * capScale;
            int capWidth = Math.max(8, (int) ((size + gap) / capScale));
            float rowW = n * size + (n - 1) * gap;
            float x = -rowW / 2;
            int capLines = 0;
            for (CinemaPage.Picture picture : page.pictures()) {
                l.pictures.add(new Slot(picture, new Rect(x, y - size, x + size, y)));
                if (!picture.caption().isEmpty()) {
                    List<FormattedCharSequence> lines = font.split(Component.literal(picture.caption()), capWidth);
                    int shown = Math.min(2, lines.size());
                    capLines = Math.max(capLines, shown);
                    for (int i = 0; i < shown; i++) {
                        l.bodyLines.add(new TextLine(lines.get(i), x + size / 2, y - size - capH * (i + 0.8f), capScale,
                                true));
                    }
                }
                x += size + gap;
            }
            y -= size + capLines * capH + 0.15f;
        }
        if (!page.body().isEmpty()) {
            List<FormattedCharSequence> body = new ArrayList<>();
            for (String paragraph : page.body().split("\n", -1)) {
                if (paragraph.isEmpty()) {
                    body.add(null);
                    continue;
                }
                body.addAll(font.split(Component.literal(paragraph), (int) (innerW / textScale)));
            }
            float total = body.size() * lineH;
            // with no title the text sits in the middle of what is free
            float start = page.title().isEmpty() ? (y + bottom) / 2 + total / 2 : y;
            float cy = start;
            for (FormattedCharSequence seq : body) {
                if (seq != null) {
                    l.bodyLines.add(new TextLine(seq, centredText ? 0 : left, cy - lineH / 2, textScale, centredText));
                }
                cy -= lineH;
            }
        }
        return l;
    }

    /** The hit under the point, or null. */
    public Hit hitAt(float x, float y) {
        for (Hit hit : hits) {
            if (hit.rect().contains(x, y)) return hit;
        }
        return null;
    }
}
