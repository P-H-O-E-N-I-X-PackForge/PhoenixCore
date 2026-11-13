package net.phoenix.core.client.infuser;

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
import net.phoenix.core.common.machine.multiblock.infuser.InfuserPackets;
import net.phoenix.core.common.machine.multiblock.infuser.PhoenixInfuserMachine;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class InfuserScreen extends CineScreen {

    private static final int C_DIM = 0xA08870;
    private static final int C_WHITE = 0xFFF1DC;
    private static final int C_GREEN = 0x7CF0B0;
    private static final int C_GOLD = 0xFFC44D;
    private static final int C_RED = 0xFF5A4A;
    private static final int C_FIRE = 0xFF8A3A;

    private final BlockPos pos;
    private int lastBurned = -1;
    private int lastRenewed = -1;
    private float burstAt = -10f;
    private float ringAt = -10f;
    private boolean wasLit;

    public InfuserScreen(BlockPos pos) {
        super("Phoenix Infuser", "infuser");
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    private PhoenixInfuserMachine machine() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return MetaMachine.getMachine(level, pos) instanceof PhoenixInfuserMachine m ? m : null;
    }

    @Override
    protected int accent() {
        return C_FIRE;
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
    protected float introSeconds() {
        return 6.0f;
    }

    @Override
    protected String introTitle() {
        return "PHOENIX INFUSER";
    }

    @Override
    protected String introSub() {
        return "It burns, and then it rises.";
    }

    private void send(int which, int value) {
        PacketDistributor.sendToServer(new InfuserPackets.Control(pos, which, value));
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        PhoenixInfuserMachine m = machine();
        int avail = width - panelW();
        float cx = panelW() + avail / 2f;
        float baseY = height * 0.78f;
        float R = Math.min(avail * 0.4f, height * 0.45f);

        if (m == null || !m.isFormed()) {
            g.drawCenteredString(font, "Structure not formed.", (int) cx, height / 2 - 4, argb(C_DIM, 0.9f));
            return;
        }
        boolean on = m.lit();
        float heat = (float) (m.heat() / 100.0);
        float grow = ease(Mth.clamp(seconds / (introSeconds() * 0.85f), 0f, 1f));
        float flame = (on ? 0.25f + 0.75f * heat : 0.06f) * grow;

        if (lastBurned >= 0 && m.burned() != lastBurned) burstAt = seconds;
        lastBurned = m.burned();
        if (lastRenewed >= 0 && m.renewed() != lastRenewed) {
            ringAt = seconds;
            flash = 0.35f;
            flashColor = 0xFFD24A;
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.0f);
        }
        lastRenewed = m.renewed();
        if (on && !wasLit && seconds > 0.5f) {
            flash = 0.5f;
            flashColor = 0xFF8A3A;
            play(SoundEvents.FIRECHARGE_USE, 0.9f, 0.9f);
        }
        wasLit = on;

        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin();
        float flick = 0.9f + 0.1f * Mth.sin(seconds * 9f) + 0.05f * Mth.sin(seconds * 17f);

        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 4; k++) {
                float open = 0.4f + 0.5f * flame;
                float rr = R * (0.45f + 0.16f * k) * grow;
                float start = side < 0 ? Mth.PI * (1.0f + 0.5f * (1f - open)) : -Mth.HALF_PI * (1f - open) - 0.15f;
                float sweep = Mth.HALF_PI * (0.5f + 0.5f * open);
                ring(buf, pose, cx + side * R * 0.18f, baseY - R * 0.5f, rr, 1.2f + 2.0f * flame,
                        side < 0 ? Mth.PI + 0.2f + 0.3f * (1f - open) : -0.2f - sweep + 0.2f, sweep,
                        argb(k % 2 == 0 ? C_FIRE : 0xFFC060, (on ? 0.5f : 0.12f) * (0.4f + 0.6f * flame)));
            }
        }

        for (int k = 5; k >= 1; k--) {
            float rr = R * (0.12f + 0.09f * k) * (0.5f + flame) * flick;
            int c = k > 3 ? 0xFF4A1A : k > 1 ? 0xFF9A3A : 0xFFE08A;
            disc(buf, pose, cx, baseY - rr * 1.1f - k * 2f, rr, argb(c, (on ? 0.4f : 0.1f) + 0.08f * (5 - k)),
                    argb(c, 0f));
        }
        disc(buf, pose, cx, baseY - R * 0.12f * (0.5f + flame), R * 0.1f * (0.5f + flame),
                argb(0xFFFFFF, on ? 0.9f : 0.2f),
                argb(0xFFE08A, 0f));

        int embers = 8 + (int) (flame * 50f);
        for (int i = 0; i < embers; i++) {
            float life = (seconds * (0.18f + 0.2f * (i % 5) / 5f) + i * 0.381966f) % 1f;
            float drift = Mth.sin(seconds * 1.1f + i * 1.7f) * R * 0.25f * life;
            float x = cx + drift + (((i * 37) % 17) - 8) * R * 0.012f;
            float y = baseY - life * R * (1.1f + 0.5f * flame);
            disc(buf, pose, x, y, 2.4f * (1f - life * 0.6f),
                    argb(life < 0.5f ? 0xFFD060 : 0xFF8A3A, (on ? 0.9f : 0.25f) *
                            (1f - life)),
                    argb(0xFF8A3A, 0f));
        }

        float sinceBurst = seconds - burstAt;
        if (sinceBurst >= 0f && sinceBurst < 1.0f) {
            for (int i = 0; i < 24; i++) {
                float a = i * 2.399f;
                float d = R * 0.7f * sinceBurst * (0.5f + ((i * 13) % 10) / 10f);
                disc(buf, pose, cx + Mth.cos(a) * d, baseY - R * 0.3f + Mth.sin(a) * d * 0.8f, 2.6f,
                        argb(0xFFE08A, 1f - sinceBurst), argb(0xFF8A3A, 0f));
            }
        }

        float sinceRing = seconds - ringAt;
        if (sinceRing >= 0f && sinceRing < 1.4f) {
            float f = sinceRing / 1.4f;
            ring(buf, pose, cx, baseY - R * 0.3f, R * (0.2f + 0.9f * f), 2.5f * (1f - f) + 0.5f, 0f, Mth.TWO_PI,
                    argb(0xFFD24A, 0.9f * (1f - f)));
        }

        float pile = Mth.clamp(m.ash() / (float) PhoenixInfuserMachine.MAX_ASH, 0f, 1f);
        for (int k = 0; k < 4; k++) {
            disc(buf, pose, cx + (k - 1.5f) * R * 0.16f, baseY + 6f, R * (0.1f + 0.2f * pile), argb(0x8A8078, 0.5f),
                    argb(0x8A8078, 0f));
        }
        line(buf, pose, cx - R * 0.8f, baseY + 10f, cx + R * 0.8f, baseY + 10f, 1.0f, argb(0x6A5040, 0.6f));
        end(buf);

        if (!on) {
            float pulse = 0.55f + 0.45f * Mth.sin(seconds * 2.2f);
            g.drawCenteredString(font, "DORMANT", (int) cx, (int) (baseY + 22), argb(C_GOLD, 0.9f * pulse));
            g.drawCenteredString(font, "It is cold until it is given power.", (int) cx, (int) (baseY + 34),
                    argb(C_DIM, 0.9f));
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
        PhoenixInfuserMachine m = machine();
        int x = 14;
        int w = panelW() - 28;
        int y = bar() + 14;
        heading(g, "PHOENIX INFUSER", x, y, ui);
        y += 20;
        if (m == null || !m.isFormed()) {
            g.drawString(font, "The structure is not formed.", x, y, argb(C_DIM, ui), false);
            button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
            return;
        }
        boolean renew = m.mode() == PhoenixInfuserMachine.RENEW;
        String status;
        int statusColor;
        if (!m.lit()) {
            status = "DORMANT";
            statusColor = C_GOLD;
        } else if (!m.powerOk()) {
            status = "NO POWER";
            statusColor = C_RED;
        } else if (renew && m.heat() < PhoenixInfuserMachine.RENEW_MIN_HEAT) {
            status = "TOO COLD TO RENEW";
            statusColor = C_GOLD;
        } else {
            status = renew ? "RENEWING" : "BURNING";
            statusColor = C_GREEN;
        }
        g.drawString(font, status, x, y, argb(statusColor, ui), false);
        y += 18;

        int half = (w - 6) / 2;
        button(g, x, y, half, 16, "BURN", mx, my, true, ui, () -> send(0, PhoenixInfuserMachine.BURN));
        button(g, x + half + 6, y, half, 16, "RENEW", mx, my, true, ui, () -> send(0, PhoenixInfuserMachine.RENEW));
        int sel = renew ? x + half + 6 : x;
        g.fill(sel, y + 17, sel + half, y + 19, argb(C_FIRE, ui));
        y += 28;

        stat(g, "HEAT", String.format(Locale.ROOT, "%.0f%%", m.heat()), C_WHITE, x, y, ui);
        bar(g, x, y + 21, w, (float) (m.heat() / 100.0), m.heat() > 66 ? 0xFFE08A : C_FIRE,
                renew ? (float) (PhoenixInfuserMachine.RENEW_MIN_HEAT / 100.0) : -1f, ui);
        y += 34;
        stat(g, "ASH", m.ash() + " of " + PhoenixInfuserMachine.MAX_ASH, C_WHITE, x, y, ui);
        bar(g, x, y + 21, w, m.ash() / (float) PhoenixInfuserMachine.MAX_ASH, 0xA89888, -1f, ui);
        y += 34;
        stat(g, "ENERGY STORED", String.format(Locale.ROOT, "%,d kEU", m.reserveKilo()), 0xFFE08A, x, y, ui);
        y += 24;
        stat(g, "GIVING", power(m.emitting()), m.emitting() > 0 ? 0xFFE08A : C_DIM, x, y, ui);
        y += 24;
        stat(g, "UPKEEP", m.lit() ? power(m.energyUse()) : "needs power", m.lit() && !m.powerOk() ? C_RED : C_WHITE,
                x, y, ui);
        y += 24;
        g.drawString(font, "burned " + m.burned() + "   renewed " + m.renewed(), x, y, argb(C_DIM, ui), false);
        y += 16;
        String tip = renew ? "Damaged items only. Burn things first for heat and ash." :
                "Feed it anything. It burns to ash, for heat and energy.";
        wrapped(g, tip, x, y, w, 0xC8B098, ui);

        button(g, 14, height - bar() - 24, 56, 15, "Close", mx, my, true, ui, this::onClose);
    }

    private void bar(GuiGraphics g, int x, int y, int w, float frac, int color, float mark, float ui) {
        g.fill(x, y, x + w, y + 4, argb(0x2A1810, ui));
        g.fill(x, y, x + (int) (w * Mth.clamp(frac, 0f, 1f)), y + 4, argb(color, ui));
        if (mark >= 0f) g.fill(x + (int) (w * mark), y - 2, x + (int) (w * mark) + 1, y + 6, argb(C_GOLD, ui));
        g.renderOutline(x, y, w, 4, argb(accent(), 0.4f * ui));
    }
}
