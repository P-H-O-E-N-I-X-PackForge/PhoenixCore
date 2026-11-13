package net.phoenix.core.client.gui.plasma;

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
import net.phoenix.core.common.machine.multiblock.electric.plasma.C2SPlasmaRingPacket;
import net.phoenix.core.common.machine.multiblock.electric.plasma.PlasmaRing;
import net.phoenix.core.common.machine.multiblock.electric.plasma.S2CPlasmaRingPacket;
import net.phoenixvine.wiki.ui.Cinematic;
import net.phoenixvine.wiki.ui.CinematicPrefs;
import net.phoenixvine.wiki.ui.GlDraw;
import net.phoenixvine.wiki.ui.UiProfiler;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.phoenixvine.wiki.ui.GlDraw.*;

@OnlyIn(Dist.CLIENT)
public class PlasmaRingScreen extends Screen {

    private static final float INTRO_SECONDS = 5f;
    private boolean introChecked;

    private static final int CYAN = 0x59E8FF;
    private static final int MAGENTA = 0xFF5CF0;
    private static final int HOT = 0xFFB060;
    private static final int WHITE = 0xFFFFFF;
    private static final int TEXT = 0xFFE8F6;
    private static final int DIM = 0x8C7098;

    private static final int[] PHASE_COLOR = { CYAN, 0x9A7CFF, MAGENTA, HOT };

    private S2CPlasmaRingPacket state;
    private float seconds;
    private float lockFlash;
    private float shake;
    private long lastNanos = -1L;
    private String message = "";
    private int messageColor = TEXT;
    private final List<Hit> hits = new ArrayList<>();
    private final Random rng = new Random();
    private final UiProfiler prof = new UiProfiler("plasma");
    private int lastDrift;

    private record Hit(int x, int y, int w, int h, Runnable action) {

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public PlasmaRingScreen(S2CPlasmaRingPacket state) {
        super(Component.literal("Containment Ring"));
        this.state = state;
        this.lastDrift = PlasmaRing.drifting(state.state());
    }

    public BlockPos pos() {
        return state.pos();
    }

    public void update(S2CPlasmaRingPacket next) {
        int drift = PlasmaRing.drifting(next.state());
        if (next.solved()) {
            lockFlash = 1f;
            shake = 1f;
            play(SoundEvents.BEACON_ACTIVATE, 1.4f, 0.8f);
            play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.6f, 0.3f);
        } else if (next.moves() != state.moves()) {
            play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f + 0.15f * (6 - drift), 0.7f);
            if (drift > lastDrift) shake = Math.max(shake, 0.25f);
        }
        lastDrift = drift;
        this.state = next;
        if (!next.message().isEmpty()) {
            message = next.message();
            messageColor = next.solved() ? 0xFF9CF4FF : 0xFFFFE8F6;
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

    private void arc(BufferBuilder buf, Matrix4f pose, float x0, float y0, float x1, float y1, float jitter, long seed,
                     int color, float alpha) {
        Random r = new Random(seed);
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 3f) return;
        int segments = Math.max(4, (int) (len / 10f));
        float nx = -dy / len;
        float ny = dx / len;
        float px = x0;
        float py = y0;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float off = i == segments ? 0 : (r.nextFloat() - 0.5f) * 2f * jitter * (float) Math.sin(Math.PI * t);
            float x = x0 + dx * t + nx * off;
            float y = y0 + dy * t + ny * off;
            line(buf, pose, px, py, x, y, 2.2f, argb(color, alpha * 0.22f));
            line(buf, pose, px, py, x, y, 0.6f, argb(WHITE, alpha));
            px = x;
            py = y;
        }
    }

    private float cx() {
        return width / 2f;
    }

    private float cy() {
        return height * 0.5f;
    }

    private float ringRadius() {
        return Math.min(width * 0.28f, height * 0.33f);
    }

    private float[] coilPos(int i) {
        double a = Math.toRadians(-90 + 60 * i);
        float r = ringRadius();
        return new float[] { cx() + (float) Math.cos(a) * r, cy() + (float) Math.sin(a) * r };
    }

    private float coilSize() {
        return Math.min(width, height) * 0.052f;
    }

    private float unrest() {
        return PlasmaRing.drifting(state.state()) / (float) PlasmaRing.CELLS;
    }

    private boolean shieldUp() {
        return state.shieldKey() == 1;
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
        lockFlash = Math.max(0f, lockFlash - dt * 1.2f);
        shake = Math.max(0f, shake - dt * 1.6f);
        prof.frame(dt);
        long renderStart = prof.start();
        hits.clear();

        g.fillGradient(0, 0, width, height, 0xFF040208, 0xFF10061A);
        g.pose().pushPose();
        if (shake > 0f) {
            g.pose().translate((rng.nextFloat() - 0.5f) * 8f * shake, (rng.nextFloat() - 0.5f) * 5f * shake, 0);
        }
        if (seconds < INTRO_SECONDS) {
            float zoom = Cinematic.pushIn(seconds, INTRO_SECONDS);
            g.pose().translate(width / 2f, height / 2f, 0);
            g.pose().scale(zoom, zoom, 1f);
            g.pose().translate(-width / 2f, -height / 2f, 0);
        }
        timed("shield", () -> drawShield(g));
        timed("plasma", () -> drawPlasma(g));
        timed("coils", () -> drawCoils(g, mouseX, mouseY));
        g.pose().popPose();

        g.fillGradient(0, 0, width, (int) (height * 0.2f), 0xDD000000, 0x00000000);
        g.fillGradient(0, (int) (height * 0.78f), width, height, 0x00000000, 0xDD000000);
        int bars = (int) (height * Cinematic.letterbox(seconds, INTRO_SECONDS, 0.06f));
        g.fill(0, 0, width, bars, 0xFF000000);
        g.fill(0, height - bars, width, height, 0xFF000000);

        float ui = Mth.clamp((seconds - INTRO_SECONDS) / 0.9f, 0f, 1f);
        if (ui > 0f) {
            drawLeft(g, ui);
            drawRight(g, mouseX, mouseY, ui);
            drawFooter(g, ui);
        }
        if (lockFlash > 0f) g.fill(0, 0, width, height, argb(0xC8F8FF, Math.min(1f, lockFlash) * 0.7f));
        float toggleAlpha = Mth.clamp(seconds / 1.0f, 0f, 1f) * (seconds < INTRO_SECONDS ? 0.55f : Math.max(ui, 0.55f));
        CinematicPrefs.draw(g, font, width, bars / 2 - 12, mouseX, mouseY, toggleAlpha);
        if (seconds < INTRO_SECONDS + 0.7f) drawIntro(g);
        prof.end("render", renderStart);
        prof.draw(g, font);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void timed(String section, Runnable r) {
        long t = prof.start();
        r.run();
        prof.end(section, t);
    }

    private void drawShield(GuiGraphics g) {
        Matrix4f pose = g.pose().last().pose();
        float health = shieldUp() ? Mth.clamp(state.shieldHealth() / 1000f, 0f, 1f) : 0f;
        BufferBuilder buf = begin();
        float r = ringRadius() * 1.5f;
        for (int k = 0; k < 4; k++) {
            float rr = r + k * 12f;
            float sweep = Mth.TWO_PI * (health > 0f ? 0.2f + 0.8f * health : 0.08f);
            float spin = seconds * (0.12f + 0.05f * k) * (k % 2 == 0 ? 1f : -1f);
            int color = health > 0f ? CYAN : 0x5A4870;
            ring(buf, pose, cx(), cy(), rr, 1.2f - k * 0.2f, spin, sweep, argb(color, health > 0f ? 0.5f - k * 0.08f :
                    0.14f));
        }
        if (health > 0f) disc(buf, pose, cx(), cy(), r, argb(CYAN, 0f), argb(CYAN, 0.06f * (0.6f + health)));
        end(buf);
    }

    private void drawPlasma(GuiGraphics g) {
        Matrix4f pose = g.pose().last().pose();
        float unrest = unrest();
        float calm = 1f - unrest;
        float base = ringRadius() * 0.42f;
        float pulse = 1f + (0.04f + 0.12f * unrest) * (float) Math.sin(seconds * (3f + 6f * unrest));
        BufferBuilder buf = begin();
        for (int k = 5; k >= 1; k--) {
            float r = base * (0.45f + k * 0.4f) * pulse;
            int c = k > 3 ? (calm > 0.5f ? CYAN : MAGENTA) : HOT;
            disc(buf, pose, cx(), cy(), r, argb(k == 1 ? WHITE : c, k == 1 ? 0.95f : 0.1f), argb(c, 0f));
        }

        long frame = (long) (seconds * (8f + 14f * unrest));
        for (int i = 0; i < PlasmaRing.CELLS; i++) {
            int phase = PlasmaRing.phase(state.state(), i);
            float[] p = coilPos(i);
            if (phase == 0) {
                arc(buf, pose, cx(), cy(), p[0], p[1], 3f, i * 7919L, CYAN, 0.28f);
            } else {
                for (int k = 0; k < phase; k++) {
                    arc(buf, pose, cx(), cy(), p[0], p[1], 14f + phase * 8f, frame * 31L + i * 131L + k,
                            PHASE_COLOR[phase],
                            0.85f);
                }
            }
        }

        for (int i = 0; i < 70; i++) {
            float a = noise(i, 1f) * Mth.TWO_PI + seconds * (0.2f + noise(i, 2f));
            float d = base * (0.3f + 1.8f * ((seconds * (0.1f + 0.3f * noise(i, 3f)) + noise(i, 4f)) % 1f)) *
                    (0.6f + unrest);
            float x = cx() + (float) Math.cos(a) * d;
            float y = cy() + (float) Math.sin(a) * d;
            float s = 0.7f + noise(i, 5f) * 1.6f;
            int c = argb(calm > 0.5f ? CYAN : MAGENTA, 0.5f);
            buf.addVertex(pose, x - s, y - s, 0).setColor(c);
            buf.addVertex(pose, x - s, y + s, 0).setColor(c);
            buf.addVertex(pose, x + s, y + s, 0).setColor(c);
            buf.addVertex(pose, x + s, y - s, 0).setColor(c);
        }
        end(buf);
    }

    private void drawCoils(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        float size = coilSize();
        BufferBuilder buf = begin();

        ring(buf, pose, cx(), cy(), ringRadius(), 0.8f, 0f, Mth.TWO_PI, argb(DIM, 0.4f));
        for (int i = 0; i < PlasmaRing.CELLS; i++) {
            float[] p = coilPos(i);
            int phase = PlasmaRing.phase(state.state(), i);
            int color = PHASE_COLOR[phase];
            float flicker = phase == 0 ? 1f : 0.7f + 0.3f * noise(i, (float) Math.floor(seconds * 14f));
            disc(buf, pose, p[0], p[1], size * 1.5f, argb(color, 0.2f * flicker), argb(color, 0f));
            ring(buf, pose, p[0], p[1], size, 1.6f, 0f, Mth.TWO_PI, argb(color, 0.9f * flicker));

            for (int k = 0; k < PlasmaRing.PHASES; k++) {
                float a = (float) Math.toRadians(-90 + 90 * k);
                float x = p[0] + (float) Math.cos(a) * size * 0.55f;
                float y = p[1] + (float) Math.sin(a) * size * 0.55f;
                float s = k == phase ? 3.2f : 1.4f;
                int c = argb(k == phase ? WHITE : color, k == phase ? 1f : 0.45f);
                buf.addVertex(pose, x - s, y - s, 0).setColor(c);
                buf.addVertex(pose, x - s, y + s, 0).setColor(c);
                buf.addVertex(pose, x + s, y + s, 0).setColor(c);
                buf.addVertex(pose, x + s, y - s, 0).setColor(c);
            }
            final int cell = i;
            if (seconds > INTRO_SECONDS - 1f) {
                hits.add(new Hit((int) (p[0] - size), (int) (p[1] - size), (int) (size * 2), (int) (size * 2),
                        () -> PacketDistributor.sendToServer(
                                new C2SPlasmaRingPacket(state.pos(), C2SPlasmaRingPacket.TUNE, cell))));
            }
        }
        end(buf);
        for (int i = 0; i < PlasmaRing.CELLS; i++) {
            float[] p = coilPos(i);
            g.drawCenteredString(font, Integer.toString(i + 1), (int) p[0], (int) (p[1] + size + 5),
                    argb(DIM, 0.9f));
        }
    }

    private void drawIntro(GuiGraphics g) {
        g.flush();

        float black = seconds < INTRO_SECONDS ? 1f : 1f - Mth.clamp((seconds - INTRO_SECONDS) / 0.7f, 0f, 1f);
        g.fill(0, 0, width, height, argb(0x000000, black));
        Cinematic.vignette(g, width, height, 0.6f * (1f - black));
        Cinematic.titleCard(g, font, width, height, seconds - 0.3f, INTRO_SECONDS - 0.9f, "CONTAINMENT",
                "Hold the plasma. Every coil in phase.", MAGENTA, CYAN);
        g.drawCenteredString(font, "click to skip", width / 2, height - Math.max(12, (int) (height * 0.03f)),
                argb(0x887799, 0.5f * Mth.clamp(1f - seconds / INTRO_SECONDS, 0f, 1f)));
    }

    private int panelW() {
        return Mth.clamp((int) (width * 0.25f), 150, 210);
    }

    private void fadePanel(GuiGraphics g, int x, int w, boolean leftEdge, float ui) {
        int strip = 3;
        for (int i = 0; i < w; i += strip) {
            float t = i / (float) w;
            float a = 0.82f * (float) Math.pow(1f - t, 1.4f) * ui;
            int sx = leftEdge ? x + i : x + w - i - strip;
            g.fill(sx, 0, sx + strip, height, argb(0x06030C, a));
        }
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean enabled,
                        Runnable action) {
        boolean over = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, over ? argb(MAGENTA, 0.35f) : argb(0x0C0612, 0.85f));
        g.renderOutline(x, y, w, h, enabled ? argb(MAGENTA, 0.85f) : 0xFF2A2036);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? TEXT : 0xFF2A2036);
        if (enabled) hits.add(new Hit(x, y, w, h, action));
    }

    private int bar() {
        return (int) (height * 0.06f);
    }

    private void drawLeft(GuiGraphics g, float ui) {
        int pw = panelW();
        fadePanel(g, 0, pw, true, ui);
        int x = 14;
        int y = bar() + 14;
        g.drawString(font, "CONTAINMENT RING", x, y, argb(MAGENTA, ui), false);
        y += 18;
        g.drawString(font, "Shield", x, y, argb(DIM, ui), false);
        y += 11;
        String shield = shieldUp() ? state.shieldHealth() + " HP" : state.shieldKey() == 3 ? "decayed" : "down";
        g.drawString(font, shield, x, y, argb(shieldUp() ? CYAN : 0xFF6A6A, ui), false);
        y += 11;
        if (!shieldUp() && state.cooldownTicks() > 0) {
            g.drawString(font, "cooling " + (state.cooldownTicks() + 19) / 20 + "s", x, y, argb(HOT, ui), false);
        }
        y += 18;
        g.drawString(font, "Coils out of phase", x, y, argb(DIM, ui), false);
        y += 11;
        g.drawString(font, PlasmaRing.drifting(state.state()) + " of " + PlasmaRing.CELLS, x, y, argb(TEXT, ui),
                false);
        y += 18;
        g.drawString(font, "Tunings", x, y, argb(DIM, ui), false);
        y += 11;
        g.drawString(font, state.moves() + "  (par " + state.par() + ")", x, y, argb(TEXT, ui), false);
        y += 18;
        g.drawString(font, "Locks so far", x, y, argb(DIM, ui), false);
        y += 11;
        g.drawString(font, Integer.toString(state.solves()), x, y, argb(TEXT, ui), false);
        y += 18;
        g.drawString(font, "Forge network", x, y, argb(DIM, ui), false);
        y += 11;
        g.drawString(font, state.linked() ? "linked" : "not linked", x, y, argb(state.linked() ? CYAN : 0xFF6A6A, ui),
                false);
    }

    private void drawRight(GuiGraphics g, int mx, int my, float ui) {
        int pw = panelW();
        fadePanel(g, width - pw, pw, false, ui);
        int x = width - pw + 12;
        int w = pw - 26;
        int y = bar() + 14;
        g.drawString(font, "HOW IT WORKS", x, y, argb(MAGENTA, ui), false);
        y += 14;
        String help = "Each coil sits at a phase from 1 to 4. The plasma is held when every coil is at phase 1." +
                " Tuning a coil moves it one phase on and drags both neighbours one phase back." +
                " A lock raises the shield. The shield only wears down while the furnace is working.";
        for (FormattedCharSequence line : font.split(Component.literal(help), w)) {
            g.drawString(font, line, x, y, argb(0xB89CD0, ui), false);
            y += 10;
        }
    }

    private void drawFooter(GuiGraphics g, float ui) {
        if (!message.isEmpty()) g.drawCenteredString(font, message, width / 2, height - bar() - 30, messageColor);
        if (!state.linked()) {
            g.drawCenteredString(font, "Not bound to the Phoenix Forge. The furnace runs fine without it.", width / 2,
                    height - bar() - 18, argb(0xFF6A6A, ui));
        }
        button(g, width - panelW() + 12, height - bar() - 24, 56, 15, "Leave", -1, -1, true, this::onClose);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == UiProfiler.TOGGLE_KEY) {
            prof.toggle();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
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
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void removed() {
        CinematicPrefs.hide();
        super.removed();
    }

    private BufferBuilder begin() {
        return GlDraw.begin(true);
    }

    private void end(BufferBuilder buf) {
        GlDraw.end(buf, prof);
    }
}
