package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.HoneyRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class HoneyRiteScreen extends RiteScreen {

    private static final float FLASH = 0.55f;
    private static final float GAP = 0.25f;

    private int[] playing = new int[0];
    private float playStart = -1f;
    private final float[] glow = new float[HoneyRite.CELLS];
    private final float[] press = new float[HoneyRite.CELLS];

    public HoneyRiteScreen(S2CRitePacket p) {
        super(Capstone.HONEY, p, "rite_honey");
    }

    @Override
    protected int accent() {
        return 0xFFB52E;
    }

    @Override
    protected int accent2() {
        return 0xFF7A1A;
    }

    @Override
    protected int bgTop() {
        return 0x0C0602;
    }

    @Override
    protected int bgBottom() {
        return 0x2A1604;
    }

    @Override
    protected String introTitle() {
        return "THE COMB";
    }

    @Override
    protected String introSub() {
        return "Remember what the comb remembers.";
    }

    @Override
    protected String rules() {
        return "The comb lights a run of cells. Press Show, watch it, and repeat the run by clicking the cells in the" +
                " same order. It will only show the run a few times, and a wrong cell crumbles the comb back to the" +
                " start of the run.";
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Shows left", Integer.toString(data[0]), data[0] > 0 ? 0xFFEBD0 : 0xFF8A70, x, y, ui);
        stat(g, "Run length", Integer.toString(data[2]), 0xFFEBD0, x, y + 24, ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        if (p.extra().length == HoneyRite.LENGTH) {
            playing = p.extra();
            playStart = seconds + 0.5f;
        }
    }

    private boolean playingNow() {
        return playStart >= 0f && seconds < playStart + playing.length * (FLASH + GAP);
    }

    private float cellR() {
        return Math.min(width, height) * 0.075f;
    }

    private float[] cellPos(int i) {
        if (i == 0) return new float[] { cx(), cy() };
        double a = Math.toRadians(-90 + 60 * (i - 1));
        float d = cellR() * 1.82f;
        return new float[] { cx() + (float) Math.cos(a) * d, cy() + (float) Math.sin(a) * d };
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();

        int lit = -1;
        if (playStart >= 0f && seconds >= playStart) {
            float t = seconds - playStart;
            int idx = (int) (t / (FLASH + GAP));
            if (idx < playing.length && t - idx * (FLASH + GAP) < FLASH) lit = playing[idx];
        }
        for (int i = 0; i < HoneyRite.CELLS; i++) {
            float target = i == lit ? 1f : 0f;
            glow[i] += (target - glow[i]) * Math.min(1f, dt * 14f);
            press[i] = Math.max(0f, press[i] - dt * 2.5f);
        }
        float r = cellR();
        float sig = Mth.clamp(data[1] / (float) HoneyRite.LENGTH, 0f, 1f);

        BufferBuilder buf = begin();

        rays(buf, pose, cx(), cy(), r * 1.2f, r * 6f, 12, seconds * 0.04f, 18f, 0xFFB52E, 0.03f + 0.07f * sig);
        disc(buf, pose, cx(), cy(), r * 4.2f, argb(0xFFB52E, 0.08f + 0.1f * sig), argb(0xFFB52E, 0f));
        for (int i = 0; i < HoneyRite.CELLS; i++) {
            float[] p = cellPos(i);
            float gl = Math.max(glow[i], press[i] * 0.8f);
            float hover = hovered(mx, my) == i && !playingNow() && !bound ? 0.18f : 0f;
            polyFill(buf, pose, p[0], p[1], r, 6, 0f, argb(0xFFD25A, 0.1f + 0.7f * gl + hover),
                    argb(0xFF9A1A, 0.05f + 0.45f * gl + hover));
            polygon(buf, pose, p[0], p[1], r, 6, 0f, 1.3f, argb(0xFFB52E, 0.55f + 0.4f * gl));
            polygon(buf, pose, p[0], p[1], r * 0.82f, 6, 0f, 0.6f, argb(0xFFB52E, 0.2f + 0.3f * gl));
            if (gl > 0.05f) disc(buf, pose, p[0], p[1], r * 1.4f, argb(0xFFD25A, 0.22f * gl), argb(0xFFB52E, 0f));
            if (!bound && seconds > introSeconds() && !playingNow()) {
                final int cell = i;
                hits.add(new Hit((int) (p[0] - r), (int) (p[1] - r), (int) (r * 2), (int) (r * 2), () -> {
                    press[cell] = 1f;
                    play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f + cell * 0.12f, 0.8f);
                    send(HoneyRite.PICK, cell);
                }));
            }
        }

        for (int i = 0; i < 50; i++) {
            float a = noise(i, 1f) * Mth.TWO_PI;
            float d = r * (1.2f + 3.6f * ((seconds * (0.04f + 0.1f * noise(i, 2f)) + noise(i, 3f)) % 1f));
            quad(buf, pose, cx() + (float) Math.cos(a + seconds * 0.1f) * d,
                    cy() + (float) Math.sin(a + seconds * 0.1f) * d,
                    1f + noise(i, 4f) * 1.4f, argb(0xFFD25A, 0.4f));
        }
        end(buf);

        buf = begin();
        float pw = 14f;
        float total = HoneyRite.LENGTH * pw;
        float py = cy() - r * 3.9f;
        for (int i = 0; i < HoneyRite.LENGTH; i++) {
            float x = cx() - total / 2f + (i + 0.5f) * pw;
            boolean done = i < data[1];
            polyFill(buf, pose, x, py, 4.5f, 6, 0f, argb(0xFFD25A, done ? 0.95f : 0.1f),
                    argb(0xFF9A1A, done ? 0.7f : 0.05f));
            polygon(buf, pose, x, py, 5f, 6, 0f, 0.5f, argb(0xFFB52E, 0.6f));
        }
        end(buf);

        boolean can = data[0] > 0 && !playingNow() && !bound;
        button(g, (int) (cx() - 50), (int) (cy() + r * 4.1f), 100, 18, "SHOW THE RUN", mx, my, can, uiAlpha(),
                () -> send(HoneyRite.SHOW));
    }

    private int hovered(int mx, int my) {
        float r = cellR();
        for (int i = 0; i < HoneyRite.CELLS; i++) {
            float[] p = cellPos(i);
            float dx = mx - p[0];
            float dy = my - p[1];
            if (dx * dx + dy * dy < r * r) return i;
        }
        return -1;
    }
}
