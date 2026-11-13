package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.common.machine.multiblock.electric.plasma.PlasmaRing;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.PlasmaRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PlasmaRiteScreen extends RiteScreen {

    private static final int[] PHASE_COLOR = { 0x59E8FF, 0x9A7CFF, 0xFF5CF0, 0xFFB060 };

    public PlasmaRiteScreen(S2CRitePacket p) {
        super(Capstone.PLASMA, p, "rite_plasma");
    }

    @Override
    protected int accent() {
        return 0xFF5CF0;
    }

    @Override
    protected int accent2() {
        return 0x59E8FF;
    }

    @Override
    protected int bgTop() {
        return 0x05020A;
    }

    @Override
    protected int bgBottom() {
        return 0x16062A;
    }

    @Override
    protected String introTitle() {
        return "CONTAINMENT";
    }

    @Override
    protected String introSub() {
        return "Hold the plasma before the coils burn out.";
    }

    @Override
    protected String rules() {
        return "Six coils, each at one of four phases. The plasma is held when every coil is at the first phase." +
                " Tuning a coil moves it one phase on and drags both neighbours one phase back. Lock the ring inside" +
                " the tuning budget, or the coils overheat and it scrambles.";
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Tunings", data[2] + " of " + data[3], data[2] >= data[3] - 1 ? 0xFF8A70 : 0xFFEBD0, x, y, ui);
        stat(g, "Coils out of phase", PlasmaRing.drifting(data[0]) + " of " + PlasmaRing.CELLS, 0xFFEBD0, x, y + 24,
                ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f + 0.1f * (6 - PlasmaRing.drifting(p.data()[0])), 0.6f);
    }

    private float ringR() {
        return Math.min((width - panelW()) * 0.3f, height * 0.3f);
    }

    private float[] coil(int i) {
        double a = Math.toRadians(-90 + 60 * i);
        return new float[] { cx() + (float) Math.cos(a) * ringR(), cy() + (float) Math.sin(a) * ringR() };
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        int state = data[0];
        float heat = Mth.clamp(data[2] / (float) Math.max(1, data[3]), 0f, 1f);
        float unrest = PlasmaRing.drifting(state) / (float) PlasmaRing.CELLS;
        float size = Math.min(width, height) * 0.05f;
        float base = ringR() * 0.4f;
        float pulse = 1f + (0.04f + 0.1f * unrest) * (float) Math.sin(seconds * (3f + 6f * unrest));

        BufferBuilder buf = begin();
        rays(buf, pose, cx(), cy(), base, ringR() * 1.9f, 12, seconds * 0.05f, 16f, unrest > 0.5f ? 0xFF5CF0 : 0x59E8FF,
                0.03f + 0.05f * unrest);
        for (int k = 5; k >= 1; k--) {
            int c = unrest > 0.5f ? 0xFF5CF0 : 0x59E8FF;
            disc(buf, pose, cx(), cy(), base * (0.45f + k * 0.4f) * pulse,
                    argb(k == 1 ? 0xFFFFFF : c, k == 1 ? 0.9f : 0.1f),
                    argb(c, 0f));
        }
        ring(buf, pose, cx(), cy(), ringR(), 0.8f, 0f, Mth.TWO_PI, argb(0x8C7098, 0.4f));
        long frame = (long) (seconds * (8f + 14f * unrest));
        for (int i = 0; i < PlasmaRing.CELLS; i++) {
            int phase = PlasmaRing.phase(state, i);
            float[] p = coil(i);
            if (phase == 0) {
                bolt(buf, pose, cx(), cy(), p[0], p[1], 3f, i * 7919L, 0x59E8FF, 0.3f);
            } else {
                for (int k = 0; k < phase; k++) {
                    bolt(buf, pose, cx(), cy(), p[0], p[1], 12f + phase * 7f, frame * 31L + i * 131L + k,
                            PHASE_COLOR[phase], 0.85f);
                }
            }
        }
        for (int i = 0; i < PlasmaRing.CELLS; i++) {
            float[] p = coil(i);
            int phase = PlasmaRing.phase(state, i);
            int color = PHASE_COLOR[phase];
            disc(buf, pose, p[0], p[1], size * 1.5f, argb(color, 0.18f + 0.2f * heat), argb(color, 0f));
            ring(buf, pose, p[0], p[1], size, 1.6f, 0f, Mth.TWO_PI, argb(color, 0.9f));
            for (int k = 0; k < PlasmaRing.PHASES; k++) {
                float a = (float) Math.toRadians(-90 + 90 * k);
                quad(buf, pose, p[0] + (float) Math.cos(a) * size * 0.55f, p[1] + (float) Math.sin(a) * size * 0.55f,
                        k == phase ? 3.2f : 1.4f, argb(k == phase ? 0xFFFFFF : color, k == phase ? 1f : 0.45f));
            }
            if (!bound && seconds > introSeconds()) {
                final int cell = i;
                hits.add(new Hit((int) (p[0] - size), (int) (p[1] - size), (int) (size * 2), (int) (size * 2),
                        () -> send(PlasmaRite.TUNE, cell)));
            }
        }
        end(buf);

        buf = begin();
        float gy = cy() + ringR() + size * 2.4f;
        float gw = ringR() * 1.4f;
        line(buf, pose, cx() - gw / 2, gy, cx() + gw / 2, gy, 2.5f, argb(0x8C7098, 0.3f));
        line(buf, pose, cx() - gw / 2, gy, cx() - gw / 2 + gw * heat, gy, 2.5f,
                argb(heat > 0.75f ? 0xFFB060 : 0xFF5CF0, 0.9f));
        end(buf);
        g.drawCenteredString(font, "COIL HEAT", (int) cx(), (int) (gy + 8), argb(0xB89CD0, 0.9f));
    }
}
