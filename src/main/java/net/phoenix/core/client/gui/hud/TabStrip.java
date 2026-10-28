package net.phoenix.core.client.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * A tab column over a machine screen: a dark cell for each page with a teal border that lights when active, hung off
 * the left edge of the panel. Used as the page itself: it draws and clicks through to whichever tab is active.
 */
public class TabStrip implements CanvasPainter {

    public record Tab(String label, CanvasPainter painter) {}

    public static final int TAB_W = 16;
    private static final int TAB_H = 16;
    private static final int GAP = 2;

    private static final int BG = 0xFF_010810;
    private static final int BG_HOVER = 0xFF_061825;
    private static final int BG_ACTIVE = 0xFF_002830;
    private static final int C_CYAN = 0xFF_00E5CC;
    private static final int C_DIM = 0xFF_3A5E6A;
    private static final int C_WHITE = 0xFF_DCF0F4;

    private final List<Tab> tabs;
    private int active = 0;

    public TabStrip(List<Tab> tabs) {
        this.tabs = tabs;
    }

    public int stripHeight() {
        return tabs.size() * (TAB_H + GAP) - GAP;
    }

    /** The vertical tab column, meant to hang off the left edge of the panel. */
    public CanvasPainter stripPainter() {
        return new CanvasPainter() {

            @Override
            public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
                Font font = Minecraft.getInstance().font;
                for (int i = 0; i < tabs.size(); i++) {
                    int y = i * (TAB_H + GAP);
                    boolean isActive = i == active;
                    boolean hovered = mouseX >= 0 && mouseX < TAB_W && mouseY >= y && mouseY < y + TAB_H;
                    g.fill(0, y, TAB_W, y + TAB_H, isActive ? BG_ACTIVE : hovered ? BG_HOVER : BG);
                    GuiDraw.drawBorder(g, 0, y, TAB_W, TAB_H,
                            isActive ? 0xFF_00AAA0 : hovered ? 0xCC_00E5CC : 0x66_00E5CC, 1);
                    String label = tabs.get(i).label();
                    int color = isActive ? C_CYAN : hovered ? C_WHITE : C_DIM;
                    g.drawString(font, label, (TAB_W - font.width(label)) / 2 + 1,
                            y + (TAB_H - font.lineHeight) / 2 + 1, color, false);
                }
            }

            @Override
            public boolean click(double mouseX, double mouseY, int button) {
                if (button != 0 || mouseX < 0 || mouseX >= TAB_W || mouseY < 0) return false;
                int idx = (int) (mouseY / (TAB_H + GAP));
                if (idx < tabs.size() && mouseY - idx * (TAB_H + GAP) < TAB_H) {
                    active = idx;
                    return true;
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
