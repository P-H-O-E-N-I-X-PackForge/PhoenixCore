package net.phoenix.core.client.stellar.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.phoenix.continuum.api.client.ContinuumRender;
import net.phoenix.core.client.gui.hud.CanvasPainter;
import net.phoenix.core.client.gui.hud.GuiDraw;
import net.phoenix.core.client.gui.hud.HudTheme;
import net.phoenix.core.client.stellar.StellarForgeClient;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import java.util.Locale;

public final class StellarViewPage implements CanvasPainter {

    private static final HudTheme T = HudTheme.DEEP_SPACE;
    private static final int VIEW = 168;
    private static final int VX = 8;
    private static final int VY = 24;

    private final StellarForgeMachine machine;
    private boolean dragging;
    private double lastX;
    private double lastY;
    private boolean overResetNow;

    public StellarViewPage(StellarForgeMachine machine) {
        this.machine = machine;
    }

    private static ContinuumRender.Body bodyOf(StellarForgeState.Stage stage) {
        return switch (stage) {
            case GAS_CLOUD, IDLE -> ContinuumRender.Body.GAS_CLOUD;
            case PROTOSTAR -> ContinuumRender.Body.PROTOSTAR;
            case STAR -> ContinuumRender.Body.STAR;
            case BLACK_HOLE -> ContinuumRender.Body.BLACK_HOLE;
        };
    }

    @Override
    public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        g.fill(0, 0, w, h, GuiDraw.withAlpha(T.bgBottom(), 0xF4));
        T.pattern(g, 0, 0, w, h);
        GuiDraw.drawBorder(g, 0, 0, w, h, T.line(), 1);
        g.drawString(font, "OBSERVATORY", 8, 7, T.accent(), false);
        if (!machine.isFormed()) {
            g.drawString(font, "Structure not formed.", 8, 24, T.dim(), false);
            return;
        }
        var stage = machine.stage();
        var anim = StellarForgeClient.animOf(machine);
        g.drawString(font, stage.label.toUpperCase(Locale.ROOT),
                w - 8 - font.width(stage.label.toUpperCase(Locale.ROOT)), 7,
                T.accent2(), false);
        g.fill(8, 19, w - 8, 20, T.line());

        g.fill(VX, VY, VX + VIEW, VY + VIEW, 0xFF_000004);
        if (stage == StellarForgeState.Stage.IDLE) {
            g.drawCenteredString(font, "Chamber empty", VX + VIEW / 2, VY + VIEW / 2 - 8, T.dim());
            g.drawCenteredString(font, "feed it gas to begin", VX + VIEW / 2, VY + VIEW / 2 + 4, T.dim());
        } else {
            float size01 = Mth.clamp((anim.radius - 0.15f) / 1.05f, 0.0f, 1.0f);
            int rgb = stage == StellarForgeState.Stage.STAR && machine.starTint() >= 0 ? machine.starTint() :
                    StellarForgeState.colorOf(machine.heat()) & 0xFFFFFF;
            boolean drawn = ContinuumRender.ready() &&
                    ContinuumRender.preview().draw(g, bodyOf(stage), rgb, size01, VX, VY, VIEW, false,
                            machine.holeTint());
            if (!drawn) fallback(g, VX + VIEW / 2, VY + VIEW / 2, 14 + size01 * 40, 0xFF_000000 | rgb);
        }
        frame(g, VX, VY, VIEW, VIEW);

        g.drawString(font, String.format(Locale.ROOT, "R %.2f", anim.radius), VX + 5, VY + VIEW - 12,
                GuiDraw.withAlpha(T.accent(), 0xC0), false);
        String hint = dragging ? "rotating" : "drag / scroll";
        g.drawString(font, hint, VX + VIEW - 5 - font.width(hint), VY + 5, GuiDraw.withAlpha(T.accent2(), 0xA0), false);

        int cx = VX + VIEW + 10;
        int cw = w - 8 - cx;
        int y = VY;
        y = vital(g, font, cx, y, cw, "MASS", String.format(Locale.ROOT, "%,.1f M", machine.mass()),
                (float) machine.massFraction(), 0xFF_44AAFF);
        var view = machine.view();
        y = vital(g, font, cx, y, cw, "HEAT", String.format(Locale.ROOT, "%,.0f K", view.kelvin()),
                machine.heat() / 100.0f, 0xFF_000000 | StellarForgeState.colorOf(machine.heat()));
        y = vital(g, font, cx, y, cw, "STABILITY", String.format(Locale.ROOT, "%.0f%%", machine.stability()),
                machine.stability() / 100.0f,
                machine.stability() > 66 ? 0xFF_33FF88 : machine.stability() > 33 ? 0xFF_FFC44D : 0xFF_FF3333);

        int by = VY + VIEW - 14;
        overResetNow = mouseX >= cx && mouseX < cx + cw && mouseY >= by && mouseY < by + 14;
        g.fill(cx, by, cx + cw, by + 14, overResetNow ? GuiDraw.mix(T.bgTop(), T.accent(), 0.2f) : T.bgTop());
        GuiDraw.drawBorder(g, cx, by, cw, 14, overResetNow ? T.accent() : GuiDraw.withAlpha(T.accent2(), 0xAA), 1);
        g.drawString(font, "RESET", cx + (cw - font.width("RESET")) / 2, by + 3, overResetNow ? T.accent() : T.dim(),
                false);

        int ty = VY + VIEW + 6;
        String note;
        if (mouseX >= VX && mouseX < VX + VIEW && mouseY >= VY && mouseY < VY + VIEW) {
            note = String.format(Locale.ROOT, "%s, %.2f blocks across, %,.0f K", stage.label, anim.radius * 2.0f,
                    view.kelvin());
        } else {
            note = "Settings are on the first tab.";
        }
        g.drawString(font, font.plainSubstrByWidth(note, w - 16), 8, ty, T.dim(), false);
    }

    private int vital(GuiGraphics g, Font font, int x, int y, int w, String label, String value, float frac,
                      int color) {
        g.drawString(font, label, x, y, T.dim(), false);
        g.drawString(font, font.plainSubstrByWidth(value, w), x, y + 10, color, false);
        g.fill(x, y + 22, x + w, y + 27, T.dim());
        g.fill(x, y + 22, x + (int) (w * Mth.clamp(frac, 0.0f, 1.0f)), y + 27, color);
        GuiDraw.drawBorder(g, x, y + 22, w, 5, GuiDraw.withAlpha(T.accent(), 0x55), 1);
        return y + 36;
    }

    private void frame(GuiGraphics g, int x, int y, int w, int h) {
        int c = T.accent();
        int len = 10;
        g.fill(x, y, x + len, y + 1, c);
        g.fill(x, y, x + 1, y + len, c);
        g.fill(x + w - len, y, x + w, y + 1, c);
        g.fill(x + w - 1, y, x + w, y + len, c);
        g.fill(x, y + h - 1, x + len, y + h, c);
        g.fill(x, y + h - len, x + 1, y + h, c);
        g.fill(x + w - len, y + h - 1, x + w, y + h, c);
        g.fill(x + w - 1, y + h - len, x + w, y + h, c);
        int faint = GuiDraw.withAlpha(c, 0x30);
        for (int i = 1; i < 8; i++) {
            int tx = x + w * i / 8;
            int ty = y + h * i / 8;
            g.fill(tx, y, tx + 1, y + (i == 4 ? 5 : 2), faint);
            g.fill(tx, y + h - (i == 4 ? 5 : 2), tx + 1, y + h, faint);
            g.fill(x, ty, x + (i == 4 ? 5 : 2), ty + 1, faint);
            g.fill(x + w - (i == 4 ? 5 : 2), ty, x + w, ty + 1, faint);
        }
    }

    private void fallback(GuiGraphics g, int cx, int cy, float r, int color) {
        for (int ring = (int) r; ring > 0; ring -= 3) {
            int a = (int) (0x18 + 0x60 * (1 - ring / r));
            for (int dy = -ring; dy <= ring; dy++) {
                int half = (int) Math.sqrt(ring * ring - dy * dy);
                g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, GuiDraw.withAlpha(color, a));
            }
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button == 0 && overResetNow) {
            ContinuumRender.preview().reset();
            return true;
        }
        if (button == 0 && mx >= VX && mx < VX + VIEW && my >= VY && my < VY + VIEW) {
            dragging = true;
            lastX = mx;
            lastY = my;
            return true;
        }
        return false;
    }

    @Override
    public void drag(double mx, double my, int button) {
        if (!dragging) return;
        ContinuumRender.preview().rotate((float) (mx - lastX), (float) (my - lastY));
        lastX = mx;
        lastY = my;
    }

    @Override
    public void release(int button) {
        dragging = false;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        if (mx >= VX && mx < VX + VIEW && my >= VY && my < VY + VIEW) {
            ContinuumRender.preview().zoom((float) amount);
            return true;
        }
        return false;
    }
}
