package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.CortexRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

@OnlyIn(Dist.CLIENT)
public class CortexRiteScreen extends RiteScreen {

    private static final int[] COLORS = { 0x35C9E8, 0x3A6BD8, 0x8A5BE0, 0x6EE07A, 0xE8E4C8 };
    private static final String[] NAMES = { "Echo", "Deep", "Void", "Moss", "Bone" };

    public CortexRiteScreen(S2CRitePacket p) {
        super(Capstone.SCULK_CORTEX, p, "rite_cortex");
    }

    @Override
    protected int accent() {
        return 0x35C9E8;
    }

    @Override
    protected int accent2() {
        return 0x8A5BE0;
    }

    @Override
    protected int bgTop() {
        return 0x020A10;
    }

    @Override
    protected int bgBottom() {
        return 0x06141C;
    }

    @Override
    protected String introTitle() {
        return "THE SPREAD";
    }

    @Override
    protected String introSub() {
        return "Let the sculk take all of it.";
    }

    @Override
    protected String rules() {
        return "Seven by seven cells in five colours. The sculk begins in the top left corner. Choose a colour and" +
                " the sculk becomes it, and every cell of that colour touching it joins in. Cover the whole square" +
                " before the budget runs out. A greedy player needs most of it; a planner needs fewer.";
    }

    private int moves() {
        return data[0];
    }

    private int budget() {
        return data[1];
    }

    private int cell(int i) {
        return data[3 + i];
    }

    private int covered() {
        int[] g = new int[CortexRite.SIZE * CortexRite.SIZE];
        for (int i = 0; i < g.length; i++) g[i] = cell(i);
        return CortexRite.covered(g);
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        stat(g, "Moves", moves() + " of " + budget(), moves() >= budget() - 2 ? 0xFF8A70 : 0xFFEBD0, x, y, ui);
        stat(g, "Covered", covered() + " of " + CortexRite.SIZE * CortexRite.SIZE, 0xFFEBD0, x, y + 24, ui);
        stat(g, "Sculk is now", NAMES[cell(0)], COLORS[cell(0)], x, y + 48, ui);
    }

    @Override
    protected void onData(S2CRitePacket p) {
        play(SoundEvents.SCULK_BLOCK_SPREAD, 1.0f, 0.8f);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        int n = CortexRite.SIZE;
        int avail = width - panelW();
        int cell = (int) Math.max(14, Math.min(avail * 0.5f / n, height * 0.46f / n));
        int gw = cell * n;
        int gx = (int) (cx() - gw / 2f);
        int gy = (int) (height * 0.2f);

        int[] grid = new int[n * n];
        for (int i = 0; i < grid.length; i++) grid[i] = cell(i);
        boolean[] mine = new boolean[grid.length];
        markRegion(grid, mine);

        for (int i = 0; i < grid.length; i++) {
            int x = gx + (i % n) * cell;
            int y = gy + (i / n) * cell;
            int c = COLORS[grid[i]];
            g.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, argb(c, mine[i] ? 1f : 0.55f));
            if (mine[i]) {
                float pulse = 0.55f + 0.45f * (float) Math.sin(seconds * 3f + i * 0.7f);
                g.fill(x + 1, y + 1, x + cell - 1, y + 3, argb(0xFFFFFF, 0.35f * pulse));
                g.renderOutline(x + 1, y + 1, cell - 2, cell - 2, argb(0xFFFFFF, 0.5f));
            }
        }

        if (bound || seconds <= introSeconds()) return;
        int chip = 64;
        int gap = 8;
        int total = CortexRite.COLORS * chip + (CortexRite.COLORS - 1) * gap;
        int bx = (int) (cx() - total / 2f);
        int by = gy + gw + 18;
        for (int i = 0; i < CortexRite.COLORS; i++) {
            final int colour = i;
            int px = bx + i * (chip + gap);
            boolean current = cell(0) == i;
            boolean over = !current && mx >= px && mx < px + chip && my >= by && my < by + 30;
            g.fill(px, by, px + chip, by + 30, argb(0x04141C, 0.9f));
            g.fill(px + 4, by + 4, px + chip - 4, by + 18, argb(COLORS[i], current ? 0.35f : 1f));
            g.renderOutline(px, by, chip, 30, argb(COLORS[i], over ? 1f : current ? 0.25f : 0.7f));
            g.drawCenteredString(font, NAMES[i], px + chip / 2, by + 20, argb(0xE0F4F8, current ? 0.35f : 0.95f));
            if (!current) hits.add(new Hit(px, by, chip, 30, () -> send(CortexRite.FLOOD, colour)));
        }
    }

    private static void markRegion(int[] g, boolean[] mark) {
        int n = CortexRite.SIZE;
        int[] stack = new int[g.length];
        int top = 0;
        stack[top++] = 0;
        mark[0] = true;
        int colour = g[0];
        while (top > 0) {
            int c = stack[--top];
            int x = c % n;
            int y = c / n;
            int[][] d = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] dd : d) {
                int nx = x + dd[0];
                int ny = y + dd[1];
                if (nx < 0 || ny < 0 || nx >= n || ny >= n) continue;
                int ni = ny * n + nx;
                if (mark[ni] || g[ni] != colour) continue;
                mark[ni] = true;
                stack[top++] = ni;
            }
        }
    }
}
