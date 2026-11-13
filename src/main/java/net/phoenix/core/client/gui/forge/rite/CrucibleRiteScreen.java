package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.CrucibleRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class CrucibleRiteScreen extends RiteScreen {

    private static final String[] METALS = { "Lead", "Tin", "Copper", "Silver", "Aether", "Gold" };
    private static final int[] COLORS = { 0x8A8F9A, 0xB8C8D8, 0xE08A4A, 0xE8F0F8, 0x6EF0C8, 0xFFD24A };

    public CrucibleRiteScreen(S2CRitePacket p) {
        super(Capstone.AETHER_CRUCIBLE, p, "rite_crucible");
    }

    @Override
    protected int accent() {
        return 0x6EF0C8;
    }

    @Override
    protected int accent2() {
        return 0xFFD24A;
    }

    @Override
    protected int bgTop() {
        return 0x03100E;
    }

    @Override
    protected int bgBottom() {
        return 0x0A0A18;
    }

    @Override
    protected String introTitle() {
        return "TRANSMUTATION";
    }

    @Override
    protected String introSub() {
        return "Many metals in, one gold out.";
    }

    @Override
    protected String rules() {
        return "A row of vessels, each holding a metal. Two neighbouring vessels of the same metal can be merged into" +
                " one vessel of the next metal up: lead to tin to copper to silver to aether to gold. Merge the" +
                " whole row down to a single gold. The order matters, and a careless merge can strand pairs that" +
                " will never meet. A bad batch can be restarted at no cost.";
    }

    private int count() {
        return data.length > 1 ? data[1] : 0;
    }

    private int metal(int i) {
        return data[2 + i];
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Vessels", String.valueOf(count()), 0xFFEBD0, x, y, ui);
        int best = 0;
        for (int i = 0; i < count(); i++) best = Math.max(best, metal(i));
        stat(g, "Finest metal", METALS[Mth.clamp(best, 0, 5)], COLORS[Mth.clamp(best, 0, 5)], x, y + 24, ui);
        stat(g, "Goal", "one vessel of gold", 0xFFD24A, x, y + 48, ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        play(SoundEvents.BREWING_STAND_BREW, 1.0f, 0.7f);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        int n = count();
        if (n <= 0) return;
        float avail = width - panelW() - 40;
        float slot = Math.min(60f, avail / n);
        float r = slot * 0.36f;
        float x0 = cx() - slot * n / 2f + slot / 2f;
        float y0 = height * 0.42f;

        BufferBuilder buf = begin();

        float heat = 1f - (n - 1) / (float) CrucibleRite.LENGTH;
        disc(buf, pose, cx(), y0 + r * 1.6f, slot * n * 0.45f, argb(0x6EF0C8, 0.08f + 0.12f * heat),
                argb(0x6EF0C8, 0f));
        for (int i = 0; i < n; i++) {
            int m = Mth.clamp(metal(i), 0, 5);
            float x = x0 + i * slot;
            int c = COLORS[m];
            float bob = (float) Math.sin(seconds * 2f + i) * 1.5f;

            ring(buf, pose, x, y0 + bob, r, 2.2f, 0f, Mth.PI, argb(0xB8B0A0, 0.9f));
            disc(buf, pose, x, y0 + bob - r * 0.15f, r * 0.86f, argb(c, 0.95f), argb(c, 0.45f));
            for (int k = 0; k < 3; k++) {
                float bx = x + (float) Math.sin(seconds * 1.4f + k * 2.1f + i) * r * 0.4f;
                float by = y0 + bob - ((seconds * 14f + k * 11f + i * 5f) % (r * 1.4f));
                disc(buf, pose, bx, by, 2f, argb(0xFFFFFF, 0.6f), argb(c, 0f));
            }
        }
        end(buf);
        for (int i = 0; i < n; i++) {
            int m = Mth.clamp(metal(i), 0, 5);
            float x = x0 + i * slot;
            g.drawCenteredString(font, METALS[m], (int) x, (int) (y0 + r + 10), argb(COLORS[m], 0.95f));
        }

        if (bound || seconds <= introSeconds()) return;
        for (int i = 0; i + 1 < n; i++) {
            if (metal(i) != metal(i + 1)) continue;
            final int idx = i;
            int bx = (int) (x0 + i * slot + slot / 2f) - 11;
            int by = (int) (y0 - r - 34);
            boolean over = mx >= bx && mx < bx + 22 && my >= by && my < by + 18;
            float pulse = 0.7f + 0.3f * (float) Math.sin(seconds * 5f + i);
            g.fill(bx, by, bx + 22, by + 18, over ? 0xFF_14403A : 0xFF_08201C);
            border(g, bx, by, 22, 18, argb(0x6EF0C8, over ? 1f : pulse));
            g.drawCenteredString(font, "+", bx + 11, by + 5, argb(0xFFFFFF, 1f));
            hits.add(new Hit(bx, by, 22, 18, () -> send(CrucibleRite.MERGE, idx)));
        }
        int rw = 110;
        int rx = (int) (cx() - rw / 2f);
        int ry = (int) (height * 0.66f);
        boolean over = mx >= rx && mx < rx + rw && my >= ry && my < ry + 18;
        g.fill(rx, ry, rx + rw, ry + 18, over ? 0xFF_201A2A : 0xFF_0C0A14);
        border(g, rx, ry, rw, 18, argb(0xB8A8D8, over ? 1f : 0.65f));
        g.drawCenteredString(font, "RESTART BATCH", rx + rw / 2, ry + 5, argb(0xE8E0F4, 0.95f));
        hits.add(new Hit(rx, ry, rw, 18, () -> send(CrucibleRite.RESTART)));
    }

    private static void border(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }
}
