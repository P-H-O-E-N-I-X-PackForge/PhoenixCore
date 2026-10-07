package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.render.ContinuumShaders;
import net.phoenix.core.integration.continuum.client.render.GlowRenderer;
import net.phoenix.core.integration.continuum.client.render.MapCamera;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
import net.phoenix.core.integration.continuum.client.render.PlanetRenderer;
import net.phoenix.core.integration.continuum.client.render.SceneRenderer;
import net.phoenix.core.integration.continuum.common.ContinuumMissions;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;
import net.phoenix.core.integration.continuum.item.ContinuumProbeItem;
import net.phoenix.core.integration.continuum.item.ContinuumRocketItem;
import net.phoenix.core.integration.continuum.network.C2SCollectMissionPacket;
import net.phoenix.core.integration.continuum.network.C2SLaunchMissionPacket;
import net.phoenix.core.integration.continuum.network.C2SRequestStatePacket;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Continuum map: a galaxy of systems, a system of bodies on orbits, and a single body up close, with eased
 * zooms between them. Everything 3D is drawn by {@link SceneRenderer} into one screen-sized offscreen target and
 * blitted; labels, glows, orbit rings and panels are GUI overlays projected from the same camera.
 *
 * <p>
 * Stages and missions come from the server's team state. Opened from a Launch Pad it can also launch missions; opened
 * with {@code /continuum map} it is view-only. Left-drag rotates, scroll zooms, click goes deeper, right-click /
 * Backspace / Esc goes back up, Q changes quality.
 */
public class ContinuumMapScreen extends Screen {

    private enum Level {
        GALAXY,
        SYSTEM,
        BODY
    }

    private enum Phase {
        NONE,
        OUT,
        IN
    }

    private record Pick(ContinuumSystem system, ContinuumBody body, float x, float y, float radius) {}

    private static final float GALAXY_SCALE = 1.6f;
    private static final float GALAXY_DISTANCE = 17.0f;
    private static final float SYSTEM_DISTANCE = 15.0f;
    private static final float BODY_DISTANCE = 4.3f;
    private static final float FOV = 40.0f;
    private static final float PHASE_SECONDS = 0.28f;
    private static final float DAYS_PER_SECOND = 4.0f;

    private static final int FRAME = 0xFF7a5cff;
    private static final int PANEL_BG = 0xD00b0a18;
    private static final int PANEL_LINE = 0xFF3a2f7a;
    private static final int TITLE = 0xFFE8D8FF;
    private static final int TEXT = 0xFFB8B0D8;
    private static final int DIM = 0xFF7a7498;

    private final MapCamera galaxyCam = new MapCamera(15, 24, GALAXY_DISTANCE, 7, 36);
    private final MapCamera systemCam = new MapCamera(20, 38, SYSTEM_DISTANCE, 6, 30);
    private final MapCamera bodyCam = new MapCamera(0, 10, BODY_DISTANCE, 2.6f, 9);

    private Level level = Level.GALAXY;
    private @Nullable ContinuumSystem system;
    private @Nullable ContinuumBody body;

    private RenderTarget scene;
    private int sceneWidth = -1;
    private int sceneHeight = -1;
    private PlanetRenderer.Quality quality = PlanetRenderer.Quality.MEDIUM;

    private Phase phase = Phase.NONE;
    private float phaseTime;
    private float fade;
    private int zoomDirection;
    private @Nullable Runnable onSwitch;

    private final List<Pick> picks = new ArrayList<>();
    private final Map<ResourceLocation, Vector3f> bodyPositions = new HashMap<>();
    private @Nullable Pick hovered;

    private boolean pressed;
    private double pressX;
    private double pressY;
    private double dragged;

    private long lastFrame = Util.getMillis();
    private float animDays;
    private float clock;
    private String flash = "";
    private float flashTime;

    /** The launch pad this map was opened from, or null when it is view-only. */
    private final @Nullable BlockPos pad;

    private int[] planRect;
    private final MissionPlannerPanel planner = new MissionPlannerPanel();
    private final List<Object[]> missionRows = new ArrayList<>();

    public ContinuumMapScreen() {
        this(null);
    }

    public ContinuumMapScreen(@Nullable BlockPos pad) {
        super(Component.literal("Continuum"));
        this.pad = pad;
    }

    @Override
    protected void init() {
        super.init();
        // the team's missions and discoveries may have moved on since this client last heard
        PhoenixNetwork.CHANNEL.sendToServer(new C2SRequestStatePacket());
    }

    /** Shows a short message near the bottom of the screen. */
    public void flashMessage(String message) {
        flash(message);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private MapCamera cam() {
        return switch (level) {
            case GALAXY -> galaxyCam;
            case SYSTEM -> systemCam;
            case BODY -> bodyCam;
        };
    }

    // ------------------------------------------------------------------ frame

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float dt = Math.min((now - lastFrame) / 1000.0f, 0.1f);
        lastFrame = now;
        clock += dt;
        animDays += dt * DAYS_PER_SECOND;
        flashTime = Math.max(0.0f, flashTime - dt);

        updateTransition(dt);
        galaxyCam.update(dt);
        systemCam.update(dt);
        bodyCam.update(dt);

        ensureScene();
        boolean sceneReady = scene != null && ContinuumShaders.ready();

        picks.clear();
        bodyPositions.clear();
        if (system != null) layoutSystem(system);

        if (sceneReady) {
            drawScene();
            blitScene(graphics);
        } else {
            graphics.fill(0, 0, width, height, 0xFF05060f);
            String message = ContinuumShaders.ready() ? "Render target unavailable" : "Continuum shaders not loaded";
            graphics.drawCenteredString(font, message, width / 2, height / 2, 0xFFff6b6b);
        }

        if (ContinuumData.systems().isEmpty()) {
            graphics.drawCenteredString(font, "No Continuum data loaded", width / 2, height / 2 + 14, 0xFFff6b6b);
        } else {
            switch (level) {
                case GALAXY -> overlayGalaxy(graphics, mouseX, mouseY);
                case SYSTEM -> overlaySystem(graphics, mouseX, mouseY);
                case BODY -> overlayBody(graphics, mouseX, mouseY);
            }
        }

        drawMissions(graphics, mouseX, mouseY);
        drawChrome(graphics);

        if (planner.isOpen() && body != null) {
            planner.draw(graphics, font, width, height, mouseX, mouseY, body,
                    ContinuumClientState.stage(body.id()), findRocket(), countProbes(),
                    ContinuumClientState.outpost(body.id()), ContinuumClientState.now());
        }

        if (fade > 0.001f) {
            graphics.fill(0, 0, width, height, ((int) (Math.min(fade, 1.0f) * 255.0f) << 24));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void ensureScene() {
        Minecraft mc = Minecraft.getInstance();
        int w = Math.max(64, mc.getWindow().getWidth());
        int h = Math.max(64, mc.getWindow().getHeight());
        if (scene != null && sceneWidth == w && sceneHeight == h) return;

        if (scene != null) scene.destroyBuffers();
        scene = new TextureTarget(w, h, true, Minecraft.ON_OSX);
        sceneWidth = w;
        sceneHeight = h;
    }

    // ------------------------------------------------------------------ 3D

    private void drawScene() {
        Matrix4f projection = SceneRenderer.projection(sceneWidth, sceneHeight, FOV);
        Matrix4f view = cam().view();
        SceneRenderer.begin(scene, cam().yaw);

        switch (level) {
            case GALAXY -> {
                // systems are drawn as glows in the overlay; the 3D pass is just the backdrop
            }
            case SYSTEM -> {
                if (system != null) drawSystemScene(system, view, projection);
            }
            case BODY -> {
                if (body != null) drawBodyScene(body, view, projection);
            }
        }

        SceneRenderer.end();
    }

    private void drawSystemScene(ContinuumSystem sys, Matrix4f view, Matrix4f projection) {
        Vector3f sun = new Vector3f();
        ContinuumBody central = centralBody(sys);
        if (central == null) {
            SceneRenderer.drawStar(sys.starColor(), false, view, projection, sun, sys.starRadius(), quality);
        } else {
            SceneRenderer.drawStar(0, true, view, projection, sun, sys.starRadius(), quality);
        }

        for (ContinuumBody b : ContinuumData.bodiesOf(sys.id())) {
            if (b.isCentral()) continue;
            DiscoveryStage stage = ContinuumClientState.stage(b.id());
            Vector3f pos = bodyPositions.get(b.id());
            if (stage == DiscoveryStage.UNKNOWN || pos == null) continue;

            PlanetParams params = stage == DiscoveryStage.SURVEYED ? b.params() : b.params().ghost();
            PlanetRenderer.Quality q = b.isMoon() ? PlanetRenderer.Quality.LOW : quality;
            SceneRenderer.drawPlanet(params, view, projection, pos, visualRadius(b), sun,
                    clock * params.spinDegPerSec(), clock * params.cloudSpinDegPerSec(), q,
                    stage == DiscoveryStage.SURVEYED);
        }
    }

    private void drawBodyScene(ContinuumBody b, Matrix4f view, Matrix4f projection) {
        Vector3f sun = new Vector3f(-60.0f, 25.0f, 40.0f);
        DiscoveryStage stage = ContinuumClientState.stage(b.id());

        if (b.type() == ContinuumBody.Type.BLACK_HOLE) {
            SceneRenderer.drawStar(0, true, view, projection, new Vector3f(), 1.2f, quality);
        } else {
            PlanetParams params = stage == DiscoveryStage.SURVEYED ? b.params() : b.params().ghost();
            SceneRenderer.drawPlanet(params, view, projection, new Vector3f(), 1.0f, sun,
                    clock * params.spinDegPerSec(), clock * params.cloudSpinDegPerSec(), quality,
                    stage == DiscoveryStage.SURVEYED);
        }

        List<ContinuumBody> moons = ContinuumData.moonsOf(b.id());
        for (int i = 0; i < moons.size(); i++) {
            ContinuumBody moon = moons.get(i);
            DiscoveryStage moonStage = ContinuumClientState.stage(moon.id());
            if (moonStage == DiscoveryStage.UNKNOWN) continue;

            Vector3f pos = moonOffset(moon, i);
            PlanetParams params = moonStage == DiscoveryStage.SURVEYED ? moon.params() : moon.params().ghost();
            SceneRenderer.drawPlanet(params, view, projection, pos, 0.14f + 0.12f * (float) Math.sqrt(moon.radius()),
                    sun, clock * params.spinDegPerSec(), 0.0f, PlanetRenderer.Quality.LOW,
                    moonStage == DiscoveryStage.SURVEYED);
        }
    }

    private Vector3f moonOffset(ContinuumBody moon, int index) {
        float angle = orbitAngle(moon);
        float distance = 2.3f + 0.9f * index;
        return new Vector3f((float) Math.cos(angle) * distance, 0.12f * (float) Math.sin(angle * 2.0f),
                (float) Math.sin(angle) * distance);
    }

    private void blitScene(GuiGraphics graphics) {
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, scene.getColorTextureId());

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

    // ------------------------------------------------------------------ layout

    private @Nullable ContinuumBody centralBody(ContinuumSystem sys) {
        for (ContinuumBody b : ContinuumData.bodiesOf(sys.id())) {
            if (b.isCentral()) return b;
        }
        return null;
    }

    private static float visualRadius(ContinuumBody b) {
        float r = 0.16f + 0.11f * (float) Math.sqrt(b.radius());
        return b.isMoon() ? r * 0.7f : r;
    }

    private static float orbitWorldRadius(float au) {
        return 2.6f + 3.0f * (float) Math.sqrt(au);
    }

    private float orbitAngle(ContinuumBody b) {
        return (float) (2.0 * Math.PI * (b.phase() + animDays / b.periodDays()));
    }

    /** Places every body of the system for this frame: planets on their rings, moons beside their parents. */
    private void layoutSystem(ContinuumSystem sys) {
        List<ContinuumBody> all = ContinuumData.bodiesOf(sys.id());
        for (ContinuumBody b : all) {
            if (b.isMoon()) continue;
            if (b.isCentral()) {
                bodyPositions.put(b.id(), new Vector3f());
                continue;
            }
            float angle = orbitAngle(b);
            float radius = orbitWorldRadius(b.orbitAu());
            bodyPositions.put(b.id(), new Vector3f((float) Math.cos(angle) * radius, 0.0f,
                    (float) Math.sin(angle) * radius));
        }
        for (ContinuumBody b : all) {
            if (!b.isMoon()) continue;
            Vector3f parent = bodyPositions.get(b.parent());
            ContinuumBody parentBody = ContinuumData.body(b.parent());
            if (parent == null || parentBody == null) continue;

            float angle = orbitAngle(b);
            float radius = visualRadius(parentBody) * 1.6f + 0.35f + 5.0f * (float) Math.sqrt(b.orbitAu());
            bodyPositions.put(b.id(), new Vector3f(parent).add((float) Math.cos(angle) * radius, 0.0f,
                    (float) Math.sin(angle) * radius));
        }
    }

    // ------------------------------------------------------------------ overlays

    private float focalPixels() {
        return (height * 0.5f) / (float) Math.tan(Math.toRadians(FOV * 0.5));
    }

    private Matrix4f viewProjection() {
        return new Matrix4f(SceneRenderer.projection(width, height, FOV)).mul(cam().view());
    }

    private void overlayGalaxy(GuiGraphics graphics, int mouseX, int mouseY) {
        Matrix4f vp = viewProjection();

        // faint reference rings on the galactic plane, so the camera has something to orient by
        GlowRenderer.Lines grid = GlowRenderer.lines(graphics);
        for (float ring : new float[] { 4.0f, 8.0f, 12.0f }) {
            float[] previous = null;
            for (int i = 0; i <= 72; i++) {
                double a = 2.0 * Math.PI * i / 72.0;
                float[] p = MapCamera.project(vp, new Vector3f((float) Math.cos(a) * ring * GALAXY_SCALE, 0.0f,
                        (float) Math.sin(a) * ring * GALAXY_SCALE), width, height);
                if (previous != null && p != null) grid.line(previous[0], previous[1], p[0], p[1], 0x6a5cd0, 0.14f);
                previous = p;
            }
        }
        grid.draw(1.0f);

        List<Object[]> labelled = new ArrayList<>();
        GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
        for (ContinuumSystem sys : ContinuumData.systems()) {
            float[] p = MapCamera.project(vp, new Vector3f(sys.galaxyX(), sys.galaxyY(), sys.galaxyZ())
                    .mul(GALAXY_SCALE), width, height);
            if (p == null) continue;

            DiscoveryStage stage = ContinuumClientState.stage(sys.id());
            float size = Math.max(0.6f, Math.min(1.6f, GALAXY_DISTANCE / p[2]));
            float flicker = 0.7f + 0.3f * (float) Math.sin(clock * 2.1 + sys.galaxyX() * 3.0);

            switch (stage) {
                case UNKNOWN -> glows.glow(p[0], p[1], 9 * size, 0x9db0d8, 0.24f * flicker);
                case DETECTED -> {
                    glows.glow(p[0], p[1], 26 * size, 0x8f9cff, 0.12f);
                    glows.glow(p[0], p[1], 15 * size, 0xcfd8ff, 0.62f);
                }
                case SURVEYED -> {
                    glows.glow(p[0], p[1], 52 * size, sys.starColor(), 0.22f);
                    glows.glow(p[0], p[1], 24 * size, sys.starColor(), 0.95f);
                }
            }
            picks.add(new Pick(sys, null, p[0], p[1], Math.max(14.0f, 16 * size)));
            if (stage != DiscoveryStage.UNKNOWN) labelled.add(new Object[] { sys, p, stage });
        }
        glows.draw();

        hovered = pickAt(mouseX, mouseY);

        GlowRenderer.Lines marks = GlowRenderer.lines(graphics);
        if (hovered != null) marks.brackets(hovered.x(), hovered.y(), hovered.radius() + 4, 6, 0xFFFFFF, 0.9f);
        marks.draw(1.5f);

        for (Object[] entry : labelled) {
            ContinuumSystem sys = (ContinuumSystem) entry[0];
            float[] p = (float[]) entry[1];
            DiscoveryStage stage = (DiscoveryStage) entry[2];
            graphics.drawCenteredString(font, sys.name(), (int) p[0], (int) p[1] + 20,
                    stage == DiscoveryStage.SURVEYED ? TITLE : DIM);
        }

        if (hovered != null) tooltipSystem(graphics, hovered.system(), mouseX, mouseY);
    }

    private void overlaySystem(GuiGraphics graphics, int mouseX, int mouseY) {
        if (system == null) return;
        Matrix4f vp = viewProjection();
        float focal = focalPixels();
        List<ContinuumBody> all = ContinuumData.bodiesOf(system.id());

        // orbit rings
        GlowRenderer.Lines rings = GlowRenderer.lines(graphics);
        for (ContinuumBody b : all) {
            if (b.isCentral()) continue;
            DiscoveryStage stage = ContinuumClientState.stage(b.id());
            float alpha = switch (stage) {
                case UNKNOWN -> 0.07f;
                case DETECTED -> 0.2f;
                case SURVEYED -> 0.32f;
            };

            Vector3f center = new Vector3f();
            float radius;
            if (b.isMoon()) {
                ContinuumBody parent = ContinuumData.body(b.parent());
                Vector3f parentPos = parent == null ? null : bodyPositions.get(parent.id());
                if (parentPos == null || stage == DiscoveryStage.UNKNOWN) continue;
                center = parentPos;
                radius = visualRadius(parent) * 1.6f + 0.35f + 5.0f * (float) Math.sqrt(b.orbitAu());
                alpha *= 0.7f;
            } else {
                radius = orbitWorldRadius(b.orbitAu());
            }

            float[] previous = null;
            for (int i = 0; i <= 96; i++) {
                double a = 2.0 * Math.PI * i / 96.0;
                float[] p = MapCamera.project(vp, new Vector3f(center).add((float) Math.cos(a) * radius, 0.0f,
                        (float) Math.sin(a) * radius), width, height);
                if (previous != null && p != null) rings.line(previous[0], previous[1], p[0], p[1], 0x8f86ff, alpha);
                previous = p;
            }
        }
        rings.draw(1.0f);

        // star glow and unknown blips
        GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
        float[] star = MapCamera.project(vp, new Vector3f(), width, height);
        if (star != null) {
            float starPx = system.starRadius() * focal / star[2];
            int color = system.isBlackHole() ? 0xff8a30 : system.starColor();
            glows.glow(star[0], star[1], starPx * 3.4f, color, system.isBlackHole() ? 0.5f : 0.55f);
            if (system.isBlackHole()) glows.glow(star[0], star[1], starPx * 1.55f, 0xffb060, 0.5f);

            ContinuumBody central = centralBody(system);
            if (central != null) picks.add(new Pick(system, central, star[0], star[1], Math.max(14.0f, starPx * 1.2f)));
        }

        List<Object[]> labelled = new ArrayList<>();
        for (ContinuumBody b : all) {
            if (b.isCentral()) continue;
            Vector3f pos = bodyPositions.get(b.id());
            if (pos == null) continue;
            float[] p = MapCamera.project(vp, pos, width, height);
            if (p == null) continue;

            DiscoveryStage stage = ContinuumClientState.stage(b.id());
            float px = visualRadius(b) * focal / p[2];
            if (stage == DiscoveryStage.UNKNOWN) {
                float flicker = 0.7f + 0.3f * (float) Math.sin(clock * 2.4 + b.phase() * 9.0);
                glows.glow(p[0], p[1], 8.0f, 0x9db0d8, 0.28f * flicker);
            } else {
                labelled.add(new Object[] { b, p, stage, px });
                var marker = ContinuumClientState.outpost(b.id());
                if (marker != null) {
                    boolean dark = marker.upkeepPerCycle() > 0 && !marker.powered();
                    glows.glow(p[0] + px + 6.0f, p[1] - px - 6.0f, 7.0f, dark ? 0xff5a5a : 0x6affd0, 0.9f);
                }
            }
            picks.add(new Pick(system, b, p[0], p[1], Math.max(11.0f, px + 3.0f)));
        }
        glows.draw();

        hovered = pickAt(mouseX, mouseY);

        GlowRenderer.Lines marks = GlowRenderer.lines(graphics);
        if (hovered != null) marks.brackets(hovered.x(), hovered.y(), hovered.radius() + 4, 5, 0xFFFFFF, 0.9f);
        marks.draw(1.5f);

        for (Object[] entry : labelled) {
            ContinuumBody b = (ContinuumBody) entry[0];
            float[] p = (float[]) entry[1];
            DiscoveryStage stage = (DiscoveryStage) entry[2];
            float px = (float) entry[3];
            var outpostView = ContinuumClientState.outpost(b.id());
            String tag = outpostView == null ? "" : outpostView.upkeepPerCycle() > 0 && !outpostView.powered() ?
                    "  [outpost - no power]" : "  [outpost]";
            graphics.drawCenteredString(font, b.name() + tag, (int) p[0], (int) (p[1] + px + 6),
                    stage == DiscoveryStage.SURVEYED ? TITLE : DIM);
        }

        if (hovered != null && hovered.body() != null) tooltipBody(graphics, hovered.body(), mouseX, mouseY);
    }

    private void overlayBody(GuiGraphics graphics, int mouseX, int mouseY) {
        if (body == null) return;
        Matrix4f vp = viewProjection();
        float focal = focalPixels();

        float[] center = MapCamera.project(vp, new Vector3f(), width, height);
        if (center != null && body.type() == ContinuumBody.Type.BLACK_HOLE) {
            float px = 1.2f * focal / center[2];
            GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
            glows.glow(center[0], center[1], px * 2.8f, 0xff8a30, 0.45f);
            glows.glow(center[0], center[1], px * 1.5f, 0xffb060, 0.5f);
            glows.draw();
        }

        List<Object[]> labelled = new ArrayList<>();
        List<ContinuumBody> moons = ContinuumData.moonsOf(body.id());
        for (int i = 0; i < moons.size(); i++) {
            ContinuumBody moon = moons.get(i);
            DiscoveryStage stage = ContinuumClientState.stage(moon.id());
            float[] p = MapCamera.project(vp, moonOffset(moon, i), width, height);
            if (p == null) continue;
            if (stage == DiscoveryStage.UNKNOWN) {
                GlowRenderer.batch(graphics).glow(p[0], p[1], 7.0f, 0x9db0d8, 0.25f).draw();
            } else {
                labelled.add(new Object[] { moon, p, stage });
            }
            picks.add(new Pick(system, moon, p[0], p[1], 12.0f));
        }

        hovered = pickAt(mouseX, mouseY);

        GlowRenderer.Lines marks = GlowRenderer.lines(graphics);
        if (hovered != null) marks.brackets(hovered.x(), hovered.y(), hovered.radius() + 4, 4, 0xFFFFFF, 0.9f);
        marks.draw(1.5f);

        for (Object[] entry : labelled) {
            ContinuumBody moon = (ContinuumBody) entry[0];
            float[] p = (float[]) entry[1];
            graphics.drawCenteredString(font, moon.name(), (int) p[0], (int) p[1] + 12,
                    entry[2] == DiscoveryStage.SURVEYED ? TITLE : DIM);
        }

        drawBodyPanel(graphics, body);
    }

    // ------------------------------------------------------------------ panels

    private void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BG);
        graphics.renderOutline(x, y, w, h, PANEL_LINE);
    }

    private int wrapped(GuiGraphics graphics, String text, int x, int y, int w, int color) {
        for (var line : font.split(Component.literal(text), w)) {
            graphics.drawString(font, line, x, y, color);
            y += 10;
        }
        return y;
    }

    private static String stageLabel(DiscoveryStage stage) {
        return switch (stage) {
            case UNKNOWN -> "Unknown signal";
            case DETECTED -> "Detected";
            case SURVEYED -> "Surveyed";
        };
    }

    private static int stageColor(DiscoveryStage stage) {
        return switch (stage) {
            case UNKNOWN -> 0xFF8896c0;
            case DETECTED -> 0xFFffd27a;
            case SURVEYED -> 0xFF7affb0;
        };
    }

    private void tooltipSystem(GuiGraphics graphics, ContinuumSystem sys, int mouseX, int mouseY) {
        DiscoveryStage stage = ContinuumClientState.stage(sys.id());
        int w = 170;
        int h = stage == DiscoveryStage.UNKNOWN ? 28 :
                28 + font.split(Component.literal(sys.description()), w - 12).size() * 10 + 6;
        int x = Math.min(mouseX + 14, width - w - 6);
        int y = Math.min(mouseY + 10, height - h - 6);

        panel(graphics, x, y, w, h);
        graphics.drawString(font, stage == DiscoveryStage.UNKNOWN ? "???" : sys.name(), x + 6, y + 6, TITLE);
        graphics.drawString(font, stageLabel(stage), x + 6, y + 17, stageColor(stage));
        if (stage != DiscoveryStage.UNKNOWN) wrapped(graphics, sys.description(), x + 6, y + 30, w - 12, TEXT);
    }

    private void tooltipBody(GuiGraphics graphics, ContinuumBody b, int mouseX, int mouseY) {
        DiscoveryStage stage = ContinuumClientState.stage(b.id());
        int w = 170;
        int descLines = stage == DiscoveryStage.SURVEYED ?
                font.split(Component.literal(b.description()), w - 12).size() : 0;
        int h = 30 + descLines * 10 + (descLines > 0 ? 4 : 0);
        int x = Math.min(mouseX + 14, width - w - 6);
        int y = Math.min(mouseY + 10, height - h - 6);

        panel(graphics, x, y, w, h);
        graphics.drawString(font, stage == DiscoveryStage.UNKNOWN ? "???" : b.name(), x + 6, y + 6, TITLE);
        String sub = stage == DiscoveryStage.UNKNOWN ? stageLabel(stage) :
                b.type().label() + "  -  " + stageLabel(stage);
        graphics.drawString(font, sub, x + 6, y + 17, stageColor(stage));
        if (descLines > 0) wrapped(graphics, b.description(), x + 6, y + 30, w - 12, TEXT);
    }

    private void drawBodyPanel(GuiGraphics graphics, ContinuumBody b) {
        DiscoveryStage stage = ContinuumClientState.stage(b.id());
        int w = 200;
        int x = width - w - 12;
        int y = 34;
        int inner = w - 14;

        int h = 276;
        panel(graphics, x, y, w, h);
        int ty = y + 8;
        graphics.drawString(font, b.name(), x + 7, ty, TITLE);
        ty += 11;
        graphics.drawString(font, b.type().label() + "  -  " + stageLabel(stage), x + 7, ty, stageColor(stage));
        ty += 14;

        if (stage == DiscoveryStage.SURVEYED) {
            ty = wrapped(graphics, b.description(), x + 7, ty, inner, TEXT) + 4;
            ty = stat(graphics, "Orbit", b.isCentral() ? "-" : String.format("%.2f AU", b.orbitAu()), x + 7, ty);
            ty = stat(graphics, "Period", b.isCentral() ? "-" : String.format("%.0f days", b.periodDays()), x + 7, ty);
            ty = stat(graphics, "Radius", String.format("%.2f (Earth = 1)", b.radius()), x + 7, ty);
            ty = stat(graphics, "Atmosphere", b.params().atmoDensity() > 0 ? "yes" : "none", x + 7, ty);
            ty = stat(graphics, "Axial tilt", String.format("%.0f deg", b.params().axialTiltDeg()), x + 7, ty);
        } else {
            ty = wrapped(graphics, "Unsurveyed. A scan return only: fly a mission here to resolve its surface and sky.",
                    x + 7, ty, inner, DIM) + 4;
            ty = stat(graphics, "Orbit", b.isCentral() ? "-" : String.format("%.2f AU", b.orbitAu()), x + 7, ty);
            ty = stat(graphics, "Radius", "??", x + 7, ty);
            ty = stat(graphics, "Atmosphere", "??", x + 7, ty);
        }
        ty = stat(graphics, "Pad tier", ContinuumMissions.tierName(ContinuumMissions.requiredPadTier(b)) + " or better",
                x + 7, ty);

        List<ContinuumBody> moons = ContinuumData.moonsOf(b.id());
        if (!moons.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (ContinuumBody moon : moons) {
                if (names.length() > 0) names.append(", ");
                names.append(ContinuumClientState.stage(moon.id()) == DiscoveryStage.UNKNOWN ? "???" : moon.name());
            }
            ty = stat(graphics, "Moons", names.toString(), x + 7, ty);
        }

        var outpost = ContinuumClientState.outpost(b.id());
        if (outpost != null) {
            ty = stat(graphics, "Outpost", outpost.probes() + " probe" + (outpost.probes() == 1 ? "" : "s"), x + 7, ty);
            ty = stat(graphics, "Ready", outpost.readyAt(ContinuumClientState.now()) + " of " + outpost.maxReady() +
                    " cycles", x + 7, ty);
            if (outpost.upkeepPerCycle() > 0) {
                graphics.drawString(font, "Power", x + 7, ty, DIM);
                graphics.drawString(font, outpost.powered() ? "OK" : "NO POWER", x + 81, ty,
                        outpost.powered() ? MapUi.GOOD : MapUi.BAD);
                ty += 11;
                ty = stat(graphics, "Upkeep", String.format(Locale.ROOT, "%,d EU / cycle", outpost.upkeepPerCycle()),
                        x + 7, ty);
                if (outpost.lostCycles() > 0) {
                    graphics.drawString(font, "Lost cycles", x + 7, ty, DIM);
                    graphics.drawString(font, String.valueOf(outpost.lostCycles()), x + 81, ty, MapUi.BAD);
                }
            }
        }

        drawPlanButton(graphics, stage, x, y + h - 30, w);
    }

    /** The button that opens the mission planner, or a note about why it cannot. */
    private void drawPlanButton(GuiGraphics graphics, DiscoveryStage stage, int x, int y, int w) {
        planRect = null;
        graphics.fill(x + 7, y - 6, x + w - 7, y - 5, PANEL_LINE);

        boolean enabled = pad != null && stage != DiscoveryStage.UNKNOWN;
        int bx = x + 7;
        int bw = w - 14;
        graphics.fill(bx, y, bx + bw, y + 18, enabled ? 0xFF2c2760 : 0xFF1a1830);
        graphics.renderOutline(bx, y, bw, 18, enabled ? FRAME : 0xFF2a2548);
        graphics.drawCenteredString(font, pad == null ? "Plan mission  (view only)" : "Plan mission", bx + bw / 2,
                y + 5,
                enabled ? TITLE : 0xFF565070);
        if (enabled) planRect = new int[] { bx, y, bw, 18 };
    }

    private int countProbes() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ContinuumProbeItem) total += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ContinuumProbeItem) total += stack.getCount();
        }
        return total;
    }

    private ItemStack findRocket() {
        var player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ContinuumRocketItem) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ContinuumRocketItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The team's missions, finished ones first: click a running mission to watch it, a landed one to collect. */
    private void drawMissions(GuiGraphics graphics, int mouseX, int mouseY) {
        missionRows.clear();
        List<ContinuumStateSnapshot.MissionView> list = new ArrayList<>(ContinuumClientState.missions());
        if (list.isEmpty()) return;

        list.sort(Comparator.comparing((ContinuumStateSnapshot.MissionView m) -> !m.state().finished())
                .thenComparing(ContinuumStateSnapshot.MissionView::endMillis));

        int rows = Math.min(5, list.size());
        int w = 214;
        int rowHeight = 24;
        int h = 16 + rows * rowHeight;
        int x = 10;
        int y = height - 30 - h;
        long now = ContinuumClientState.now();

        panel(graphics, x, y, w, h);
        graphics.drawString(font, "Missions  (" + list.size() + ")", x + 6, y + 5, TITLE);

        for (int i = 0; i < rows; i++) {
            ContinuumStateSnapshot.MissionView mission = list.get(i);
            ContinuumBody destination = ContinuumData.body(mission.destination());
            String name = destination != null ? destination.name() : mission.destination().getPath();

            int ry = y + 16 + i * rowHeight;
            boolean hover = MapUi.inside(mouseX, mouseY, x + 3, ry, w - 6, rowHeight - 2);
            if (hover) graphics.fill(x + 3, ry, x + w - 3, ry + rowHeight - 2, 0x44ffffff);

            String status;
            int color;
            if (mission.state() == Mission.State.SUCCESS) {
                status = "Landed - collect";
                color = MapUi.GOOD;
            } else if (mission.state() == Mission.State.FAILED) {
                status = "Failed - collect";
                color = MapUi.BAD;
            } else {
                status = MapUi.duration(mission.endMillis() - now);
                color = TEXT;
            }
            String label = switch (mission.type()) {
                case SURVEY -> name;
                case EXTRACT -> name + "  (extract x" + mission.probes() + ")";
                case DEPLOY -> name + "  (outpost x" + mission.probes() + ")";
                case HAUL -> name + "  (haul)";
            };
            graphics.drawString(font, label, x + 7, ry + 2, TITLE);
            graphics.drawString(font, status, x + w - 7 - font.width(status), ry + 2, color);
            MapUi.bar(graphics, x + 7, ry + 14, w - 14, 4, mission.progress(now),
                    mission.state() == Mission.State.FAILED ? MapUi.BAD : mission.state().finished() ? MapUi.GOOD :
                            FRAME);

            missionRows.add(new Object[] { mission.id(), mission.state().finished(), x + 3, ry, w - 6,
                    rowHeight - 2 });
        }
    }

    private int stat(GuiGraphics graphics, String label, String value, int x, int y) {
        graphics.drawString(font, label, x, y, DIM);
        graphics.drawString(font, value, x + 74, y, TEXT);
        return y + 11;
    }

    private void drawChrome(GuiGraphics graphics) {
        graphics.renderOutline(1, 1, width - 2, height - 2, FRAME);

        String crumb = "Galaxy";
        if (system != null && level != Level.GALAXY) {
            crumb += "  >  " + system.name();
            if (body != null && level == Level.BODY) crumb += "  >  " + body.name();
        }
        graphics.drawString(font, "CONTINUUM", 10, 8, TITLE);
        graphics.drawString(font, crumb, 10, 19, DIM);

        if (level != Level.GALAXY) {
            graphics.fill(width - 62, 8, width - 10, 22, 0xAA1a1830);
            graphics.renderOutline(width - 62, 8, 52, 14, PANEL_LINE);
            graphics.drawCenteredString(font, "< Back", width - 36, 11, TEXT);
        }

        String hint = "drag: rotate   scroll: zoom   click: open   right-click / Esc: back   [Q] quality: " +
                quality.name().toLowerCase(Locale.ROOT) + (pad == null ? "   (view only)" : "");
        graphics.drawCenteredString(font, hint, width / 2, height - 12, 0xFF6a6488);

        if (flashTime > 0.0f) {
            int alpha = (int) (Math.min(1.0f, flashTime) * 255.0f) << 24;
            graphics.drawCenteredString(font, flash, width / 2, height - 30, (0x00ffd27a | alpha));
        }
    }

    // ------------------------------------------------------------------ picking & navigation

    private @Nullable Pick pickAt(double mx, double my) {
        if (phase != Phase.NONE || planner.isOpen()) return null;
        Pick best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Pick pick : picks) {
            double dx = mx - pick.x();
            double dy = my - pick.y();
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d <= pick.radius() && d < bestDistance) {
                best = pick;
                bestDistance = d;
            }
        }
        return best;
    }

    private void flash(String message) {
        flash = message;
        flashTime = 2.0f;
    }

    private void transition(int direction, Runnable apply) {
        if (phase != Phase.NONE) return;
        planner.close();
        phase = Phase.OUT;
        phaseTime = 0.0f;
        zoomDirection = direction;
        onSwitch = apply;

        // the current view dives toward (or pulls away from) its target while it fades out
        MapCamera camera = cam();
        camera.targetDistance = direction > 0 ? camera.targetDistance * 0.5f : camera.targetDistance * 1.7f;
    }

    private void updateTransition(float dt) {
        if (phase == Phase.NONE) return;

        phaseTime += dt;
        float t = Math.min(phaseTime / PHASE_SECONDS, 1.0f);
        if (phase == Phase.OUT) {
            fade = t;
            if (t >= 1.0f) {
                if (onSwitch != null) onSwitch.run();
                onSwitch = null;
                phase = Phase.IN;
                phaseTime = 0.0f;
            }
        } else {
            fade = 1.0f - t;
            if (t >= 1.0f) {
                phase = Phase.NONE;
                fade = 0.0f;
            }
        }
    }

    private void enterSystem(ContinuumSystem sys) {
        level = Level.SYSTEM;
        system = sys;
        body = null;
        systemCam.targetYaw = systemCam.yaw = 20.0f;
        systemCam.targetPitch = systemCam.pitch = 38.0f;
        systemCam.focus.set(0, 0, 0);
        systemCam.targetFocus.set(0, 0, 0);
        systemCam.targetDistance = SYSTEM_DISTANCE;
        systemCam.snapDistance(SYSTEM_DISTANCE * (zoomDirection > 0 ? 1.9f : 0.55f));
    }

    private void enterBody(ContinuumBody target) {
        level = Level.BODY;
        body = target;
        bodyCam.targetYaw = bodyCam.yaw = 0.0f;
        bodyCam.targetPitch = bodyCam.pitch = 10.0f;
        bodyCam.focus.set(0, 0, 0);
        bodyCam.targetFocus.set(0, 0, 0);
        bodyCam.targetDistance = BODY_DISTANCE;
        bodyCam.snapDistance(BODY_DISTANCE * (zoomDirection > 0 ? 1.9f : 0.55f));
    }

    private void enterGalaxy() {
        level = Level.GALAXY;
        system = null;
        body = null;
        galaxyCam.targetFocus.set(0, 0, 0);
        galaxyCam.targetDistance = GALAXY_DISTANCE;
        galaxyCam.snapDistance(GALAXY_DISTANCE * 0.6f);
    }

    private void goBack() {
        switch (level) {
            case GALAXY -> onClose();
            case SYSTEM -> transition(-1, this::enterGalaxy);
            case BODY -> {
                ContinuumSystem owner = system;
                transition(-1, () -> {
                    if (owner != null) enterSystem(owner);
                    else enterGalaxy();
                });
            }
        }
    }

    private void open(Pick pick) {
        if (level == Level.GALAXY) {
            ContinuumSystem sys = pick.system();
            if (ContinuumClientState.stage(sys.id()) == DiscoveryStage.UNKNOWN) {
                flash("Unknown signal. More research is needed to resolve it.");
                return;
            }
            galaxyCam.targetFocus.set(sys.galaxyX(), sys.galaxyY(), sys.galaxyZ()).mul(GALAXY_SCALE);
            transition(1, () -> enterSystem(sys));
        } else if (pick.body() != null) {
            ContinuumBody target = pick.body();
            if (ContinuumClientState.stage(target.id()) == DiscoveryStage.UNKNOWN) {
                flash("Unknown signal. Nothing to look at yet.");
                return;
            }
            transition(1, () -> enterBody(target));
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            pressed = true;
            pressX = mouseX;
            pressY = mouseY;
            dragged = 0.0;
        } else if (button == 1 && phase == Phase.NONE) {
            if (planner.isOpen()) planner.close();
            else goBack();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (planner.isOpen()) {
            if (button == 0) {
                MissionPlannerPanel.Result result = planner.click(mouseX, mouseY);
                if (result.launchType() != null && body != null && pad != null) {
                    PhoenixNetwork.CHANNEL.sendToServer(
                            new C2SLaunchMissionPacket(body.id(), pad, result.launchType(), result.probes()));
                }
            }
            pressed = false;
            return true;
        }

        if (button == 0 && pressed) {
            pressed = false;
            if (dragged < 4.0 && phase == Phase.NONE) {
                if (level != Level.GALAXY && mouseX >= width - 62 && mouseX <= width - 10 && mouseY >= 8 &&
                        mouseY <= 22) {
                    goBack();
                } else if (clickPlanner(mouseX, mouseY)) {
                    // handled
                } else {
                    Pick pick = pickAt(mouseX, mouseY);
                    if (pick != null) open(pick);
                }
            }
        }
        return true;
    }

    /** Launch button and mission rows. */
    private boolean clickPlanner(double mouseX, double mouseY) {
        if (planRect != null && level == Level.BODY && body != null && pad != null &&
                MapUi.inside(mouseX, mouseY, planRect[0], planRect[1], planRect[2], planRect[3])) {
            var outpost = ContinuumClientState.outpost(body.id());
            planner.open(ContinuumClientState.stage(body.id()),
                    outpost != null && outpost.readyAt(ContinuumClientState.now()) > 0);
            return true;
        }

        for (Object[] row : missionRows) {
            if (MapUi.inside(mouseX, mouseY, (int) row[2], (int) row[3], (int) row[4], (int) row[5])) {
                UUID id = (UUID) row[0];
                if ((boolean) row[1]) {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SCollectMissionPacket(id));
                } else {
                    Minecraft.getInstance().setScreen(new ContinuumTransitScreen(id, pad));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && phase == Phase.NONE && !planner.isOpen()) {
            dragged += Math.abs(dragX) + Math.abs(dragY);
            cam().rotate((float) dragX * 0.4f, (float) dragY * 0.35f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (phase == Phase.NONE && !planner.isOpen()) cam().zoom((float) delta);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_BACKSPACE) {
            if (planner.isOpen()) planner.close();
            else if (phase == Phase.NONE) goBack();
            return true;
        }
        if (keyCode == InputConstants.KEY_Q) {
            quality = quality.next();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (scene != null) {
            scene.destroyBuffers();
            scene = null;
            sceneWidth = -1;
            sceneHeight = -1;
        }
        super.removed();
    }
}
