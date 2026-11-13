package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.ColliderRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ColliderRiteScreen extends RiteScreen {

    public ColliderRiteScreen(S2CRitePacket p) {
        super(Capstone.ANTIMATTER_COLLIDER, p, "rite_collider");
    }

    @Override
    protected int accent() {
        return 0xFF4DD8;
    }

    @Override
    protected int accent2() {
        return 0x59E8FF;
    }

    @Override
    protected int bgTop() {
        return 0x04020A;
    }

    @Override
    protected int bgBottom() {
        return 0x120820;
    }

    @Override
    protected String introTitle() {
        return "THE BEAMLINE";
    }

    @Override
    protected String introSub() {
        return "Steer the beam into the detector.";
    }

    @Override
    protected String rules() {
        return "The beam enters on the left at the arrow and must leave on the right at the detector. Click a mirror" +
                " to turn it between its two slants. Some mirrors are in the way and do nothing useful; the right" +
                " ones turn the beam up, along and across. There is no limit: reason out where the beam goes.";
    }

    private int size() {
        return data[0];
    }

    private int src() {
        return data[1];
    }

    private int tgt() {
        return data[2];
    }

    private int[] cells() {
        int[] c = new int[size() * size()];
        System.arraycopy(data, 3, c, 0, c.length);
        return c;
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        int[] c = cells();
        int mirrors = 0;
        for (int v : c) if (v != 0) mirrors++;
        boolean ok = ColliderRite.solved(c, src(), tgt());
        stat(g, "Mirrors", String.valueOf(mirrors), 0xFFEBD0, x, y, ui);
        stat(g, "Beam", ok ? "in the detector" : "lost", ok ? 0x7CF0B0 : 0xFF8A70, x, y + 24, ui);
        stat(g, "Enters row", String.valueOf(src() + 1), 0xB8A890, x, y + 48, ui);
        stat(g, "Detector row", String.valueOf(tgt() + 1), 0xB8A890, x, y + 72, ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.6f);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        int n = size();
        int avail = width - panelW();
        int cell = (int) Math.max(18, Math.min(avail * 0.56f / n, height * 0.6f / n));
        int gw = cell * n;
        int gx = (int) (cx() - gw / 2f);
        int gy = (int) (height * 0.2f);
        int[] c = cells();

        List<Integer> path = new ArrayList<>();
        boolean ok = ColliderRite.trace(c, src(), tgt(), path);

        for (int i = 0; i < n * n; i++) {
            int x = gx + (i % n) * cell;
            int y = gy + (i / n) * cell;
            g.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, argb(0x0A0618, 0.85f));
            g.renderOutline(x, y, cell, cell, argb(0x3A2A58, 0.8f));
        }

        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin();

        float px = gx - cell * 0.5f;
        float py = gy + src() * cell + cell / 2f;
        int beam = ok ? 0xFFFFFF : 0x59E8FF;
        for (int idx : path) {
            float qx = gx + (idx % n) * cell + cell / 2f;
            float qy = gy + (idx / n) * cell + cell / 2f;
            line(buf, pose, px, py, qx, qy, 4.0f, argb(beam, 0.25f));
            line(buf, pose, px, py, qx, qy, 1.4f, argb(beam, 0.95f));
            px = qx;
            py = qy;
        }
        if (!path.isEmpty()) {
            float ex = ok ? gx + gw + cell * 0.4f : px;
            float ey = ok ? gy + tgt() * cell + cell / 2f : py;
            line(buf, pose, px, py, ex, ey, 1.4f, argb(beam, 0.95f));
        }

        for (int i = 0; i < n * n; i++) {
            if (c[i] == 0) continue;
            float mx0 = gx + (i % n) * cell + cell / 2f;
            float my0 = gy + (i / n) * cell + cell / 2f;
            float h = cell * 0.36f;
            int col = 0xFF4DD8;
            if (c[i] == 1) line(buf, pose, mx0 - h, my0 + h, mx0 + h, my0 - h, 3.0f, argb(col, 0.95f));
            else line(buf, pose, mx0 - h, my0 - h, mx0 + h, my0 + h, 3.0f, argb(col, 0.95f));
        }

        quad(buf, pose, gx - cell * 0.5f, gy + src() * cell + cell / 2f, 6f, argb(0x59E8FF, 1f));
        float dx = gx + gw + cell * 0.3f;
        float dy = gy + tgt() * cell + cell / 2f;
        disc(buf, pose, dx, dy, cell * (ok ? 0.5f : 0.34f), argb(ok ? 0xFFFFFF : 0xFF4DD8, ok ? 0.9f : 0.5f),
                argb(0xFF4DD8, 0f));
        end(buf);

        if (bound || seconds <= introSeconds()) return;
        for (int i = 0; i < n * n; i++) {
            if (c[i] == 0) continue;
            final int idx = i;
            int x = gx + (i % n) * cell;
            int y = gy + (i / n) * cell;
            boolean over = mx >= x && mx < x + cell && my >= y && my < y + cell;
            if (over) g.renderOutline(x + 1, y + 1, cell - 2, cell - 2, argb(0xFFFFFF, 0.8f));
            hits.add(new Hit(x, y, cell, cell, () -> send(ColliderRite.TURN, idx)));
        }
    }
}
