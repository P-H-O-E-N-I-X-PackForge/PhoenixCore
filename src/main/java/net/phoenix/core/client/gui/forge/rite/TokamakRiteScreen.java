package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.S2CRitePacket;
import net.phoenix.core.rebirth.rite.TokamakRite;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TokamakRiteScreen extends RiteScreen {

    private static final String[] FEED_NAMES = { "DEUTERIUM", "TRITIUM", "HELIUM-3" };
    private static final int[] FEED_COLOR = { 0x59E8FF, 0xFF8A5C, 0xC98CFF };

    private final int[] mix = { 4, 4, 4 };

    public TokamakRiteScreen(S2CRitePacket p) {
        super(Capstone.TOKAMAK, p, "rite_tokamak");
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
        return "IGNITION";
    }

    @Override
    protected String introSub() {
        return "Find the mix that lights the plasma.";
    }

    @Override
    protected String rules() {
        return "Set the three feeds from 0 to 9 and inject. The reactor does not say which way to turn a feed. It only" +
                " reports the confinement lost, which is how far the three feeds are from the true mix, added" +
                " together. Zero is ignition. Work it out from the losses; the field collapses after eight tries.";
    }

    private int tries() {
        return data.length > 0 ? data[0] : 0;
    }

    private int budget() {
        return data.length > 1 ? data[1] : TokamakRite.BUDGET;
    }

    private int lastLoss() {
        int t = tries();
        return t == 0 ? -1 : data[2 + (t - 1) * (TokamakRite.FEEDS + 1) + TokamakRite.FEEDS];
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Injections", tries() + " of " + budget(), tries() >= budget() - 1 ? 0xFF8A70 : 0xFFEBD0, x, y, ui);
        int ry = y + 28;
        g.drawString(font, "D  T  He   LOST", x, ry, argb(0x8C9CA8, ui), false);
        ry += 11;
        for (int t = 0; t < tries(); t++) {
            int base = 2 + t * (TokamakRite.FEEDS + 1);
            int loss = data[base + TokamakRite.FEEDS];
            String row = data[base] + "  " + data[base + 1] + "  " + data[base + 2] + "    " + loss;
            int col = loss == 0 ? 0x7CF0B0 : loss <= 3 ? 0xFFC040 : 0xFFEBD0;
            g.drawString(font, row, x, ry, argb(col, ui), false);
            ry += 10;
        }
    }

    @Override
    protected void onData(S2CRitePacket p) {
        int loss = lastLoss();
        play(SoundEvents.BEACON_POWER_SELECT, 0.6f + 0.6f * (1f - Mth.clamp(loss / 27f, 0f, 1f)), 0.8f);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        float loss = lastLoss();
        float heat = loss < 0 ? 0.15f : 1f - Mth.clamp(loss / TokamakRite.WORST, 0f, 1f);
        boolean lit = status == S2CRitePacket.BOUND || loss == 0;
        int hot = lit ? 0xFFFFFF : heat > 0.66f ? 0xFFE08A : heat > 0.33f ? 0xFF8A5C : 0x59A8FF;

        float R = Math.min((width - panelW()) * 0.28f, height * 0.2f);
        float tx = cx();
        float ty = height * 0.27f;

        BufferBuilder buf = begin();

        ring(buf, pose, tx, ty, R, 1.4f, 0f, Mth.TWO_PI, argb(0x2A9AA0, 0.8f));
        ring(buf, pose, tx, ty, R * 0.5f, 1.4f, 0f, Mth.TWO_PI, argb(0x2A9AA0, 0.8f));
        for (int i = 0; i < 12; i++) {
            float a = Mth.TWO_PI * i / 12f + 0.26f;
            float cx0 = tx + (float) Math.cos(a) * R * 1.12f;
            float cy0 = ty + (float) Math.sin(a) * R * 1.12f;
            float on = Mth.clamp(heat * 1.2f - 0.1f * (i % 3), 0f, 1f);
            disc(buf, pose, cx0, cy0, 5f, argb(on > 0.3f ? 0x00E5CC : 0x1A4A52, 0.4f + 0.6f * on), argb(0x00E5CC, 0f));
        }

        float mid = R * 0.75f;
        for (int k = 3; k >= 1; k--) {
            ring(buf, pose, tx, ty, mid, 1.5f + k * 2.2f * heat, seconds * (0.6f + 2f * heat), Mth.TWO_PI,
                    argb(hot, (lit ? 0.35f : 0.12f) * (0.4f + heat)));
        }
        ring(buf, pose, tx, ty, mid, 1.4f, seconds * 1.2f, Mth.TWO_PI * (0.25f + 0.7f * heat), argb(0xFFFFFF, 0.9f));
        int motes = 3 + (int) (heat * 9f);
        for (int i = 0; i < motes; i++) {
            float a = seconds * (0.9f + 0.1f * (i % 4)) * (1f + 2f * heat) + i * 2.399f;
            float jitter = (1f - heat) * 5f * (float) Math.sin(seconds * 7f + i);
            disc(buf, pose, tx + (float) Math.cos(a) * (mid + jitter), ty + (float) Math.sin(a) * (mid + jitter), 3.2f,
                    argb(0xFFFFFF, 0.95f), argb(hot, 0f));
        }
        end(buf);
        String reading = loss < 0 ? "cold plasma" : lit ? "IGNITION" :
                String.format(java.util.Locale.ROOT, "confinement lost: %d", (int) loss);
        g.drawCenteredString(font, reading, (int) tx, (int) (ty + R * 1.5f), argb(lit ? 0x7CF0B0 : 0xB8E8F0, 0.95f));

        if (bound) return;

        int colW = 74;
        int gap = 12;
        int total = 3 * colW + 2 * gap;
        int x0 = (int) (cx() - total / 2f);
        int fy = (int) (height * 0.54f);
        for (int i = 0; i < 3; i++) {
            int fx = x0 + i * (colW + gap);
            int color = FEED_COLOR[i];
            g.fill(fx, fy, fx + colW, fy + 62, 0xAA_020A10);
            border(g, fx, fy, colW, 62, argb(color, 0.7f));
            g.drawCenteredString(font, FEED_NAMES[i], fx + colW / 2, fy + 5, argb(color, 0.95f));
            g.drawCenteredString(font, String.valueOf(mix[i]), fx + colW / 2, fy + 25, 0xFF_FFFFFF);
            for (int k = 0; k < 10; k++) {
                int px = fx + 5 + k * 6;
                int hgt = 3 + k;
                g.fill(px, fy + 56 - hgt, px + 4, fy + 56, k <= mix[i] ? argb(color, 0.95f) : argb(color, 0.2f));
            }
            final int idx = i;
            if (seconds > introSeconds()) {
                button2(g, fx + 4, fy + 20, 16, 16, "-", mx, my, () -> mix[idx] = Math.max(0, mix[idx] - 1));
                button2(g, fx + colW - 20, fy + 20, 16, 16, "+", mx, my,
                        () -> mix[idx] = Math.min(TokamakRite.MAX, mix[idx] + 1));
            }
        }
        if (seconds > introSeconds()) {
            int iw = 120;
            int ix = (int) (cx() - iw / 2f);
            int iy = fy + 72;
            button2(g, ix, iy, iw, 18, "INJECT", mx, my, () -> send(TokamakRite.INJECT, mix[0], mix[1], mix[2]));
        }
    }

    private void button2(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, Runnable action) {
        boolean over = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, over ? 0xFF_0C3038 : 0xFF_04161C);
        border(g, x, y, w, h, over ? 0xFF_00E5CC : 0xAA_00A8A0);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, over ? 0xFF_FFFFFF : 0xFF_BFEFF2);
        hits.add(new Hit(x, y, w, h, action));
    }

    private static void border(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }
}
