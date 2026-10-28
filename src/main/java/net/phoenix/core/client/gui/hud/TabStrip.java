package net.phoenix.core.client.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * The tabs of a machine screen, laid out the way its {@link HudTheme} says: a column of squares or diamonds down the
 * left edge, or a bar along the top. Used as the page itself: it draws and clicks through to whichever tab is active.
 */
public class TabStrip implements CanvasPainter {

    public record Tab(String label, CanvasPainter painter) {}

    public static final int TAB_W = 16;
    private static final int TAB_H = 16;
    private static final int GAP = 2;
    private static final int TOP_W = 30;

    private static final int C_WHITE = 0xFF_DCF0F4;

    private final List<Tab> tabs;
    private final HudTheme theme;
    private int active = 0;

    public TabStrip(List<Tab> tabs) {
        this(tabs, HudTheme.TEAL);
    }

    public TabStrip(List<Tab> tabs, HudTheme theme) {
        this.tabs = tabs;
        this.theme = theme;
    }

    public HudTheme theme() {
        return theme;
    }

    public int activeIndex() {
        return active;
    }

    public boolean horizontal() {
        return theme.tabs() == HudTheme.TabStyle.TOP;
    }

    /** Width of the strip as a widget. */
    public int stripWidth() {
        return horizontal() ? tabs.size() * (TOP_W + GAP) - GAP : TAB_W;
    }

    /** Height of the strip as a widget. */
    public int stripHeight() {
        return horizontal() ? TAB_H : tabs.size() * (TAB_H + GAP) - GAP;
    }

    private int slotX(int i) {
        return horizontal() ? i * (TOP_W + GAP) : 0;
    }

    private int slotY(int i) {
        return horizontal() ? 0 : i * (TAB_H + GAP);
    }

    private int slotW() {
        return horizontal() ? TOP_W : TAB_W;
    }

    /** The tab strip, meant to hang off the edge of the panel. */
    public CanvasPainter stripPainter() {
        return new CanvasPainter() {

            @Override
            public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
                Font font = Minecraft.getInstance().font;
                for (int i = 0; i < tabs.size(); i++) {
                    int x = slotX(i);
                    int y = slotY(i);
                    int sw = slotW();
                    boolean isActive = i == active;
                    boolean hovered = mouseX >= x && mouseX < x + sw && mouseY >= y && mouseY < y + TAB_H;
                    int fill = isActive ? GuiDraw.mix(theme.bgTop(), theme.accent(), 0.28f) :
                            hovered ? GuiDraw.mix(theme.bgTop(), theme.accent(), 0.12f) : theme.bgBottom();
                    int edge = isActive ? theme.accent() : hovered ? theme.accent2() : GuiDraw.withAlpha(theme.accent2(), 0x80);
                    int text = isActive ? theme.accent() : hovered ? C_WHITE : theme.dim();
                    String label = tabs.get(i).label();
                    switch (theme.tabs()) {
                        case DIAMOND -> {
                            // a diamond: rows narrowing to the points
                            int cx = x + sw / 2;
                            int cy = y + TAB_H / 2;
                            int r = TAB_H / 2;
                            for (int dy = -r; dy <= r; dy++) {
                                int half = r - Math.abs(dy);
                                g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, fill);
                                g.fill(cx - half, cy + dy, cx - half + 1, cy + dy + 1, edge);
                                g.fill(cx + half, cy + dy, cx + half + 1, cy + dy + 1, edge);
                            }
                            g.fill(cx, y, cx + 1, y + 1, edge);
                            g.fill(cx, y + TAB_H - 1, cx + 1, y + TAB_H, edge);
                            g.drawString(font, label, cx - font.width(label) / 2 + 1, cy - font.lineHeight / 2 + 1, text, false);
                        }
                        case TOP -> {
                            // a flat tab hanging from a rule, the active one filled and lit along its top
                            g.fill(x, y, x + sw, y + TAB_H, fill);
                            g.fill(x, y + TAB_H - 1, x + sw, y + TAB_H, isActive ? theme.accent() : theme.line());
                            g.fill(x, y, x + 1, y + TAB_H, edge);
                            g.fill(x + sw - 1, y, x + sw, y + TAB_H, edge);
                            if (isActive) g.fill(x, y, x + sw, y + 2, theme.accent());
                            g.drawString(font, label, x + (sw - font.width(label)) / 2 + 1,
                                    y + (TAB_H - font.lineHeight) / 2 + 1, text, false);
                        }
                        default -> {
                            g.fill(x, y, x + sw, y + TAB_H, fill);
                            GuiDraw.drawBorder(g, x, y, sw, TAB_H, edge, 1);
                            if (isActive) g.fill(x + sw - 2, y + 2, x + sw, y + TAB_H - 2, theme.accent());
                            g.drawString(font, label, x + (sw - font.width(label)) / 2 + 1,
                                    y + (TAB_H - font.lineHeight) / 2 + 1, text, false);
                        }
                    }
                }
            }

            @Override
            public boolean click(double mouseX, double mouseY, int button) {
                if (button != 0 || mouseX < 0 || mouseY < 0) return false;
                for (int i = 0; i < tabs.size(); i++) {
                    if (mouseX >= slotX(i) && mouseX < slotX(i) + slotW() && mouseY >= slotY(i) &&
                            mouseY < slotY(i) + TAB_H) {
                        active = i;
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        tabs.get(active).painter().render(g, w, h, mouseX, mouseY);
    }

    @Override
    public boolean click(double mouseX, double mouseY, int button) {
        return tabs.get(active).painter().click(mouseX, mouseY, button);
    }

    @Override
    public void drag(double mouseX, double mouseY, int button) {
        tabs.get(active).painter().drag(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        tabs.get(active).painter().release(button);
    }

    @Override
    public boolean scroll(double mouseX, double mouseY, double amount) {
        return tabs.get(active).painter().scroll(mouseX, mouseY, amount);
    }
}
