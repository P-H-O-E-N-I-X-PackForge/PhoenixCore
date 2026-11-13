package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.client.nullwell.NullWellArt;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.NullWellRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class NullWellRiteScreen extends RiteScreen {

    private static final String[] LABELS = { "-9", "-6", "-3", "0", "+3", "+6", "+9" };

    public NullWellRiteScreen(S2CRitePacket p) {
        super(Capstone.NULL_WELL, p, "rite_null_well");
    }

    @Override
    protected int accent() {
        return 0xC98CFF;
    }

    @Override
    protected int accent2() {
        return 0x7FE6FF;
    }

    @Override
    protected int bgTop() {
        return 0x030108;
    }

    @Override
    protected int bgBottom() {
        return 0x140A24;
    }

    @Override
    protected String introTitle() {
        return "THE HORIZON";
    }

    @Override
    protected String introSub() {
        return "Hold the hole where it is.";
    }

    @Override
    protected String rules() {
        return "The hole sits at 50. Every turn it is pulled by a force the Well has told you about: the first pull" +
                " shown is the one that lands now. Feed it or draw it down, in steps of three, to answer the pull" +
                " and keep its mass inside the band. The band narrows every turn. Hold it for twelve turns, or the" +
                " field slips.";
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Turn", Math.min(data[1] + 1, data[2]) + " of " + data[2], 0xFFEBD0, x, y, ui);
        stat(g, "Mass", data[0] + " (hold " + (NullWellRite.MID - data[3]) + " to " + (NullWellRite.MID + data[3]) +
                ")", Math.abs(data[0] - NullWellRite.MID) >= data[3] - 1 ? 0xFF8A70 : 0xFFEBD0, x, y + 24, ui);
        stat(g, "Pull now", signed(data[4]), 0xFFEBD0, x, y + 48, ui);
        stat(g, "Pull after", data[1] + 1 < data[2] ? signed(data[5]) : "none", 0xB8A890, x, y + 72, ui);
    }

    private static String signed(int v) {
        return String.format(Locale.ROOT, "%+d", v);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f, 0.5f);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        int size = (int) Math.min((width - panelW()) * 0.55f, height * 0.5f);
        int x = (int) (cx() - size / 2f);
        int y = (int) (cy() - size / 2f - 14);
        int mass = data[0];
        int half = data[3];
        float R = size / 2.0f - 3.0f;

        float move = data[7];
        float feed = Math.max(0, move) / 9.0f;
        float draw = Math.max(0, -move) / 9.0f;
        float stability = 100.0f -
                100.0f * Math.min(1.0f, Math.abs(mass - NullWellRite.MID) / (float) Math.max(1, half));
        NullWellArt.draw(g, x, y, size, mass / 100.0f, 0.5f, Math.max(5.0f, stability), feed, draw, true);

        NullWellArt.markRing(g, (int) cx(), y + size / 2, R, (NullWellRite.MID - half) / 100.0f,
                0xC0_7CF0B0);
        NullWellArt.markRing(g, (int) cx(), y + size / 2, R, (NullWellRite.MID + half) / 100.0f,
                0xC0_7CF0B0);

        int pullNow = data[4];
        int ax = x + size + 10;
        int ay = y + size / 2;
        g.fill(ax, ay, ax + 3, ay + 1, 0xFF_F0E8FF);
        int len = Math.abs(pullNow) * 5;
        int dir = pullNow >= 0 ? 1 : -1;
        g.fill(ax + 1, ay - (dir > 0 ? len : 0), ax + 3, ay + (dir > 0 ? 0 : len), pullNow >= 0 ? 0xFF_FF8A70 :
                0xFF_7FE6FF);
        g.drawString(font, signed(pullNow), ax - 2, ay + (dir > 0 ? 6 : -len - 14), 0xFF_F0E8FF, false);

        if (bound || seconds <= introSeconds()) return;
        int bw = 30;
        int gap = 6;
        int total = NullWellRite.CHOICES * bw + (NullWellRite.CHOICES - 1) * gap;
        int bx = (int) (cx() - total / 2f);
        int by = y + size + 10;
        for (int i = 0; i < NullWellRite.CHOICES; i++) {
            final int choice = i;
            int px = bx + i * (bw + gap);
            boolean over = mx >= px && mx < px + bw && my >= by && my < by + 16;
            int col = i < 3 ? 0x7FE6FF : i > 3 ? 0xFF8A70 : 0xC8B8E8;
            g.fill(px, by, px + bw, by + 16, over ? 0xFF_261340 : 0xFF_0E0618);
            drawBorder(g, px, by, bw, 16, over ? col : (col & 0xFFFFFF) | 0xAA000000);
            g.drawCenteredString(font, LABELS[i], px + bw / 2, by + 4, col | 0xFF000000);
            hits.add(new Hit(px, by, bw, 16, () -> send(NullWellRite.ACT, choice)));
        }
        g.drawCenteredString(font, "draw  <  >  feed", (int) cx(), by + 22, 0xFF_8C7AAE);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }
}
