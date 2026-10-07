package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.render.ContinuumShaders;
import net.phoenix.core.integration.continuum.client.render.GlowRenderer;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
import net.phoenix.core.integration.continuum.client.render.PlanetRenderer;
import net.phoenix.core.integration.continuum.client.render.SceneRenderer;
import net.phoenix.core.integration.continuum.client.render.SceneTarget;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;

/**
 * The launch sequence: the home world falling away as the rocket climbs out of the atmosphere. It is purely
 * cinematic - the server accepted the mission before this opens - and ends in the transit view of that mission (or the
 * map, if the mission has not arrived in the client's state yet). Esc skips it.
 */
public class ContinuumAscentScreen extends Screen {

    private static final float SECONDS = 7.0f;
    private static final ResourceLocation HOME_WORLD = new ResourceLocation("phoenixcore", "anvil");
    private static final String[] STAGES = { "Lift-off", "Max-Q", "Stage separation", "Orbit insertion" };

    private final ResourceLocation destination;
    private final @Nullable BlockPos pad;
    private final SceneTarget scene = new SceneTarget();

    private float t;
    private float spin;
    private long lastFrame = Util.getMillis();
    private boolean finished;

    public ContinuumAscentScreen(ResourceLocation destination, @Nullable BlockPos pad) {
        super(Component.literal("Ascent"));
        this.destination = destination;
        this.pad = pad;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static float smooth(float x) {
        x = Math.max(0.0f, Math.min(1.0f, x));
        return x * x * (3.0f - 2.0f * x);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float dt = Math.min((now - lastFrame) / 1000.0f, 0.1f);
        lastFrame = now;

        t += dt / SECONDS;
        spin += dt * 5.0f;
        if (t >= 1.0f) {
            finish();
            return;
        }

        float eased = smooth(t);
        float distance = 1.28f + (13.0f - 1.28f) * (float) Math.pow(eased, 1.15);
        float pitch = 62.0f + (10.0f - 62.0f) * eased;

        RenderTarget target = scene.ensure();
        if (target != null && ContinuumShaders.ready()) {
            ContinuumBody home = ContinuumData.body(HOME_WORLD);
            PlanetParams params = home != null ? home.params() : PlanetParams.ANVIL;

            Matrix4f projection = SceneRenderer.projection(target.width, target.height, 40.0f);
            Matrix4f view = new Matrix4f().translate(0.0f, 0.0f, -distance).rotateX((float) Math.toRadians(pitch));

            SceneRenderer.begin(target, spin);
            SceneRenderer.drawPlanet(params, view, projection, new Vector3f(), 1.0f, new Vector3f(-40.0f, 18.0f, 30.0f),
                    spin, spin * 1.4f, PlanetRenderer.Quality.HIGH, true);
            SceneRenderer.end();
            scene.blit(graphics, width, height);
        } else {
            graphics.fill(0, 0, width, height, 0xFF05060f);
        }

        // engine plume: strongest at lift-off, gone by the time the atmosphere is below
        float plume = Math.max(0.0f, 1.0f - t * 2.4f);
        if (plume > 0.0f) {
            float flicker = 0.85f + 0.15f * (float) Math.sin(now * 0.04);
            GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
            for (int i = 0; i < 6; i++) {
                float y = height * 0.9f + i * 26.0f * (1.0f - t);
                glows.glow(width / 2.0f, y, 46.0f + i * 10.0f, i == 0 ? 0xfff0c0 : 0xff9a40,
                        plume * flicker * (0.9f - i * 0.12f));
            }
            glows.draw();
        }

        drawReadout(graphics, distance);

        // fade in from black at the start and out at the end
        float fade = Math.max(Math.max(0.0f, 1.0f - t * 10.0f), Math.max(0.0f, (t - 0.93f) * 14.0f));
        if (fade > 0.001f) graphics.fill(0, 0, width, height, (int) (Math.min(fade, 1.0f) * 255.0f) << 24);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawReadout(GuiGraphics graphics, float distance) {
        ContinuumBody body = ContinuumData.body(destination);
        String target = body != null ? body.name() : destination.getPath();

        graphics.drawString(font, "ASCENT", 14, 12, MapUi.TITLE);
        graphics.drawString(font, "Bound for " + target, 14, 24, MapUi.DIM);

        int stage = Math.min(STAGES.length - 1, (int) (t * STAGES.length));
        graphics.drawString(font, STAGES[stage], 14, 40, MapUi.WARN);

        double altitudeKm = (distance - 1.0) * 1200.0;
        graphics.drawString(font, String.format(Locale.ROOT, "ALT  %,d km", Math.round(altitudeKm)), 14, 56, MapUi.TEXT);
        graphics.drawString(font, "T+ " + MapUi.duration((long) (t * SECONDS * 1000.0f)), 14, 67, MapUi.TEXT);

        MapUi.bar(graphics, width / 2 - 100, height - 22, 200, 6, t, MapUi.FRAME);
        graphics.drawCenteredString(font, "Esc to skip", width / 2, height - 12, 0xFF6a6488);
    }

    private void finish() {
        if (finished) return;
        finished = true;

        // the newest mission to this destination is the one that was just launched
        ContinuumStateSnapshot.MissionView newest = null;
        for (ContinuumStateSnapshot.MissionView mission : ContinuumClientState.missions()) {
            if (!mission.destination().equals(destination)) continue;
            if (newest == null || mission.startMillis() > newest.startMillis()) newest = mission;
        }

        Minecraft mc = Minecraft.getInstance();
        if (newest != null) mc.setScreen(new ContinuumTransitScreen(newest.id(), pad));
        else mc.setScreen(new ContinuumMapScreen(pad));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_SPACE) {
            finish();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        scene.close();
        super.removed();
    }
}
