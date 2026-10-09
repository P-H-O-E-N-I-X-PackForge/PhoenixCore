package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.client.FlightProfiles;
import net.phoenix.core.integration.continuum.client.render.ContinuumShaders;
import net.phoenix.core.integration.continuum.client.render.GlowRenderer;
import net.phoenix.core.integration.continuum.client.render.PlanetParams;
import net.phoenix.core.integration.continuum.client.render.SceneRenderer;
import net.phoenix.core.integration.continuum.client.render.SceneTarget;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.network.C2SCollectMissionPacket;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.UUID;

public class ContinuumArrivalScreen extends Screen {

    private static final float SECONDS = 9.0f;
    private static final float CARD_AT = 0.78f;
    private static final float START_DISTANCE = 3.9f;

    private final UUID missionId;
    private final @Nullable BlockPos pad;
    private final SceneTarget scene = new SceneTarget();

    private float t;
    private float spin;
    private long lastFrame = Util.getMillis();
    private boolean resolvedSound;
    private ContinuumSounds.Loop rumble;
    private FlightProfiles.Landing profile = FlightProfiles.Landing.ENTRY;
    private boolean touchedDown;

    public ContinuumArrivalScreen(UUID missionId, @Nullable BlockPos pad) {
        super(Component.literal("Arrival"));
        this.missionId = missionId;
        this.pad = pad;
    }

    @Override
    protected void init() {
        super.init();
        if (rumble == null) rumble = ContinuumSounds.engineLoop();
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

    private boolean failed(ContinuumStateSnapshot.MissionView mission) {
        return mission.state() == Mission.State.FAILED;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float dt = Math.min((now - lastFrame) / 1000.0f, 0.1f);
        lastFrame = now;
        t = Math.min(1.0f, t + dt / SECONDS);
        spin += dt * 4.0f;

        ContinuumStateSnapshot.MissionView mission = ContinuumClientState.mission(missionId);
        if (mission == null) {
            leave();
            return;
        }
        ContinuumBody destination = ContinuumData.body(mission.destination());
        profile = FlightProfiles.Landing.of(destination);
        boolean crash = failed(mission);
        boolean hole = destination != null && (destination.type() == ContinuumBody.Type.BLACK_HOLE ||
                destination.type() == ContinuumBody.Type.STAR);
        float atmosphere = destination != null ? destination.params().atmoDensity() : 0.0f;

        float heat = smooth((t - 0.18f) / 0.22f) * (1.0f - smooth((t - 0.66f) / 0.16f));
        heat *= hole ? 0.0f : profile.heat * (0.35f + 0.65f * Math.min(1.0f, atmosphere * 1.4f));
        float shake = (crash ? 1.0f : 0.35f) * heat;

        drawScene(graphics, destination, hole, crash, shake, now);
        drawHeat(graphics, heat, crash, hole, now);
        drawRetroAndDust(graphics, crash, hole, now);
        drawTint(graphics, now);
        drawReadout(graphics, mission, destination, hole);

        if (crash && t > 0.7f && t < 0.78f) {
            graphics.fill(0, 0, width, height, ((int) (Math.min(0.5f, (0.78f - t) * 8.0f) * 255.0f) << 24) | 0xff2010);
        }

        float cardIn = smooth((t - CARD_AT) / 0.1f);
        if (cardIn > 0.0f) drawCard(graphics, mouseX, mouseY, mission, destination, hole, cardIn);

        updateSound(mission, hole);

        float fade = Math.max(0.0f, 1.0f - t * 12.0f);
        if (fade > 0.001f) graphics.fill(0, 0, width, height, (int) (Math.min(fade, 1.0f) * 255.0f) << 24);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawScene(GuiGraphics graphics, @Nullable ContinuumBody destination, boolean hole, boolean crash,
                           float shake, long now) {
        RenderTarget target = scene.ensure();
        if (target == null || !ContinuumShaders.ready() || destination == null) {
            graphics.fill(0, 0, width, height, 0xFF05060f);
            return;
        }

        float eased = smooth(t / CARD_AT);
        float endDistance = hole ? 2.9f : 1.32f;
        float distance = START_DISTANCE + (endDistance - START_DISTANCE) * (float) Math.pow(eased, 0.85);
        float pitch = hole ? 9.0f : 9.0f + 46.0f * smooth(t / CARD_AT);
        float jx = (float) Math.sin(now * 0.071) * 0.012f * shake;
        float jy = (float) Math.sin(now * 0.093) * 0.012f * shake;

        Matrix4f projection = SceneRenderer.projection(target.width, target.height, 40.0f);
        Matrix4f view = new Matrix4f().translate(jx, jy, -distance).rotateX((float) Math.toRadians(pitch));

        SceneRenderer.begin(target, spin);
        if (hole) {
            SceneRenderer.drawCentralBody(destination, view, projection, new Vector3f(), 1.2f,
                    net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality());
        } else {
            PlanetParams params = destination.params();
            SceneRenderer.drawPlanet(params, view, projection, new Vector3f(), 1.0f, new Vector3f(-60.0f, 24.0f, 40.0f),
                    spin * params.spinDegPerSec() * 0.4f, spin * params.cloudSpinDegPerSec() * 0.4f,
                    net.phoenix.core.integration.continuum.client.ContinuumVisuals.quality(), true);
        }
        SceneRenderer.end();
        scene.blit(graphics, width, height);
    }

    private void drawHeat(GuiGraphics graphics, float heat, boolean crash, boolean hole, long now) {
        if (heat <= 0.01f) return;
        float flicker = 0.85f + 0.15f * (float) Math.sin(now * 0.05) + 0.05f * (float) Math.sin(now * 0.17);
        int core = crash ? 0xffb090 : 0xfff2d0;
        int mid = crash ? 0xff4a30 : 0xff9a40;
        int outer = crash ? 0xc01810 : 0xff5a20;

        GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
        float cx = width / 2.0f;
        float base = height * 1.05f;
        glows.glow(cx, base, width * 0.55f * heat, outer, 0.55f * heat * flicker);
        glows.glow(cx, base - height * 0.08f, width * 0.36f * heat, mid, 0.6f * heat * flicker);
        glows.glow(cx, base - height * 0.14f, width * 0.2f * heat, core, 0.7f * heat * flicker);

        for (int i = 0; i < 7; i++) {
            float wobble = (float) Math.sin(now * 0.004 + i * 1.7);
            float x = cx + (i - 3) * width * 0.055f + wobble * 10.0f;
            float y = height * (0.78f - 0.05f * (float) Math.abs(i - 3) / 3.0f) + wobble * 6.0f;
            glows.glow(x, y, 26.0f + 18.0f * heat, i % 2 == 0 ? mid : outer, 0.35f * heat * flicker);
        }
        glows.draw();
    }

    private float retroCurve() {
        return smooth((t - 0.35f) / 0.25f) * (1.0f - smooth((t - 0.74f) / 0.06f));
    }

    private void drawRetroAndDust(GuiGraphics graphics, boolean crash, boolean hole, long now) {
        if (hole) return;
        float cx = width / 2.0f;
        float retro = profile.retro * retroCurve();
        if (retro > 0.01f) {
            float flicker = 0.85f + 0.15f * (float) Math.sin(now * 0.05);
            GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
            glows.glow(cx, height * 1.0f, width * 0.22f * retro, crash ? 0xff6030 : 0x80c0ff, 0.5f * retro * flicker);
            glows.glow(cx, height * 0.93f, width * 0.12f * retro, crash ? 0xffc080 : 0xd8f0ff, 0.7f * retro * flicker);
            glows.draw();
        }

        float p = (t - 0.68f) / (CARD_AT + 0.04f - 0.68f);
        if (profile.dust != 0 && p > 0.0f && p < 1.0f) {
            GlowRenderer.Batch glows = GlowRenderer.batch(graphics);
            for (int i = 0; i < 10; i++) {
                float a = i / 10.0f * (float) Math.PI * 2.0f;
                float lift = profile.spray ?
                        p * (1.0f - p) * height * 0.35f * (0.5f + 0.5f * (float) Math.abs(Math.sin(a * 2.0f))) : 0.0f;
                float x = cx + (float) Math.cos(a) * p * width * 0.32f;
                float y = height * 0.9f + (float) Math.sin(a) * p * height * 0.04f - lift;
                glows.glow(x, y, 18.0f + 44.0f * p, profile.dust, (1.0f - p) * 0.45f);
            }
            glows.draw();
        }
    }

    private void drawTint(GuiGraphics graphics, long now) {
        if (profile.tintAlpha > 0.0f) {
            float pulse = profile == FlightProfiles.Landing.SOLAR ? 0.75f + 0.25f * (float) Math.sin(now * 0.004) :
                    1.0f;
            int alpha = (int) (Math.min(1.0f, profile.tintAlpha * pulse * smooth(t / 0.5f)) * 255.0f);
            graphics.fill(0, 0, width, height, (alpha << 24) | profile.tint);
        }
        if (profile.vignette) {
            int edge = ((int) (0xB0 * smooth(t / 0.6f))) << 24;
            graphics.fillGradient(0, 0, width, height / 3, edge, 0);
            graphics.fillGradient(0, height * 2 / 3, width, height, 0, edge);
        }
    }

    private void drawReadout(GuiGraphics graphics, ContinuumStateSnapshot.MissionView mission,
                             @Nullable ContinuumBody destination, boolean hole) {
        String name = destination != null ? destination.name() : mission.destination().getPath();
        graphics.drawString(font, profile.title, 14, 12, MapUi.TITLE);
        graphics.drawString(font, name, 14, 24, MapUi.DIM);

        if (!hole) {
            double altitudeKm = (1.0 - Math.min(1.0, t / CARD_AT)) * 380.0 + 4.0;
            graphics.drawString(font, String.format(Locale.ROOT, "ALT  %,d km", Math.round(altitudeKm)), 14, 40,
                    MapUi.TEXT);
        }
        if (t < CARD_AT) graphics.drawCenteredString(font, "Esc to skip", width / 2, height - 12, 0xFF6a6488);
    }

    private int cardW() {
        return Math.min(320, width - 40);
    }

    private int cardX() {
        return (width - cardW()) / 2;
    }

    private int cardY() {
        return height / 2 - 70;
    }

    private int collectY() {
        return cardY() + 128;
    }

    private void drawCard(GuiGraphics graphics, int mouseX, int mouseY, ContinuumStateSnapshot.MissionView mission,
                          @Nullable ContinuumBody destination, boolean hole, float alpha) {
        boolean crash = failed(mission);
        int w = cardW();
        int x = cardX();
        int y = cardY() - (int) ((1.0f - alpha) * 14.0f);
        int a = (int) (alpha * 255.0f) << 24;

        graphics.fill(x, y, x + w, y + 154, (0xD0 * (int) (alpha * 255.0f) / 255 << 24) | 0x0b0a18);
        graphics.renderOutline(x, y, w, 154, a | (crash ? 0xff6b6b : 0x7a5cff));

        String name = destination != null ? destination.name() : mission.destination().getPath();
        String title = crash ? "LANDING FAILED" : profile.success;
        graphics.drawCenteredString(font, title, x + w / 2, y + 10, a | (crash ? 0xff6b6b : 0xE8D8FF));
        graphics.drawCenteredString(font, name + "  -  " + mission.type().label(), x + w / 2, y + 24, a | 0xB8B0D8);

        String result = switch (mission.type()) {
            case SURVEY -> crash ? "The rocket came back damaged; nothing was learned." :
                    "Survey complete. The surface data is on the map.";
            case EXTRACT -> crash ? "The probes were lost and the rocket is damaged." :
                    "The probes are back. Resources are ready to collect.";
            case DEPLOY -> crash ? "The probes were lost and the rocket is damaged." :
                    "The outpost is down and producing.";
            case HAUL -> crash ? "The haul was lost and the rocket is damaged." :
                    "The stockpile is loaded. Ready to collect.";
            case REPAIR -> crash ? "The repair kits were lost and the outpost is still broken." :
                    "The outpost is repaired and producing again.";
            case RESCUE -> crash ? "The repair kits were lost and the stranded rocket still waits." :
                    "The stranded rocket is recovered.";
            case STATION -> crash ? "The station crew's probes were lost and the rocket is damaged." :
                    "The orbital station is built.";
        };
        if (mission.stranded()) result = "The rocket is stranded. A distress signal is going out.";
        graphics.drawCenteredString(font, result, x + w / 2, y + 48, a | (crash ? 0xffb0b0 : 0xB8B0D8));

        float wear = mission.wearAfter();
        graphics.drawCenteredString(font, String.format(Locale.ROOT, "Rocket wear after this trip: %d%%",
                Math.round(wear * 100.0f)), x + w / 2, y + 66, a | (MapUi.wearColor(wear) & 0xFFFFFF));
        MapUi.bar(graphics, x + 30, y + 80, w - 60, 6, wear, MapUi.wearColor(wear));

        int ey = y + 94;
        for (String id : mission.events()) {
            net.phoenix.core.integration.continuum.common.MissionEvent event = net.phoenix.core.integration.continuum.common.MissionEvent
                    .byId(id);
            if (event == null) continue;
            int color = event.bad ? 0xFFffb070 : 0xFF7affd0;
            for (net.minecraft.util.FormattedCharSequence line : font.split(
                    net.minecraft.network.chat.FormattedText.of(event.text), w - 24)) {
                graphics.drawCenteredString(font, line, x + w / 2, ey, (a & 0xFF000000) | (color & 0xFFFFFF));
                ey += 10;
            }
        }

        int bw = (w - 36) / 2;
        boolean hasHaul = !crash && (mission.type() == Mission.Type.EXTRACT || mission.type() == Mission.Type.HAUL);
        button(graphics, x + 12, collectY(), bw, mouseX, mouseY, hasHaul ? "Collect rocket + haul" : "Collect rocket",
                true);
        button(graphics, x + 24 + bw, collectY(), bw, mouseX, mouseY, "Back to map", false);
    }

    private void button(GuiGraphics graphics, int x, int y, int w, int mouseX, int mouseY, String label,
                        boolean primary) {
        boolean hover = MapUi.inside(mouseX, mouseY, x, y, w, 18);
        graphics.fill(x, y, x + w, y + 18, hover ? 0xFF2c2760 : primary ? 0xFF1d1a40 : 0xFF14122a);
        graphics.renderOutline(x, y, w, 18, primary ? MapUi.FRAME : MapUi.PANEL_LINE);
        graphics.drawCenteredString(font, label, x + w / 2, y + 5, primary ? MapUi.TITLE : MapUi.TEXT);
    }

    private void updateSound(ContinuumStateSnapshot.MissionView mission, boolean hole) {
        if (rumble != null) {
            float heat = Math.max(0.0f, 1.0f - Math.abs(t - 0.45f) * 2.6f);
            rumble.setTarget(hole ? 0.0f : Math.max(0.15f + 0.85f * heat, profile.retro * retroCurve()));
            if (t > 0.78f) {
                rumble.fadeOut();
                rumble = null;
            }
        }
        if (!touchedDown && t >= 0.7f && !hole) {
            touchedDown = true;
            ContinuumSounds.touchdown(profile.touch, failed(mission));
        }
        if (!resolvedSound && t >= CARD_AT) {
            resolvedSound = true;
            if (failed(mission)) ContinuumSounds.warning();
            else ContinuumSounds.chime();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (t >= CARD_AT + 0.05f && button == 0) {
            int w = cardW();
            int bw = (w - 36) / 2;
            int x = cardX();
            if (MapUi.inside(mouseX, mouseY, x + 12, collectY(), bw, 18)) {
                ContinuumSounds.click();
                PhoenixNetwork.CHANNEL.sendToServer(new C2SCollectMissionPacket(missionId));
                leave();
                return true;
            }
            if (MapUi.inside(mouseX, mouseY, x + 24 + bw, collectY(), bw, 18)) {
                ContinuumSounds.click();
                leave();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_SPACE) {
            if (t < CARD_AT) t = CARD_AT;
            else leave();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void leave() {
        Minecraft.getInstance().setScreen(new ContinuumMapScreen(pad));
    }

    @Override
    public void removed() {
        if (rumble != null) rumble.fadeOut();
        scene.close();
        super.removed();
    }
}
