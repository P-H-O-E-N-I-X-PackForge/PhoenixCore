package net.phoenix.core.client.nullwell;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.client.gui.forge.CineScreen;
import net.phoenix.core.common.machine.multiblock.nullwell.C2SNullWellPacket;
import net.phoenix.core.common.machine.multiblock.nullwell.NullWellControls;
import net.phoenix.core.common.machine.multiblock.nullwell.NullWellMachine;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class NullWellScreen extends CineScreen {

    private static final int C_DIM = 0x8C7AAE;
    private static final int C_WHITE = 0xF0E8FF;
    private static final int C_GREEN = 0x7CF0B0;
    private static final int C_GOLD = 0xFFC44D;
    private static final int C_RED = 0xFF4A5A;
    private static final int C_CYAN = 0x7FE6FF;

    private final BlockPos pos;
    private int dragging = -1;
    private int lastSent = -1;
    private final int[] sliderX = new int[2];
    private final int[] sliderY = new int[2];
    private int sliderW;
    private int breaches = -1;
    private boolean wasLit;

    public NullWellScreen(BlockPos pos) {
        super("Null Well", "null_well");
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    private NullWellMachine machine() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return MetaMachine.getMachine(level, pos) instanceof NullWellMachine m ? m : null;
    }

    @Override
    protected int accent() {
        return 0xC98CFF;
    }

    @Override
    protected int accent2() {
        return 0x7FE6FF;
    }

    @Override
    protected int bgTop() {
        return 0x030108;
    }

    @Override
    protected int bgBottom() {
        return 0x120A22;
    }

    @Override
    protected float introSeconds() {
        return 6.0f;
    }

    @Override
    protected String introTitle() {
        return "NULL WELL";
    }

    @Override
    protected String introSub() {
        return "A hole the size of a grain.";
    }

    private void send(int which, int value) {
        PacketDistributor.sendToServer(new C2SNullWellPacket(pos, which, value));
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        NullWellMachine m = machine();

        long seed = 7919L;
        for (int i = 0; i < 160; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sx = (int) ((seed >>> 33) % Math.max(1, width));
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sy = (int) ((seed >>> 33) % Math.max(1, height));
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * Mth.sin(seconds * (0.6f + (i % 5) * 0.3f) + i));
            g.fill(sx, sy, sx + 1 + (i % 7 == 0 ? 1 : 0), sy + 1 + (i % 7 == 0 ? 1 : 0), argb(0xB8A8E8, 0.45f * tw));
        }

        int avail = width - panelW();
        int size = (int) (Math.min(avail, height) * 0.86f);
        int x = panelW() + (avail - size) / 2;
        int y = (height - size) / 2;
        float grow = ease(Mth.clamp(seconds / (introSeconds() * 0.85f), 0f, 1f));

        if (m == null || !m.isFormed()) {
            g.drawCenteredString(font, "Structure not formed.", panelW() + avail / 2, height / 2 - 4,
                    argb(C_DIM, 0.9f));
            return;
        }
        boolean lit = m.lit();
        double mass = m.mass();
        float stability = m.stability();
        NullWellArt.draw(g, x, y, size, (float) (mass / NullWellMachine.LIMIT) * grow,
                (float) (NullWellMachine.COMFORT / NullWellMachine.LIMIT), stability, m.feedSetting() / 100.0f,
                m.drawSetting() / 100.0f, lit, false);

        if (!lit) {
            float pulse = 0.55f + 0.45f * Mth.sin(seconds * 2.2f);
            g.drawCenteredString(font, "DORMANT", panelW() + avail / 2, height / 2 + size / 2 - 40,
                    argb(C_GOLD, 0.9f * pulse));
            g.drawCenteredString(font, "No field until it is given power.", panelW() + avail / 2,
                    height / 2 + size / 2 - 28, argb(C_DIM, 0.9f));
        }

        if (lit && !wasLit && seconds > 0.5f) {
            flash = 0.5f;
            flashColor = 0xC98CFF;
            shake = 0.5f;
            play(SoundEvents.BEACON_ACTIVATE, 0.7f, 0.9f);
        }
        wasLit = lit;
        if (breaches >= 0 && m.breaches() > breaches) {
            flash = 1.0f;
            flashColor = 0xFF4A5A;
            shake = 1.0f;
            play(SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 0.9f);
        }
        breaches = m.breaches();
    }

    private static String power(long eu) {
        if (eu >= 1_000_000L) return String.format(Locale.ROOT, "%.2f MEU/t", eu / 1.0e6);
        if (eu >= 10_000L) return String.format(Locale.ROOT, "%.1f kEU/t", eu / 1.0e3);
        return String.format(Locale.ROOT, "%,d EU/t", eu);
    }

    @Override
    protected void drawUi(GuiGraphics g, int mx, int my, float ui) {
        fadePanel(g, true, ui);
        NullWellMachine m = machine();
        int x = 14;
        int w = panelW() - 28;
        int y = bar() + 14;
        heading(g, "NULL WELL", x, y, ui);
        y += 20;
        if (m == null || !m.isFormed()) {
            g.drawString(font, "The structure is not formed.", x, y, argb(C_DIM, ui), false);
            button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
            return;
        }
        float stability = m.stability();
        double mass = m.mass();
        String status;
        int statusColor;
        if (!m.lit()) {
            status = "DORMANT";
            statusColor = C_GOLD;
        } else if (!m.powerOk()) {
            status = "NO POWER";
            statusColor = C_RED;
        } else if (mass > NullWellMachine.LIMIT) {
            status = "OVER LIMIT";
            statusColor = C_RED;
        } else if (stability < 40.0f) {
            status = "FAILING";
            statusColor = C_RED;
        } else if (mass > NullWellMachine.COMFORT) {
            status = "STRAINED";
            statusColor = C_GOLD;
        } else {
            status = "HELD";
            statusColor = C_GREEN;
        }
        g.drawString(font, status, x, y, argb(statusColor, ui), false);
        y += 18;

        stat(g, "MASS", String.format(Locale.ROOT, "%,.1f of %,.0f", mass, NullWellMachine.LIMIT), C_WHITE, x, y, ui);
        bar(g, x, y + 21, w, (float) (mass / NullWellMachine.LIMIT), 0xA992CF, (float) (NullWellMachine.COMFORT /
                NullWellMachine.LIMIT), ui);
        y += 34;
        stat(g, "FIELD", String.format(Locale.ROOT, "%.0f%%", stability), NullWellArt.ringColor(stability) & 0xFFFFFF,
                x, y, ui);
        bar(g, x, y + 21, w, stability / 100.0f, NullWellArt.ringColor(stability) & 0xFFFFFF, -1f, ui);
        y += 34;
        stat(g, "CONTAINMENT", m.lit() ? power(m.energyUse()) : "needs power",
                m.lit() && !m.powerOk() ? C_RED : C_WHITE, x, y, ui);
        y += 24;
        stat(g, "RADIATION", power(m.emitting()), m.emitting() > 0 ? C_CYAN : C_DIM, x, y, ui);
        y += 24;
        stat(g, "FEEDING", String.format(Locale.ROOT, "%,d mB/t", m.feeding()), m.feeding() > 0 ? C_WHITE : C_DIM, x,
                y, ui);
        y += 28;

        sliderW = w;
        slider(g, "FEED", 0, x, y, m.feedSetting(), 0xA992CF, ui);
        y += 28;
        slider(g, "DRAW", 1, x, y, m.drawSetting(), C_CYAN, ui);
        y += 30;

        button(g, x, y, 60, 15, "VENT", mx, my, mass > 1.0, ui, () -> send(NullWellControls.VENT, 0));
        if (m.breaches() > 0) {
            g.drawString(font, "breaches: " + m.breaches(), x + 68, y + 4, argb(C_RED, ui * 0.8f), false);
        }
        y += 24;
        String tip = !m.lit() ? "Give it energy and the field starts." : m.feeding() > 0 ? "" :
                "Fluid in raises mass. Draw turns it into energy.";
        if (!tip.isEmpty()) wrapped(g, tip, x, y, w, 0xB0A0C8, ui);

        button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
    }

    private void bar(GuiGraphics g, int x, int y, int w, float frac, int color, float mark, float ui) {
        g.fill(x, y, x + w, y + 4, argb(0x2A1E44, ui));
        g.fill(x, y, x + (int) (w * Mth.clamp(frac, 0f, 1f)), y + 4, argb(color, ui));
        if (mark >= 0f) g.fill(x + (int) (w * mark), y - 2, x + (int) (w * mark) + 1, y + 6, argb(C_GOLD, ui));
        g.renderOutline(x, y, w, 4, argb(accent(), 0.4f * ui));
    }

    private void slider(GuiGraphics g, String name, int index, int x, int y, int value, int color, float ui) {
        g.drawString(font, name, x, y, argb(C_DIM, ui), false);
        String v = value + "%";
        g.drawString(font, v, x + sliderW - font.width(v), y, argb(C_WHITE, ui), false);
        int ty = y + 13;
        g.fill(x, ty, x + sliderW, ty + 5, argb(0x160B26, ui));
        g.fill(x, ty, x + Math.round(sliderW * value / 100.0f), ty + 5, argb(color, 0.7f * ui));
        g.renderOutline(x, ty, sliderW, 5, argb(accent(), 0.5f * ui));
        int hx = x + Math.round(sliderW * value / 100.0f);
        g.fill(hx - 2, ty - 3, hx + 2, ty + 8, argb(color, ui));
        g.fill(hx - 1, ty - 2, hx + 1, ty + 7, argb(0xF0E8FF, ui));
        sliderX[index] = x;
        sliderY[index] = ty - 5;
    }

    private int sliderAt(double mx, double my) {
        for (int i = 0; i < 2; i++) {
            if (sliderW > 0 && my >= sliderY[i] && my < sliderY[i] + 20 && mx >= sliderX[i] - 4 &&
                    mx <= sliderX[i] + sliderW + 4) {
                return i;
            }
        }
        return -1;
    }

    private void setFromMouse(int which, double mx) {
        int value = (int) Math.round(Mth.clamp((mx - sliderX[which]) / sliderW, 0.0, 1.0) * 100.0);
        int key = which * 1000 + value;
        if (key == lastSent) return;
        lastSent = key;
        send(which == 0 ? NullWellControls.FEED : NullWellControls.DRAW, value);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && seconds >= introSeconds() && uiAlpha() > 0.6f) {
            int which = sliderAt(vx(mx), vy(my));
            if (which >= 0) {
                dragging = which;
                lastSent = -1;
                setFromMouse(which, vx(mx));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) {
            setFromMouse(dragging, vx(mx));
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
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int which = sliderAt(vx(mx), vy(my));
        NullWellMachine m = machine();
        if (which >= 0 && m != null) {
            int now = which == 0 ? m.feedSetting() : m.drawSetting();
            send(which == 0 ? NullWellControls.FEED : NullWellControls.DRAW,
                    (int) Mth.clamp(now + Math.signum(scrollY) * 5, 0, 100));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
