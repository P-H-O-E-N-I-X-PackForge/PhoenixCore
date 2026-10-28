package net.phoenix.core.client.stellar.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.phoenix.core.client.gui.hud.CanvasPainter;
import net.phoenix.core.client.gui.hud.GuiDraw;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import java.util.Locale;

/** The rules the forge follows, with the numbers it is using right now put in. Scroll for more. */
public final class StellarMathPage implements CanvasPainter {

    private static final int CONTENT_HEIGHT = 330;

    private final StellarForgeMachine machine;
    private int scroll = 0;

    public StellarMathPage(StellarForgeMachine machine) {
        this.machine = machine;
    }

    @Override
    public void render(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        int maxScroll = Math.max(0, CONTENT_HEIGHT - h);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        g.fill(0, 0, w, h, 0xEE_010810);
        GuiDraw.drawBorder(g, 0, 0, w, h, 0x33_00E5CC, 1);
        GuiDraw.enableScissor(g, 0, 0, w, h);
        g.pose().pushPose();
        g.pose().translate(0, -scroll, 0);
        paint(g, w - 10);
        g.pose().popPose();
        if (maxScroll > 0) {
            int thumbH = Math.max(12, (int) (h * (h / (double) CONTENT_HEIGHT)));
            int thumbY = (int) ((h - thumbH) * (scroll / (double) maxScroll));
            g.fill(w - 4, 0, w, h, 0x33_00E5CC);
            g.fill(w - 4, thumbY, w, thumbY + thumbH, 0xAA_00E5CC);
        }
        g.disableScissor();
    }

    @Override
    public boolean scroll(double mouseX, double mouseY, double amount) {
        scroll -= (int) (amount * 12);
        return true;
    }

    private int textWidth = 240;

    private int line(GuiGraphics g, Font font, int x, int y, String text, int color) {
        return GuiDraw.drawWrapped(g, font, text, x, y, textWidth - (x - 8), color);
    }

    private int heading(GuiGraphics g, Font font, int x, int y, int w, String text) {
        g.fill(x, y, x + w, y + 1, 0x33_00E5CC);
        g.fill(x, y + 3, x + 3, y + 11, 0xFF_00AAA0);
        g.drawString(font, text, x + 7, y + 3, StellarHudPage.C_CYAN, false);
        return y + 15;
    }

    private void paint(GuiGraphics g, int w) {
        Font font = Minecraft.getInstance().font;
        if (!machine.isFormed()) {
            g.drawString(font, "Structure not formed.", 8, 8, StellarHudPage.C_DIM, false);
            return;
        }
        textWidth = w - 24;
        StellarForgeState s = machine.view();
        boolean ok = machine.powerOk();
        int y = 6;
        int x = 8;
        int dim = StellarHudPage.C_DIM;
        int mid = StellarHudPage.C_MID;
        int white = StellarHudPage.C_WHITE;
        int gold = StellarHudPage.C_GOLD;

        y = heading(g, font, x, y, w - 16, "STAGE " + s.stage.label.toUpperCase(Locale.ROOT));
        y = line(g, font, x, y, String.format(Locale.ROOT, "mass %,.2f M   next at %s", s.mass,
                s.stage == StellarForgeState.Stage.BLACK_HOLE ? "-" : String.format(Locale.ROOT, "%,.0f M", s.nextMass())),
                mid);
        y = line(g, font, x, y, String.format(Locale.ROOT, "hold %d / %d ticks   fail %d / %d ticks", s.holdTicks,
                StellarForgeState.HOLD_TICKS_TO_ADVANCE, s.failTicks, StellarForgeState.FAIL_TICKS_TO_REGRESS), mid);
        y += 4;

        y = heading(g, font, x, y, w - 16, "BANDS (move with mass)");
        var fb = s.fieldBand();
        var sb = s.spinBand();
        var hb = s.heatBand();
        y = line(g, font, x, y, String.format(Locale.ROOT, "field  %d%%  in [%.0f, %.0f]", s.field, fb.low(), fb.high()),
                fb.contains(s.field) ? StellarHudPage.C_GREEN : gold);
        y = line(g, font, x, y, String.format(Locale.ROOT, "spin   %d%%  in [%.0f, %.0f]", s.spin, sb.low(), sb.high()),
                sb.contains(s.spin) ? StellarHudPage.C_GREEN : gold);
        y = line(g, font, x, y, String.format(Locale.ROOT, "heat   %.0f%%  in [%.0f, %.0f]", s.heat, hb.low(), hb.high()),
                hb.contains(s.heat) ? StellarHudPage.C_GREEN : gold);
        y = line(g, font, x, y, "(every run shifts and widens the bands, and they drift as it goes)", dim);
        y += 4;

        y = heading(g, font, x, y, w - 16, "HEAT");
        y = line(g, font, x, y, "heat -> target, 2% a tick", dim);
        y = line(g, font, x, y, "target = mid(band) * (0.5 + 0.5 * mass/next)", mid);
        y = line(g, font, x, y, "       + 0.4 (feed - 50) + 0.3 (spin - spin mid)", mid);
        y = line(g, font, x, y, "       - 0.4 (field - field mid)", mid);
        y = line(g, font, x, y, String.format(Locale.ROOT, "target now = %.1f   heat = %.1f", s.heatTarget(ok), s.heat),
                white);
        y += 4;

        y = heading(g, font, x, y, w - 16, "STABILITY");
        y = line(g, font, x, y, "stability -> target, 5% a tick", dim);
        y = line(g, font, x, y, "target = 100 (1 - (missField + missSpin + missHeat) / 2)", mid);
        y = line(g, font, x, y, String.format(Locale.ROOT, "missField %.2f  missSpin %.2f  missHeat %.2f",
                s.missField(ok), s.missSpin(), s.missHeat()), mid);
        y = line(g, font, x, y, String.format(Locale.ROOT, "target now = %.1f   stability = %.1f",
                s.stabilityTarget(ok, false), s.stability), white);
        y = line(g, font, x, y, "no power caps it at 30, a full output hatch at 60", dim);
        y += 4;

        y = heading(g, font, x, y, w - 16, "ENERGY AND FEED");
        y = line(g, font, x, y, "EU/t = (256 + 40 field + 20 spin + 12 feed) * 2^(stage-1)", mid);
        y = line(g, font, x, y, String.format(Locale.ROOT, "= %,d EU/t  (drawing %,d)", s.energyPerTick(),
                machine.energyUse()), white);
        y = line(g, font, x, y, String.format(Locale.ROOT, "feed = %.0f mB/t max * %d%% = %.1f mB/t",
                s.maxFeedPerTick(), s.feed, s.feedPerTick()), white);
        y = line(g, font, x, y, "hydrogen 0.01 M/mB, helium 0.02, neutronium 1.0", dim);
        y += 4;

        y = heading(g, font, x, y, w - 16, "OUTPUT AND SIZE");
        y = line(g, font, x, y, "plasma = clamp(mass / 50, 1, 64) * stability   (x2 black hole)", mid);
        y = line(g, font, x, y, String.format(Locale.ROOT, "= %.1f mB/t of oxygen plasma", s.plasmaPerTick()), white);
        y = line(g, font, x, y, String.format(Locale.ROOT, "size %.2f blocks, temperature %,.0f K", s.radius(), s.kelvin()),
                white);
        y += 4;

        y = heading(g, font, x, y, w - 16, "FALLING IN");
        boolean direct = (s.stage == StellarForgeState.Stage.GAS_CLOUD || s.stage == StellarForgeState.Stage.PROTOSTAR);
        y = line(g, font, x, y, String.format(Locale.ROOT, "direct: mass >= %,.0f M, field >= 85, feed >= 80", StellarForgeState.DIRECT_COLLAPSE),
                direct ? white : dim);
        y = line(g, font, x, y, String.format(Locale.ROOT, "star: mass >= %,.0f M, field >= 70, armed", StellarForgeState.STAR_TO_HOLE),
                s.stage == StellarForgeState.Stage.STAR ? white : dim);
    }
}
