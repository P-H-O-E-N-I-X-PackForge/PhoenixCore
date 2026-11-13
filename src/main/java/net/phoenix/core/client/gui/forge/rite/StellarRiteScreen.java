package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.S2CRitePacket;
import net.phoenix.core.rebirth.rite.StellarRite;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class StellarRiteScreen extends RiteScreen {

    private static final String[] ACTIONS = { "HYDROGEN", "HELIUM", "NEUTRONIUM", "VENT" };
    private static final String[] EFFECTS = { "+2 mass  +2 heat", "+3 mass  +3 heat", "+6 mass  +5 heat",
            "-3 heat" };
    private static final int[] ACTION_COLOR = { 0x8CC8FF, 0xFFD27A, 0xC98CFF, 0x7CF0B0 };

    public StellarRiteScreen(S2CRitePacket p) {
        super(Capstone.STELLAR_FORGE, p, "rite_stellar");
    }

    @Override
    protected int accent() {
        return 0xFFB347;
    }

    @Override
    protected int accent2() {
        return 0x8CA8FF;
    }

    @Override
    protected int bgTop() {
        return 0x02030A;
    }

    @Override
    protected int bgBottom() {
        return 0x1A0E08;
    }

    @Override
    protected String introTitle() {
        return "THE NURSERY";
    }

    @Override
    protected String introSub() {
        return "Raise a star, and let it fall.";
    }

    @Override
    protected String rules() {
        return "Each turn add fuel or vent heat. Mass has to climb through three thresholds: protostar, star and" +
                " black hole. As mass crosses each one the core's heat must sit in that stage's green band, or it" +
                " scatters. Heat also falls by one every turn, and above nine the core flares apart. You have" +
                " twelve turns.";
    }

    private int mass() {
        return data[0];
    }

    private int heat() {
        return data[1];
    }

    private int turn() {
        return data[2];
    }

    private int stage() {
        return data[4];
    }

    private int threshold(int s) {
        return data[5 + s];
    }

    private int lo(int s) {
        return data[8 + 2 * s];
    }

    private int hi(int s) {
        return data[9 + 2 * s];
    }

    private int hmax() {
        return data[14];
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Turn", Math.min(turn() + 1, data[3]) + " of " + data[3], turn() >= data[3] - 2 ? 0xFF8A70 : 0xFFEBD0,
                x, y, ui);
        stat(g, "Mass", mass() + " (next " + (stage() < 3 ? threshold(stage()) : "-") + ")", 0xFFEBD0, x, y + 24, ui);
        int s = Math.min(stage(), 2);
        boolean in = heat() >= lo(s) && heat() <= hi(s);
        stat(g, "Heat", heat() + " (band " + lo(s) + " to " + hi(s) + ")", in ? 0x7CF0B0 : 0xFFC040, x, y + 48, ui);
        stat(g, "Next stage", Capstone.STELLAR_FORGE == cap ? StellarRite.STAGE_NAMES[s] : "", 0xB8A890, x, y + 72,
                ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        play(SoundEvents.FIRECHARGE_USE, 0.8f + 0.1f * p.data()[4], 0.6f);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        int st = stage();
        float tx = cx();
        float ty = height * 0.27f;
        float R = Math.min((width - panelW()) * 0.18f, height * 0.16f);
        float heatF = Mth.clamp(heat() / (float) hmax(), 0f, 1f);

        BufferBuilder buf = begin();
        int rgb = heatF > 0.66f ? 0xFFE08A : heatF > 0.33f ? 0xFF9A4A : 0xFF6A3A;
        if (st == 0) {

            for (int i = 0; i < 14; i++) {
                float a = i * 2.399f + seconds * 0.2f;
                float d = R * (0.2f + 0.7f * ((i * 0.618f) % 1f));
                disc(buf, pose, tx + (float) Math.cos(a) * d, ty + (float) Math.sin(a) * d * 0.8f, R * 0.4f,
                        argb(0x8CA8FF, 0.16f), argb(0x8CA8FF, 0f));
            }
            disc(buf, pose, tx, ty, R * (0.2f + 0.4f * heatF), argb(rgb, 0.5f), argb(rgb, 0f));
        } else if (st == 1) {
            disc(buf, pose, tx, ty, R * 1.3f, argb(rgb, 0.3f), argb(rgb, 0f));
            disc(buf, pose, tx, ty, R * 0.55f, argb(0xFFF4E0, 0.95f), argb(rgb, 0.7f));
            rays(buf, pose, tx, ty, R * 0.6f, R * 1.4f, 8, seconds * 0.2f, 8f, rgb, 0.4f);
        } else if (st == 2) {
            disc(buf, pose, tx, ty, R * 1.6f, argb(0xFFE08A, 0.35f), argb(0xFFE08A, 0f));
            disc(buf, pose, tx, ty, R * 0.8f, argb(0xFFFFFF, 1f), argb(0xFFE08A, 0.9f));
            rays(buf, pose, tx, ty, R * 0.8f, R * 2.0f, 12, seconds * 0.3f, 10f, 0xFFE08A, 0.45f);
        } else {
            disc(buf, pose, tx, ty, R * 0.5f, argb(0x000000, 1f), argb(0x000000, 1f));
            ring(buf, pose, tx, ty, R * 0.7f, 2.5f, seconds * 2f, Mth.TWO_PI, argb(0xFFB347, 0.8f));
        }
        end(buf);

        int gw = (int) Math.min((width - panelW()) * 0.7f, 420f);
        int gx = (int) (cx() - gw / 2f);
        int my0 = (int) (height * 0.48f);

        g.drawString(font, "MASS", gx, my0 - 11, argb(0xB8A890, 0.9f), false);
        int top = threshold(2) + 4;
        g.fill(gx, my0, gx + gw, my0 + 6, 0xFF_1A1410);
        g.fill(gx, my0, gx + (int) (gw * Mth.clamp(mass() / (float) top, 0f, 1f)), my0 + 6, argb(0xFFB347, 0.9f));
        for (int s = 0; s < 3; s++) {
            int tx0 = gx + (int) (gw * threshold(s) / (float) top);
            g.fill(tx0, my0 - 4, tx0 + 1, my0 + 10, 0xFF_FFE08A);
            String nm = StellarRite.STAGE_NAMES[s];
            g.drawString(font, nm, tx0 - font.width(nm) / 2, my0 + 12, argb(0xFFE08A, 0.8f), false);
        }
        border(g, gx, my0, gw, 6, argb(0xFFB347, 0.6f));

        int hy = my0 + 38;
        g.drawString(font, "CORE HEAT", gx, hy - 11, argb(0xB8A890, 0.9f), false);
        g.fill(gx, hy, gx + gw, hy + 8, 0xFF_1A1410);
        int s = Math.min(st, 2);
        float cell = gw / (float) (hmax() + 1);
        g.fill(gx + (int) (lo(s) * cell), hy, gx + (int) ((hi(s) + 1) * cell), hy + 8, argb(0x7CF0B0, 0.4f));
        for (int i = 0; i <= hmax(); i++) g.fill(gx + (int) (i * cell), hy, gx + (int) (i * cell) + 1, hy + 8,
                0x55_FFFFFF);
        g.fill(gx + (int) (heat() * cell), hy - 3, gx + (int) ((heat() + 1) * cell), hy + 11,
                heat() >= lo(s) && heat() <= hi(s) ? argb(0x7CF0B0, 0.9f) : argb(0xFF8A5A, 0.9f));
        border(g, gx, hy, gw, 8, argb(0xFFB347, 0.6f));

        if (bound || seconds <= introSeconds()) return;
        int bw = 92;
        int gap = 8;
        int total = 4 * bw + 3 * gap;
        int bx = (int) (cx() - total / 2f);
        int by = hy + 34;
        for (int i = 0; i < 4; i++) {
            final int choice = i;
            int px = bx + i * (bw + gap);
            boolean over = mx >= px && mx < px + bw && my >= by && my < by + 34;
            g.fill(px, by, px + bw, by + 34, over ? 0xFF_2A1A0E : 0xFF_120A06);
            border(g, px, by, bw, 34, argb(ACTION_COLOR[i], over ? 1f : 0.65f));
            g.drawCenteredString(font, ACTIONS[i], px + bw / 2, by + 6, argb(ACTION_COLOR[i], 1f));
            g.drawCenteredString(font, EFFECTS[i], px + bw / 2, by + 20, argb(0xE8D8C0, 0.85f));
            hits.add(new Hit(px, by, bw, 34, () -> send(StellarRite.ACT, choice)));
        }
    }

    private static void border(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }
}
