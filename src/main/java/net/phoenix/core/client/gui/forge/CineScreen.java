package net.phoenix.core.client.gui.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenixvine.wiki.ui.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public abstract class CineScreen extends Screen {

    public record Hit(int x, int y, int w, int h, Runnable action) {

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    protected float seconds;
    protected float dt = 0.016f;
    protected float shake;
    protected float flash;
    protected int flashColor = 0xFFF4DC;
    protected String message = "";
    protected int messageColor = 0xFFFFEBD0;
    protected final List<Hit> hits = new ArrayList<>();
    protected final Random rng = new Random();
    protected final UiProfiler prof;
    private long lastNanos = -1L;
    private boolean introChecked;

    protected CineScreen(String title, String profilerName) {
        super(Component.literal(title));
        this.prof = new UiProfiler(profilerName);
    }

    protected abstract int accent();

    protected abstract int accent2();

    protected abstract int bgTop();

    protected abstract int bgBottom();

    protected abstract float introSeconds();

    protected abstract String introTitle();

    protected abstract String introSub();

    protected abstract void drawScene(GuiGraphics g, int mx, int my);

    protected abstract void drawUi(GuiGraphics g, int mx, int my, float ui);

    protected static final float FADE_UP = 0.7f;

    protected float uiAlpha() {
        return Mth.clamp((seconds - introSeconds()) / 0.9f, 0f, 1f);
    }

    protected void afterFrame(GuiGraphics g) {}

    @Override
    public void removed() {
        CinematicPrefs.hide();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected void renderBlurredBackground(float partialTick) {}

    protected float fit = 1f;

    protected double vx(double x) {
        return x / fit;
    }

    protected double vy(double y) {
        return y / fit;
    }

    @Override
    public void render(GuiGraphics g, int rawMouseX, int rawMouseY, float partialTick) {
        fit = ScreenFit.apply(this);
        g.pose().pushPose();
        g.pose().scale(fit, fit, 1f);
        renderStage(g, (int) (rawMouseX / fit), (int) (rawMouseY / fit), partialTick);
        g.pose().popPose();
    }

    private void renderStage(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        dt = lastNanos < 0 ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        if (!introChecked) {
            introChecked = true;
            if (!CinematicPrefs.playIntro()) seconds = introSeconds();
        }
        seconds += dt;
        flash = Math.max(0f, flash - dt * 1.1f);
        shake = Math.max(0f, shake - dt * 1.5f);
        prof.frame(dt);
        long renderStart = prof.start();
        hits.clear();

        g.fillGradient(0, 0, width, height, 0xFF000000 | bgTop(), 0xFF000000 | bgBottom());
        g.pose().pushPose();
        if (shake > 0f) {
            g.pose().translate((rng.nextFloat() - 0.5f) * 9f * shake, (rng.nextFloat() - 0.5f) * 6f * shake, 0);
        }
        long t = prof.start();
        if (seconds < introSeconds()) {
            float zoom = Cinematic.pushIn(seconds, introSeconds());
            g.pose().translate(width / 2f, height / 2f, 0);
            g.pose().scale(zoom, zoom, 1f);
            g.pose().translate(-width / 2f, -height / 2f, 0);
        }
        if (seconds < introSeconds()) {
            TextlessGraphics quiet = new TextlessGraphics(g);
            drawScene(quiet, mouseX, mouseY);
            quiet.flush();
        } else {
            drawScene(g, mouseX, mouseY);
        }
        prof.end("scene", t);
        g.pose().popPose();

        g.flush();

        g.fillGradient(0, 0, width, (int) (height * 0.2f), 0xDD000000, 0x00000000);
        g.fillGradient(0, (int) (height * 0.78f), width, height, 0x00000000, 0xDD000000);
        int bars = bar();
        g.fill(0, 0, width, bars, 0xFF000000);
        g.fill(0, height - bars, width, height, 0xFF000000);

        float ui = uiAlpha();
        if (ui > 0f) {
            t = prof.start();
            drawUi(g, mouseX, mouseY, ui);
            prof.end("ui", t);
        }
        afterFrame(g);
        float toggleAlpha = Mth.clamp(seconds, 0f, 1f) *
                (seconds < introSeconds() ? 0.55f : Math.max(ui, 0.55f));
        CinematicPrefs.draw(g, font, width, bar() / 2 - 12, mouseX, mouseY, toggleAlpha);
        if (flash > 0f) g.fill(0, 0, width, height, argb(flashColor, Math.min(1f, flash)));
        if (seconds < introSeconds() + FADE_UP) drawIntro(g);
        prof.end("render", renderStart);
        prof.draw(g, font);
        super.render(g, mouseX, mouseY, partialTick);
    }

    protected int bar() {
        return (int) (height * Cinematic.letterbox(seconds, introSeconds(), 0.06f));
    }

    private void drawIntro(GuiGraphics g) {
        g.flush();
        float total = introSeconds();

        float black = seconds < total ? 0.45f + 0.55f * (1f - Mth.clamp(seconds / 1.2f, 0f, 1f)) :
                0.45f * (1f - Mth.clamp((seconds - total) / FADE_UP, 0f, 1f));
        g.fill(0, 0, width, height, argb(0x000000, black));
        g.flush();
        Cinematic.vignette(g, width, height, 0.6f * (1f - black));
        if (seconds < total) {
            Cinematic.titleCard(g, font, width, height, seconds - 0.3f, total - 0.9f, introTitle(), introSub(),
                    accent(), accent2());
            g.drawCenteredString(font, "click to skip", width / 2, height - Math.max(12, (int) (height * 0.03f)),
                    argb(0x887766, 0.5f * Mth.clamp(1f - seconds / total, 0f, 1f)));
        }
    }

    protected static int argb(int rgb, float alpha) {
        return GlDraw.argb(rgb, alpha);
    }

    protected static float ease(float t) {
        return GlDraw.ease(t);
    }

    protected static float noise(float a, float b) {
        return GlDraw.noise(a, b);
    }

    protected void glowText(GuiGraphics g, String text, float cx, float y, float scale, int color, float alpha,
                            int glow) {
        if (alpha <= 0.01f) return;
        g.pose().pushPose();
        g.pose().translate(Math.round(cx), Math.round(y), 0);
        g.pose().scale(scale, scale, 1f);
        int x = -font.width(text) / 2;
        g.drawString(font, text, x + 2, 2, argb(0x000000, alpha * 0.9f), false);
        g.drawString(font, text, x + 1, 1, argb(0x000000, alpha * 0.7f), false);
        g.drawString(font, text, x - 1, 0, argb(glow, alpha * 0.28f), false);
        g.drawString(font, text, x + 1, 0, argb(glow, alpha * 0.28f), false);
        g.drawString(font, text, x, -1, argb(glow, alpha * 0.28f), false);
        g.drawString(font, text, x, 1, argb(glow, alpha * 0.28f), false);
        g.drawString(font, text, x, 0, argb(color, alpha), false);
        g.pose().popPose();
    }

    protected void wrapped(GuiGraphics g, String text, int x, int y, int w, int color, float ui) {
        for (FormattedCharSequence line : font.split(Component.literal(text), w)) {
            g.drawString(font, line, x, y, argb(color, ui), false);
            y += 10;
        }
    }

    protected int wrappedHeight(String text, int w) {
        return font.split(Component.literal(text), w).size() * 10;
    }

    protected void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    protected int panelW() {
        return Mth.clamp((int) (width * 0.24f), 150, 210);
    }

    protected void fadePanel(GuiGraphics g, boolean left, float ui) {
        int w = panelW();
        int strip = 3;
        for (int i = 0; i < w; i += strip) {
            float t = i / (float) w;
            float a = 0.96f * (t < 0.7f ? 1f : (float) Math.pow(1f - (t - 0.7f) / 0.3f, 1.2f)) * ui;
            int sx = left ? i : width - i - strip;
            g.fill(sx, 0, sx + strip, height, argb(0x050304, a));
        }
    }

    protected void heading(GuiGraphics g, String text, int x, int y, float ui) {
        g.drawString(font, text, x, y, argb(accent(), ui), false);
        g.fill(x, y + 10, x + font.width(text), y + 11, argb(accent(), ui * 0.5f));
    }

    protected void stat(GuiGraphics g, String label, String value, int valueColor, int x, int y, float ui) {
        g.drawString(font, label, x, y, argb(0x8C7A68, ui), false);
        g.drawString(font, value, x, y + 10, argb(valueColor, ui), false);
    }

    protected void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean enabled,
                          float ui, Runnable action) {
        boolean over = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, over ? argb(accent(), 0.35f * ui) : argb(0x0C0806, 0.85f * ui));
        g.renderOutline(x, y, w, h, argb(accent(), (enabled ? 0.85f : 0.2f) * ui));
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, argb(enabled ? 0xFFEBD0 : 0x6A5A4A, ui));
        if (enabled && ui > 0.6f) hits.add(new Hit(x, y, w, h, action));
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
    public boolean mouseClicked(double rawX, double rawY, int button) {
        double mx = vx(rawX);
        double my = vy(rawY);
        if (button == 0 && CinematicPrefs.click(mx, my)) return true;
        if (seconds < introSeconds()) {
            seconds = introSeconds();
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
        return super.mouseClicked(rawX, rawY, button);
    }

    protected BufferBuilder begin() {
        return GlDraw.begin(true);
    }

    protected void end(BufferBuilder buf) {
        GlDraw.end(buf, prof);
    }

    protected void quad(BufferBuilder buf, Matrix4f pose, float x, float y, float s, int color) {
        GlDraw.quad(buf, pose, x, y, s, color);
    }

    protected void line(BufferBuilder buf, Matrix4f pose, float x0, float y0, float x1, float y1, float w0, float w1,
                        int c0, int c1) {
        GlDraw.line(buf, pose, x0, y0, x1, y1, w0, w1, c0, c1);
    }

    protected void line(BufferBuilder buf, Matrix4f pose, float x0, float y0, float x1, float y1, float w, int c) {
        GlDraw.line(buf, pose, x0, y0, x1, y1, w, c);
    }

    protected void ring(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, float thick, float start,
                        float sweep, int color) {
        GlDraw.ring(buf, pose, cx, cy, r, thick, start, sweep, color);
    }

    protected void disc(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, int inner, int outer) {
        GlDraw.disc(buf, pose, cx, cy, r, inner, outer);
    }

    protected void tri(BufferBuilder buf, Matrix4f pose, float x0, float y0, float x1, float y1, float x2, float y2,
                       int c0, int c1, int c2) {
        GlDraw.tri(buf, pose, x0, y0, x1, y1, x2, y2, c0, c1, c2);
    }

    protected void polygon(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, int sides, float rot,
                           float thick, int color) {
        GlDraw.polygon(buf, pose, cx, cy, r, sides, rot, thick, color);
    }

    protected void polyFill(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, int sides, float rot,
                            int inner, int outer) {
        GlDraw.polyFill(buf, pose, cx, cy, r, sides, rot, inner, outer);
    }

    protected void rays(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r0, float r1, int count, float rot,
                        float halfWidth, int color, float alpha) {
        GlDraw.rays(buf, pose, cx, cy, r0, r1, count, rot, halfWidth, color, alpha);
    }

    protected void ticks(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, int count, int major, float rot,
                         float len, int color) {
        GlDraw.ticks(buf, pose, cx, cy, r, count, major, rot, len, color);
    }

    protected void bolt(BufferBuilder buf, Matrix4f pose, float x0, float y0, float x1, float y1, float jitter,
                        long seed, int color, float alpha) {
        GlDraw.bolt(buf, pose, x0, y0, x1, y1, jitter, seed, color, alpha);
    }
}
