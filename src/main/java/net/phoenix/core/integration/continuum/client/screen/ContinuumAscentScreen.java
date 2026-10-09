package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.client.FlightProfiles;
import net.phoenix.core.integration.continuum.client.render.ContinuumShaders;
import net.phoenix.core.integration.continuum.client.render.GlowRenderer;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
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

public class ContinuumAscentScreen extends Screen {

    private static final ResourceLocation HOME_WORLD = new ResourceLocation("phoenixcore", "anvil");

    private final ResourceLocation destination;
    private final @Nullable BlockPos pad;
    private final SceneTarget scene = new SceneTarget();

    private float t;
    private float spin;
    private long lastFrame = Util.getMillis();
    private boolean finished;
    private boolean separated;
    private final FlightProfiles.Ascent profile;

    public ContinuumAscentScreen(ResourceLocation destination, @Nullable BlockPos pad) {
        super(Component.literal("Ascent"));
        this.destination = destination;
        this.pad = pad;
        this.profile = FlightProfiles.Ascent.of(ContinuumData.body(destination));
    }

    private ContinuumSounds.Loop rumble;

    @Override
    protected void init() {
        super.init();
        if (rumble == null) {
            rumble = ContinuumSounds.engineLoop();
            ContinuumSounds.liftoff();
        }
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

        t += dt / profile.seconds;
        spin += dt * 5.0f;
        if (t >= 1.0f) {
            finish();
            return;
        }

        float eased = smooth(t);
        float distance = 1.28f + (profile.endDistance - 1.28f) * (float) Math.pow(eased, 1.15);
        float pitch = 62.0f + (profile.endPitch - 62.0f) * eased;

        RenderTarget target = scene.ensure();
        if (target != null && ContinuumShaders.ready()) {
            ContinuumBody home = ContinuumData.body(HOME_WORLD);
            PlanetParams params = home != null ? home.params() : PlanetParams.ANVIL;

            Matrix4f projection = SceneRenderer.projection(target.width, target.height, 40.0f);
            Matrix4f view = new Matrix4f().translate(0.0f, 0.0f, -distance).rotateX((float) Math.toRadians(pitch));

            SceneRenderer.begin(target, spin);
            SceneRenderer.drawPlanet(params, view, projection, new Vector3f(), 1.0f, new Vector3f(-40.0f, 18.0f, 30.0f),
                    spin, spin * 1.4f, net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality(), true);
            SceneRenderer.end();
            scene.blit(graphics, width, height);
        } else {
            graphics.fill(0, 0, width, height, 0xFF05060f);
        }

        drawRocket(graphics, now);

        if (!separated && t >= 0.5f) {
            separated = true;
            ContinuumSounds.stageSeparation();
        }
        float flash = Math.max(0.0f, 1.0f - Math.abs(t - 0.5f) * 22.0f);
        if (flash > 0.0f) graphics.fill(0, 0, width, height, ((int) (flash * 0.28f * 255.0f) << 24) | 0xffffff);

        if (profile.tintAlpha > 0.0f) {
            int alpha = (int) (profile.tintAlpha * smooth((t - 0.25f) / 0.6f) * 255.0f);
            graphics.fill(0, 0, width, height, (alpha << 24) | profile.tint);
        }
        if (profile.vignette) {
            int edge = ((int) (0xB0 * smooth((t - 0.2f) / 0.6f))) << 24;
            graphics.fillGradient(0, 0, width, height / 3, edge, 0);
            graphics.fillGradient(0, height * 2 / 3, width, height, 0, edge);
        }
        if (rumble != null) rumble.setTarget(0.2f + 0.8f * Math.max(0.0f, 1.0f - t * 1.5f));

        drawReadout(graphics, distance);

        float fade = Math.max(Math.max(0.0f, 1.0f - t * 10.0f), Math.max(0.0f, (t - 0.93f) * 14.0f));
        if (fade > 0.001f) graphics.fill(0, 0, width, height, (int) (Math.min(fade, 1.0f) * 255.0f) << 24);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRocket(GuiGraphics graphics, long now) {
        float lift = smooth(t / 0.45f);
        float after = Math.max(0.0f, (t - 0.45f) / 0.5f);
        float scale = 1.0f - 0.65f * after;
        float alpha = 1.0f - smooth((t - 0.68f) / 0.24f);
        if (alpha <= 0.01f) return;

        float rocketHeight = height * 0.30f * scale;
        float tailY = height * (0.90f - 0.34f * lift - 0.18f * after);
        float shake = (1.0f - lift * 0.8f) * Math.max(0.0f, 1.0f - t * 2.0f) * 2.2f;
        float cx = width / 2.0f + (float) Math.sin(now * 0.09) * shake;
        tailY += (float) Math.sin(now * 0.13) * shake * 0.6f;

        float burn = Math.max(0.0f, 1.0f - t * 1.5f) * alpha;
        if (burn > 0.0f) {
            float flicker = 0.85f + 0.15f * (float) Math.sin(now * 0.045);
            GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
            for (int i = 0; i < 6; i++) {
                float y = tailY + (6.0f + i * 22.0f * (1.0f - t * 0.6f)) * scale;
                glows.glow(cx, y, (34.0f + i * 9.0f) * scale, i == 0 ? profile.plumeCore : profile.plumeOuter,
                        burn * flicker * (0.95f - i * 0.12f));
            }
            glows.draw();
        }

        int top = Math.round(tailY - rocketHeight);
        int rows = Math.max(2, Math.round(rocketHeight));
        float bodyHalf = rocketHeight * 0.085f;
        int a = (int) (alpha * 255.0f) << 24;

        for (int i = 0; i < rows; i++) {
            float u = i / (float) rows;

            float half = u < 0.26f ? bodyHalf * (float) Math.sin(Math.PI / 2.0 * (u / 0.26f)) : bodyHalf;
            boolean fin = u > 0.80f;
            float outer = fin ? half + bodyHalf * 0.95f * ((u - 0.80f) / 0.20f) : half;

            int y = top + i;
            int body = Math.max(1, Math.round(half));
            int finW = Math.max(body, Math.round(outer));

            int hull = u < 0.20f ? 0xd8362f : (u > 0.58f && u < 0.64f ? 0x3a3f55 : 0xe8ecf4);
            int shade = darken(hull, 0.62f);
            int light = lighten(hull, 1.18f);

            if (fin && finW > body) {
                graphics.fill((int) cx - finW, y, (int) cx - body, y + 1, a | 0xb02a2a);
                graphics.fill((int) cx + body, y, (int) cx + finW, y + 1, a | 0x7a1c1c);
            }
            graphics.fill((int) cx - body, y, (int) cx, y + 1, a | hull);
            graphics.fill((int) cx, y, (int) cx + body, y + 1, a | shade);
            graphics.fill((int) cx - body + Math.max(1, body / 4), y, (int) cx - body + Math.max(2, body / 2), y + 1,
                    a | light);
        }

        int wy = top + Math.round(rocketHeight * 0.38f);
        int wr = Math.max(2, Math.round(bodyHalf * 0.42f));
        graphics.fill((int) cx - wr - 1, wy - wr - 1, (int) cx + wr + 1, wy + wr + 1, a | 0x2a3150);
        graphics.fill((int) cx - wr, wy - wr, (int) cx + wr, wy + wr, a | 0x6cc4ff);

        int nozzle = Math.max(2, Math.round(bodyHalf * 0.55f));
        graphics.fill((int) cx - nozzle, top + rows, (int) cx + nozzle, top + rows + Math.max(2, nozzle / 2),
                a | 0x2a2d3a);
    }

    private static int darken(int rgb, float f) {
        return scale(rgb, f);
    }

    private static int lighten(int rgb, float f) {
        return scale(rgb, f);
    }

    private static int scale(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }

    private void drawReadout(GuiGraphics graphics, float distance) {
        ContinuumBody body = ContinuumData.body(destination);
        String target = body != null ? body.name() : destination.getPath();

        graphics.drawString(font, "ASCENT", 14, 12, MapUi.TITLE);
        graphics.drawString(font, "Bound for " + target, 14, 24, MapUi.DIM);

        int stage = Math.min(profile.stages.length - 1, (int) (t * profile.stages.length));
        graphics.drawString(font, profile.stages[stage], 14, 40, MapUi.WARN);

        double altitudeKm = (distance - 1.0) * 1200.0;
        graphics.drawString(font, String.format(Locale.ROOT, "ALT  %,d km", Math.round(altitudeKm)), 14, 56,
                MapUi.TEXT);
        graphics.drawString(font, "T+ " + MapUi.duration((long) (t * profile.seconds * 1000.0f)), 14, 67, MapUi.TEXT);

        MapUi.bar(graphics, width / 2 - 100, height - 22, 200, 6, t, MapUi.FRAME);
        graphics.drawCenteredString(font, "Esc to skip", width / 2, height - 12, 0xFF6a6488);
    }

    private void finish() {
        if (finished) return;
        finished = true;

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
        if (rumble != null) rumble.fadeOut();
        scene.close();
        super.removed();
    }
}
