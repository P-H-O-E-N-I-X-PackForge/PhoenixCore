package net.phoenix.core.client.renderer.cinema.cutscene;

import net.phoenix.core.client.renderer.cinema.cutscene.CutsceneDefinition.MusicSettings;
import net.phoenix.core.client.renderer.cinema.cutscene.CutsceneDefinition.SoundSettings;
import net.phoenix.core.client.renderer.cinema.cutscene.CutsceneDefinition.TextSettings;
import net.phoenix.core.client.renderer.cinema.cutscene.background.CutsceneBackground;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a {@link CutsceneDefinition}: black screen, then a slowly fading-in
 * {@link CutsceneBackground}, with each page typed out letter by letter in gently waving text.
 * Click / Space / Enter finishes the current page or moves on; ESC skips the whole thing.
 */
public class CutsceneScreen extends Screen {

    private static final float PAGE_FADE_OUT = 0.4f;
    private static final float LETTER_FADE_IN = 0.15f;
    private static final float LETTER_RISE = 3f;
    private static final float NOT_COMPLETED = Float.MAX_VALUE;

    private final CutsceneDefinition definition;
    private final long startMillis = Util.getMillis();
    private final RandomSource random = RandomSource.create();

    private int page = 0;
    private float pageStart;
    /** Page-relative time at which the player clicked to reveal the rest of the page. */
    private float completedAt = NOT_COMPLETED;
    private float pageEndingAt = -1;
    private float outroAt = -1;
    private boolean closed;

    private CutsceneMusic music;
    private float pushedMusicVolume = -1;

    private List<Glyph> glyphs = List.of();
    private int lineCount;
    private int soundedGlyphs;

    private record Glyph(FormattedCharSequence text, float x, int line, float revealAt, boolean silent) {}

    public CutsceneScreen(CutsceneDefinition definition) {
        super(Component.empty());
        this.definition = definition;
        this.pageStart = definition.textStart();
    }

    @Override
    protected void init() {
        layoutPage();
    }

    private float now() {
        return (Util.getMillis() - startMillis) / 1000f;
    }

    // ---------------------------------------------------------------- layout

    private void layoutPage() {
        TextSettings text = definition.text();
        int wrapWidth = Math.max(40, (int) (width * text.maxWidth() / text.scale()));
        List<FormattedCharSequence> lines = font.split(definition.pages().get(page), wrapWidth);

        List<Glyph> result = new ArrayList<>();
        float letterTime = 1f / Math.max(0.01f, definition.charsPerSecond());
        float[] time = { 0 };
        for (int line = 0; line < lines.size(); line++) {
            FormattedCharSequence sequence = lines.get(line);
            float[] x = { -font.width(sequence) / 2f };
            int lineIndex = line;
            sequence.accept((index, style, codepoint) -> {
                FormattedCharSequence glyph = FormattedCharSequence.codepoint(codepoint, style);
                result.add(new Glyph(glyph, x[0], lineIndex, time[0], Character.isWhitespace(codepoint)));
                x[0] += font.width(glyph);
                time[0] += letterTime + pauseAfter(codepoint);
                return true;
            });
        }
        glyphs = result;
        lineCount = lines.size();
    }

    private float pauseAfter(int codepoint) {
        return switch (codepoint) {
            case '.', '!', '?', '…' -> definition.punctuationPause();
            case ',', ';', ':', '—' -> definition.punctuationPause() * 0.5f;
            default -> 0;
        };
    }

    // ---------------------------------------------------------------- flow

    private float pageRevealEnd() {
        float end = glyphs.isEmpty() ? 0 : glyphs.get(glyphs.size() - 1).revealAt();
        return Math.min(end, completedAt);
    }

    private boolean pageFullyShown(float time) {
        return time - pageStart >= pageRevealEnd();
    }

    private void update(float time) {
        if (outroAt >= 0) {
            if (time - outroAt >= definition.fadeOut()) close();
            return;
        }
        if (pageEndingAt >= 0) {
            if (time - pageEndingAt >= PAGE_FADE_OUT) nextPage(time);
            return;
        }
        if (definition.autoAdvance() >= 0 && time >= pageStart &&
                time - pageStart - pageRevealEnd() >= definition.autoAdvance()) {
            advance(time);
        }
    }

    /** Click behaviour: finish typing the current page, otherwise move to the next one. */
    private void advance(float time) {
        if (outroAt >= 0 || pageEndingAt >= 0 || time < pageStart) return;
        if (!pageFullyShown(time)) {
            completedAt = time - pageStart;
            soundedGlyphs = glyphs.size();
        } else if (page >= definition.pages().size() - 1) {
            outroAt = time;
        } else {
            pageEndingAt = time;
        }
    }

    private void nextPage(float time) {
        page++;
        pageStart = time;
        pageEndingAt = -1;
        completedAt = NOT_COMPLETED;
        soundedGlyphs = 0;
        layoutPage();
    }

    private void skip() {
        if (outroAt < 0) outroAt = now();
    }

    private void close() {
        if (closed) return;
        closed = true;
        onClose();
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        advance(now());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (definition.skippable()) skip();
                return true;
            }
            case GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                advance(now());
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return definition.pauseGame();
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float time = now();
        update(time);
        if (closed) return;

        float global = outroAt < 0 ? 1 : 1 - clamp01((time - outroAt) / Math.max(0.01f, definition.fadeOut()));
        float black = 1 - smooth(clamp01((time - definition.blackDuration()) / Math.max(0.01f, definition.fadeIn())));

        definition.background().render(graphics,
                new CutsceneBackground.BackgroundContext(width, height, time, global, mouseX, mouseY));
        if (black > 0) graphics.fill(0, 0, width, height, argb(black * global, 0x000000));
        renderText(graphics, time, global);
        renderHints(graphics, time, global);
        updateMusic(time, global);
    }

    private void updateMusic(float time, float global) {
        MusicSettings settings = definition.music().orElse(null);
        if (settings == null || minecraft == null) return;
        if (settings.stopGameMusic()) minecraft.getMusicManager().stopPlaying();

        if (music == null) {
            // Singleplayer pauses every playing sound on the frame after a pause screen opens, so wait for that
            // before starting (with a timeout in case the pause never comes).
            boolean willPause = isPauseScreen() && minecraft.hasSingleplayerServer() &&
                    !minecraft.getSingleplayerServer().isPublished();
            if (time < settings.start() || (willPause && !minecraft.isPaused() && time < 0.5f)) return;
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getOptional(settings.sound())
                    .orElseGet(() -> SoundEvent.createVariableRangeEvent(settings.sound()));
            music = new CutsceneMusic(event, settings.source(), settings.pitch(), settings.loop());
            minecraft.getSoundManager().play(music);
        }

        float fade = smooth(clamp01((time - settings.start()) / Math.max(0.01f, settings.fadeIn())));
        float volume = settings.volume() * fade * global;
        music.setVolume(volume);
        // Tickable sounds are not updated while the game is paused, so push the new volume to the engine directly.
        // Any non-master category makes the engine recalculate every playing sound, including ours.
        if (minecraft.isPaused() && Math.abs(volume - pushedMusicVolume) > 0.005f) {
            minecraft.getSoundManager().updateSourceVolume(SoundSource.MUSIC,
                    minecraft.options.getSoundSourceVolume(SoundSource.MUSIC));
            pushedMusicVolume = volume;
        }
    }

    @Override
    public void removed() {
        if (music != null && minecraft != null) minecraft.getSoundManager().stop(music);
        music = null;
    }

    private void renderText(GuiGraphics graphics, float time, float global) {
        float pageTime = time - pageStart;
        if (pageTime < 0) return;

        TextSettings text = definition.text();
        float pageAlpha = pageEndingAt < 0 ? 1 : 1 - clamp01((time - pageEndingAt) / PAGE_FADE_OUT);
        float lineHeight = font.lineHeight + text.lineSpacing();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(width / 2f, height / 2f - lineCount * lineHeight * text.scale() / 2f, 0);
        pose.scale(text.scale(), text.scale(), 1);

        int revealed = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph glyph = glyphs.get(i);
            float revealAt = Math.min(glyph.revealAt(), completedAt);
            if (revealAt > pageTime) break;
            revealed++;

            float letterAlpha = clamp01((pageTime - revealAt) / LETTER_FADE_IN);
            float alpha = letterAlpha * pageAlpha * global;
            if (alpha < 0.02f || glyph.silent()) continue;

            float wave = Mth.sin(time * text.waveSpeed() + i * text.waveFrequency()) * text.waveAmplitude();
            pose.pushPose();
            pose.translate(glyph.x(), glyph.line() * lineHeight + wave + (1 - letterAlpha) * LETTER_RISE, 0);
            graphics.drawString(font, glyph.text(), 0, 0, argb(alpha, text.color().getValue()), text.shadow());
            pose.popPose();
        }
        pose.popPose();

        playTypeSound(revealed);
    }

    private void playTypeSound(int revealed) {
        if (revealed <= soundedGlyphs || pageEndingAt >= 0 || outroAt >= 0 || minecraft == null) return;
        SoundSettings sound = definition.sound().orElse(null);
        int newest = revealed - 1;
        boolean due = sound != null && !glyphs.get(newest).silent() &&
                newest % Math.max(1, sound.everyNLetters()) == 0;
        soundedGlyphs = revealed;
        if (!due) return;

        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getOptional(sound.id())
                .orElseGet(() -> SoundEvent.createVariableRangeEvent(sound.id()));
        float pitch = sound.pitch() + (random.nextFloat() * 2 - 1) * sound.pitchVariance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, sound.volume()));
    }

    private void renderHints(GuiGraphics graphics, float time, float global) {
        // The font renderer treats near-zero alpha as fully opaque, so hide hints entirely at the end of the fade.
        if (global < 0.05f) return;
        if (definition.skippable()) {
            Component skip = Component.translatableWithFallback("cutscene.phoenixcore.skip", "Press ESC to skip");
            graphics.drawString(font, skip, width - font.width(skip) - 8, height - font.lineHeight - 6,
                    argb(0.45f * global, 0xFFFFFF), false);
        }

        // Bobbing "continue" arrow once the page is fully typed and waiting for a click.
        boolean waiting = definition.autoAdvance() < 0 && outroAt < 0 && pageEndingAt < 0 &&
                time >= pageStart && time - pageStart - pageRevealEnd() > 0.3f;
        if (waiting) {
            TextSettings text = definition.text();
            float blockBottom = height / 2f +
                    lineCount * (font.lineHeight + text.lineSpacing()) * text.scale() / 2f;
            float pulse = 0.55f + 0.35f * Mth.sin(time * 4f);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(width / 2f, blockBottom + 8 + Mth.sin(time * 3f) * 2f, 0);
            Component arrow = Component.literal("▼");
            graphics.drawString(font, arrow, -font.width(arrow) / 2, 0, argb(pulse * global, 0xFFFFFF), false);
            pose.popPose();
        }
    }

    // ---------------------------------------------------------------- utils

    private static float clamp01(float value) {
        return Mth.clamp(value, 0f, 1f);
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static int argb(float alpha, int rgb) {
        return ((int) (clamp01(alpha) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
