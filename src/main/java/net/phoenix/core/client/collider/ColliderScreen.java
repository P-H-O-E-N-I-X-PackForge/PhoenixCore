package net.phoenix.core.client.collider;

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
import net.phoenix.core.common.machine.multiblock.collider.AntimatterColliderMachine;
import net.phoenix.core.common.machine.multiblock.collider.ColliderPackets;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class ColliderScreen extends CineScreen {

    private static final int C_DIM = 0x9A88B8;
    private static final int C_WHITE = 0xF4ECFF;
    private static final int C_GREEN = 0x7CF0B0;
    private static final int C_GOLD = 0xFFC44D;
    private static final int C_RED = 0xFF5A6A;
    private static final int MATTER = 0x59E8FF;
    private static final int ANTI = 0xFF4DD8;

    private static final float CROSS = 0.625f;

    private final BlockPos pos;
    private int lastCollisions = -1;
    private int lastItems = -1;
    private int burstPoint;
    private float burstAt = -10f;
    private float itemAt = -10f;
    private boolean wasLit;
    private int dragging = -1;
    private int lastSent = -1;
    private int sliderX;
    private int sliderY;
    private int sliderW;

    public ColliderScreen(BlockPos pos) {
        super("Antimatter Collider", "collider");
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    private AntimatterColliderMachine machine() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return MetaMachine.getMachine(level, pos) instanceof AntimatterColliderMachine m ? m : null;
    }

    @Override
    protected int accent() {
        return ANTI;
    }

    @Override
    protected int accent2() {
        return MATTER;
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
    protected float introSeconds() {
        return 6.0f;
    }

    @Override
    protected String introTitle() {
        return "ANTIMATTER COLLIDER";
    }

    @Override
    protected String introSub() {
        return "Matter one way, antimatter the other.";
    }

    private void send(int which, int value) {
        PacketDistributor.sendToServer(new ColliderPackets.Control(pos, which, value));
    }

    private static float[] pointOn(float cx, float cy, float rx, float ry, float t) {
        return new float[] { cx + Mth.cos(t) * rx, cy + Mth.sin(t) * ry };
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        AntimatterColliderMachine m = machine();
        int avail = width - panelW();
        float cx = panelW() + avail / 2f;
        float cy = height / 2f;
        float R = Math.min(avail * 0.42f, height * 0.42f);

        if (m == null || !m.isFormed()) {
            g.drawCenteredString(font, "Structure not formed.", (int) cx, (int) cy - 4, argb(C_DIM, 0.9f));
            return;
        }
        boolean on = m.lit();
        float beam = m.beam() / 100f;
        float grow = ease(Mth.clamp(seconds / (introSeconds() * 0.85f), 0f, 1f));
        R *= grow;
        boolean powerMode = m.mode() == AntimatterColliderMachine.POWER;

        if (lastCollisions >= 0 && m.collisions() != lastCollisions && on) {
            burstAt = seconds;
            burstPoint = rng.nextInt(4);
        }
        lastCollisions = m.collisions();
        if (lastItems >= 0 && m.itemsMade() != lastItems) {
            itemAt = seconds;
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.1f, 0.7f);
        }
        lastItems = m.itemsMade();
        if (on && !wasLit && seconds > 0.5f) {
            flash = 0.5f;
            flashColor = 0xFF4DD8;
            play(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.0f);
        }
        wasLit = on;

        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin();
        float dim = on ? 1f : 0.35f;

        for (int i = 0; i < 96; i++) {
            float t0 = Mth.TWO_PI * i / 96f;
            float t1 = Mth.TWO_PI * (i + 1) / 96f;
            float[] a0 = pointOn(cx, cy, R, R * 0.8f, t0);
            float[] a1 = pointOn(cx, cy, R, R * 0.8f, t1);
            line(buf, pose, a0[0], a0[1], a1[0], a1[1], 1.4f, argb(MATTER, 0.35f * dim));
            float[] b0 = pointOn(cx, cy, R * 0.8f, R, t0);
            float[] b1 = pointOn(cx, cy, R * 0.8f, R, t1);
            line(buf, pose, b0[0], b0[1], b1[0], b1[1], 1.4f, argb(ANTI, 0.35f * dim));
        }

        int dots = on ? 4 + (int) (beam * 14f) : 2;
        float speed = 0.25f + 1.8f * beam;
        for (int i = 0; i < dots; i++) {
            float ta = seconds * speed + i * Mth.TWO_PI / dots;
            float[] a = pointOn(cx, cy, R, R * 0.8f, ta);
            disc(buf, pose, a[0], a[1], 7f, argb(MATTER, 0.9f * dim), argb(MATTER, 0f));
            float tb = -seconds * speed + i * Mth.TWO_PI / dots + 0.4f;
            float[] b = pointOn(cx, cy, R * 0.8f, R, tb);
            disc(buf, pose, b[0], b[1], 7f, argb(ANTI, 0.9f * dim), argb(ANTI, 0f));
        }

        for (int q = 0; q < 4; q++) {
            float sx = (q % 2 == 0 ? 1f : -1f) * CROSS * R;
            float sy = (q < 2 ? 1f : -1f) * CROSS * R;
            polygon(buf, pose, cx + sx, cy + sy, 11f, 6, seconds * 0.2f, 1.2f, argb(C_WHITE, 0.6f * dim));
        }

        float since = seconds - burstAt;
        if (since >= 0f && since < 0.9f) {
            float sx = (burstPoint % 2 == 0 ? 1f : -1f) * CROSS * R;
            float sy = (burstPoint < 2 ? 1f : -1f) * CROSS * R;
            float f = since / 0.9f;
            disc(buf, pose, cx + sx, cy + sy, 26f * (1f - f) + 4f, argb(0xFFFFFF, 0.9f * (1f - f)), argb(ANTI, 0f));
            for (int k = 0; k < 14; k++) {
                float a = k * 0.449f + burstPoint;
                float len = (14f + 60f * f) * (0.5f + ((k * 7) % 10) / 10f);
                int c = k % 2 == 0 ? MATTER : ANTI;
                line(buf, pose, cx + sx, cy + sy, cx + sx + Mth.cos(a) * len, cy + sy + Mth.sin(a) * len, 1.0f,
                        argb(c, 0.8f * (1f - f)));
            }
        }

        float fill = Mth.clamp(m.trap() / (float) AntimatterColliderMachine.TRAP_CAP, 0f, 1f);
        float tr = R * 0.1f + R * 0.3f * (float) Math.sqrt(fill);
        disc(buf, pose, cx, cy, tr * 1.8f, argb(ANTI, (on ? 0.22f : 0.06f) * (0.4f + fill)), argb(ANTI, 0f));
        disc(buf, pose, cx, cy, tr, argb(0xFFFFFF, on ? 0.9f : 0.25f), argb(ANTI, on ? 0.7f : 0.2f));
        ring(buf, pose, cx, cy, R * 0.42f, 1.0f, seconds * 0.3f, Mth.TWO_PI * 0.7f, argb(C_WHITE, 0.25f * dim));

        if (on && m.emitting() > 0 && powerMode) {
            for (int k = 0; k < 12; k++) {
                float a = k * Mth.TWO_PI / 12f + seconds * 0.15f;
                float pulse = 0.5f + 0.5f * Mth.sin(seconds * 6f + k);
                line(buf, pose, cx + Mth.cos(a) * tr, cy + Mth.sin(a) * tr, cx + Mth.cos(a) * R * 0.4f,
                        cy + Mth.sin(a) * R * 0.4f, 0.8f, 2.6f, argb(0xFFE08A, 0f), argb(0xFFE08A, 0.7f * pulse));
            }
        }
        float sinceItem = seconds - itemAt;
        if (sinceItem >= 0f && sinceItem < 1.2f) {
            float f = sinceItem / 1.2f;
            for (int k = 0; k < 5; k++) {
                float a = k * 1.26f + 0.3f;
                quad(buf, pose, cx + Mth.cos(a) * (tr + R * 0.5f * f), cy + Mth.sin(a) * (tr + R * 0.5f * f), 4f,
                        argb(0xE8F0FF, 1f - f));
            }
        }
        end(buf);

        if (!on) {
            float pulse = 0.55f + 0.45f * Mth.sin(seconds * 2.2f);
            g.drawCenteredString(font, "DORMANT", (int) cx, (int) (cy + R + 14), argb(C_GOLD, 0.9f * pulse));
            g.drawCenteredString(font, "The beams do not run until it is given power.", (int) cx,
                    (int) (cy + R + 26), argb(C_DIM, 0.9f));
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
        AntimatterColliderMachine m = machine();
        int x = 14;
        int w = panelW() - 28;
        int y = bar() + 14;
        heading(g, "ANTIMATTER COLLIDER", x, y, ui);
        y += 20;
        if (m == null || !m.isFormed()) {
            g.drawString(font, "The structure is not formed.", x, y, argb(C_DIM, ui), false);
            button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
            return;
        }
        boolean powerMode = m.mode() == AntimatterColliderMachine.POWER;
        String status;
        int statusColor;
        if (!m.lit()) {
            status = "DORMANT";
            statusColor = C_GOLD;
        } else if (!m.powerOk()) {
            status = "NO POWER";
            statusColor = C_RED;
        } else if (m.beam() > 0 && !m.matterOk()) {
            status = "NO MATTER";
            statusColor = C_GOLD;
        } else if (m.beam() == 0) {
            status = "BEAMS OFF";
            statusColor = C_DIM;
        } else {
            status = "COLLIDING";
            statusColor = C_GREEN;
        }
        g.drawString(font, status, x, y, argb(statusColor, ui), false);
        y += 18;

        int half = (w - 6) / 2;
        button(g, x, y, half, 16, "POWER", mx, my, true, ui, () -> send(1, AntimatterColliderMachine.POWER));
        button(g, x + half + 6, y, half, 16, "ITEMS", mx, my, true, ui, () -> send(1, AntimatterColliderMachine.ITEMS));
        int sel = powerMode ? x : x + half + 6;
        g.fill(sel, y + 17, sel + half, y + 19, argb(ANTI, ui));
        y += 28;

        g.drawString(font, "BEAM", x, y, argb(C_DIM, ui), false);
        String v = m.beam() + "%";
        g.drawString(font, v, x + w - font.width(v), y, argb(C_WHITE, ui), false);
        sliderX = x;
        sliderW = w;
        sliderY = y + 8;
        int ty = y + 13;
        g.fill(x, ty, x + w, ty + 5, argb(0x1A0C2A, ui));
        g.fill(x, ty, x + Math.round(w * m.beam() / 100f), ty + 5, argb(ANTI, 0.7f * ui));
        g.renderOutline(x, ty, w, 5, argb(accent(), 0.5f * ui));
        int hx = x + Math.round(w * m.beam() / 100f);
        g.fill(hx - 2, ty - 3, hx + 2, ty + 8, argb(ANTI, ui));
        g.fill(hx - 1, ty - 2, hx + 1, ty + 7, argb(0xF4ECFF, ui));
        y += 34;

        stat(g, "TRAP", String.format(Locale.ROOT, "%,d of %,d", m.trap(), AntimatterColliderMachine.TRAP_CAP),
                C_WHITE, x, y, ui);
        float fill = m.trap() / (float) AntimatterColliderMachine.TRAP_CAP;
        g.fill(x, y + 21, x + w, y + 25, argb(0x1A0C2A, ui));
        g.fill(x, y + 21, x + (int) (w * Mth.clamp(fill, 0f, 1f)), y + 25, argb(fill > 0.9f ? C_RED : ANTI, ui));
        g.renderOutline(x, y + 21, w, 4, argb(accent(), 0.4f * ui));
        y += 34;
        stat(g, "UPKEEP", m.lit() ? power(m.energyUse()) : "needs power", m.lit() && !m.powerOk() ? C_RED : C_WHITE,
                x, y, ui);
        y += 24;
        if (powerMode) {
            stat(g, "ANNIHILATING", power(m.emitting()), m.emitting() > 0 ? 0xFFE08A : C_DIM, x, y, ui);
        } else {
            stat(g, "CONDENSED", m.itemsMade() + " items", m.itemsMade() > 0 ? 0xE8F0FF : C_DIM, x, y, ui);
        }
        y += 24;
        String tip = powerMode ? "Antimatter in the trap is annihilated for energy." :
                "Antimatter condenses into rare materials: 50 for each item.";
        wrapped(g, tip + " Shift-click the controller to switch.", x, y, w, 0xB8A8D0, ui);

        button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
    }

    private boolean overSlider(double mx, double my) {
        return sliderW > 0 && my >= sliderY && my < sliderY + 20 && mx >= sliderX - 4 && mx <= sliderX + sliderW + 4;
    }

    private void setFromMouse(double mx) {
        int value = (int) Math.round(Mth.clamp((mx - sliderX) / sliderW, 0.0, 1.0) * 100.0);
        if (value == lastSent) return;
        lastSent = value;
        send(0, value);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && seconds >= introSeconds() && uiAlpha() > 0.6f && overSlider(vx(mx), vy(my))) {
            dragging = 1;
            lastSent = -1;
            setFromMouse(vx(mx));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) {
            setFromMouse(vx(mx));
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
        AntimatterColliderMachine m = machine();
        if (overSlider(vx(mx), vy(my)) && m != null) {
            send(0, (int) Mth.clamp(m.beam() + Math.signum(scrollY) * 5, 0, 100));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
