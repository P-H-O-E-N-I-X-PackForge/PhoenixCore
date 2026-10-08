package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.client.render.ContinuumShaders;
import net.phoenix.core.integration.continuum.client.render.GlowRenderer;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
import net.phoenix.core.integration.continuum.client.render.PlanetRenderer;
import net.phoenix.core.integration.continuum.client.render.SceneRenderer;
import net.phoenix.core.integration.continuum.client.render.SceneTarget;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;
import net.phoenix.core.integration.continuum.network.C2SCollectMissionPacket;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.UUID;

/**
 * Watching one mission fly: the destination grows in view as the real-time countdown runs down, with a few waypoints
 * along the way. When the mission lands it offers to collect the rocket. The mission state comes from the server's
 * snapshots, so this keeps working across reconnects and while other missions come and go.
 */
public class ContinuumTransitScreen extends Screen {

    private static final float START_DISTANCE = 70.0f;
    private static final float END_DISTANCE = 3.9f;

    private static final float[] WAYPOINT_AT = { 0.12f, 0.38f, 0.66f, 0.90f };
    private static final String[] WAYPOINT_TEXT = { "Clearing the home system", "Crossing the outer dark",
            "Course correction burn", "Final approach" };

    private final UUID missionId;
    private final @Nullable BlockPos pad;
    private final SceneTarget scene = new SceneTarget();

    private float clock;
    private long lastFrame = Util.getMillis();
    private ContinuumSounds.Loop hum;
    private int waypointsPassed = -1;
    private boolean wasRunning;

    @Override
    protected void init() {
        super.init();
        if (hum == null) {
            hum = ContinuumSounds.humLoop();
            hum.setTarget(0.4f);
        }
    }

    public ContinuumTransitScreen(UUID missionId, @Nullable BlockPos pad) {
        super(Component.literal("Transit"));
        this.missionId = missionId;
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long nowMillis = Util.getMillis();
        float dt = Math.min((nowMillis - lastFrame) / 1000.0f, 0.1f);
        lastFrame = nowMillis;
        clock += dt;

        ContinuumStateSnapshot.MissionView mission = ContinuumClientState.mission(missionId);
        if (mission == null) {
            Minecraft.getInstance().setScreen(new ContinuumMapScreen(pad));
            return;
        }

        long serverNow = ContinuumClientState.now();
        float progress = mission.progress(serverNow);
        ContinuumBody destination = ContinuumData.body(mission.destination());

        // a mission that lands while it is being watched rolls straight into the landing scene
        if (!mission.state().finished()) {
            wasRunning = true;
        } else if (wasRunning) {
            Minecraft.getInstance().setScreen(new ContinuumArrivalScreen(missionId, pad));
            return;
        }

        int passed = 0;
        for (float waypoint : WAYPOINT_AT) {
            if (progress >= waypoint) passed++;
        }
        if (waypointsPassed >= 0 && passed > waypointsPassed && !mission.state().finished()) {
            ContinuumSounds.waypoint();
        }
        waypointsPassed = passed;

        drawScene(graphics, mission, destination, progress);
        drawOverlay(graphics, mouseX, mouseY, mission, destination, progress, serverNow);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawScene(GuiGraphics graphics, ContinuumStateSnapshot.MissionView mission,
                           @Nullable ContinuumBody destination, float progress) {
        RenderTarget target = scene.ensure();
        if (target == null || !ContinuumShaders.ready() || destination == null) {
            graphics.fill(0, 0, width, height, 0xFF05060f);
            return;
        }

        DiscoveryStage stage = ContinuumClientState.stage(destination.id());
        PlanetParams params = stage == DiscoveryStage.SURVEYED || mission.state() == Mission.State.SUCCESS ?
                destination.params() : destination.params().ghost();

        // quick at first, settling into orbit as it arrives
        float approach = 1.0f - (1.0f - progress) * (1.0f - progress);
        float distance = START_DISTANCE + (END_DISTANCE - START_DISTANCE) * approach;

        Matrix4f projection = SceneRenderer.projection(target.width, target.height, 40.0f);
        Matrix4f view = new Matrix4f().translate(0.0f, 0.0f, -distance).rotateX((float) Math.toRadians(9.0));

        SceneRenderer.begin(target, clock * 3.0f);
        if (destination.type() == ContinuumBody.Type.BLACK_HOLE) {
            SceneRenderer.drawStar(0, true, view, projection, new Vector3f(), 1.2f, net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality());
            var owner = net.phoenix.core.integration.continuum.data.ContinuumData.system(destination.system());
            SceneRenderer.drawBlackHole(view, projection, new Vector3f(), 1.2f, owner != null && owner.isQuasar());
        } else {
            SceneRenderer.drawPlanet(params, view, projection, new Vector3f(), 1.0f, new Vector3f(-60.0f, 24.0f, 40.0f),
                    clock * params.spinDegPerSec(), clock * params.cloudSpinDegPerSec(),
                    net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality(), true);
        }
        SceneRenderer.end();
        scene.blit(graphics, width, height);

        // the rocket's engine, a small flame low in the view while under way
        if (!mission.state().finished()) {
            float flicker = 0.8f + 0.2f * (float) Math.sin(clock * 38.0);
            GlowRenderer.batch(graphics)
                    .glow(width / 2.0f, height * 0.9f, 22.0f, 0xfff0c0, 0.7f * flicker)
                    .glow(width / 2.0f, height * 0.9f + 14.0f, 38.0f, 0xff9a40, 0.45f * flicker)
                    .draw();
        }
    }

    private int backX() {
        return width - 62;
    }

    private int replayX() {
        return width - 140;
    }

    private int collectX() {
        return width / 2 - 62;
    }

    private int collectY() {
        return height - 58;
    }

    private void drawOverlay(GuiGraphics graphics, int mouseX, int mouseY, ContinuumStateSnapshot.MissionView mission,
                             @Nullable ContinuumBody destination, float progress, long serverNow) {
        String name = destination != null ? destination.name() : mission.destination().getPath();

        graphics.renderOutline(1, 1, width - 2, height - 2, MapUi.FRAME);
        boolean extraction = mission.type() == Mission.Type.EXTRACT;
        boolean payload = extraction || mission.type() == Mission.Type.DEPLOY;
        graphics.drawString(font, mission.type().label().toUpperCase(java.util.Locale.ROOT), 14, 12, MapUi.TITLE);
        graphics.drawString(font, "Bound for " + name + (payload ? "  -  " + mission.probes() + " probe" +
                (mission.probes() == 1 ? "" : "s") : ""), 14, 24, MapUi.TEXT);
        graphics.drawString(font, "Launched by " + mission.launcherName(), 14, 35, MapUi.DIM);

        // waypoints
        int y = 56;
        for (int i = 0; i < WAYPOINT_AT.length; i++) {
            boolean passed = progress >= WAYPOINT_AT[i];
            graphics.drawString(font, (passed ? "[x] " : "[ ] ") + WAYPOINT_TEXT[i], 14, y,
                    passed ? MapUi.TEXT : MapUi.DIM);
            y += 11;
        }

        if (mission.state().finished()) {
            graphics.fill(replayX(), 8, replayX() + 70, 22, 0xAA1a1830);
            graphics.renderOutline(replayX(), 8, 70, 14, MapUi.PANEL_LINE);
            graphics.drawCenteredString(font, "Replay landing", replayX() + 35, 11, MapUi.TEXT);
        }

        graphics.fill(backX(), 8, backX() + 52, 22, 0xAA1a1830);
        graphics.renderOutline(backX(), 8, 52, 14, MapUi.PANEL_LINE);
        graphics.drawCenteredString(font, "< Map", backX() + 26, 11, MapUi.TEXT);

        // progress and countdown
        int barW = Math.min(420, width - 80);
        int barX = (width - barW) / 2;
        int barY = height - 28;
        boolean finished = mission.state().finished();

        MapUi.bar(graphics, barX, barY, barW, 8, progress,
                mission.state() == Mission.State.FAILED ? MapUi.BAD : MapUi.FRAME);
        for (float waypoint : WAYPOINT_AT) {
            int wx = barX + Math.round(barW * waypoint);
            graphics.fill(wx, barY - 3, wx + 1, barY + 11, progress >= waypoint ? 0xFFB8B0D8 : 0xFF4a4468);
        }

        String status;
        int statusColor;
        if (mission.state() == Mission.State.SUCCESS) {
            status = switch (mission.type()) {
                case SURVEY -> "ARRIVED  -  survey complete";
                case EXTRACT -> "ARRIVED  -  resources are ready to collect";
                case DEPLOY -> "ARRIVED  -  the outpost is up and producing";
                case HAUL -> "ARRIVED  -  the stockpile is ready to collect";
                case REPAIR -> "ARRIVED  -  the outpost is repaired and producing again";
            };
            statusColor = MapUi.GOOD;
        } else if (mission.state() == Mission.State.FAILED) {
            status = switch (mission.type()) {
                case SURVEY -> "MISSION FAILED  -  the rocket came back damaged";
                case EXTRACT, DEPLOY -> "MISSION FAILED  -  the probes were lost and the rocket is damaged";
                case HAUL -> "MISSION FAILED  -  the haul was lost and the rocket is damaged";
                case REPAIR -> "MISSION FAILED  -  the repair kits were lost and the outpost is still broken";
            };
            statusColor = MapUi.BAD;
        } else {
            status = MapUi.duration(mission.endMillis() - serverNow) + " remaining";
            statusColor = MapUi.TEXT;
        }
        graphics.drawCenteredString(font, status, width / 2, barY - 14, statusColor);

        String wear = String.format(Locale.ROOT, "Rocket wear after this trip: %d%%",
                Math.round(mission.wearAfter() * 100.0f));
        graphics.drawCenteredString(font, wear, width / 2, barY + 12, MapUi.wearColor(mission.wearAfter()));

        if (finished) {
            boolean hover = MapUi.inside(mouseX, mouseY, collectX(), collectY(), 124, 18);
            graphics.fill(collectX(), collectY(), collectX() + 124, collectY() + 18, hover ? 0xFF2c2760 : 0xFF1d1a40);
            graphics.renderOutline(collectX(), collectY(), 124, 18, MapUi.FRAME);
            boolean hasHaul = mission.state() == Mission.State.SUCCESS &&
                    (extraction || mission.type() == Mission.Type.HAUL);
            graphics.drawCenteredString(font, hasHaul ? "Collect rocket + haul" : "Collect rocket", collectX() + 62,
                    collectY() + 5, MapUi.TITLE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MapUi.inside(mouseX, mouseY, backX(), 8, 52, 14)) {
                ContinuumSounds.click();
                leave();
                return true;
            }
            ContinuumStateSnapshot.MissionView mission = ContinuumClientState.mission(missionId);
            if (mission != null && mission.state().finished() && MapUi.inside(mouseX, mouseY, replayX(), 8, 70, 14)) {
                ContinuumSounds.click();
                Minecraft.getInstance().setScreen(new ContinuumArrivalScreen(missionId, pad));
                return true;
            }
            if (mission != null && mission.state().finished() &&
                    MapUi.inside(mouseX, mouseY, collectX(), collectY(), 124, 18)) {
                ContinuumSounds.click();
                PhoenixNetwork.CHANNEL.sendToServer(new C2SCollectMissionPacket(missionId));
                leave();
                return true;
            }
        } else if (button == 1) {
            leave();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_BACKSPACE) {
            leave();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void leave() {
        Minecraft.getInstance().setScreen(new ContinuumMapScreen(pad));
    }

    @Override
    public void removed() {
        if (hum != null) hum.fadeOut();
        scene.close();
        super.removed();
    }
}
