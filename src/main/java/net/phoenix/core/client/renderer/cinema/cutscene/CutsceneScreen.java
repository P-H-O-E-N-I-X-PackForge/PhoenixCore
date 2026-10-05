package net.phoenix.core.client.renderer.cinema.cutscene;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.cinema.cutscene.CutsceneDefinition.MusicSettings;
import net.phoenix.core.client.renderer.cinema.cutscene.CutsceneDefinition.SoundSettings;
import net.phoenix.core.client.renderer.cinema.cutscene.CutsceneDefinition.TextSettings;
import net.phoenix.core.client.renderer.cinema.cutscene.CutscenePage.Choice;
import net.phoenix.core.client.renderer.cinema.cutscene.CutscenePage.CutsceneImage;
import net.phoenix.core.client.renderer.cinema.cutscene.background.CutsceneBackground;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCutsceneActionPacket;
import net.phoenix.core.network.packet.C2SCutsceneRequestPacket;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plays a {@link CutsceneDefinition}: black screen, then a slowly fading-in {@link CutsceneBackground}, with each
 * {@link CutscenePage} typed out letter by letter in gently waving text, plus its images and choices.
 * Click / Space / Enter finishes the current page or moves on, 1-9 or clicking picks a choice, ESC skips.
 */
public class CutsceneScreen extends Screen {

    private static final float PAGE_FADE_OUT = 0.4f;
    private static final float LETTER_FADE_IN = 0.15f;
    private static final float LETTER_RISE = 3f;
    private static final float CHOICE_DELAY = 0.25f;
    private static final float CHOICE_FADE_IN = 0.4f;
    private static final float CHOICE_SCALE = 1.15f;
    private static final float TEXT_Z = 400;
    private static final float NOT_COMPLETED = Float.MAX_VALUE;
    private static final ResourceLocation PLAYER = new ResourceLocation("minecraft", "player");

    private final ResourceLocation id;
    private final CutsceneDefinition definition;
    private final Map<String, Integer> labels = new HashMap<>();
    private final long startMillis = Util.getMillis();
    private final RandomSource random = RandomSource.create();

    private int page = 0;
    private float pageStart;
    /** Page-relative time at which the player clicked to reveal the rest of the page. */
    private float completedAt = NOT_COMPLETED;
    private float pageEndingAt = -1;
    private int targetPage;
    private float outroAt = -1;
    private boolean pageActionsSent;
    private boolean closed;

    private CutsceneMusic music;
    private float pushedMusicVolume = -1;

    private List<Glyph> glyphs = List.of();
    private int lineCount;
    private int soundedGlyphs;
    private final List<ChoiceBox> choiceBoxes = new ArrayList<>();
    private final Map<CutsceneImage, Optional<LivingEntity>> entities = new HashMap<>();
    private final Map<CutsceneImage, ItemStack> items = new HashMap<>();

    private record Glyph(FormattedCharSequence text, float x, int line, float revealAt, boolean silent) {}

    private record ChoiceBox(int index, float x0, float y0, float x1, float y1) {

        boolean contains(double x, double y) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1;
        }
    }

    public CutsceneScreen(ResourceLocation id, CutsceneDefinition definition) {
        super(Component.empty());
        this.id = id;
        this.definition = definition;
        this.pageStart = definition.textStart();
        List<CutscenePage> pages = definition.pages();
        for (int i = 0; i < pages.size(); i++) {
            int index = i;
            pages.get(i).id().ifPresent(label -> {
                if (labels.putIfAbsent(label, index) != null) {
                    PhoenixCore.LOGGER.warn("Cutscene {} has more than one page with id '{}'", id, label);
                }
            });
        }
    }

    @Override
    protected void init() {
        layoutPage();
    }

    private float now() {
        return (Util.getMillis() - startMillis) / 1000f;
    }

    private CutscenePage currentPage() {
        return definition.pages().get(page);
    }

    // ---------------------------------------------------------------- layout

    private void layoutPage() {
        TextSettings text = definition.text();
        int wrapWidth = Math.max(40, (int) (width * text.maxWidth() / text.scale()));
        List<FormattedCharSequence> lines = font.split(currentPage().content(), wrapWidth);

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

    private float lineHeight() {
        return font.lineHeight + definition.text().lineSpacing();
    }

    private float textTop() {
        float centre = currentPage().textY().orElse(0.5f) * height;
        return centre - lineCount * lineHeight() * definition.text().scale() / 2f;
    }

    private float textBottom() {
        return textTop() + lineCount * lineHeight() * definition.text().scale();
    }

    // ---------------------------------------------------------------- flow

    private float pageRevealEnd() {
        float end = glyphs.isEmpty() ? 0 : glyphs.get(glyphs.size() - 1).revealAt();
        return Math.min(end, completedAt);
    }

    private boolean pageFullyShown(float time) {
        return time - pageStart >= pageRevealEnd();
    }

    private boolean choicesVisible(float time) {
        return !currentPage().choices().isEmpty() && outroAt < 0 && pageEndingAt < 0 &&
                time - pageStart - pageRevealEnd() >= CHOICE_DELAY;
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
        if (!pageActionsSent && time >= pageStart) {
            pageActionsSent = true;
            sendActions(currentPage().actions());
        }
        if (definition.autoAdvance() >= 0 && currentPage().choices().isEmpty() && time >= pageStart &&
                time - pageStart - pageRevealEnd() >= definition.autoAdvance()) {
            advance(time);
        }
    }

    /** Click behaviour: finish typing the current page, otherwise move on (unless a choice has to be made). */
    private void advance(float time) {
        if (outroAt >= 0 || pageEndingAt >= 0 || time < pageStart) return;
        if (!pageFullyShown(time)) {
            completedAt = time - pageStart;
            soundedGlyphs = glyphs.size();
            return;
        }
        CutscenePage current = currentPage();
        if (!current.choices().isEmpty()) return;
        leavePage(time, current.end() ? -1 : resolve(current.gotoLabel(), page + 1));
    }

    private void select(int index) {
        float time = now();
        if (!choicesVisible(time) || index < 0 || index >= currentPage().choices().size()) return;
        Choice choice = currentPage().choices().get(index);
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 0.25f));
        }
        sendActions(choice.actions());
        if (choice.cutscene().isPresent()) {
            // The server answers with the new cutscene, which replaces this screen.
            PhoenixNetwork.CHANNEL.sendToServer(new C2SCutsceneRequestPacket(choice.cutscene().get()));
            startOutro(time);
        } else if (choice.end()) {
            startOutro(time);
        } else {
            leavePage(time, resolve(choice.gotoLabel(), page + 1));
        }
    }

    private int resolve(Optional<String> label, int fallback) {
        if (label.isEmpty()) return fallback;
        Integer target = labels.get(label.get());
        if (target == null) {
            PhoenixCore.LOGGER.warn("Cutscene {} jumps to unknown page id '{}'", id, label.get());
            return fallback;
        }
        return target;
    }

    private void leavePage(float time, int target) {
        if (target < 0 || target >= definition.pages().size()) {
            startOutro(time);
        } else {
            targetPage = target;
            pageEndingAt = time;
        }
    }

    private void nextPage(float time) {
        page = targetPage;
        pageStart = time;
        pageEndingAt = -1;
        completedAt = NOT_COMPLETED;
        soundedGlyphs = 0;
        pageActionsSent = false;
        layoutPage();
    }

    private void startOutro(float time) {
        if (outroAt >= 0) return;
        outroAt = time;
        sendActions(definition.onFinish());
    }

    private void sendActions(List<ResourceLocation> actions) {
        for (ResourceLocation action : actions) {
            PhoenixNetwork.CHANNEL.sendToServer(new C2SCutsceneActionPacket(id, action));
        }
    }

    private void close() {
        if (closed) return;
        closed = true;
        onClose();
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float time = now();
        if (choicesVisible(time)) {
            for (ChoiceBox box : choiceBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    select(box.index());
                    break;
                }
            }
            return true;
        }
        advance(time);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            select(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (definition.skippable()) startOutro(now());
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
        float pageAlpha = pageEndingAt < 0 ? 1 : 1 - clamp01((time - pageEndingAt) / PAGE_FADE_OUT);

        definition.background().render(graphics,
                new CutsceneBackground.BackgroundContext(width, height, time, global, mouseX, mouseY));
        if (black > 0) graphics.fill(0, 0, width, height, argb(black * global, 0x000000));
        renderImages(graphics, time, pageAlpha * global);

        // Items and entities draw with depth, so keep text in front of them.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, TEXT_Z);
        renderText(graphics, time, pageAlpha, global);
        renderChoices(graphics, time, mouseX, mouseY, global);
        renderHints(graphics, time, global);
        graphics.pose().popPose();
        updateMusic(time, global);
    }

    private void renderImages(GuiGraphics graphics, float time, float alpha) {
        float pageTime = time - pageStart;
        if (pageTime < 0 || alpha <= 0.01f) return;
        for (CutsceneImage image : currentPage().images()) {
            CutscenePage.Animation animation = image.animation();
            float t = pageTime - animation.delay();
            if (t < 0) continue;
            float appear = smooth(clamp01(t / Math.max(0.01f, animation.fadeIn())));
            float a = appear * alpha;
            if (a <= 0.01f) continue;
            float cx = image.x() * width + (1 - appear) * animation.slideX();
            float cy = image.y() * height + (1 - appear) * animation.slideY() + Mth.sin(time * 2f) * animation.bob();

            if (image.texture().isPresent()) {
                renderTexture(graphics, image, cx, cy, a);
            } else if (image.item().isPresent()) {
                ItemStack stack = items.computeIfAbsent(image,
                        img -> new ItemStack(BuiltInRegistries.ITEM.get(img.item().get())));
                float scale = image.scaleOr(4f);
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(cx, cy, 0);
                pose.scale(scale, scale, 1);
                graphics.setColor(a, a, a, 1f);
                graphics.renderItem(stack, -8, -8);
                graphics.setColor(1f, 1f, 1f, 1f);
                pose.popPose();
            } else {
                entityFor(image).ifPresent(entity -> renderEntity(graphics, image, entity, cx, cy, time, a));
            }
        }
    }

    private void renderTexture(GuiGraphics graphics, CutsceneImage image, float cx, float cy, float alpha) {
        int tint = image.tint();
        int w = image.width(), h = image.height();
        int x0 = Math.round(cx - w / 2f), y0 = Math.round(cy - h / 2f);
        RenderSystem.enableBlend();
        graphics.setColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f,
                ((tint >>> 24) / 255f) * alpha);
        ResourceLocation texture = image.texture().get();
        if (image.region().isPresent()) {
            CutscenePage.Region r = image.region().get();
            graphics.blit(texture, x0, y0, w, h, r.u(), r.v(), r.width(), r.height(), r.textureWidth(),
                    r.textureHeight());
        } else {
            graphics.blit(texture, x0, y0, w, h, 0f, 0f, 1, 1, 1, 1);
        }
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private Optional<LivingEntity> entityFor(CutsceneImage image) {
        if (minecraft == null || minecraft.level == null) return Optional.empty();
        ResourceLocation type = image.entity().get();
        if (type.equals(PLAYER)) return Optional.ofNullable(minecraft.player);
        return entities.computeIfAbsent(image, img -> {
            Optional<LivingEntity> entity = EntityType.byString(type.toString())
                    .map(t -> t.create(minecraft.level))
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast);
            if (entity.isEmpty()) PhoenixCore.LOGGER.warn("Cutscene {} can't show entity {}", id, type);
            return entity;
        });
    }

    /** Draws an entity standing at (x, y), turned to a fixed angle, faded up from darkness. */
    private void renderEntity(GuiGraphics graphics, CutsceneImage image, LivingEntity entity, float x, float y,
                              float time, float alpha) {
        float angle = 180f + image.yaw() + time * image.rotation();
        float bodyRot = entity.yBodyRot, bodyRotO = entity.yBodyRotO, yRot = entity.getYRot(),
                xRot = entity.getXRot(), headRot = entity.yHeadRot, headRotO = entity.yHeadRotO;
        entity.yBodyRot = entity.yBodyRotO = angle;
        entity.setYRot(angle);
        entity.setXRot(0);
        entity.yHeadRot = entity.yHeadRotO = angle;

        graphics.setColor(alpha, alpha, alpha, 1f);
        InventoryScreen.renderEntityInInventory(graphics, Math.round(x), Math.round(y),
                Math.round(image.scaleOr(50f)), new Quaternionf().rotateZ((float) Math.PI), null, entity);
        graphics.setColor(1f, 1f, 1f, 1f);

        // The player's own entity is shared with the world, so put everything back.
        entity.yBodyRot = bodyRot;
        entity.yBodyRotO = bodyRotO;
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yHeadRot = headRot;
        entity.yHeadRotO = headRotO;
    }

    private void renderText(GuiGraphics graphics, float time, float pageAlpha, float global) {
        float pageTime = time - pageStart;
        if (pageTime < 0) return;

        TextSettings text = definition.text();
        float lineHeight = lineHeight();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(width / 2f, textTop(), 0);
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

    private void renderChoices(GuiGraphics graphics, float time, int mouseX, int mouseY, float global) {
        choiceBoxes.clear();
        if (!choicesVisible(time)) return;
        float alpha = smooth(clamp01((time - pageStart - pageRevealEnd() - CHOICE_DELAY) / CHOICE_FADE_IN)) * global;
        if (alpha < 0.05f) return;

        List<Choice> choices = currentPage().choices();
        float rowHeight = (font.lineHeight + 7) * CHOICE_SCALE;
        float y = textBottom() + 18;
        PoseStack pose = graphics.pose();
        for (int i = 0; i < choices.size(); i++) {
            Component label = Component.literal((i + 1) + ".  ").append(choices.get(i).text());
            float w = font.width(label) * CHOICE_SCALE;
            float x0 = width / 2f - w / 2f, y0 = y + i * rowHeight;
            ChoiceBox box = new ChoiceBox(i, x0 - 8, y0 - 4, x0 + w + 8, y0 + font.lineHeight * CHOICE_SCALE + 4);
            choiceBoxes.add(box);
            boolean hovered = box.contains(mouseX, mouseY);

            if (hovered) {
                graphics.fill(Math.round(box.x0()), Math.round(box.y0()), Math.round(box.x1()), Math.round(box.y1()),
                        argb(0.18f * alpha, 0xFFFFFF));
            }
            pose.pushPose();
            pose.translate(x0 + (hovered ? 2 : 0), y0, 0);
            pose.scale(CHOICE_SCALE, CHOICE_SCALE, 1);
            graphics.drawString(font, label, 0, 0, argb(alpha, hovered ? 0xFFD27F : 0xD8D8D8), true);
            pose.popPose();
        }
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
        boolean waiting = definition.autoAdvance() < 0 && currentPage().choices().isEmpty() && outroAt < 0 &&
                pageEndingAt < 0 && time >= pageStart && time - pageStart - pageRevealEnd() > 0.3f;
        if (waiting) {
            float pulse = 0.55f + 0.35f * Mth.sin(time * 4f);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(width / 2f, textBottom() + 8 + Mth.sin(time * 3f) * 2f, 0);
            Component arrow = Component.literal("▼");
            graphics.drawString(font, arrow, -font.width(arrow) / 2, 0, argb(pulse * global, 0xFFFFFF), false);
            pose.popPose();
        }
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
