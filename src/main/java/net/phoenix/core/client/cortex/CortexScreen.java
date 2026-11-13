package net.phoenix.core.client.cortex;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.client.gui.forge.CineScreen;
import net.phoenix.core.common.machine.multiblock.cortex.SculkCortexMachine;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class CortexScreen extends CineScreen {

    private static final int NODES = 4 + 4 * SculkCortexMachine.MAX_LEVEL;
    private static final int C_DIM = 0x6A9CA8;
    private static final int C_WHITE = 0xE0F8FF;
    private static final int C_GREEN = 0x7CF0B0;
    private static final int C_GOLD = 0xFFC44D;
    private static final int C_RED = 0xFF5A5A;
    private static final int C_TEAL = 0x35C9E8;

    private record Pulse(int[] path, float start) {}

    private final BlockPos pos;
    private final List<Pulse> pulses = new ArrayList<>();
    private int lastDigested = -1;
    private int lastLevel = -1;
    private int lastWithers = -1;
    private boolean wasLit;

    public CortexScreen(BlockPos pos) {
        super("Sculk Cortex", "cortex");
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    private SculkCortexMachine machine() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return MetaMachine.getMachine(level, pos) instanceof SculkCortexMachine m ? m : null;
    }

    @Override
    protected int accent() {
        return C_TEAL;
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
    protected float introSeconds() {
        return 6.0f;
    }

    @Override
    protected String introTitle() {
        return "SCULK CORTEX";
    }

    @Override
    protected String introSub() {
        return "It remembers what it eats.";
    }

    private float[] node(int i, float cx, float cy, float rx, float ry) {
        float a = i * 2.39996f;
        float r = (float) Math.sqrt((i + 0.6f) / (NODES + 0.6f));
        return new float[] { cx + Mth.cos(a) * r * rx, cy + Mth.sin(a) * r * ry };
    }

    private static List<Integer> neighbours(int i, int unlocked) {
        List<Integer> out = new ArrayList<>();
        for (int d : new int[] { 3, 5, 8 }) {
            if (i - d >= 0) out.add(i - d);
            if (i + d < unlocked) out.add(i + d);
        }
        return out;
    }

    private void spawnPulse(int unlocked) {
        int[] path = new int[6];
        int at = 0;
        path[0] = 0;
        for (int k = 1; k < path.length; k++) {
            var next = neighbours(at, unlocked);
            if (next.isEmpty()) {
                path[k] = at;
                continue;
            }
            at = next.get(rng.nextInt(next.size()));
            path[k] = at;
        }
        pulses.add(new Pulse(path, seconds));
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        SculkCortexMachine m = machine();
        int avail = width - panelW();
        float cx = panelW() + avail / 2f;
        float cy = height / 2f;
        float rx = Math.min(avail * 0.44f, height * 0.5f);
        float ry = rx * 0.82f;

        if (m == null || !m.isFormed()) {
            g.drawCenteredString(font, "Structure not formed.", (int) cx, (int) cy - 4, argb(C_DIM, 0.9f));
            return;
        }
        int level = m.level();
        int unlocked = Math.min(NODES, SculkCortexMachine.capacity(level));
        int lit = Math.min(unlocked, m.engramCount());
        boolean on = m.lit();
        float grow = ease(Mth.clamp(seconds / (introSeconds() * 0.85f), 0f, 1f));

        if (lastDigested >= 0 && m.digested() != lastDigested && on) spawnPulse(unlocked);
        lastDigested = m.digested();
        if (lastLevel >= 0 && level > lastLevel) {
            flash = 0.4f;
            flashColor = 0x35C9E8;
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.9f);
        }
        lastLevel = level;
        if (lastWithers >= 0 && m.withers() > lastWithers) {
            flash = 0.9f;
            flashColor = 0xFF3A3A;
            shake = 0.8f;
            play(SoundEvents.WARDEN_HEARTBEAT, 0.6f, 1.0f);
        }
        lastWithers = m.withers();
        if (on && !wasLit && seconds > 0.5f) {
            flash = 0.5f;
            flashColor = 0x35C9E8;
            play(SoundEvents.SCULK_CATALYST_BLOOM, 0.9f, 1.0f);
        }
        wasLit = on;

        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin();
        float breathe = 0.9f + 0.1f * Mth.sin(seconds * 1.3f);
        float starve = m.starving() ? 0.5f + 0.5f * Mth.sin(seconds * 6f) : 0f;
        int core = m.starving() ? 0xFF5A5A : C_TEAL;

        disc(buf, pose, cx, cy, rx * (0.35f + 0.08f * level) * grow, argb(core, (on ? 0.14f : 0.04f) * breathe),
                argb(core, 0f));
        for (int k = 0; k < 4; k++) {
            ring(buf, pose, cx, cy, rx * (0.35f + 0.16f * k) * grow, 1.0f, seconds * (0.05f + 0.02f * k),
                    Mth.TWO_PI * 0.55f, argb(C_TEAL, (on ? 0.10f : 0.04f)));
        }

        for (int i = 1; i < NODES; i++) {
            float[] a = node(i, cx, cy, rx * grow, ry * grow);
            for (int d : new int[] { 3, 5, 8 }) {
                int j = i - d;
                if (j < 0) continue;
                float[] b = node(j, cx, cy, rx * grow, ry * grow);
                boolean both = i < unlocked && j < unlocked;
                boolean bothLit = i < lit && j < lit;
                float alpha = !both ? 0.03f : bothLit ? 0.55f : 0.16f;
                if (!on) alpha *= 0.4f;
                line(buf, pose, a[0], a[1], b[0], b[1], bothLit ? 1.6f : 0.8f,
                        argb(bothLit ? (m.starving() ? 0xFF8A8A : 0x7FE6FF) : C_TEAL, alpha));
            }
        }

        for (int i = 0; i < NODES; i++) {
            float[] p = node(i, cx, cy, rx * grow, ry * grow);
            if (i >= unlocked) {
                disc(buf, pose, p[0], p[1], 1.6f, argb(C_DIM, 0.12f), argb(C_DIM, 0.12f));
            } else if (i < lit) {
                float tw = 0.7f + 0.3f * Mth.sin(seconds * 2.2f + i);
                int c = m.starving() ? 0xFF7A7A : 0xBFF4FF;
                disc(buf, pose, p[0], p[1], 9f, argb(core, (on ? 0.35f : 0.1f) * tw), argb(core, 0f));
                disc(buf, pose, p[0], p[1], 3.6f, argb(c, on ? 0.95f : 0.4f), argb(c, on ? 0.95f : 0.4f));
            } else {
                ring(buf, pose, p[0], p[1], 3.6f, 0.9f, 0f, Mth.TWO_PI, argb(C_TEAL, on ? 0.55f : 0.2f));
            }
        }

        pulses.removeIf(pl -> seconds - pl.start() > pl.path().length / 3f + 0.3f);
        for (Pulse pl : pulses) {
            float t = (seconds - pl.start()) * 3f;
            int seg = (int) t;
            if (seg + 1 >= pl.path().length) continue;
            float f = t - seg;
            float[] a = node(pl.path()[seg], cx, cy, rx * grow, ry * grow);
            float[] b = node(pl.path()[seg + 1], cx, cy, rx * grow, ry * grow);
            float px = Mth.lerp(f, a[0], b[0]);
            float py = Mth.lerp(f, a[1], b[1]);
            line(buf, pose, Mth.lerp(Math.max(0f, f - 0.4f), a[0], b[0]), Mth.lerp(Math.max(0f, f - 0.4f), a[1], b[1]),
                    px, py, 3.0f, argb(0xFFFFFF, 0.8f));
            disc(buf, pose, px, py, 8f, argb(0xFFFFFF, 0.95f), argb(core, 0f));
        }
        end(buf);

        if (!on) {
            float pulse = 0.55f + 0.45f * Mth.sin(seconds * 2.2f);
            g.drawCenteredString(font, "DORMANT", (int) cx, (int) (cy + ry + 8), argb(C_GOLD, 0.9f * pulse));
            g.drawCenteredString(font, "It does not wake until it is given power.", (int) cx, (int) (cy + ry + 20),
                    argb(C_DIM, 0.9f));
        } else if (starve > 0f) {
            g.drawCenteredString(font, "STARVING", (int) cx, (int) (cy + ry + 8), argb(C_RED, 0.5f + 0.5f * starve));
        }
    }

    private static String power(long eu) {
        if (eu >= 1_000_000L) return String.format(Locale.ROOT, "%.2f MEU/t", eu / 1.0e6);
        if (eu >= 10_000L) return String.format(Locale.ROOT, "%.1f kEU/t", eu / 1.0e3);
        return String.format(Locale.ROOT, "%,d EU/t", eu);
    }

    @Override
    protected void drawUi(GuiGraphics g, int mx, int my, float ui) {
        fadePanel(g, true, ui);
        SculkCortexMachine m = machine();
        int x = 14;
        int w = panelW() - 28;
        int y = bar() + 14;
        heading(g, "SCULK CORTEX", x, y, ui);
        y += 20;
        if (m == null || !m.isFormed()) {
            g.drawString(font, "The structure is not formed.", x, y, argb(C_DIM, ui), false);
            button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
            return;
        }
        String status;
        int statusColor;
        if (!m.lit()) {
            status = "DORMANT";
            statusColor = C_GOLD;
        } else if (!m.powerOk()) {
            status = "NO POWER";
            statusColor = C_RED;
        } else if (m.starving()) {
            status = "WITHERING";
            statusColor = C_RED;
        } else if (m.hunger() < 20.0) {
            status = "HUNGRY";
            statusColor = C_GOLD;
        } else {
            status = "THINKING";
            statusColor = C_GREEN;
        }
        g.drawString(font, status, x, y, argb(statusColor, ui), false);
        y += 18;

        int level = m.level();
        stat(g, "SIZE", "level " + level + " of " + SculkCortexMachine.MAX_LEVEL, C_WHITE, x, y, ui);
        float xpFrac = level >= SculkCortexMachine.MAX_LEVEL ? 1f :
                (float) (m.xp() / SculkCortexMachine.xpToNext(level));
        bar(g, x, y + 21, w, xpFrac, C_TEAL, ui);
        y += 34;
        double hunger = m.hunger();
        stat(g, "HUNGER", String.format(Locale.ROOT, "%.0f%% fed", hunger), C_WHITE, x, y, ui);
        bar(g, x, y + 21, w, (float) (hunger / 100.0), hunger > 50 ? C_GREEN : hunger > 20 ? C_GOLD : C_RED, ui);
        y += 34;
        stat(g, "MEMORY", m.engramCount() + " of " + m.capacity() + " kinds", C_WHITE, x, y, ui);
        y += 24;
        if (!m.lastLearned().isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth("learned: " + m.lastLearned(), w), x, y,
                    argb(0x8AC8D8, ui * 0.9f), false);
            y += 14;
        }
        y += 4;
        stat(g, "UPKEEP", m.lit() ? power(m.energyUse()) : "needs power", m.lit() && !m.powerOk() ? C_RED : C_WHITE,
                x, y, ui);
        y += 24;
        stat(g, "THOUGHT", power(m.emitting()), m.emitting() > 0 ? 0x7FE6FF : C_DIM, x, y, ui);
        y += 24;
        if (m.withers() > 0) {
            g.drawString(font, "withered " + m.withers() + "x", x, y, argb(C_RED, ui * 0.8f), false);
            y += 14;
        }
        String tip = !m.lit() ? "Give it energy and it wakes." :
                "Feed items into its input bus. What it eats once, it remembers.";
        wrapped(g, tip, x, y + 4, w, 0xA0C0CC, ui);

        button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
    }

    private void bar(GuiGraphics g, int x, int y, int w, float frac, int color, float ui) {
        g.fill(x, y, x + w, y + 4, argb(0x0E2A32, ui));
        g.fill(x, y, x + (int) (w * Mth.clamp(frac, 0f, 1f)), y + 4, argb(color, ui));
        g.renderOutline(x, y, w, 4, argb(accent(), 0.4f * ui));
    }
}
