package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.InfuserRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

@OnlyIn(Dist.CLIENT)
public class InfuserRiteScreen extends RiteScreen {

    private static final int[] DISK_COLOR = { 0xFFE08A, 0xFFB347, 0xFF7A3A, 0xE0402A };

    private int selected = -1;

    public InfuserRiteScreen(S2CRitePacket p) {
        super(Capstone.PHOENIX_INFUSER, p, "rite_infuser");
    }

    @Override
    protected int accent() {
        return 0xFF8A3A;
    }

    @Override
    protected int accent2() {
        return 0xFFD24A;
    }

    @Override
    protected int bgTop() {
        return 0x0A0402;
    }

    @Override
    protected int bgBottom() {
        return 0x24100A;
    }

    @Override
    protected String introTitle() {
        return "THE ASH HEAP";
    }

    @Override
    protected String introSub() {
        return "Raise the tower from the ashes.";
    }

    @Override
    protected String rules() {
        return "Four disks on the first peg. Move the whole tower to the last peg, one disk at a time, taking the top" +
                " disk of a peg. A larger disk can never rest on a smaller one. It can be done in fifteen moves;" +
                " you have twenty-two before the fire gutters and the tower is rebuilt. Click a peg to pick up its" +
                " top disk, then click where it goes.";
    }

    private int moves() {
        return data[0];
    }

    private int peg(int disk) {
        return data[3 + disk];
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Moves", moves() + " of " + data[1], moves() >= data[1] - 3 ? 0xFF8A70 : 0xFFEBD0, x, y, ui);
        int home = 0;
        for (int d = 0; d < InfuserRite.DISKS; d++) if (peg(d) == InfuserRite.PEGS - 1) home++;
        stat(g, "On the last peg", home + " of " + InfuserRite.DISKS, 0xFFEBD0, x, y + 24, ui);
        stat(g, "Fewest possible", "15", 0xB8A890, x, y + 48, ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        selected = -1;
        play(SoundEvents.FIRE_AMBIENT, 1.0f, 0.6f);
    }

    private int topOf(int p) {
        for (int d = 0; d < InfuserRite.DISKS; d++) if (peg(d) == p) return d;
        return -1;
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        int avail = width - panelW();
        int pegGap = (int) Math.min(avail * 0.26f, 150f);
        int diskH = 16;
        int baseY = (int) (height * 0.58f);
        int maxW = (int) (pegGap * 0.86f);
        int towerH = diskH * (InfuserRite.DISKS + 1);

        for (int p = 0; p < InfuserRite.PEGS; p++) {
            int px = (int) (cx() + (p - 1) * pegGap);
            boolean isSel = selected == p;

            g.fill(px - 2, baseY - towerH - 10, px + 2, baseY, argb(0x6A5040, 0.9f));
            g.fill(px - maxW / 2 - 4, baseY, px + maxW / 2 + 4, baseY + 5, argb(0x8A6A50, 0.9f));
            if (isSel) g.renderOutline(px - maxW / 2 - 8, baseY - towerH - 14, maxW + 16, towerH + 24,
                    argb(0xFFD24A, 0.7f));

            int stack = 0;
            for (int d = InfuserRite.DISKS - 1; d >= 0; d--) {
                if (peg(d) != p) continue;
                int dw = (int) (maxW * (0.34f + 0.22f * d));
                int dy = baseY - diskH * (stack + 1);
                boolean lifted = isSel && d == topOf(p);
                if (lifted) dy -= 8;
                g.fill(px - dw / 2, dy, px + dw / 2, dy + diskH - 2, argb(DISK_COLOR[d], 0.95f));
                g.fill(px - dw / 2, dy, px + dw / 2, dy + 2, argb(0xFFFFFF, 0.3f));
                g.renderOutline(px - dw / 2, dy, dw, diskH - 2, argb(0x200800, 0.8f));
                stack++;
            }
            if (!bound && seconds > introSeconds()) {
                final int idx = p;
                hits.add(new Hit(px - maxW / 2 - 8, baseY - towerH - 14, maxW + 16, towerH + 24, () -> {
                    if (selected < 0) {
                        if (topOf(idx) >= 0) selected = idx;
                    } else if (selected == idx) {
                        selected = -1;
                    } else {
                        send(InfuserRite.MOVE, selected, idx);
                        selected = -1;
                    }
                }));
            }
        }
        String note = selected < 0 ? "Pick a peg" : "Now pick where it goes";
        if (!bound) g.drawCenteredString(font, note, (int) cx(), baseY + 20, argb(0xE8D0B0, 0.9f));
    }
}
