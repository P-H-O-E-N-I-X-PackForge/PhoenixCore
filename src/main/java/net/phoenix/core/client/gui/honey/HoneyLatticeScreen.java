package net.phoenix.core.client.gui.honey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.common.machine.multiblock.electric.honey.C2SHoneyLatticePacket;
import net.phoenix.core.common.machine.multiblock.electric.honey.HoneyLattice;
import net.phoenix.core.common.machine.multiblock.electric.honey.S2CHoneyLatticePacket;
import net.phoenixvine.wiki.ui.Cinematic;
import net.phoenixvine.wiki.ui.CinematicPrefs;
import net.phoenixvine.wiki.ui.GlDraw;
import net.phoenixvine.wiki.ui.UiProfiler;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static net.phoenixvine.wiki.ui.GlDraw.*;

@OnlyIn(Dist.CLIENT)
public class HoneyLatticeScreen extends Screen {

    private static final int AMBER = 0xFFB52E;
    private static final int GOLD = 0xFFE08A;
    private static final int DEEP = 0xC2770A;
    private static final int TEXT = 0xFFF1D6;
    private S2CHoneyLatticePacket state;
    private static final float INTRO_SECONDS = 4.6f;
    private boolean introChecked;
    private float seconds;
    private long lastNanos = -1L;
    private float flash;
    private float burst = -1f;
    private String message = "";
    private int messageColor = TEXT;
    private int hover = -1;

    private final float[] changedAt = new float[HoneyLattice.CELLS];
    private final List<Ripple> ripples = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final UiProfiler prof = new UiProfiler("honey");

    private record Ripple(float x, float y, float born) {}

    private record Hit(int x, int y, int w, int h, Runnable action) {

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public HoneyLatticeScreen(S2CHoneyLatticePacket state) {
        super(Component.literal("Honey Crystallization Chamber"));
        this.state = state;
        java.util.Arrays.fill(changedAt, -10f);
    }

    public BlockPos pos() {
        return state.pos();
    }

    public void update(S2CHoneyLatticePacket next) {
        int diff = state.state() ^ next.state();
        for (int i = 0; i < HoneyLattice.CELLS; i++) if ((diff >> i & 1) != 0) changedAt[i] = seconds;
        boolean solved = next.solved();
        this.state = next;
        if (!next.message().isEmpty()) {
            message = next.message();
            messageColor = solved ? GOLD | 0xFF000000 : TEXT;
        }
        if (solved) {
            flash = 1f;
            burst = 0f;
            play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, 1f);
            play(SoundEvents.BEACON_POWER_SELECT, 1.4f, 0.6f);
            java.util.Arrays.fill(changedAt, seconds);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected void renderBlurredBackground(float partialTick) {}

    private void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private static float cornerX(float cx, float r, int i) {
        return cx + r * (float) Math.cos(Math.toRadians(30 + 60 * i));
    }

    private static float cornerY(float cy, float r, int i) {
        return cy + r * (float) Math.sin(Math.toRadians(30 + 60 * i));
    }

    private void hexOutline(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, float w, int color) {
        for (int i = 0; i < 6; i++) {
            line(buf, pose, cornerX(cx, r, i), cornerY(cy, r, i), cornerX(cx, r, i + 1), cornerY(cy, r, i + 1), w,
                    color);
        }
    }

    private void crystal(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, float light, float alpha) {
        for (int i = 0; i < 6; i++) {
            float facing = (float) Math.cos(Math.toRadians(30 + 60 * i + 30 + 135));
            float shade = 0.55f + 0.45f * (facing * 0.5f + 0.5f);
            int rgb = scale(GOLD, shade * light);
            int core = argb(scale(0xFFFFFF, Math.min(1f, shade * light + 0.15f)), alpha);
            int edge = argb(rgb, alpha);
            buf.addVertex(pose, cx, cy, 0).setColor(core);
            buf.addVertex(pose, cornerX(cx, r, i), cornerY(cy, r, i), 0).setColor(edge);
            buf.addVertex(pose, cornerX(cx, r, i + 1), cornerY(cy, r, i + 1), 0).setColor(edge);
            buf.addVertex(pose, cornerX(cx, r, i + 1), cornerY(cy, r, i + 1), 0).setColor(edge);
        }
    }

    private static int scale(int rgb, float k) {
        int r = Mth.clamp((int) (((rgb >> 16) & 0xFF) * k), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 0xFF) * k), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * k), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private float cell() {
        float byWidth = (width - 2 * panelW() - 24f) / (5f * (float) Math.sqrt(3));
        float byHeight = (height - 70f) / 8.4f;
        return Math.max(10f, Math.min(Math.min(byWidth, byHeight), Math.min(width * 0.5f, height) * 0.115f));
    }

    private float cellX(int cell, float cx, float r) {
        return cx + r * (float) Math.sqrt(3) * (HoneyLattice.q(cell) + HoneyLattice.r(cell) / 2f);
    }

    private float cellY(int cell, float cy, float r) {
        return cy + r * 1.5f * HoneyLattice.r(cell);
    }

    private int cellAt(double mx, double my, float cx, float cy, float r) {
        int best = -1;
        float bestD = r * r * 0.95f;
        for (int i = 0; i < HoneyLattice.CELLS; i++) {
            float dx = (float) mx - cellX(i, cx, r);
            float dy = (float) my - cellY(i, cy, r);
            float d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private float fitK = 1f;

    @Override
    public void render(GuiGraphics g, int rawMouseX, int rawMouseY, float partialTick) {
        fitK = net.phoenixvine.wiki.ui.ScreenFit.apply(this);
        g.pose().pushPose();
        g.pose().scale(fitK, fitK, 1f);
        renderStage(g, (int) (rawMouseX / fitK), (int) (rawMouseY / fitK), partialTick);
        g.pose().popPose();
    }

    private void renderStage(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = lastNanos < 0 ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        if (!introChecked) {
            introChecked = true;
            if (!CinematicPrefs.playIntro()) seconds = INTRO_SECONDS;
        }
        seconds += dt;
        flash = Math.max(0f, flash - dt * 1.4f);
        if (burst >= 0f) {
            burst += dt * 0.8f;
            if (burst > 1f) burst = -1f;
        }
        prof.frame(dt);
        long renderStart = prof.start();
        hits.clear();

        float intro = Mth.clamp((seconds - INTRO_SECONDS) / 0.9f, 0f, 1f);
        g.fillGradient(0, 0, width, height, 0xFF0A0603, 0xFF1D1006);
        timed("background", () -> drawBackground(g));

        float cx = width / 2f;
        float cy = height * 0.5f;
        float combShow = Math.max(intro, Mth.clamp((seconds - 0.5f) / 3f, 0f, 1f) * 0.75f);
        float r = cell() * (0.55f + 0.45f * combShow);
        hover = intro >= 1f ? cellAt(mouseX, mouseY, cx, cy, r) : -1;
        timed("comb", () -> drawComb(g, cx, cy, r, combShow));
        drawRipples(g, cx, cy, r);
        if (burst >= 0f) drawBurst(g, cx, cy, r);

        g.fillGradient(0, 0, width, (int) (height * 0.18f), 0xCC000000, 0x00000000);
        g.fillGradient(0, (int) (height * 0.82f), width, height, 0x00000000, 0xCC000000);

        if (intro > 0.02f) {
            timed("leftPanel", () -> drawLeft(g, mouseX, mouseY, intro));
            timed("rightPanel", () -> drawRight(g, intro));
        }
        if (!message.isEmpty() && intro >= 1f) {
            g.drawCenteredString(font, message, width / 2, height - 36, messageColor);
        }
        if (flash > 0f) g.fill(0, 0, width, height, argb(GOLD, 0.45f * flash));
        CinematicPrefs.draw(g, font, width, 2, mouseX, mouseY,
                Mth.clamp(seconds / 1.0f, 0f, 1f) * (seconds < INTRO_SECONDS ? 0.55f : 0.85f));
        if (seconds < INTRO_SECONDS + 0.7f) drawIntro(g);
        prof.end("render", renderStart);
        prof.draw(g, font);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBackground(GuiGraphics g) {
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        float size = Math.min(width, height) * 0.12f;
        float w = size * (float) Math.sqrt(3);
        for (int row = -1; row < height / (size * 1.5f) + 2; row++) {
            for (int col = -1; col < width / w + 2; col++) {
                float x = col * w + (row % 2 == 0 ? 0f : w / 2f);
                float y = row * size * 1.5f;
                hexOutline(buf, pose, x, y, size * 0.98f, 0.5f, argb(AMBER, 0.045f));
            }
        }
        for (int i = 0; i < 9; i++) {
            float x = width * (0.06f + 0.88f * noise(i, 1f));
            float len = height * (0.18f + 0.35f * noise(i, 2f));
            float sag = (seconds * (8f + noise(i, 3f) * 10f) + noise(i, 4f) * height) % (height + len);
            float top = Math.max(0f, sag - len);
            line(buf, pose, x, top, x, Math.min(height, sag), 1.4f, argb(DEEP, 0.2f));

            float bx = x;
            float by = Math.min(height, sag);
            buf.addVertex(pose, bx - 2.4f, by - 2.4f, 0).setColor(argb(GOLD, 0.45f));
            buf.addVertex(pose, bx - 2.4f, by + 3.5f, 0).setColor(argb(GOLD, 0.45f));
            buf.addVertex(pose, bx + 2.4f, by + 3.5f, 0).setColor(argb(GOLD, 0.45f));
            buf.addVertex(pose, bx + 2.4f, by - 2.4f, 0).setColor(argb(GOLD, 0.45f));
        }
        for (int i = 0; i < 70; i++) {
            float x = noise(i, 7f) * width + (float) Math.sin(seconds * 0.5f + i) * 12f;
            float y = height + 10f - ((seconds * (10f + noise(i, 8f) * 22f) + noise(i, 9f) * height) % (height + 20f));
            float a = 0.15f + 0.3f * (0.5f + 0.5f * (float) Math.sin(seconds * 1.7f + i));
            float s = 0.8f + noise(i, 10f) * 1.2f;
            int c = argb(GOLD, a);
            buf.addVertex(pose, x - s, y - s, 0).setColor(c);
            buf.addVertex(pose, x - s, y + s, 0).setColor(c);
            buf.addVertex(pose, x + s, y + s, 0).setColor(c);
            buf.addVertex(pose, x + s, y - s, 0).setColor(c);
        }
        end(buf);
    }

    private void drawComb(GuiGraphics g, float cx, float cy, float r, float intro) {
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder bed = begin(false);
        for (int i = 0; i < HoneyLattice.CELLS; i++) {
            float x = cellX(i, cx, r);
            float y = cellY(i, cy, r);

            for (int k = 0; k < 6; k++) {
                int c = argb(0x120A04, 0.92f * intro);
                bed.addVertex(pose, x, y, 0).setColor(c);
                bed.addVertex(pose, cornerX(x, r * 0.94f, k), cornerY(y, r * 0.94f, k), 0).setColor(c);
                bed.addVertex(pose, cornerX(x, r * 0.94f, k + 1), cornerY(y, r * 0.94f, k + 1), 0).setColor(c);
                bed.addVertex(pose, cornerX(x, r * 0.94f, k + 1), cornerY(y, r * 0.94f, k + 1), 0).setColor(c);
            }
        }
        end(bed);

        BufferBuilder glow = begin(true);
        for (int i = 0; i < HoneyLattice.CELLS; i++) {
            float x = cellX(i, cx, r);
            float y = cellY(i, cy, r);
            boolean lit = HoneyLattice.isLit(state.state(), i);
            boolean flips = hover >= 0 && (HoneyLattice.pressMask(hover) >> i & 1) != 0;
            hexOutline(glow, pose, x, y, r * 0.96f, 0.8f, argb(AMBER, (lit ? 0.5f : 0.22f) * intro));
            if (flips) hexOutline(glow, pose, x, y, r * 0.86f, 1.2f, argb(0xFFFFFF, 0.55f));
            if (lit) crystal(glow, pose, x, y, r * 1.25f, 0.5f, 0.05f * intro);
        }
        end(glow);

        BufferBuilder facets = begin(true);
        for (int i = 0; i < HoneyLattice.CELLS; i++) {
            float x = cellX(i, cx, r);
            float y = cellY(i, cy, r);
            if (HoneyLattice.isLit(state.state(), i)) {
                float since = seconds - changedAt[i];
                float pop = since < 0.35f ? 1f + 0.28f * (1f - since / 0.35f) : 1f;
                float breath = 0.94f + 0.06f * (float) Math.sin(seconds * 2.1f + i);
                crystal(facets, pose, x, y, r * 0.84f * pop * breath, 0.85f + 0.15f * breath, 0.95f * intro);
            } else if (seconds - changedAt[i] < 0.3f) {
                float f = 1f - (seconds - changedAt[i]) / 0.3f;
                crystal(facets, pose, x, y, r * 0.84f * (1f + 0.3f * (1f - f)), 0.9f, 0.5f * f);
            }
        }
        end(facets);
    }

    private void drawRipples(GuiGraphics g, float cx, float cy, float r) {
        ripples.removeIf(rp -> seconds - rp.born() > 0.7f);
        if (ripples.isEmpty()) return;
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        for (Ripple rp : ripples) {
            float t = (seconds - rp.born()) / 0.7f;
            float radius = r * (0.3f + 2.4f * t);
            int segments = 36;
            for (int i = 0; i < segments; i++) {
                float a0 = Mth.TWO_PI * i / segments;
                float a1 = Mth.TWO_PI * (i + 1) / segments;
                line(buf, pose, rp.x() + (float) Math.cos(a0) * radius, rp.y() + (float) Math.sin(a0) * radius,
                        rp.x() + (float) Math.cos(a1) * radius, rp.y() + (float) Math.sin(a1) * radius, 1.2f,
                        argb(GOLD, (1f - t) * 0.5f));
            }
        }
        end(buf);
    }

    private void drawBurst(GuiGraphics g, float cx, float cy, float r) {
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        float t = burst;
        for (int i = 0; i < 90; i++) {
            float a = noise(i, 1f) * Mth.TWO_PI;
            float dist = (0.2f + noise(i, 2f) * 3.4f) * r * (1f - (1f - t) * (1f - t));
            float x = cx + (float) Math.cos(a) * dist;
            float y = cy + (float) Math.sin(a) * dist + t * t * 40f;
            float s = (1.2f + noise(i, 3f) * 2.2f) * (1f - t);
            int c = argb(i % 3 == 0 ? 0xFFFFFF : GOLD, (1f - t) * 0.9f);
            buf.addVertex(pose, x, y - s, 0).setColor(c);
            buf.addVertex(pose, x - s, y, 0).setColor(c);
            buf.addVertex(pose, x, y + s, 0).setColor(c);
            buf.addVertex(pose, x + s, y, 0).setColor(c);
        }
        end(buf);
    }

    private int panelW() {
        return Mth.clamp((int) (width * 0.25f), 130, 200);
    }

    private void fadePanel(GuiGraphics g, int x, int w, boolean leftEdge, float ui) {
        int strip = 3;
        for (int i = 0; i < w; i += strip) {
            float t = i / (float) w;
            float a = 0.8f * (float) Math.pow(1f - t, 1.4f) * ui;
            int sx = leftEdge ? x + i : x + w - i - strip;
            g.fill(sx, 0, sx + strip, height, argb(0x080402, a));
        }
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, float ui,
                        Runnable action) {
        if (ui <= 0.02f) return;
        boolean over = ui >= 1f && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, over ? argb(AMBER, 0.3f * ui) : argb(0x120A04, 0.85f * ui));
        g.renderOutline(x, y, w, h, argb(AMBER, 0.8f * ui));
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, argb(TEXT, ui));
        if (ui >= 1f) hits.add(new Hit(x, y, w, h, action));
    }

    private void drawLeft(GuiGraphics g, int mx, int my, float ui) {
        int pw = panelW();
        fadePanel(g, 0, pw, true, ui);
        int x = 14;
        int y = 22;
        g.drawString(font, "CRYSTALLIZATION", x, y, argb(AMBER, ui), false);
        y += 12;
        g.drawString(font, "pattern " + (state.solves() + 1), x, y, argb(0x9A8461, ui), false);
        y += 22;
        int vx = x + font.width("Presses") + 6;
        g.drawString(font, "Presses", x, y, argb(0x9A8461, ui), false);
        g.drawString(font, state.moves() + "  (par " + state.par() + ")", vx, y,
                argb(state.moves() <= state.par() ? GOLD : 0xFFE0C090, ui), false);
        y += 14;
        g.drawString(font, "Grade now", x, y, argb(0x9A8461, ui), false);
        g.drawString(font, HoneyLattice.grade(Math.max(1, state.moves()), state.par()),
                x + font.width("Grade now") + 6, y, argb(TEXT, ui), false);
        y += 24;
        g.drawString(font, "Seed crystals", x, y, argb(0x9A8461, ui), false);
        y += 11;
        g.drawString(font, state.charges() + " cycles", x, y, argb(GOLD, ui), false);
        y += 11;
        for (FormattedCharSequence line : font.split(Component.literal(
                "Each seed crystal makes one crystallization 30% faster and 10% cheaper."), pw - 24)) {
            g.drawString(font, line, x, y, argb(0x7A6A4C, ui), false);
            y += 10;
        }
        y += 14;
        button(g, x, y, pw - 28, 15, "Clear comb", mx, my, ui, () -> send(C2SHoneyLatticePacket.CLEAR, 0));
        y += 19;
        button(g, x, y, pw - 28, 15, "Leave", mx, my, ui, this::onClose);
    }

    private void drawRight(GuiGraphics g, float ui) {
        int pw = panelW();
        fadePanel(g, width - pw, pw, false, ui);
        int x = width - pw + 12;
        g.drawString(font, "THE CHAMBER WANTS", x, 22, argb(AMBER, ui), false);

        float r = Math.min(14f, (pw - 40) / 8f);
        float cx = width - pw / 2f + 4;
        float cy = 90f;
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder glow = begin(true);
        for (int i = 0; i < HoneyLattice.CELLS; i++) {
            boolean want = HoneyLattice.isLit(state.target(), i);
            hexOutline(glow, pose, cellX(i, cx, r), cellY(i, cy, r), r * 0.95f, 0.6f,
                    argb(AMBER, (want ? 0.6f : 0.18f) * ui));
        }
        end(glow);
        BufferBuilder facets = begin(true);
        for (int i = 0; i < HoneyLattice.CELLS; i++) {
            if (!HoneyLattice.isLit(state.target(), i)) continue;
            boolean have = HoneyLattice.isLit(state.state(), i);
            crystal(facets, pose, cellX(i, cx, r), cellY(i, cy, r), r * 0.8f, have ? 1f : 0.55f,
                    (have ? 0.95f : 0.55f) * ui);
        }
        end(facets);
        int y = (int) (cy + r * 4.4f);
        int want = HoneyLattice.litCount(state.target());
        int match = HoneyLattice.litCount(state.state() & state.target());
        g.drawString(font, match + " of " + want + " wanted cells lit", x, y, argb(GOLD, ui), false);
        y += 18;
        for (FormattedCharSequence line : font.split(Component.literal(
                "Press a cell to flip it and every cell touching it. Light every cell of this pattern; extra lit cells are fine. Fewer presses " +
                        "earn more seed crystals."),
                pw - 24)) {
            g.drawString(font, line, x, y, argb(0x9A8461, ui), false);
            y += 10;
        }
    }

    private void send(int action, int cell) {
        PacketDistributor.sendToServer(new C2SHoneyLatticePacket(state.pos(), action, cell));
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == UiProfiler.TOGGLE_KEY) {
            prof.toggle();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void timed(String section, Runnable r) {
        long t = prof.start();
        r.run();
        prof.end(section, t);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        mx /= fitK;
        my /= fitK;
        if (button == 0 && CinematicPrefs.click(mx, my)) return true;
        if (seconds < INTRO_SECONDS) {
            seconds = INTRO_SECONDS;
            return true;
        }
        if (button == 0) {
            for (Hit h : new ArrayList<>(hits)) {
                if (h.contains(mx, my)) {
                    h.action().run();
                    play(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 0.6f);
                    return true;
                }
            }
            float r = cell();
            int cell = cellAt(mx, my, width / 2f, height * 0.5f, r);
            if (cell >= 0) {
                ripples.add(new Ripple(cellX(cell, width / 2f, r), cellY(cell, height * 0.5f, r), seconds));
                play(HoneyLattice.isLit(state.state(), cell) ? SoundEvents.AMETHYST_BLOCK_BREAK :
                        SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f + 0.05f * HoneyLattice.litCount(state.state()), 0.7f);
                send(C2SHoneyLatticePacket.PRESS, cell);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void drawIntro(GuiGraphics g) {
        g.flush();

        float black = seconds < INTRO_SECONDS ? 0.4f + 0.6f * (1f - Mth.clamp(seconds / 1.2f, 0f, 1f)) :
                0.4f * (1f - Mth.clamp((seconds - INTRO_SECONDS) / 0.7f, 0f, 1f));
        g.fill(0, 0, width, height, argb(0x000000, black));
        Cinematic.vignette(g, width, height, 0.6f * (1f - black));
        int bar = (int) (height * Cinematic.letterbox(seconds, INTRO_SECONDS, 0f));
        g.fill(0, 0, width, bar, 0xFF000000);
        g.fill(0, height - bar, width, height, 0xFF000000);
        Cinematic.titleCard(g, font, width, height, seconds - 0.3f, INTRO_SECONDS - 0.9f, "THE COMB",
                "Crystallize the lattice. Light every cell the pattern asks for.", AMBER, GOLD);
        g.drawCenteredString(font, "click to skip", width / 2, height - Math.max(12, (int) (height * 0.03f)),
                argb(0x887766, 0.5f * Mth.clamp(1f - seconds / INTRO_SECONDS, 0f, 1f)));
    }

    @Override
    public void removed() {
        CinematicPrefs.hide();
        super.removed();
    }

    private BufferBuilder begin(boolean additive) {
        return GlDraw.begin(additive);
    }

    private void end(BufferBuilder buf) {
        GlDraw.end(buf, prof);
    }
}
