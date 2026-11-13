package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.EngineRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class EngineRiteScreen extends RiteScreen {

    private float engineFlash;
    private int engineBranch = -1;
    private int engineTake;

    public EngineRiteScreen(S2CRitePacket p) {
        super(Capstone.ENGINE, p, "rite_engine");
    }

    @Override
    protected int accent() {
        return 0x6BE890;
    }

    @Override
    protected int accent2() {
        return 0xB0FFC8;
    }

    @Override
    protected int bgTop() {
        return 0x020A06;
    }

    @Override
    protected int bgBottom() {
        return 0x06240F;
    }

    @Override
    protected String introTitle() {
        return "THE LAST LEAF";
    }

    @Override
    protected String introSub() {
        return "The Engine has read every move you could make.";
    }

    @Override
    protected String rules() {
        return "Three branches, each with leaves. On your turn click a leaf to strip it and every leaf beyond it on" +
                " that branch. The Engine then does the same. Whoever takes the last leaf wins. The Engine never" +
                " slips, but you move first, and there is a way to win.";
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Leaves left", Integer.toString(data[0] + data[1] + data[2]), 0xFFEBD0, x, y, ui);
        if (engineBranch >= 0 && engineFlash > 0f) {
            stat(g, "The Engine took", engineTake + " from branch " + (engineBranch + 1), 0x9CFFB8, x, y + 24, ui);
        }
    }

    @Override
    protected void onData(S2CRitePacket p) {
        if (data.length >= 5 && data[3] >= 0) {
            engineBranch = data[3];
            engineTake = data[4];
            engineFlash = 1.5f;
            play(SoundEvents.AMETHYST_BLOCK_BREAK, 0.7f, 0.8f);
        }
    }

    private float[] leaf(int branch, int index, int count) {
        float baseX = cx();
        float baseY = cy() + height * 0.26f;
        double a = Math.toRadians(-90 + (branch - 1) * 38);
        float len = height * 0.5f;
        float t = (index + 1) / 7.5f;
        float bend = (branch - 1) * 0.06f * t * t * len;
        float x = baseX + (float) Math.cos(a) * len * t + bend;
        float y = baseY + (float) Math.sin(a) * len * t;
        return new float[] { x, y };
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        engineFlash = Math.max(0f, engineFlash - dt);
        float baseX = cx();
        float baseY = cy() + height * 0.26f;
        float leafR = Math.min(width, height) * 0.022f;

        int hoverBranch = -1;
        int hoverIndex = -1;
        if (!bound && seconds > introSeconds()) {
            for (int b = 0; b < 3; b++) {
                for (int i = 0; i < data[b]; i++) {
                    float[] p = leaf(b, i, data[b]);
                    float dx = mx - p[0];
                    float dy = my - p[1];
                    if (dx * dx + dy * dy < leafR * leafR * 2.6f) {
                        hoverBranch = b;
                        hoverIndex = i;
                    }
                }
            }
        }

        BufferBuilder buf = begin();
        rays(buf, pose, baseX, baseY - height * 0.2f, 20f, height * 0.75f, 14, seconds * 0.03f, 20f, 0x6BE890, 0.035f);

        line(buf, pose, baseX, baseY + height * 0.12f, baseX, baseY, 4f, 2.5f, argb(0x2E9A5A, 0.8f),
                argb(0x6BE890, 0.9f));
        for (int b = 0; b < 3; b++) {
            float[] tip = leaf(b, 6, 7);
            float[] mid = leaf(b, 2, 7);
            line(buf, pose, baseX, baseY, mid[0], mid[1], 2.5f, 1.8f, argb(0x6BE890, 0.8f), argb(0x6BE890, 0.7f));
            line(buf, pose, mid[0], mid[1], tip[0], tip[1], 1.8f, 0.7f, argb(0x6BE890, 0.7f), argb(0x6BE890, 0.3f));
        }

        for (int b = 0; b < 3; b++) {
            float flashHere = b == engineBranch ? engineFlash : 0f;
            for (int i = 0; i < data[b]; i++) {
                float[] p = leaf(b, i, data[b]);
                boolean marked = b == hoverBranch && i >= hoverIndex;
                int color = marked ? 0xFFD060 : 0x6BE890;
                float sway = (float) Math.sin(seconds * 1.4f + b * 2f + i) * 1.4f;
                disc(buf, pose, p[0] + sway, p[1], leafR * 1.9f, argb(color, marked ? 0.28f : 0.12f), argb(color, 0f));
                polyFill(buf, pose, p[0] + sway, p[1], leafR, 4, (float) Math.PI / 4f + b, argb(0xF0FFF4, 0.95f),
                        argb(color, 0.85f));
            }
            if (flashHere > 0f) {
                float[] tip = leaf(b, 6, 7);
                disc(buf, pose, tip[0], tip[1], leafR * 7f * flashHere, argb(0xB0FFC8, 0.3f * flashHere),
                        argb(0x6BE890, 0f));
            }
            if (!bound && seconds > introSeconds()) {

                for (int i = 0; i < data[b]; i++) {
                    float[] p = leaf(b, i, data[b]);
                    final int branch = b;
                    final int take = data[b] - i;
                    hits.add(new Hit((int) (p[0] - leafR * 1.4f), (int) (p[1] - leafR * 1.4f), (int) (leafR * 2.8f),
                            (int) (leafR * 2.8f), () -> {
                                play(SoundEvents.AZALEA_LEAVES_BREAK, 0.9f, 0.9f);
                                send(EngineRite.TAKE, branch, take);
                            }));
                }
            }
        }
        end(buf);
        for (int b = 0; b < 3; b++) {
            float[] tip = leaf(b, 6, 7);
            g.drawCenteredString(font, "BRANCH " + (b + 1) + "  (" + data[b] + ")", (int) tip[0],
                    (int) (tip[1] - leafR * 2.8f), argb(0xB0FFC8, 0.9f));
        }
    }
}
