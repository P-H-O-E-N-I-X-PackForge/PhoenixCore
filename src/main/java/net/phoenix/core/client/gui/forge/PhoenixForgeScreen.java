package net.phoenix.core.client.gui.forge;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.client.renderer.phoenix.PhoenixModel;
import net.phoenix.core.rebirth.C2SRebirthActionPacket;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.S2CRebirthStatePacket;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PhoenixForgeScreen extends CineScreen {

    private static final float REBIRTH_SECONDS = 28.5f;
    private static final float STRIKE = 7.4f;

    private static final int FIRE = 0xFF8A1F;
    private static final int GOLD = 0xFFD25A;
    private static final int WHITE_HOT = 0xFFF2D0;
    private static final int TEXT = 0xFFEBD0;
    private static final int DIM = 0x8C735A;

    private S2CRebirthStatePacket state;
    private float rebirth = -1f;
    private int colA = GOLD;
    private int colB = FIRE;
    private int colC = WHITE_HOT;
    private EndingLook.Plan plan = EndingLook.Plan.DEFAULT;
    private boolean stingPlayed;
    private int beat;
    private int pylonsFired;
    private final List<Integer> order = new ArrayList<>();

    public PhoenixForgeScreen(S2CRebirthStatePacket state) {
        super("Phoenix Forge", "forge");
        this.state = state;
        for (int v : state.ring()) order.add(v);
        flashColor = WHITE_HOT;
    }

    public BlockPos pos() {
        return state.forge();
    }

    public void update(S2CRebirthStatePacket next) {
        this.state = next;
        if (!next.message().isEmpty()) {
            message = next.message();
            messageColor = next.closed() || next.ignited() ? 0xFFFFD25A : TEXT;
        }
        if (next.ignited()) {

            if (seconds < introSeconds()) seconds = introSeconds();
            rebirth = 0f;
            beat = 0;
            pylonsFired = 0;
            stingPlayed = false;
            flash = 0f;
            shake = 0f;
            plan = EndingLook.plan(ClientEnding.current());
        }
        if (next.closed()) order.clear();
    }

    @Override
    protected int accent() {
        return FIRE;
    }

    @Override
    protected int accent2() {
        return GOLD;
    }

    @Override
    protected int bgTop() {
        return 0x050202;
    }

    @Override
    protected int bgBottom() {
        return 0x1A0803;
    }

    @Override
    protected float introSeconds() {
        return 7f;
    }

    @Override
    protected String introTitle() {
        return "THE PHOENIX FORGE";
    }

    @Override
    protected String introSub() {
        return "Every fire, and one that was always going to be lit.";
    }

    @Override
    protected int panelW() {
        return Mth.clamp((int) (width * 0.2f), 130, 210);
    }

    @Override
    protected float uiAlpha() {
        if (rebirth >= 0f) {
            if (rebirth < 1.5f) return 1f - rebirth / 1.5f;
            if (rebirth > REBIRTH_SECONDS - 3f) return Mth.clamp((rebirth - (REBIRTH_SECONDS - 3f)) / 3f, 0f, 1f);
            return 0f;
        }
        return super.uiAlpha();
    }

    private int linked() {
        int n = 0;
        for (int i = 0; i < Capstone.COUNT; i++) if (state.isBound(i)) n++;
        return n;
    }

    private boolean allBound() {
        return linked() == Capstone.COUNT;
    }

    private boolean live() {
        return state.closed() || state.reborn();
    }

    private float heat() {
        float h = linked() / (float) Capstone.COUNT * 0.5f;
        if (state.closed()) h = 0.8f;
        if (state.reborn()) h = 1f;
        return h;
    }

    private float radius() {
        if (Capstone.COUNT > 8) {

            float room = width / 2f - panelW() - 14f;
            float tall = (height * 0.5f - bar() - 16f) / 1.2f;
            return Math.max(36f, Math.min(room / 1.55f, tall));
        }
        int label = 0;
        for (Capstone c : Capstone.values()) label = Math.max(label, font.width(c.shortName));
        float room = width / 2f - panelW() - label / 2f - 10f;
        return Math.max(40f, Math.min(Math.min(width * 0.2f, height * 0.3f), room / 1.55f));
    }

    private float cx() {
        return width / 2f;
    }

    private float cy() {
        return height * 0.5f;
    }

    private float[] pylon(int i) {
        double a = Math.toRadians(-90 + 360.0 * i / Capstone.COUNT);
        float squash = Capstone.COUNT > 6 ? 1.2f : 1.05f;
        return new float[] { cx() + (float) Math.cos(a) * radius() * 1.55f,
                cy() + (float) Math.sin(a) * radius() * squash };
    }

    private float pylonSize() {
        return Math.min(width, height) * 0.07f * Math.min(1f, 6f / Capstone.COUNT);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        if (rebirth >= 0f) {
            rebirth += dt;
            tickRebirth();
            if (rebirth > REBIRTH_SECONDS) rebirth = -1f;
        }
        boolean witnessing = rebirth >= 0f;
        float heat = heat();

        float surge = witnessing ? 1f + 2.2f * ease(rebirth / STRIKE) : 1f;
        float coreGrow = 1f + 0.35f * (surge - 1f);
        float r = radius();
        Matrix4f pose = g.pose().last().pose();

        BufferBuilder buf = begin();

        float col = (0.04f + 0.16f * heat) * surge;
        buf.addVertex(pose, cx() - 6f, 0, 0).setColor(argb(GOLD, 0f));
        buf.addVertex(pose, cx() - r * 0.7f, cy(), 0).setColor(argb(GOLD, col));
        buf.addVertex(pose, cx() + r * 0.7f, cy(), 0).setColor(argb(GOLD, col));
        buf.addVertex(pose, cx() + 6f, 0, 0).setColor(argb(GOLD, 0f));
        rays(buf, pose, cx(), cy(), r * 0.35f, r * 3.6f, 12, seconds * 0.035f, 15f + 9f * heat,
                GOLD, Math.min(0.2f, (0.02f + 0.07f * heat) * surge));
        rays(buf, pose, cx(), cy(), r * 0.35f, r * 2.4f, 12, -seconds * 0.05f + 0.26f, 9f, FIRE,
                Math.min(0.16f, (0.015f + 0.05f * heat) * surge));

        float lit = 0.25f + 0.6f * heat;
        ring(buf, pose, cx(), cy(), r, 0.9f, 0f, Mth.TWO_PI, argb(FIRE, 0.5f * lit * surge));
        ring(buf, pose, cx(), cy(), r * 1.28f, 0.6f, seconds * 0.12f, Mth.TWO_PI * 0.82f, argb(GOLD, 0.35f * lit));
        ring(buf, pose, cx(), cy(), r * 0.72f, 0.6f, -seconds * 0.2f, Mth.TWO_PI * 0.7f, argb(GOLD, 0.4f * lit));
        ticks(buf, pose, cx(), cy(), r * 1.28f + 3f, 72, 6, seconds * 0.12f, 3f, argb(GOLD, 0.35f * lit));
        ticks(buf, pose, cx(), cy(), r - 8f, 48, 4, -seconds * 0.08f, 3f, argb(FIRE, 0.35f * lit));
        polygon(buf, pose, cx(), cy(), r * 1.0f, 4, seconds * 0.05f, 0.7f, argb(GOLD, 0.25f * lit));
        polygon(buf, pose, cx(), cy(), r * 1.0f, 4, seconds * 0.05f + (float) Math.PI / 4f, 0.7f,
                argb(GOLD, 0.25f * lit));

        int count = (int) Math.min(220, (70 + 120 * heat) * surge);
        for (int i = 0; i < count; i++) {
            float x = cx() + (noise(i, 1f) - 0.5f) * r * 3.2f + (float) Math.sin(seconds * 0.7f + i) * 8f;
            float speed = (14f + noise(i, 2f) * 30f) * (0.6f + heat) * surge;
            float y = cy() + r * 1.1f - ((seconds * speed + noise(i, 3f) * height) % (height * 0.8f));
            float a = 0.15f + 0.4f * (0.5f + 0.5f * (float) Math.sin(seconds * 2.4f + i * 1.3f));
            quad(buf, pose, x, y, 0.8f + noise(i, 4f) * 1.4f, argb(i % 4 == 0 ? WHITE_HOT : GOLD, a * (0.4f + heat)));
        }

        float pulse = 0.92f + 0.08f * (float) Math.sin(seconds * 3f);
        for (int k = 4; k >= 1; k--) {
            disc(buf, pose, cx(), cy(), r * 0.16f * (0.6f + k * 0.5f) * pulse * coreGrow,
                    argb(k == 1 ? WHITE_HOT : FIRE,
                            Math.min(1f, (k == 1 ? 0.9f : 0.12f) * (0.2f + 0.8f * heat) * surge)),
                    argb(FIRE, 0f));
        }
        confluxSigil(buf, pose, r, heat);
        end(buf);

        drawPylons(g, mx, my, surge);
        if (witnessing && rebirth < STRIKE + 0.3f) drawIgnition(g);
        if (witnessing) drawRebirth(g);
    }

    private void drawPylons(GuiGraphics g, int mx, int my, float surge) {
        Matrix4f pose = g.pose().last().pose();
        float size = pylonSize();
        long frame = (long) (seconds * 14f);

        BufferBuilder buf = begin();
        for (int i = 0; i < Capstone.COUNT; i++) {
            Capstone cap = Capstone.values()[i];
            float[] p = pylon(i);
            boolean bound = state.isBound(i);
            boolean awake = bound && live();
            float lit = awake ? 1f : bound ? 0.55f : 0.14f;
            int color = bound ? cap.color : 0x777777;
            float breath = 0.92f + 0.08f * (float) Math.sin(seconds * 2f + i * 1.7f);

            polyFill(buf, pose, p[0], p[1], size * 1.1f, 6, (float) Math.PI / 6f, argb(color, 0.1f * lit * breath),
                    argb(color, 0.02f * lit));
            polygon(buf, pose, p[0], p[1], size * 1.1f, 6, (float) Math.PI / 6f, 1.2f, argb(color, 0.6f * lit));
            polygon(buf, pose, p[0], p[1], size * 0.92f, 6, (float) Math.PI / 6f, 0.5f, argb(color, 0.3f * lit));
            if (awake) disc(buf, pose, p[0], p[1], size * 1.7f, argb(color, 0.14f), argb(color, 0f));
            sigil(buf, pose, cap, p[0], p[1], size, lit, color, frame, breath);

            if (awake) {
                float pulse = 0.55f + 0.45f * (float) Math.sin(seconds * 4f - i);
                line(buf, pose, p[0], p[1], cx(), cy(), 1.1f * surge, 2.8f * surge, argb(color, 0.1f * surge),
                        argb(GOLD, Math.min(0.9f, 0.38f * pulse * surge)));
                for (int k = 0; k < 3; k++) {
                    float t = (seconds * 0.45f + k / 3f + i * 0.17f) % 1f;
                    disc(buf, pose, Mth.lerp(t, p[0], cx()), Mth.lerp(t, p[1], cy()), 3.4f,
                            argb(0xFFFFFF, 0.9f * (1f - t)),
                            argb(color, 0f));
                }
            }
            if (allBound() && !state.closed() && rebirth < 0f && seconds > introSeconds()) {
                final int idx = i;
                hits.add(new Hit((int) (p[0] - size), (int) (p[1] - size), (int) (size * 2), (int) (size * 2),
                        () -> toggleOrder(idx)));
            }
        }
        end(buf);

        int hover = hoveredPylon(mx, my, size);
        for (int i = 0; i < Capstone.COUNT; i++) {
            float[] p = pylon(i);
            Capstone cap = Capstone.values()[i];
            boolean bound = state.isBound(i);
            int color = bound ? cap.color : 0x998877;
            float ui = uiAlpha();
            boolean crowded = Capstone.COUNT > 8;
            if (!crowded || i == hover) {
                String name = cap.shortName;
                float ty = p[1] + size * 1.35f;
                float tx = p[0];
                if (crowded) {
                    int tw = font.width(name);
                    tx = Mth.clamp(tx, tw / 2f + 6f, width - tw / 2f - 6f);
                    g.fill((int) (tx - tw / 2f) - 3, (int) ty - 2, (int) (tx + tw / 2f) + 3, (int) ty + 10,
                            argb(0x000000, 0.75f * ui));
                    g.renderOutline((int) (p[0] - size), (int) (p[1] - size), (int) (size * 2), (int) (size * 2),
                            argb(color, 0.8f * ui));
                }
                glowTextPlain(g, name, tx, ty, color, ui);
            }

            if (width >= 1100 && Capstone.COUNT <= 6) {
                String status = !bound ? "unbound" : live() ? "linked" : "bound";
                glowTextPlain(g, status, p[0], p[1] + size * 1.35f + 11f, bound ? 0xCCBB99 : 0x776655, ui * 0.9f);
            }
            int idx = order.indexOf(i);
            if (idx >= 0) {
                g.drawCenteredString(font, Integer.toString(idx + 1), (int) p[0], (int) (p[1] - 4), 0xFFFFFFFF);
            }
        }
    }

    private int hoveredPylon(int mx, int my, float size) {
        if (listHover >= 0) return listHover;
        int best = -1;
        float bestD = size * size * 2.6f;
        for (int i = 0; i < Capstone.COUNT; i++) {
            float[] p = pylon(i);
            float d = (p[0] - mx) * (p[0] - mx) + (p[1] - my) * (p[1] - my);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private void glowTextPlain(GuiGraphics g, String text, float cx, float y, int color, float alpha) {
        if (alpha <= 0.02f) return;
        g.drawString(font, text, (int) (cx - font.width(text) / 2f), (int) y, argb(color, alpha), false);
    }

    private void toggleOrder(int capstone) {
        if (!order.remove((Integer) capstone) && order.size() < Capstone.COUNT) order.add(capstone);
        play(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 0.5f);
    }

    private void sigil(BufferBuilder buf, Matrix4f pose, Capstone cap, float x, float y, float size, float lit,
                       int color,
                       long frame, float breath) {
        switch (cap) {
            case TESLA -> {
                for (int k = 0; k < 4; k++) {
                    float a = Mth.TWO_PI * k / 4f + 0.5f;
                    if (noise(frame, k) < 0.2f + 0.6f * lit) {
                        bolt(buf, pose, x, y, x + (float) Math.cos(a) * size * 0.8f,
                                y + (float) Math.sin(a) * size * 0.8f,
                                size * 0.25f, frame * 31L + k, color, 0.85f * lit);
                    }
                }
                disc(buf, pose, x, y, size * 0.22f, argb(0xFFFFFF, 0.9f * lit), argb(color, 0f));
            }
            case HONEY -> polyFill(buf, pose, x, y, size * 0.6f * breath, 6, 0f, argb(0xFFFFFF, 0.8f * lit),
                    argb(color, 0.6f * lit));
            case NULL_WELL -> {
                disc(buf, pose, x, y, size * 0.34f, argb(0x000000, 0.95f * lit), argb(0x000000, 0.95f * lit));
                ring(buf, pose, x, y, size * 0.42f, 1.6f, seconds * 1.6f, Mth.TWO_PI * 0.7f, argb(color, 0.9f * lit));
                ring(buf, pose, x, y, size * 0.62f, 0.9f, -seconds * 1.1f, Mth.TWO_PI * 0.45f, argb(color, 0.5f * lit));
            }
            case ASCENSION_REACTOR -> {
                polyFill(buf, pose, x, y, size * 0.3f, 4, Mth.PI / 4f, argb(0xFFFFFF, 0.9f * lit),
                        argb(color, 0.7f * lit));
                ring(buf, pose, x, y, size * 0.6f, 1.4f, seconds * 0.8f, Mth.TWO_PI * 0.85f, argb(color, 0.8f * lit));
                for (int k = 0; k < 4; k++) {
                    float a = k * Mth.HALF_PI + seconds * 0.8f;
                    disc(buf, pose, x + (float) Math.cos(a) * size * 0.6f, y + (float) Math.sin(a) * size * 0.6f,
                            size * 0.07f, argb(0xFFFFFF, 0.9f * lit), argb(color, 0f));
                }
            }
            case ANTIMATTER_COLLIDER -> {
                ring(buf, pose, x, y, size * 0.6f, 1.2f, 0f, Mth.TWO_PI, argb(0x59E8FF, 0.6f * lit));
                ring(buf, pose, x, y, size * 0.36f, 1.2f, 0f, Mth.TWO_PI, argb(color, 0.6f * lit));
                float ang = seconds * 2.6f;
                disc(buf, pose, x + (float) Math.cos(ang) * size * 0.6f, y + (float) Math.sin(ang) * size * 0.6f,
                        size * 0.1f, argb(0x59E8FF, 0.95f * lit), argb(0x59E8FF, 0f));
                disc(buf, pose, x + (float) Math.cos(-ang) * size * 0.36f, y + (float) Math.sin(-ang) * size * 0.36f,
                        size * 0.1f, argb(color, 0.95f * lit), argb(color, 0f));
            }
            case PHOENIX_INFUSER -> {
                disc(buf, pose, x, y + size * 0.1f, size * 0.3f, argb(0xFFE08A, 0.95f * lit), argb(color, 0.5f * lit));
                for (int side = -1; side <= 1; side += 2) {
                    ring(buf, pose, x + side * size * 0.1f, y, size * 0.6f, 1.4f,
                            side < 0 ? Mth.PI + 0.3f : -Mth.HALF_PI, Mth.HALF_PI * 0.9f, argb(color, 0.8f * lit));
                }
            }
            case SCULK_CORTEX -> {
                float[][] pts = { { 0f, 0f }, { -0.45f, -0.3f }, { 0.4f, -0.4f }, { 0.5f, 0.25f }, { -0.35f, 0.4f } };
                for (int k = 1; k < pts.length; k++) {
                    line(buf, pose, x, y, x + pts[k][0] * size, y + pts[k][1] * size, 1.0f, argb(color, 0.6f * lit));
                }
                for (float[] pt : pts) {
                    disc(buf, pose, x + pt[0] * size, y + pt[1] * size, size * 0.12f, argb(0xFFFFFF, 0.9f * lit),
                            argb(color, 0.3f * lit));
                }
            }
            case STELLAR_FORGE -> {
                disc(buf, pose, x, y, size * 0.42f, argb(0xFFFFFF, 0.95f * lit), argb(color, 0.7f * lit));
                rays(buf, pose, x, y, size * 0.45f, size * 0.85f, 8, seconds * 0.4f, 5f, color, 0.6f * lit);
            }
            case AETHER_CRUCIBLE -> {
                ring(buf, pose, x, y + size * 0.05f, size * 0.55f, 1.6f, 0f, Mth.PI, argb(color, 0.9f * lit));
                for (int k = 0; k < 3; k++) {
                    float by = y - ((seconds * 9f + k * 7f) % (size * 0.6f)) + size * 0.2f;
                    disc(buf, pose, x + (k - 1) * size * 0.22f, by, size * 0.08f, argb(0xFFFFFF, 0.8f * lit),
                            argb(color, 0f));
                }
            }
            case TOKAMAK -> {
                ring(buf, pose, x, y, size * 0.62f, 1.2f, 0f, Mth.TWO_PI, argb(color, 0.6f * lit));
                ring(buf, pose, x, y, size * 0.3f, 1.2f, 0f, Mth.TWO_PI, argb(color, 0.6f * lit));
                float ang = seconds * 2.4f;
                disc(buf, pose, x + (float) Math.cos(ang) * size * 0.46f, y + (float) Math.sin(ang) * size * 0.46f,
                        size * 0.13f, argb(0xFFFFFF, 0.95f * lit), argb(color, 0f));
            }
            case PLASMA -> {
                ring(buf, pose, x, y, size * 0.6f, 1.4f, seconds * 2.2f, Mth.TWO_PI * 0.6f, argb(color, 0.85f * lit));
                ring(buf, pose, x, y, size * 0.4f, 1.4f, -seconds * 3.1f, Mth.TWO_PI * 0.5f,
                        argb(0xFFFFFF, 0.85f * lit));
                disc(buf, pose, x, y, size * 0.18f, argb(0xFFFFFF, 0.9f * lit), argb(color, 0f));
            }
            case CONTINUUM -> {
                disc(buf, pose, x, y, size * 0.2f, argb(0xFFFFFF, 0.9f * lit), argb(color, 0f));
                for (int k = 0; k < 2; k++) {
                    float rr = size * (0.38f + 0.22f * k);
                    ring(buf, pose, x, y, rr, 0.7f, 0f, Mth.TWO_PI, argb(color, 0.5f * lit));
                    float ang = seconds * (1.4f - 0.6f * k) + k * 2.1f;
                    disc(buf, pose, x + (float) Math.cos(ang) * rr, y + (float) Math.sin(ang) * rr, size * 0.1f,
                            argb(0xFFFFFF, 0.95f * lit), argb(color, 0f));
                }
            }
            case ENGINE -> {
                line(buf, pose, x, y + size * 0.5f, x, y - size * 0.15f, 1.8f, argb(color, 0.75f * lit));
                for (int k = 0; k < 3; k++) {
                    float by = y + size * (0.3f - 0.24f * k);
                    float len = size * (0.55f - 0.12f * k);
                    line(buf, pose, x, by, x - len, by - len * 0.7f, 1.1f, argb(color, 0.75f * lit));
                    line(buf, pose, x, by, x + len, by - len * 0.7f, 1.1f, argb(color, 0.75f * lit));
                    disc(buf, pose, x - len, by - len * 0.7f, size * 0.13f, argb(color, 0.85f * lit), argb(color, 0f));
                    disc(buf, pose, x + len, by - len * 0.7f, size * 0.13f, argb(color, 0.85f * lit), argb(color, 0f));
                }
            }
        }
    }

    private static final float[] FIRE_AT = fireTimes();

    private static float[] fireTimes() {
        float[] out = new float[Capstone.COUNT];
        for (int i = 0; i < out.length; i++) out[i] = 0.3f + 3.5f * i / Math.max(1, out.length - 1);
        return out;
    }

    private void tickRebirth() {
        float t = rebirth;

        for (int i = 0; i < Capstone.COUNT; i++) {
            if ((pylonsFired & (1 << i)) == 0 && t >= FIRE_AT[i]) {
                pylonsFired |= 1 << i;
                flash = Math.max(flash, 0.3f);
                flashColor = Capstone.values()[i].color;
                shake = Math.max(shake, 0.45f);
                switch (Capstone.values()[i]) {
                    case TESLA -> {
                        play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
                        play(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 0.6f);
                    }
                    case HONEY -> {
                        play(SoundEvents.HONEY_BLOCK_PLACE, 0.6f, 1f);
                        play(SoundEvents.BEEHIVE_ENTER, 0.7f, 1f);
                    }
                    case PLASMA -> {
                        play(SoundEvents.BEACON_POWER_SELECT, 1.4f, 0.9f);
                        play(SoundEvents.FIRECHARGE_USE, 0.7f, 0.9f);
                    }
                    case CONTINUUM -> {
                        play(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 0.9f);
                        play(SoundEvents.BEACON_POWER_SELECT, 0.7f, 0.8f);
                    }
                    case ENGINE -> {
                        play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 1f);
                        play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 0.9f);
                    }
                    case ASCENSION_REACTOR -> {
                        play(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.6f);
                        play(SoundEvents.TRIDENT_THUNDER.value(), 0.6f, 1.2f);
                    }
                    case ANTIMATTER_COLLIDER -> {
                        play(SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.4f);
                        play(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 0.9f);
                    }
                    case PHOENIX_INFUSER -> {
                        play(SoundEvents.FIRECHARGE_USE, 1.0f, 0.7f);
                        play(SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 0.9f);
                    }
                    case SCULK_CORTEX -> {
                        play(SoundEvents.SCULK_CATALYST_BLOOM, 1.0f, 0.9f);
                        play(SoundEvents.WARDEN_HEARTBEAT, 0.8f, 1.0f);
                    }
                    case STELLAR_FORGE -> {
                        play(SoundEvents.FIRECHARGE_USE, 1.0f, 0.5f);
                        play(SoundEvents.BLAZE_SHOOT, 0.6f, 0.6f);
                    }
                    case AETHER_CRUCIBLE -> {
                        play(SoundEvents.BREWING_STAND_BREW, 1.0f, 0.8f);
                        play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.2f);
                    }
                    case TOKAMAK -> {
                        play(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f);
                        play(SoundEvents.FIRECHARGE_USE, 0.8f, 0.6f);
                    }
                    case NULL_WELL -> {
                        play(SoundEvents.PORTAL_AMBIENT, 1.2f, 0.5f);
                        play(SoundEvents.WARDEN_SONIC_BOOM, 0.5f, 1.4f);
                    }
                }
            }
        }
        float[] beats = { 4.4f, 5.1f, 5.7f, 6.2f, 6.6f, 6.95f, 7.15f, 7.3f };
        while (beat < beats.length && t >= beats[beat]) {
            play(SoundEvents.WARDEN_HEARTBEAT, 0.6f + 0.08f * beat, 1f);
            shake = Math.max(shake, 0.25f + 0.08f * beat);
            beat++;
        }
        if (beat == beats.length && t >= STRIKE) {
            beat++;
            flashColor = WHITE_HOT;
            flash = 1.5f;
            shake = 1f;
            play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 0.7f);
            play(SoundEvents.ENDER_DRAGON_GROWL, 1.3f, 0.35f);
            play(SoundEvents.BEACON_ACTIVATE, 0.7f, 0.8f);
        }
        if (beat == beats.length + 1 && t >= STRIKE + 2.3f) {
            beat++;
            play(SoundEvents.FIRECHARGE_USE, 0.6f, 1f);
            play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f, 1f);
        }
        if (beat == beats.length + 2 && t >= STRIKE + 8.8f) {
            beat++;
            play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 0.8f);
        }
    }

    private void drawIgnition(GuiGraphics g) {
        float t = rebirth;
        Matrix4f pose = g.pose().last().pose();
        float size = pylonSize();
        long frame = (long) (seconds * 16f);
        BufferBuilder buf = begin();
        for (int i = 0; i < Capstone.COUNT; i++) {
            float since = t - FIRE_AT[i];
            if (since < 0f) continue;
            Capstone cap = Capstone.values()[i];
            float a = ease(since / 0.8f);
            float ramp = ease(since / (STRIKE - FIRE_AT[i]));
            float[] p = pylon(i);
            int color = cap.color;
            float dx = cx() - p[0];
            float dy = cy() - p[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float ux = dx / len;
            float uy = dy / len;

            disc(buf, pose, p[0], p[1], size * (1.6f + 2.4f * a + 0.6f * (float) Math.sin(seconds * 9f + i)),
                    argb(color, 0.4f * a), argb(color, 0f));
            buf.addVertex(pose, p[0] - 5f, p[1], 0).setColor(argb(color, 0.3f * a));
            buf.addVertex(pose, p[0] + 5f, p[1], 0).setColor(argb(color, 0.3f * a));
            buf.addVertex(pose, p[0] + 1f, 0, 0).setColor(argb(color, 0f));
            buf.addVertex(pose, p[0] - 1f, 0, 0).setColor(argb(color, 0f));

            line(buf, pose, p[0], p[1], cx(), cy(), 1.2f + 3f * ramp, 2.5f + 8f * ramp, argb(color, 0.12f * a),
                    argb(GOLD, 0.5f * a * (0.6f + 0.4f * ramp)));

            switch (cap) {
                case TESLA -> {
                    int bolts = 2 + (int) (4 * ramp);
                    for (int k = 0; k < bolts; k++) {
                        bolt(buf, pose, p[0], p[1], cx() + (noise(k, frame) - 0.5f) * 30f,
                                cy() + (noise(k, frame + 7) - 0.5f) * 30f, 28f + 30f * ramp, frame * 13L + k * 101L,
                                color,
                                0.9f * a);
                    }
                    for (int k = 0; k < 6; k++) {
                        float ang = noise(k, frame * 0.5f) * Mth.TWO_PI;
                        bolt(buf, pose, p[0], p[1], p[0] + (float) Math.cos(ang) * size * 2.2f,
                                p[1] + (float) Math.sin(ang) * size * 2.2f, size * 0.5f, frame * 17L + k, color,
                                0.8f * a);
                    }
                }
                case HONEY -> {
                    for (int k = 0; k < 4; k++) {
                        float age = (seconds * 0.9f + k * 0.25f) % 1f;
                        polygon(buf, pose, p[0], p[1], size * (0.9f + 3.6f * age), 6, k * 0.3f,
                                1.4f * (1f - age) + 0.3f, argb(color, 0.6f * (1f - age) * a));
                    }
                    int shards = 8 + (int) (10 * ramp);
                    for (int k = 0; k < shards; k++) {
                        float u = (seconds * (0.45f + 0.2f * ramp) + k / (float) shards) % 1f;
                        float wob = (float) Math.sin(seconds * 3f + k * 2f) * 8f;
                        float sx = p[0] + dx * u - uy * wob;
                        float sy = p[1] + dy * u + ux * wob;
                        polyFill(buf, pose, sx, sy, 4f + 4f * ramp, 6, seconds + k, argb(0xFFF0C0, 0.9f * a),
                                argb(color, 0.6f * a));
                    }
                }
                case PLASMA -> {
                    int n = 46;
                    float amp = 10f + 18f * ramp;
                    for (int k = 0; k <= n; k++) {
                        float u = k / (float) n;
                        float off = (float) Math.sin(u * 20f - seconds * 9f) * amp * (float) Math.sin(Math.PI * u);
                        float bx = p[0] + dx * u;
                        float by = p[1] + dy * u;
                        quad(buf, pose, bx - uy * off, by + ux * off, 2.6f, argb(0xFF5CF0, 0.95f * a));
                        quad(buf, pose, bx + uy * off, by - ux * off, 2.6f, argb(0x59E8FF, 0.95f * a));
                        if (k % 4 == 0) {
                            line(buf, pose, bx - uy * off, by + ux * off, bx + uy * off, by - ux * off, 0.5f,
                                    argb(0xFFFFFF, 0.25f * a));
                        }
                    }
                    ring(buf, pose, p[0], p[1], size * 1.5f, 1.6f, seconds * 3f, Mth.TWO_PI * 0.6f,
                            argb(color, 0.9f * a));
                    ring(buf, pose, p[0], p[1], size * 1.9f, 1.2f, -seconds * 4f, Mth.TWO_PI * 0.4f,
                            argb(0x59E8FF, 0.8f * a));
                }
                case CONTINUUM -> {

                    for (int k = 0; k < 3; k++) {
                        float rr = radius() * (0.55f + 0.3f * k);
                        float rot = seconds * (0.8f - 0.25f * k) + k;
                        for (int m = 0; m < 48; m++) {
                            float a0 = Mth.TWO_PI * m / 48f + rot;
                            float a1 = Mth.TWO_PI * (m + 1) / 48f + rot;
                            line(buf, pose, cx() + (float) Math.cos(a0) * rr, cy() + (float) Math.sin(a0) * rr * 0.45f,
                                    cx() + (float) Math.cos(a1) * rr, cy() + (float) Math.sin(a1) * rr * 0.45f, 0.5f,
                                    argb(color, 0.35f * a));
                        }
                        float sa = seconds * (1.6f - 0.4f * k) + k * 2f;
                        disc(buf, pose, cx() + (float) Math.cos(sa) * rr, cy() + (float) Math.sin(sa) * rr * 0.45f, 6f,
                                argb(0xFFFFFF, 0.95f * a), argb(color, 0f));
                    }
                    for (int k = 0; k < 3; k++) {
                        float climb = (seconds * (0.5f + 0.15f * ramp) + k / 3f) % 1f;
                        float hy = p[1] - climb * p[1];
                        line(buf, pose, p[0], Math.min(p[1], hy + 120f * climb), p[0], hy, 0.2f, 2.4f,
                                argb(0xFFD8A0, 0f),
                                argb(0xFFFFFF, 0.9f * a));
                        disc(buf, pose, p[0], hy, 7f, argb(0xFFFFFF, 0.9f * a), argb(color, 0f));
                    }
                }
                case ASCENSION_REACTOR -> {
                    for (int k = 0; k < 3; k++) {
                        ring(buf, pose, p[0], p[1], size * (0.9f + 0.55f * k), 1.0f, seconds * (1.6f - 0.6f * k),
                                Mth.TWO_PI * 0.8f, argb(color, 0.7f * a));
                    }
                    polyFill(buf, pose, p[0], p[1], size * 0.6f, 4, Mth.PI / 4f, argb(0xFFFFFF, 0.9f * a),
                            argb(color, 0f));
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 3.2f, argb(color, 0f), argb(0xE0FFF4, 0.85f * a));
                }
                case ANTIMATTER_COLLIDER -> {
                    for (int k = 0; k < 2; k++) {
                        ring(buf, pose, p[0], p[1], size * (1.0f + 0.7f * k), 1.0f, seconds * (k == 0 ? 2f : -2.4f),
                                Mth.TWO_PI * 0.7f, argb(k == 0 ? 0x59E8FF : color, 0.7f * a));
                    }
                    disc(buf, pose, p[0], p[1], size * 0.7f, argb(0xFFFFFF, 0.9f * a), argb(color, 0f));
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 3.0f, argb(color, 0f), argb(0xFFE0F4, 0.8f * a));
                }
                case PHOENIX_INFUSER -> {
                    for (int k = 0; k < 8; k++) {
                        float rise = ((seconds * 0.6f + k / 8f) % 1f);
                        disc(buf, pose, p[0] + (float) Math.sin(seconds * 3f + k * 2f) * size * 0.7f,
                                p[1] - rise * size * 2.6f, size * 0.2f, argb(0xFFD060, 0.9f * a * (1f - rise)),
                                argb(color, 0f));
                    }
                    disc(buf, pose, p[0], p[1], size * 1.2f, argb(0xFFE08A, 0.5f * a), argb(color, 0f));
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 3.0f, argb(color, 0f), argb(0xFFE8C0, 0.8f * a));
                }
                case SCULK_CORTEX -> {
                    for (int k = 0; k < 5; k++) {
                        float ang = k * 1.26f + seconds * 0.5f;
                        float rr = size * (1.1f + 0.5f * ramp);
                        float nx = p[0] + (float) Math.cos(ang) * rr;
                        float ny = p[1] + (float) Math.sin(ang) * rr;
                        line(buf, pose, p[0], p[1], nx, ny, 0.8f, argb(color, 0.7f * a));
                        disc(buf, pose, nx, ny, size * 0.16f, argb(0xFFFFFF, 0.9f * a), argb(color, 0f));
                    }
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 2.6f, argb(color, 0f), argb(0xC8FFFF, 0.8f * a));
                }
                case STELLAR_FORGE -> {
                    disc(buf, pose, p[0], p[1], size * 1.1f, argb(0xFFFFFF, 0.9f * a), argb(color, 0f));
                    rays(buf, pose, p[0], p[1], size * 0.5f, size * 2.2f, 10, seconds * 0.5f, 6f, color, 0.6f * a);
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 3.0f, argb(color, 0f), argb(0xFFF0C0, 0.8f * a));
                }
                case AETHER_CRUCIBLE -> {
                    for (int k = 0; k < 6; k++) {
                        float rise = ((seconds * 0.7f + k / 6f) % 1f);
                        disc(buf, pose, p[0] + (float) Math.sin(seconds * 2f + k) * size * 0.8f,
                                p[1] - rise * size * 2.4f, size * 0.22f, argb(0xFFFFFF, 0.8f * a * (1f - rise)),
                                argb(color, 0f));
                    }
                    ring(buf, pose, p[0], p[1], size * 1.2f, 1.2f, seconds, Mth.TWO_PI * 0.6f, argb(color, 0.7f * a));
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 2.6f, argb(color, 0f), argb(0xD0FFF0, 0.8f * a));
                }
                case TOKAMAK -> {
                    for (int k = 0; k < 2; k++) {
                        ring(buf, pose, p[0], p[1], size * (1.1f + 0.8f * k), 1.0f, seconds * (1.8f - k),
                                Mth.TWO_PI, argb(color, 0.6f * a));
                    }
                    float ang = seconds * 3.2f;
                    disc(buf, pose, p[0] + (float) Math.cos(ang) * size * 1.4f,
                            p[1] + (float) Math.sin(ang) * size * 1.4f,
                            size * 0.28f, argb(0xFFFFFF, 0.95f * a), argb(color, 0f));
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 2.6f, argb(color, 0f), argb(0xE0FFFF, 0.8f * a));
                }
                case NULL_WELL -> {
                    for (int k = 0; k < 3; k++) {
                        float rr = size * (1.0f + 0.7f * k) * (1.2f - 0.5f * ramp);
                        ring(buf, pose, p[0], p[1], rr, 0.9f, seconds * (1.4f - 0.4f * k), Mth.TWO_PI * 0.6f,
                                argb(color, 0.7f * a));
                    }
                    disc(buf, pose, p[0], p[1], size * 0.5f, argb(0x000000, 0.95f * a), argb(0x000000, 0.95f * a));
                    line(buf, pose, p[0], p[1], cx(), cy(), 0.4f, 3.0f, argb(color, 0f), argb(0xFFFFFF, 0.8f * a));
                }
                case ENGINE -> {
                    float ang = (float) Math.atan2(dy, dx);
                    tree(buf, pose, p[0], p[1], ang, len * 0.34f, 0, 5, ramp, color, a);
                    tree(buf, pose, cx(), cy(), ang + (float) Math.PI, len * 0.12f, 0, 3, ramp, color, a * 0.7f);
                }
            }
            rush(buf, pose, cap, p, dx, dy, ux, uy, size, color, a, ramp);
        }

        float conv = t - (STRIKE - 2.6f);
        if (conv > 0f) {
            float r = radius();
            for (int k = 0; k < 5; k++) {
                float phase = (conv * 0.7f + k * 0.2f) % 1f;
                ring(buf, pose, cx(), cy(), r * (3.4f * (1f - phase) + 0.15f), 0.8f + 1.6f * phase, 0f, Mth.TWO_PI,
                        argb(k % 2 == 0 ? GOLD : WHITE_HOT, 0.6f * phase * Math.min(1f, conv)));
            }
        }
        end(buf);

        String[] captions = { "THE STORM ANSWERS", "THE COMB FILLS", "THE RING HOLDS", "THE ENGINE REMEMBERS",
                "THE WINDOW OPENS", "THE WELL DRINKS", "THE FIELD HOLDS", "THE STAR IS MADE", "THE GOLD RUNS",
                "THE CORTEX THINKS", "THE PHOENIX RISES", "THE BEAMS MEET", "THE REACTOR ASCENDS" };
        for (int i = 0; i < Capstone.COUNT; i++) {
            float since = t - FIRE_AT[i];
            if (since < 0f || since > 2.6f) continue;
            float al = Mth.clamp(since / 0.4f, 0f, 1f) * Mth.clamp((2.6f - since) / 0.6f, 0f, 1f);
            float[] p = pylon(i);
            glowText(g, captions[i], p[0], p[1] - size * 2.3f, 1f, 0xFFFFFF, al, Capstone.values()[i].color);
        }
    }

    private void rush(BufferBuilder buf, Matrix4f pose, Capstone cap, float[] p, float dx, float dy, float ux, float uy,
                      float size, int color, float a, float ramp) {
        float qx = -uy;
        float qy = ux;
        switch (cap) {
            case NULL_WELL -> {

                for (int k = 0; k < 9; k++) {
                    float u = (seconds * 0.42f + k / 9f) % 1f;
                    float swirl = (float) Math.sin(u * 9f + k) * (1f - u) * size * 1.3f;
                    float x = p[0] + dx * u * u + qx * swirl;
                    float y = p[1] + dy * u * u + qy * swirl;
                    disc(buf, pose, x, y, 3.2f + 2f * (1f - u), argb(0x000000, 0.9f * a), argb(color, 0f));
                    ring(buf, pose, x, y, 3.8f, 0.7f, 0f, Mth.TWO_PI, argb(color, 0.8f * a * (1f - u * 0.5f)));
                }
            }
            case TOKAMAK -> {

                for (int k = 0; k < 10; k++) {
                    float u = (seconds * 0.6f + k / 10f) % 1f;
                    float wob = (float) Math.sin(u * Mth.TWO_PI * 3f + seconds * 4f) * size * 0.6f;
                    float x = p[0] + dx * u + qx * wob;
                    float y = p[1] + dy * u + qy * wob;
                    disc(buf, pose, x, y, 4f, argb(0xFFFFFF, 0.9f * a), argb(color, 0f));
                }
            }
            case STELLAR_FORGE -> {

                for (int k = 0; k < 4; k++) {
                    float u = (seconds * 0.5f + k / 4f) % 1f;
                    float hx = p[0] + dx * u;
                    float hy = p[1] + dy * u;
                    float tu = Math.max(0f, u - 0.16f);
                    line(buf, pose, p[0] + dx * tu, p[1] + dy * tu, hx, hy, 0.4f, 4.5f, argb(color, 0f),
                            argb(0xFFE8B0, 0.9f * a));
                    disc(buf, pose, hx, hy, 5f + 5f * ramp, argb(0xFFFFFF, 0.95f * a), argb(color, 0f));
                }
            }
            case AETHER_CRUCIBLE -> {

                for (int k = 0; k < 8; k++) {
                    float u = (seconds * 0.5f + k / 8f) % 1f;
                    float x = p[0] + dx * u;
                    float y = p[1] + dy * u - (float) Math.sin(u * Math.PI) * size * 1.4f;
                    disc(buf, pose, x, y, 3.6f, argb(k % 2 == 0 ? 0xFFD24A : 0xE8FFF4, 0.9f * a), argb(color, 0f));
                }
            }
            case SCULK_CORTEX -> {

                float prevX = p[0];
                float prevY = p[1];
                for (int k = 1; k <= 6; k++) {
                    float u = k / 6f;
                    float zig = (k % 2 == 0 ? 1f : -1f) * size * 0.5f * (1f - u);
                    float x = p[0] + dx * u + qx * zig;
                    float y = p[1] + dy * u + qy * zig;
                    line(buf, pose, prevX, prevY, x, y, 0.8f, argb(color, 0.6f * a));
                    disc(buf, pose, x, y, 3f, argb(0xFFFFFF, 0.8f * a), argb(color, 0f));
                    prevX = x;
                    prevY = y;
                }
                float pu = (seconds * 0.9f) % 1f;
                disc(buf, pose, p[0] + dx * pu, p[1] + dy * pu, 6f, argb(0xFFFFFF, 0.95f * a), argb(color, 0f));
            }
            case PHOENIX_INFUSER -> {

                for (int k = 0; k < 14; k++) {
                    float u = (seconds * 0.55f + k * 0.0714f) % 1f;
                    float flick = (float) Math.sin(seconds * 7f + k * 1.9f) * size * 0.5f * (1f - u);
                    float x = p[0] + dx * u + qx * flick;
                    float y = p[1] + dy * u + qy * flick;
                    disc(buf, pose, x, y, 3.2f, argb(u < 0.5f ? 0xFF8A3A : 0xFFE08A, 0.9f * a * (1f - 0.3f * u)),
                            argb(color, 0f));
                }
            }
            case ANTIMATTER_COLLIDER -> {

                for (int k = 0; k < 8; k++) {
                    float u = (seconds * 0.7f + k / 8f) % 1f;
                    float off = size * 0.7f * (1f - u);
                    float x = p[0] + dx * u;
                    float y = p[1] + dy * u;
                    disc(buf, pose, x + qx * off, y + qy * off, 3.6f, argb(0x59E8FF, 0.9f * a), argb(0x59E8FF, 0f));
                    disc(buf, pose, x - qx * off, y - qy * off, 3.6f, argb(0xFF4DD8, 0.9f * a), argb(0xFF4DD8, 0f));
                }
            }
            case ASCENSION_REACTOR -> {

                for (int k = 0; k < 5; k++) {
                    float u = (seconds * 0.45f + k / 5f) % 1f;
                    float x = p[0] + dx * u;
                    float y = p[1] + dy * u;
                    polyFill(buf, pose, x, y, 4f + 3f * u, 4, seconds * 2f + k, argb(0xFFFFFF, 0.9f * a),
                            argb(color, 0.5f * a));
                    ring(buf, pose, x, y, 4f + 9f * ((u * 3f) % 1f), 0.7f, 0f, Mth.TWO_PI,
                            argb(color, 0.5f * a * (1f - (u * 3f) % 1f)));
                }
            }
            default -> {}
        }
    }

    private void confluxSigil(BufferBuilder buf, Matrix4f pose, float r, float heat) {
        float s = r * 0.34f;
        float lit = 0.35f + 0.65f * heat;
        polygon(buf, pose, cx(), cy(), s * 0.56f, 6, seconds * 0.15f, 1.2f, argb(GOLD, 0.6f * lit));
        polygon(buf, pose, cx(), cy(), s * 0.56f, 6, -seconds * 0.15f + (float) Math.PI / 6f, 0.8f,
                argb(FIRE, 0.5f * lit));
        ticks(buf, pose, cx(), cy(), s * 1.18f, 52, 6, -seconds * 0.1f, 2.5f, argb(GOLD, 0.4f * lit));
        for (int i = 0; i < Capstone.COUNT; i++) {
            double a = Math.toRadians(-90 + 360.0 * i / Capstone.COUNT);
            float ca = (float) Math.cos(a);
            float sa = (float) Math.sin(a);
            boolean bound = state.isBound(i);
            int c = bound ? Capstone.values()[i].color : 0x554433;
            float breath = 0.7f + 0.3f * (float) Math.sin(seconds * 2f + i);
            line(buf, pose, cx() + ca * s * 0.62f, cy() + sa * s * 0.62f, cx() + ca * s * 1.02f,
                    cy() + sa * s * 1.02f, 1.0f, 1.8f, argb(c, (bound ? 0.8f : 0.25f) * breath),
                    argb(c, bound ? 0.95f : 0.3f));
            disc(buf, pose, cx() + ca * s * 1.06f, cy() + sa * s * 1.06f, 2.6f, argb(c, bound ? 0.95f : 0.35f),
                    argb(c, 0f));
        }
        polyFill(buf, pose, cx(), cy(), s * 0.3f, 4, (float) Math.PI / 4f, argb(WHITE_HOT, 0.95f * lit),
                argb(FIRE, 0.7f * lit));
        polyFill(buf, pose, cx(), cy(), s * 0.3f, 4, 0f, argb(GOLD, 0.35f * lit), argb(FIRE, 0.2f * lit));
    }

    private void tree(BufferBuilder buf, Matrix4f pose, float x, float y, float ang, float len, int depth, int maxDepth,
                      float grow, int color, float alpha) {
        if (depth > maxDepth) return;
        float g = Mth.clamp(grow * (maxDepth + 1) - depth, 0f, 1f);
        if (g <= 0f) return;
        float ex = x + (float) Math.cos(ang) * len * g;
        float ey = y + (float) Math.sin(ang) * len * g;
        float w = Math.max(0.5f, 2.2f - depth * 0.35f);
        line(buf, pose, x, y, ex, ey, w, w * 0.7f, argb(color, 0.9f * alpha), argb(0xFFFFFF, 0.6f * alpha));
        if (depth == maxDepth || g < 1f) {
            disc(buf, pose, ex, ey, 3.6f, argb(0xD8FFE4, 0.95f * alpha), argb(color, 0f));
            return;
        }
        float spread = 0.5f + 0.15f * noise(depth, ex);
        tree(buf, pose, ex, ey, ang - spread, len * 0.68f, depth + 1, maxDepth, grow, color, alpha);
        tree(buf, pose, ex, ey, ang + spread, len * 0.68f, depth + 1, maxDepth, grow, color, alpha);
        tree(buf, pose, ex, ey, ang + 0.1f * (noise(depth, ey) - 0.5f), len * 0.78f, depth + 1, maxDepth, grow, color,
                alpha);
    }

    private static int mix(int a, int b, float f) {
        f = Mth.clamp(f, 0f, 1f);
        int r = (int) (((a >> 16) & 255) * (1f - f) + ((b >> 16) & 255) * f);
        int gr = (int) (((a >> 8) & 255) * (1f - f) + ((b >> 8) & 255) * f);
        int bl = (int) ((a & 255) * (1f - f) + (b & 255) * f);
        return (r << 16) | (gr << 8) | bl;
    }

    private void drawRebirth(GuiGraphics g) {
        float t = rebirth;
        Matrix4f pose = g.pose().last().pose();
        float r = radius();
        EndingLook.Plan p = plan;

        float m = p.morph() ? ease(Mth.clamp((t - (STRIKE - 1f)) / 7f, 0f, 1f)) : 1f;
        colA = mix(p.fromColor(), p.toColor(), m);
        colB = mix(p.toColor(), p.fromColor(), 0.5f * (1f - m));
        colC = mix(colA, 0xFFFFFF, 0.65f);

        if (t < STRIKE) g.fill(0, 0, width, height, argb(colC, 0.5f * ease((t - (STRIKE - 2.2f)) / 2.2f)));

        if (t >= STRIKE - 1f) {
            drawLook(g, p.first(), p.morph() ? 1f - m : 0f, t);
            drawLook(g, p.second(), m, t);
        }

        if (t >= STRIKE) {
            if (!stingPlayed) {
                stingPlayed = true;
                sting(p.second());
            }
            BufferBuilder buf = begin();
            for (int k = 0; k < 4; k++) {
                float age = t - STRIKE - k * 0.55f;
                if (age < 0f || age > 3.2f) continue;
                float rr = r * (0.3f + 3.4f * ease(age / 3.2f));
                ring(buf, pose, cx(), cy(), rr, 2.2f * (1f - age / 3.2f) + 0.6f, 0f, Mth.TWO_PI,
                        argb(k % 2 == 0 ? colA : colC, 0.6f * (1f - age / 3.2f)));
            }
            end(buf);

            float rise = ease((t - (STRIKE + 1f)) / 7f);
            float size = Math.min(width, height) * (0.26f + 0.2f * rise);
            float py = Mth.lerp(rise, cy() + height * 0.14f, height * 0.44f);
            drawEndingPhoenix(g, p.second(), pose, py, size, t,
                    Mth.clamp((t - STRIKE - 0.1f) / 1.6f, 0f, 1f) *
                            Mth.clamp((REBIRTH_SECONDS - 0.3f - t) / 2.6f, 0f, 1f));
        }
        drawRebirthText(g, t);
    }

    private void drawEndingPhoenix(GuiGraphics g, EndingLook.Look look, Matrix4f pose, float py, float size, float t,
                                   float alpha) {
        switch (look) {
            case ECHO -> {
                for (int k = 2; k >= 1; k--) {
                    drawPhoenix(pose, cx(), py - k * size * 0.1f, size * (1f + 0.1f * k), t - 0.45f * k,
                            alpha * 0.3f / k);
                }
                drawPhoenix(pose, cx(), py, size, t, alpha);
            }
            case MIRROR -> {
                drawPhoenix(pose, cx() - size * 0.55f, py, size * 0.9f, t, alpha);
                g.pose().pushPose();
                g.pose().translate(cx() + size * 0.55f, 0, 0);
                g.pose().scale(-1f, 1f, 1f);
                drawPhoenix(g.pose().last().pose(), 0f, py, size * 0.9f, t, alpha * 0.6f);
                g.pose().popPose();
            }
            case NULL -> {
                BufferBuilder b = begin();
                disc(b, pose, cx(), py, size * 0.95f, argb(0x000000, 0.75f * alpha), argb(0x000000, 0f));
                end(b);
                drawPhoenix(pose, cx(), py, size * 0.78f, t, alpha);
            }
            case IRON -> {
                BufferBuilder b = begin();
                polygon(b, pose, cx(), py, size * 0.95f, 4, (float) Math.PI / 4f, 1.2f, argb(colA, 0.6f * alpha));
                polygon(b, pose, cx(), py, size * 1.15f, 4, (float) Math.PI / 4f, 0.7f, argb(colA, 0.3f * alpha));
                end(b);
                drawPhoenix(pose, cx(), py, size, t, alpha);
            }
            default -> drawPhoenix(pose, cx(), py, size, t, alpha);
        }
    }

    private void drawLook(GuiGraphics g, EndingLook.Look look, float weight, float t) {
        float since = Math.max(0f, t - (STRIKE - 1f));
        float a = weight * Mth.clamp(since / 2f, 0f, 1f) * Mth.clamp((REBIRTH_SECONDS - 1.5f - t) / 2f, 0f, 1f);
        if (a <= 0.01f) return;
        Matrix4f pose = g.pose().last().pose();
        float r = radius();
        float cx = cx();
        float cy = cy();
        BufferBuilder buf = begin();
        switch (look) {
            case ECHO -> {
                for (int k = 0; k < 7; k++) {
                    float age = (since * 0.9f + k * 0.55f) % 3.85f;
                    float rr = r * (0.25f + 3.4f * ease(age / 3.85f));
                    ring(buf, pose, cx, cy, rr, 0.7f + 1.2f * (1f - age / 3.85f), 0f, Mth.TWO_PI,
                            argb(k % 2 == 0 ? colA : colB, a * 0.5f * (1f - age / 3.85f)));
                }
                for (int i = 0; i < 46; i++) {
                    float x = cx + (noise(i, 1f) - 0.5f) * width * 0.9f + (float) Math.sin(seconds * 0.5f + i) * 14f;
                    float y = cy + (noise(i, 2f) - 0.5f) * height * 0.8f - ((seconds * (4f + noise(i, 3f) * 8f)) % 40f);
                    quad(buf, pose, x, y, 1f + noise(i, 4f) * 1.8f,
                            argb(colC, a * (0.15f + 0.3f * noise(i, (float) Math.floor(seconds * 2f)))));
                }
            }
            case NULL -> {
                for (int i = 0; i < 52; i++) {
                    float ang = noise(i, 1f) * Mth.TWO_PI;
                    float life = (since * 0.35f * (0.5f + noise(i, 2f)) + noise(i, 3f)) % 1f;
                    float d0 = r * (3.6f * (1f - life) + 0.3f);
                    float d1 = d0 + r * 0.55f * (1f - life);
                    float ca = (float) Math.cos(ang);
                    float sa = (float) Math.sin(ang);
                    line(buf, pose, cx + ca * d1, cy + sa * d1, cx + ca * d0, cy + sa * d0, 0.9f,
                            argb(colA, a * 0.5f * life));
                }
                disc(buf, pose, cx, cy, r * 0.38f, argb(0x000000, 0.85f * a), argb(0x000000, 0f));
                ring(buf, pose, cx, cy, r * 0.4f, 1.3f, 0f, Mth.TWO_PI, argb(colC, 0.7f * a));
                ring(buf, pose, cx, cy, r * 0.55f * (1f + 0.05f * (float) Math.sin(seconds * 3f)), 0.6f, 0f,
                        Mth.TWO_PI, argb(colA, 0.4f * a));
                for (int i = 0; i < 70; i++) {
                    float twinkle = 0.3f + 0.7f * noise(i, (float) Math.floor(seconds * 3f + i));
                    quad(buf, pose, noise(i, 5f) * width, noise(i, 6f) * height, 0.8f,
                            argb(0xFFFFFF, a * 0.5f * twinkle));
                }
            }
            case IRON -> {
                int step = Math.max(20, (int) (Math.min(width, height) / 14f));
                for (int x = 0; x < width; x += step) line(buf, pose, x, 0, x, height, 0.4f, argb(colA, a * 0.1f));
                for (int y = 0; y < height; y += step) line(buf, pose, 0, y, width, y, 0.4f, argb(colA, a * 0.1f));
                float sy = (since * 90f) % height;
                line(buf, pose, 0, sy, width, sy, 1.4f, argb(colC, a * 0.35f));
                line(buf, pose, 0, sy - 10f, width, sy - 10f, 5f, argb(colA, a * 0.08f));
                for (int k = 0; k < 4; k++) {
                    float age = (since * 0.5f + k * 0.25f) % 1f;
                    polygon(buf, pose, cx, cy, r * (0.4f + 2.6f * ease(age)), 4, 0f, 1f,
                            argb(colA, a * 0.5f * (1f - age)));
                }
                ticks(buf, pose, cx, cy, r * 1.9f, 60, 5, 0f, 5f, argb(colA, a * 0.35f));
                float hand = (float) Math.floor(since * 2f) * Mth.TWO_PI / 60f * 5f - Mth.HALF_PI;
                line(buf, pose, cx, cy, cx + (float) Math.cos(hand) * r * 1.9f, cy + (float) Math.sin(hand) * r * 1.9f,
                        1.2f, argb(colC, a * 0.5f));
            }
            case MIRROR -> {
                line(buf, pose, cx, 0, cx, height, 1.3f, argb(colC, a * 0.5f));
                line(buf, pose, cx, 0, cx, height, 6f, argb(colA, a * 0.08f));
                long frame = (long) (seconds * 10f);
                for (int i = 0; i < 11; i++) {
                    float y = noise(i, frame) * height;
                    float h = 3f + noise(i, frame + 1f) * 14f;
                    float off = (noise(i, frame + 2f) - 0.5f) * 70f;
                    line(buf, pose, cx + off - width * 0.32f, y, cx + off + width * 0.32f, y, h * 0.5f,
                            argb(i % 2 == 0 ? colA : colB, a * 0.12f));
                }
            }
            default -> {
                for (int i = 0; i < 90; i++) {
                    float life = (since * (0.2f + noise(i, 1f) * 0.4f) + noise(i, 2f)) % 1f;
                    float x = cx + (noise(i, 3f) - 0.5f) * width * 0.8f + (float) Math.sin(seconds * 2f + i) * 6f;
                    float y = height * 0.95f - life * height * 0.95f;
                    quad(buf, pose, x, y, (0.8f + noise(i, 4f) * 1.6f) * (1f - life * 0.5f),
                            argb(i % 3 == 0 ? colC : colA, a * (1f - life) * 0.8f));
                }
                for (int k = 0; k < 6; k++) {
                    float x = width * (0.1f + 0.16f * k);
                    for (int s = 0; s < 14; s++) {
                        float y0 = height * (1f - s / 14f);
                        float y1 = height * (1f - (s + 1) / 14f);
                        float w0 = (float) Math.sin(seconds * 3f + s * 0.7f + k) * 6f;
                        float w1 = (float) Math.sin(seconds * 3f + (s + 1) * 0.7f + k) * 6f;
                        line(buf, pose, x + w0, y0, x + w1, y1, 1.4f, argb(colB, a * 0.07f));
                    }
                }
            }
        }
        end(buf);
    }

    private void sting(EndingLook.Look look) {
        switch (look) {
            case ECHO -> {
                play(SoundEvents.WARDEN_HEARTBEAT, 0.8f, 1f);
                play(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.6f, 0.5f);
            }
            case NULL -> {
                play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 1f);
                play(SoundEvents.END_PORTAL_SPAWN, 1.4f, 0.4f);
            }
            case IRON -> {
                play(SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1f);
                play(SoundEvents.ANVIL_LAND, 0.7f, 0.5f);
            }
            case MIRROR -> {
                play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f, 1f);
                play(SoundEvents.GLASS_BREAK, 1.3f, 0.5f);
            }
            default -> play(SoundEvents.FIRECHARGE_USE, 0.7f, 1f);
        }
    }

    private void drawRebirthText(GuiGraphics g, float t) {
        var ending = ClientEnding.current();
        List<String> words = ending != null && ending.lines().size() >= 5 ? ending.lines() :
                List.of("You were the fire.", "You are the forge.", "Rise.", "R E B O R N", "The Conflux remembers.");
        int accent = ending != null ? 0xFF000000 | ending.accent() : GOLD;
        int accent2 = ending != null ? 0xFF000000 | ending.accent2() : FIRE;
        Object[][] lines = { { words.get(0), 9.6f, 14.2f, 1.6f, 0.20f },
                { words.get(1), 11.6f, 16.2f, 1.6f, 0.27f },
                { words.get(2), 13.6f, 17.7f, 1.6f, 0.34f }, { words.get(3), 16.7f, 27f, 4.5f, 0.17f },
                { words.get(4), 19.7f, 27f, 1.3f, 0.17f } };
        float ui = net.phoenixvine.wiki.ui.Cinematic.scaleFor(width, height);
        if (t > 16.2f && t < 27.5f) {
            float band = Mth.clamp((t - 16.2f) / 1f, 0f, 1f) * Mth.clamp((27.5f - t) / 1f, 0f, 1f);
            int y0 = (int) (height * 0.1f);
            int y1 = (int) (height * 0.36f);
            g.fillGradient(0, y0, width, (y0 + y1) / 2, argb(0x000000, 0f), argb(0x000000, 0.6f * band));
            g.fillGradient(0, (y0 + y1) / 2, width, y1, argb(0x000000, 0.6f * band), argb(0x000000, 0f));
        }
        for (int i = 0; i < lines.length; i++) {
            float start = (float) lines[i][1];
            float end = (float) lines[i][2];
            if (t < start || t > end) continue;
            String text = (String) lines[i][0];
            if (text.isEmpty()) continue;
            float a = Mth.clamp((t - start) / 0.7f, 0f, 1f) * Mth.clamp((end - t) / 0.9f, 0f, 1f);
            float scale = Math.min((float) lines[i][3] * ui, net.phoenixvine.wiki.ui.Cinematic.fit(font, text,
                    (float) lines[i][3] * ui, width * 0.9f));
            float y = height * (float) lines[i][4] + (i == 4 ? 44f * ui : 0f);
            glowText(g, text, width / 2f, y, scale, i == 3 ? WHITE_HOT : accent, a, accent2);
        }
    }

    private void drawPhoenix(Matrix4f pose, float cx, float cy, float size, float t, float alpha) {
        BufferBuilder buf = begin();
        float glow = alpha * (0.88f + 0.12f * (float) Math.sin(t * 5f));
        for (int k = 5; k >= 1; k--) {
            disc(buf, pose, cx, cy, size * (0.45f + 0.3f * k), argb(colA, 0.05f * glow), argb(colB, 0f));
        }
        rays(buf, pose, cx, cy, size * 0.2f, size * 1.6f, 16, t * 0.07f, size * 0.06f, colA, 0.06f * glow);

        float yaw = 0.1f * (float) Math.sin(t * 0.7f);
        float pitch = 0.5f;
        float cyaw = (float) Math.cos(yaw);
        float syaw = (float) Math.sin(yaw);
        float cp = (float) Math.cos(pitch);
        float sp = (float) Math.sin(pitch);
        float scale = size * 0.62f;
        PhoenixModel.Sink sink = (ax, ay, az, ac, bx, by, bz, bc, cx2, cy2, cz2, cc) -> {
            float[] a = project(ax, ay, az, cyaw, syaw, cp, sp, scale, cx, cy);
            float[] b2 = project(bx, by, bz, cyaw, syaw, cp, sp, scale, cx, cy);
            float[] c = project(cx2, cy2, cz2, cyaw, syaw, cp, sp, scale, cx, cy);
            buf.addVertex(pose, a[0], a[1], 0).setColor(ac);
            buf.addVertex(pose, b2[0], b2[1], 0).setColor(bc);
            buf.addVertex(pose, c[0], c[1], 0).setColor(cc);
            buf.addVertex(pose, c[0], c[1], 0).setColor(cc);
        };
        PhoenixModel.build(sink, t, glow);

        for (int i = 0; i < 46; i++) {
            float x = cx + (noise(i, 7f) - 0.5f) * size * 3.4f;
            float speed = 0.7f + noise(i, 8f);
            float y = cy - size * 1.6f + ((t * speed * size * 0.9f + noise(i, 9f) * size * 3.2f) % (size * 3.4f));
            float l = size * (0.15f + 0.3f * noise(i, 10f));
            line(buf, pose, x, y - l, x, y, 0.3f, 0.9f, argb(colA, 0f),
                    argb(i % 3 == 0 ? colC : colA, 0.35f * glow));
        }
        for (int i = 0; i < 80; i++) {
            float life = (t * (0.4f + noise(i, 1f) * 0.6f) + noise(i, 2f)) % 1f;
            float a = noise(i, 3f) * Mth.TWO_PI;
            float d = size * (0.25f + 0.9f * noise(i, 4f)) * (0.4f + life);
            quad(buf, pose, cx + (float) Math.cos(a) * d, cy + (float) Math.sin(a) * d * 0.7f + life * size * 0.45f,
                    (1.1f + noise(i, 5f) * 2f) * (1f - life),
                    argb(i % 3 == 0 ? colC : colA, (1f - life) * 0.9f * glow));
        }
        end(buf);
    }

    private static float[] project(float x, float y, float z, float cyaw, float syaw, float cp, float sp, float scale,
                                   float cx, float cy) {
        float vx = x * cyaw + z * syaw;
        float vz = -x * syaw + z * cyaw;
        float vy = (vz + 0.4f) * cp + y * sp;
        return new float[] { cx + vx * scale, cy - vy * scale };
    }

    @Override
    protected void drawUi(GuiGraphics g, int mx, int my, float ui) {
        lastMx = mx;
        lastMy = my;
        drawLeft(g, ui);
        drawRight(g, mx, my, ui);
        drawFooter(g, mx, my, ui);
    }

    private static final int ROW_H = 13;
    private int listHover = -1;
    private int lastMx;
    private int lastMy;
    private float leftScroll;
    private int leftRows = 1;
    private int leftMaxScroll;

    private void drawLeft(GuiGraphics g, float ui) {
        fadePanel(g, true, ui);
        int pw = panelW();
        int x = 14;
        int y = bar() + 14;
        heading(g, "PHOENIX FORGE", x, y, ui);
        y += 20;

        int statsH = 3 * 12 + 6;
        int bottom = height - bar() - 10 - statsH;
        int rows = Math.max(3, Math.min(Capstone.COUNT, (bottom - y) / ROW_H));
        leftRows = rows;
        leftMaxScroll = Math.max(0, Capstone.COUNT - rows);
        leftScroll = Mth.clamp(leftScroll, 0f, leftMaxScroll);
        int first = (int) leftScroll;
        int listW = pw - 36;
        listHover = -1;
        for (int r = 0; r < rows && first + r < Capstone.COUNT; r++) {
            Capstone cap = Capstone.PROGRESSION[first + r];
            int i = cap.ordinal();
            boolean bound = state.isBound(i);
            int ry = y + r * ROW_H;
            if (lastMx >= 0 && lastMx < pw && lastMy >= ry - 1 && lastMy < ry + ROW_H - 1) {
                listHover = i;
                g.fill(x - 4, ry - 1, pw - 12, ry + ROW_H - 2, argb(cap.color, 0.14f * ui));
            }
            g.fill(x, ry + 2, x + 5, ry + 8, argb(bound ? cap.color : 0x554433, ui));
            String status = !bound ? "unbound" : live() ? "linked" : "bound";
            int sw = font.width(status);
            g.drawString(font, font.plainSubstrByWidth(cap.shortName, listW - sw - 12), x + 10, ry,
                    argb(bound ? TEXT : DIM, ui), false);
            g.drawString(font, status, x + listW - sw + 6, ry, argb(bound ? 0xCCBB99 : 0x776655, ui * 0.9f), false);
        }
        if (leftMaxScroll > 0) {
            int trackH = rows * ROW_H;
            int barH = Math.max(10, trackH * rows / Capstone.COUNT);
            int barY = y + (int) ((trackH - barH) * (leftScroll / leftMaxScroll));
            g.fill(pw - 10, y, pw - 8, y + trackH, argb(0x332211, ui));
            g.fill(pw - 10, barY, pw - 8, barY + barH, argb(0xCC9966, ui));
        }
        y = bottom + 4;
        String ring = state.closed() ? "closed" : state.attempts() == 0 ? "not tried" :
                state.aligned() + " of " + Capstone.COUNT + " in place";
        g.drawString(font, "Ring: " + ring, x, y, argb(state.closed() ? GOLD : TEXT, ui), false);
        g.drawString(font, "Tries: " + state.attempts(), x, y + 12, argb(TEXT, ui), false);
        g.drawString(font, "Rebirth: " + (state.reborn() ? "lit" : "unlit"), x, y + 24,
                argb(state.reborn() ? GOLD : TEXT, ui), false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (vx(mx) < panelW() && leftMaxScroll > 0) {
            leftScroll = Mth.clamp(leftScroll - (float) scrollY, 0f, leftMaxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    private void drawRight(GuiGraphics g, int mx, int my, float ui) {
        fadePanel(g, false, ui);
        int pw = panelW();
        int x = width - pw + 12;
        int w = pw - 24;
        int y = bar() + 14;
        heading(g, "THE RING", x, y, ui);
        y += 18;
        String help = state.closed() ? "The ring is closed. The capstones are awake." : allBound() ?
                "Click the pylons in the order they come into reach, lowest tier first (the list on the left reads in that order). The Forge says how many are in place." :
                "Win a binding rite at each capstone first: use the Rebirth Binder on it.";
        wrapped(g, help, x, y, w, 0xB89C80, ui);
        y += wrappedHeight(help, w) + 8;
        int cols = Capstone.COUNT > 8 ? 2 : 1;
        int perCol = (Capstone.COUNT + cols - 1) / cols;
        int colW = (w - (cols - 1) * 4) / cols;
        for (int i = 0; i < Capstone.COUNT; i++) {
            boolean set = i < order.size();
            int rx = x + (i / perCol) * (colW + 4);
            int ry = y + (i % perCol) * 11;
            g.fill(rx, ry, rx + colW, ry + 10, argb(0x120805, 0.8f * ui));
            g.drawString(font,
                    font.plainSubstrByWidth((i + 1) + " " + (set ? Capstone.values()[order.get(i)].shortName : "-"),
                            colW - 4),
                    rx + 2, ry + 1, argb(set ? Capstone.values()[order.get(i)].color : 0x554433, ui), false);
        }
        y += perCol * 11 + 8;
        y = Math.min(y, height - bar() - 48);
        boolean can = allBound() && !state.closed() && order.size() == Capstone.COUNT;
        int closeW = Math.max(54, (int) (w * 0.62f));
        button(g, x, y, closeW, 16, "Close ring", mx, my, can, ui, () -> PacketDistributor.sendToServer(
                new C2SRebirthActionPacket(state.forge(), C2SRebirthActionPacket.SUBMIT_RING, toArray())));
        button(g, x + closeW + 4, y, Math.max(30, w - closeW - 4), 16, "Clear", mx, my, !state.closed(), ui,
                order::clear);
    }

    private void drawFooter(GuiGraphics g, int mx, int my, float ui) {
        int c = width / 2;
        if (!message.isEmpty()) {
            g.drawCenteredString(font, message, c, height - bar() - 48, argb(messageColor, ui));
        }
        if (state.closed() && rebirth < 0f) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(seconds * 3f);
            int w = 160;
            int y = height - bar() - 34;
            g.fill(c - w / 2, y, c + w / 2, y + 22, argb(FIRE, 0.25f * pulse * ui));
            g.renderOutline(c - w / 2, y, w, 22, argb(GOLD, ui));
            g.drawCenteredString(font, state.reborn() ? "WITNESS AGAIN" : "IGNITE THE FORGE", c, y + 7,
                    argb(WHITE_HOT, ui));
            if (ui > 0.6f) {
                hits.add(new Hit(c - w / 2, y, w, 22, () -> PacketDistributor.sendToServer(
                        new C2SRebirthActionPacket(state.forge(), C2SRebirthActionPacket.IGNITE, new int[0]))));
            }
        }
        button(g, width - panelW() + 12, height - bar() - 24, 56, 15, "Leave", mx, my, true, ui, this::onClose);
    }

    private int[] toArray() {
        int[] out = new int[order.size()];
        for (int i = 0; i < out.length; i++) out[i] = order.get(i);
        return out;
    }
}
