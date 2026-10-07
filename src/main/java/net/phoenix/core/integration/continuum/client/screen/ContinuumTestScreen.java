package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.continuum.client.render.ContinuumShaders;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
import net.phoenix.core.integration.continuum.client.render.PlanetRenderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Milestone 1 test bench: one planet in a framed view, to judge the renderer before any game logic exists.
 * Drag to rotate, scroll to zoom, 1-5 to switch body, Q to change quality, Space to pause the spin.
 */
public class ContinuumTestScreen extends Screen {

    private static final int FRAME = 0xFF7a5cff;

    private RenderTarget target;
    private int targetWidth = -1;
    private int targetHeight = -1;

    private int presetIndex = 0;
    private PlanetRenderer.Quality quality = PlanetRenderer.Quality.MEDIUM;

    private float yaw = 0.0f;
    private float pitch = 12.0f;
    private float distance = 4.3f;
    private boolean spinning = true;

    private float surfaceSpin = 0.0f;
    private float cloudSpin = 0.0f;
    private long lastFrameMillis = Util.getMillis();

    public ContinuumTestScreen() {
        super(Component.literal("Continuum"));
    }

    private PlanetParams current() {
        return PlanetParams.PRESETS.get(presetIndex);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        long now = Util.getMillis();
        float dt = Math.min((now - lastFrameMillis) / 1000.0f, 0.1f);
        lastFrameMillis = now;

        PlanetParams params = current();
        if (spinning) {
            surfaceSpin += params.spinDegPerSec() * dt;
            cloudSpin += params.cloudSpinDegPerSec() * dt;
        }

        // the scene fills the whole window: the planet sits in the middle and the backdrop reaches every edge
        Minecraft mc = Minecraft.getInstance();
        ensureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());

        boolean drawn = target != null && PlanetRenderer.render(target, params,
                new PlanetRenderer.View(yaw, pitch, distance, surfaceSpin, cloudSpin), quality);

        if (drawn) {
            blit(graphics);
        } else {
            graphics.fill(0, 0, width, height, 0xFF05060f);
            String message = ContinuumShaders.ready() ? "Render target unavailable" : "Continuum shaders not loaded";
            graphics.drawCenteredString(font, message, width / 2, height / 2, 0xFFff6b6b);
        }

        graphics.renderOutline(1, 1, width - 2, height - 2, FRAME);

        graphics.drawCenteredString(font, Component.literal("CONTINUUM  -  renderer prototype"), width / 2, 12,
                0xFFd9ccff);
        graphics.drawCenteredString(font, Component.literal(params.name() + "  -  " + params.description()),
                width / 2, 22, 0xFFa8a0c8);

        String hint = "[1-" + PlanetParams.PRESETS.size() + "] body   [Q] quality: " + quality.name().toLowerCase() +
                " (" + PlanetRenderer.mesh(quality).triangleCount() + " tris)   [Space] " +
                (spinning ? "pause" : "resume") + " spin   drag: rotate   scroll: zoom";
        graphics.drawCenteredString(font, hint, width / 2, height - 14, 0xFF8a84a8);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** The offscreen target matches the window in real pixels, so the planet stays sharp at any GUI scale. */
    private void ensureTarget(int pixelsWide, int pixelsHigh) {
        pixelsWide = Math.max(64, pixelsWide);
        pixelsHigh = Math.max(64, pixelsHigh);
        if (target != null && targetWidth == pixelsWide && targetHeight == pixelsHigh) return;

        if (target != null) target.destroyBuffers();
        target = new TextureTarget(pixelsWide, pixelsHigh, true, Minecraft.ON_OSX);
        targetWidth = pixelsWide;
        targetHeight = pixelsHigh;
    }

    private void blit(GuiGraphics graphics) {
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // a framebuffer texture is upside down relative to the GUI
        bb.vertex(pose, 0, height, 0).uv(0, 0).endVertex();
        bb.vertex(pose, width, height, 0).uv(1, 0).endVertex();
        bb.vertex(pose, width, 0, 0).uv(1, 1).endVertex();
        bb.vertex(pose, 0, 0, 0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableBlend();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            yaw += (float) dragX * 0.5f;
            pitch = Math.max(-80.0f, Math.min(80.0f, pitch + (float) dragY * 0.4f));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        distance = Math.max(2.4f, Math.min(9.0f, distance - (float) delta * 0.3f));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int index = keyCode - InputConstants.KEY_1;
        List<PlanetParams> presets = PlanetParams.PRESETS;
        if (index >= 0 && index < presets.size()) {
            presetIndex = index;
            surfaceSpin = 0.0f;
            cloudSpin = 0.0f;
            return true;
        }
        if (keyCode == InputConstants.KEY_Q) {
            quality = quality.next();
            return true;
        }
        if (keyCode == InputConstants.KEY_SPACE) {
            spinning = !spinning;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
            targetWidth = -1;
            targetHeight = -1;
        }
        super.removed();
    }
}
