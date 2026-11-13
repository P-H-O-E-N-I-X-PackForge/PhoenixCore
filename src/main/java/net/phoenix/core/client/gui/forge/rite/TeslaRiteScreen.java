package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.S2CRitePacket;
import net.phoenix.core.rebirth.rite.TeslaRite;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TeslaRiteScreen extends RiteScreen {

    private static final String[] NAMES = { "FREQUENCY", "VOLTAGE", "PHASE" };

    private final float[] dial = { 50, 50, 50 };
    private int signal;
    private int dragging = -1;
    private float probeTimer;
    private boolean dirty = true;

    public TeslaRiteScreen(S2CRitePacket p) {
        super(Capstone.TESLA, p, "rite_tesla");
    }

    @Override
    protected int accent() {
        return 0x59D8FF;
    }

    @Override
    protected int accent2() {
        return 0xA070FF;
    }

    @Override
    protected int bgTop() {
        return 0x02060C;
    }

    @Override
    protected int bgBottom() {
        return 0x08142A;
    }

    @Override
    protected String introTitle() {
        return "RESONANCE";
    }

    @Override
    protected String introSub() {
        return "Find the setting where the tower rings.";
    }

    @Override
    protected String rules() {
        return "Three dials, one signal. The dials pull on each other, so a setting that was right stops being right" +
                " when another dial moves. Drag a dial, or scroll over it, until the signal is nearly full, then lock.";
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Signal", signal / 10 + "%", signal >= TeslaRite.LOCK_SIGNAL ? 0x9CF0FF : 0xFFEBD0, x, y, ui);
        stat(g, "Lock needs", TeslaRite.LOCK_SIGNAL / 10 + "%", 0xFFEBD0, x, y + 24, ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        if (p.extra().length > 0) signal = p.extra()[0];
    }

    private float dialX(int i) {
        return cx() + (i - 1) * Math.min(width * 0.16f, 150f);
    }

    private float dialY() {
        return height * 0.72f;
    }

    private float dialR() {
        return Math.min(width, height) * 0.065f;
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        float sig = Mth.clamp(signal / 1000f, 0f, 1f);
        float sw = Math.min(width * 0.46f, 520f);
        float sh = height * 0.3f;
        float sx = cx();
        float sy = height * 0.35f;

        if (!bound && dirty && seconds > introSeconds()) {
            probeTimer -= dt;
            if (probeTimer <= 0f) {
                probeTimer = 0.1f;
                dirty = false;
                send(TeslaRite.PROBE, Math.round(dial[0]), Math.round(dial[1]), Math.round(dial[2]));
            }
        }

        BufferBuilder buf = begin();
        rays(buf, pose, sx, sy, sh * 0.5f, sh * 2.4f, 18, seconds * 0.05f, 9f + 10f * sig, 0x59D8FF,
                0.025f + 0.07f * sig);

        float x0 = sx - sw / 2;
        float x1 = sx + sw / 2;
        float y0 = sy - sh / 2;
        float y1 = sy + sh / 2;
        int frame = argb(0x59D8FF, 0.55f + 0.3f * sig);
        line(buf, pose, x0, y0, x1, y0, 1f, frame);
        line(buf, pose, x1, y0, x1, y1, 1f, frame);
        line(buf, pose, x1, y1, x0, y1, 1f, frame);
        line(buf, pose, x0, y1, x0, y0, 1f, frame);
        for (int i = 1; i < 8; i++) {
            float gx = Mth.lerp(i / 8f, x0, x1);
            line(buf, pose, gx, y0, gx, y1, 0.4f, argb(0x59D8FF, 0.08f));
        }
        for (int i = 1; i < 4; i++) {
            float gy = Mth.lerp(i / 4f, y0, y1);
            line(buf, pose, x0, gy, x1, gy, 0.4f, argb(0x59D8FF, i == 2 ? 0.16f : 0.08f));
        }

        int n = 140;
        float amp = sh * 0.36f;
        float px = x0;
        float py = sy;
        float noiseAmp = (1f - sig) * (1f - sig);
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float x = Mth.lerp(t, x0, x1);
            float carrier = (float) Math.sin(t * Mth.TWO_PI * 4f - seconds * 5f);
            float detune = (float) Math.sin(t * Mth.TWO_PI * (4f + 7f * noiseAmp) + seconds * 3f + dial[2] * 0.1f) *
                    noiseAmp;
            float jag = (noise(i, (float) Math.floor(seconds * 24f)) - 0.5f) * 2f * noiseAmp;
            float y = sy + (carrier * (0.35f + 0.65f * sig) * 0.7f + detune * 0.6f + jag * 0.8f) * amp;
            if (i > 0) {
                line(buf, pose, px, py, x, y, 2.4f, argb(0x59D8FF, 0.16f));
                line(buf, pose, px, py, x, y, 0.8f, argb(sig > 0.95f ? 0xD8FCFF : 0x8CE8FF, 0.95f));
            }
            px = x;
            py = y;
        }

        long frameNo = (long) (seconds * 12f);
        int count = (int) (sig * 6f);
        for (int i = 0; i < count; i++) {
            float bx = Mth.lerp(noise(i, frameNo * 0.5f), x0, x1);
            bolt(buf, pose, bx, i % 2 == 0 ? y0 : y1, bx + (noise(i, 9f) - 0.5f) * 80f,
                    i % 2 == 0 ? y0 - 70f : y1 + 70f, 14f, frameNo * 7L + i, 0x59D8FF, 0.7f);
        }
        end(buf);

        buf = begin();
        float r = dialR();
        for (int i = 0; i < 3; i++) {
            float x = dialX(i);
            float y = dialY();
            float v = dial[i] / TeslaRite.RANGE;
            boolean active = dragging == i;
            disc(buf, pose, x, y, r * 1.5f, argb(0x59D8FF, active ? 0.18f : 0.08f), argb(0x59D8FF, 0f));
            ring(buf, pose, x, y, r, 1.4f, 0f, Mth.TWO_PI, argb(0x59D8FF, 0.8f));
            ticks(buf, pose, x, y, r + 4f, 40, 5, 0f, 3f, argb(0x59D8FF, 0.45f));

            float a = (float) Math.toRadians(135 + 270 * v);
            line(buf, pose, x, y, x + (float) Math.cos(a) * r * 0.9f, y + (float) Math.sin(a) * r * 0.9f, 1.6f, 0.4f,
                    argb(0xFFFFFF, 0.95f), argb(0x59D8FF, 0.4f));

            ring(buf, pose, x, y, r * 0.72f, 0.8f, (float) Math.toRadians(135), (float) Math.toRadians(270 * v),
                    argb(0xA070FF, 0.8f));
            final int dialIndex = i;
            hits.add(new Hit((int) (x - r * 1.2f), (int) (y - r * 1.2f), (int) (r * 2.4f), (int) (r * 2.4f),
                    () -> dragging = dialIndex));
        }
        end(buf);
        for (int i = 0; i < 3; i++) {
            g.drawCenteredString(font, NAMES[i], (int) dialX(i), (int) (dialY() + dialR() + 12), argb(0xB8D8E8, 0.9f));
            g.drawCenteredString(font, Integer.toString(Math.round(dial[i])), (int) dialX(i), (int) (dialY() - 4),
                    0xFFFFFFFF);
        }

        g.drawString(font, "SIGNAL " + signal / 10 + "%", (int) x0, (int) (y1 + 10),
                argb(sig > 0.95f ? 0x9CF0FF : 0xB8D8E8, 1f), false);
        boolean ready = signal >= TeslaRite.LOCK_SIGNAL && !bound;
        button(g, (int) (x1 - 100), (int) (y1 + 6), 100, 18, "LOCK IN", mx, my, ready, uiAlpha(), () -> send(
                TeslaRite.LOCK, Math.round(dial[0]), Math.round(dial[1]), Math.round(dial[2])));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        dragging = -1;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) {
            dial[dragging] = Mth.clamp(dial[dragging] + (float) (dx - dy) / fit * 0.5f, 0, TeslaRite.RANGE);
            dirty = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        for (int i = 0; i < 3; i++) {
            float dx = (float) vx(mx) - dialX(i);
            float dy = (float) vy(my) - dialY();
            if (dx * dx + dy * dy < dialR() * dialR() * 2.2f) {
                dial[i] = Mth.clamp(dial[i] + (float) Math.signum(sy), 0, TeslaRite.RANGE);
                dirty = true;
                play(SoundEvents.COPPER_BULB_TURN_ON, 1.4f + dial[i] / 100f, 0.3f);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
