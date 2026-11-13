package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.AscensionRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

@OnlyIn(Dist.CLIENT)
public class AscensionRiteScreen extends RiteScreen {

    private static final String[] GAUGES = { "FLUX", "HEAT", "PLASMA" };
    private static final int[] COLORS = { 0x44AAFF, 0xFF8833, 0xC98CFF };

    public AscensionRiteScreen(S2CRitePacket p) {
        super(Capstone.ASCENSION_REACTOR, p, "rite_ascension");
    }

    @Override
    protected int accent() {
        return 0x00E5CC;
    }

    @Override
    protected int accent2() {
        return 0xFFC040;
    }

    @Override
    protected int bgTop() {
        return 0x02101A;
    }

    @Override
    protected int bgBottom() {
        return 0x010508;
    }

    @Override
    protected String introTitle() {
        return "ASCENSION";
    }

    @Override
    protected String introSub() {
        return "Hold three coupled loops in their bands.";
    }

    @Override
    protected String rules() {
        return "Flux, heat and plasma start at 50 and must stay between 35 and 65 after every turn, for twelve turns." +
                " Each turn a disturbance pulls on all three; the pull for this turn and the next are shown. Answer" +
                " with one move. Every move shifts all three gauges a little differently, so fixing one disturbs" +
                " another. Plan a turn ahead.";
    }

    private int turn() {
        return data[0];
    }

    private int g(int k) {
        return data[2 + k];
    }

    private int pull(int which, int k) {
        return data[7 + which * 3 + k];
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Turn", Math.min(turn() + 1, data[1]) + " of " + data[1], 0xFFEBD0, x, y, ui);
        for (int k = 0; k < 3; k++) {
            boolean safe = g(k) > AscensionRite.LO + 3 && g(k) < AscensionRite.HI - 3;
            stat(g, GAUGES[k], g(k) + "  (pull " + String.format("%+d", pull(0, k)) + ")",
                    safe ? 0xFFEBD0 : 0xFF8A70, x, y + 24 + 24 * k, ui);
        }
    }

    @Override
    protected void onData(S2CRitePacket p) {
        play(SoundEvents.BEACON_POWER_SELECT, 0.8f, 0.5f);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        int avail = width - panelW();
        int gh = (int) Math.min(height * 0.3f, 150f);
        int gw = 46;
        int gap = (int) Math.min(avail * 0.16f, 90f);
        int top = (int) (height * 0.17f);
        int range = 100;

        for (int k = 0; k < 3; k++) {
            int gx = (int) (cx() + (k - 1) * (gw + gap) - gw / 2f);

            g.fill(gx, top, gx + gw, top + gh, argb(0x04141C, 0.9f));
            int bandTop = top + gh - gh * AscensionRite.HI / range;
            int bandBot = top + gh - gh * AscensionRite.LO / range;
            g.fill(gx + 1, bandTop, gx + gw - 1, bandBot, argb(0x33FF88, 0.22f));
            g.renderOutline(gx, top, gw, gh, argb(0x00B8A5, 0.8f));
            int vy = top + gh - gh * Mth.clamp(g(k), 0, range) / range;
            boolean in = g(k) >= AscensionRite.LO && g(k) <= AscensionRite.HI;
            g.fill(gx - 4, vy - 1, gx + gw + 4, vy + 2, argb(in ? COLORS[k] : 0xFF4444, 1f));
            g.drawCenteredString(font, GAUGES[k], gx + gw / 2, top - 12, argb(COLORS[k], 0.95f));
            g.drawCenteredString(font, String.valueOf(g(k)), gx + gw / 2, top + gh + 4, argb(0xE0F8F8, 0.95f));

            for (int w2 = 0; w2 < 2; w2++) {
                int p = pull(w2, k);
                int ax = gx + gw + 8 + w2 * 12;
                int ay = top + gh / 2;
                int len = Math.abs(p) * 3;
                int col = p >= 0 ? 0xFF8A5A : 0x59B8FF;
                int a = w2 == 0 ? 255 : 120;
                g.fill(ax, p >= 0 ? ay - len : ay, ax + 3, p >= 0 ? ay : ay + len, (a << 24) | col);
                g.fill(ax - 1, ay, ax + 4, ay + 1, 0x88FFFFFF);
            }
        }
        g.drawCenteredString(font, "pull now, then next", (int) cx(), top + gh + 18, argb(0x7FA3AE, 0.9f));

        if (bound || seconds <= introSeconds()) return;
        int bw = 86;
        int bh = 24;
        int cols = 4;
        int bx0 = (int) (cx() - (cols * bw + (cols - 1) * 6) / 2f);
        int by0 = top + gh + 40;
        for (int i = 0; i < AscensionRite.MOVES; i++) {
            final int move = i;
            int px = bx0 + (i % cols) * (bw + 6);
            int py = by0 + (i / cols) * (bh + 6);
            boolean over = mx >= px && mx < px + bw && my >= py && my < py + bh;
            g.fill(px, py, px + bw, py + bh, over ? 0xFF0C3038 : 0xFF04161C);
            g.renderOutline(px, py, bw, bh, argb(0x00E5CC, over ? 1f : 0.6f));
            g.drawCenteredString(font, AscensionRite.NAMES[i], px + bw / 2, py + 4, argb(0xE0F8F8, 1f));
            int[] e = AscensionRite.EFFECT[i];
            g.drawCenteredString(font, String.format("%+d %+d %+d", e[0], e[1], e[2]), px + bw / 2, py + 14,
                    argb(0x7FA3AE, 0.95f));
            hits.add(new Hit(px, py, bw, bh, () -> send(AscensionRite.ACT, move)));
        }
        g.drawCenteredString(font, "each move shifts  flux  heat  plasma", (int) cx(), by0 + 2 * (bh + 6) + 2,
                argb(0x7FA3AE, 0.8f));
    }
}
