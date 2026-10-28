package net.phoenix.core.client.stellar;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.phoenix.continuum.api.client.ContinuumRender;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws what a Stellar Forge is making inside its chamber: a gas cloud, a protostar, a star or a black hole, held by
 * rings of containment field that turn with the spin and glow with the field, and fed by beams from the floor that
 * run with the feed. It is drawn before the translucent blocks, so the glass of the chamber is in front of it.
 */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class StellarForgeRenderer {

    private static final double RANGE = 64.0;
    private static long lastReport;

    private StellarForgeRenderer() {}

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        StellarForgeClient.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (System.currentTimeMillis() - lastReport > 5000L) {
            lastReport = System.currentTimeMillis();
            var report = new StringBuilder();
            for (var entry : StellarForgeClient.all()) {
                var m = entry.getKey();
                report.append(String.format(java.util.Locale.ROOT, "[%s formed=%s stage=%s radius=%.2f dist=%.1f] ",
                        m.getBlockPos().toShortString(), m.isFormed(), m.stage(), entry.getValue().radius,
                        m.chamberCentre().distanceTo(event.getCamera().getPosition())));
            }
            PhoenixCore.LOGGER.debug("[StellarRender] shadersReady={} forges={}", ContinuumRender.ready(), report);
        }
        if (!ContinuumRender.ready()) return;

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f view = new Matrix4f(event.getModelViewMatrix());
        Matrix4f projection = event.getProjectionMatrix();
        Matrix4f pose = new Matrix4f();
        var buffers = mc.renderBuffers().bufferSource();
        float seconds = (float) (System.currentTimeMillis() % 100000L) / 1000.0f;

        for (var entry : StellarForgeClient.all()) {
            StellarForgeMachine machine = entry.getKey();
            var anim = entry.getValue();
            if (!machine.isFormed() || machine.getLevel() != mc.level) continue;
            Vec3 centre = machine.chamberCentre().subtract(camera);
            if (centre.lengthSqr() > RANGE * RANGE) continue;
            var stage = machine.stage();
            if (stage == StellarForgeState.Stage.IDLE && anim.radius < 0.02f) continue;

            boolean falling = anim.collapse >= 0.0f;
            float p = falling ? Mth.clamp(anim.collapse / StellarForgeClient.Anim.COLLAPSE_SECONDS, 0.0f, 1.0f) : 0.0f;
            float pre = falling ? Mth.clamp(p / StellarForgeClient.Anim.COLLAPSE_FLASH_AT, 0.0f, 1.0f) : 0.0f;
            float post = falling ? Mth.clamp((p - 0.62f) / 0.38f, 0.0f, 1.0f) : 1.0f;
            // the star draws itself in as it falls: slowly at first, then all at once
            float squeeze = pre * pre * pre;

            // a dye overrides the colour the heat gives
            var shownStage = falling && p < StellarForgeClient.Anim.COLLAPSE_FLASH_AT ? anim.collapseFrom : stage;
            int color = shownStage == StellarForgeState.Stage.STAR && machine.starTint() >= 0 ? machine.starTint() :
                    shownStage == StellarForgeState.Stage.BLACK_HOLE && machine.holeTint() >= 0 ? machine.holeTint() :
                            StellarForgeState.colorOf(anim.heat);
            int holeTint = machine.holeTint();
            if (falling && p < StellarForgeClient.Anim.COLLAPSE_FLASH_AT) {
                color = net.phoenix.core.client.gui.hud.GuiDraw.mix(0xFF_000000 | color, 0xFF_FFFFFF, squeeze) & 0xFFFFFF;
            }
            Vector3f at = new Vector3f((float) centre.x, (float) centre.y, (float) centre.z);
            if (falling && p < StellarForgeClient.Anim.COLLAPSE_FLASH_AT) {
                // a tremble that grows as it goes
                float shake = 0.02f * pre * pre;
                at.add((float) Math.sin(seconds * 71.0f) * shake, (float) Math.sin(seconds * 83.0f + 1.0f) * shake,
                        (float) Math.sin(seconds * 67.0f + 2.0f) * shake);
            }
            float radius = Math.max(0.02f, anim.radius);
            float field = machine.fieldSetting() / 100.0f;
            float flicker = machine.powerOk() ? 1.0f : 0.5f + 0.5f * (float) Math.sin(seconds * 31.0f);
            // once the body is all there and nothing is happening the light settles and breathes; a push flares it
            float calm = 0.16f + 0.05f * (float) Math.sin(seconds * 1.1f);
            float live = Math.max(anim.surge, machine.feedSetting() / 100.0f * 0.7f);
            float level = Mth.lerp(anim.settle, 1.0f, calm + (1.0f - calm) * live);
            float spin = machine.spinSetting() / 100.0f;
            if (falling) {
                // the containment winds up as it tightens, and lets go in the flash
                float wind = p < StellarForgeClient.Anim.COLLAPSE_FLASH_AT ? squeeze : 1.0f - post;
                spin = Math.min(1.0f, spin + wind * 1.2f);
                field = Math.min(1.0f, field + wind);
                level = Math.max(level, 0.6f + 0.8f * wind);
            }

            VertexConsumer lines = buffers.getBuffer(StellarBeamRenderType.BEAMS);
            containment(lines, pose, at, spin, field, seconds, color, flicker * level, anim.flash);
            injectors(lines, pose, machine, at, seconds, color, flicker * level);
            if (falling && p >= StellarForgeClient.Anim.COLLAPSE_FLASH_AT) shockwave(lines, pose, at, post);
            buffers.endBatch(StellarBeamRenderType.BEAMS);

            if (falling) {
                if (p < StellarForgeClient.Anim.COLLAPSE_FLASH_AT) {
                    // the old body, squeezed down to a point and whitened
                    float r = anim.collapseRadius * (1.0f - 0.92f * squeeze);
                    drawBody(anim.collapseFrom, Math.max(0.02f, r), color, -1, view, projection, at);
                } else if (post > 0.0f) {
                    // the black hole opening where it was: small and growing, its disk spinning up
                    float target = StellarForgeState.radius(StellarForgeState.Stage.BLACK_HOLE, machine.mass());
                    float grow = 1.0f - (1.0f - post) * (1.0f - post);
                    drawBody(StellarForgeState.Stage.BLACK_HOLE, Math.max(0.02f, target * grow), color, holeTint, view,
                            projection, at);
                }
            } else {
                drawBody(stage, radius, color, holeTint, view, projection, at);
            }
        }
    }

    /**
     * One body of a stage at a size the chamber can hold. The cloud's and protostar's glow and the black hole's disk are
     * flat pictures that cannot be cut off by the walls, so they are kept inside them (about 1.35 blocks from the middle,
     * the chamber being 1.5): bigger and they draw over the casing. The star is a real sphere and can be larger.
     */
    private static void drawBody(StellarForgeState.Stage stage, float radius, int color, int holeTint, Matrix4f view,
                                 Matrix4f projection, Vector3f at) {
        // drawn before the translucent blocks: write depth where lit, so water and glass behind do not show over it
        ContinuumRender.beginWorldObject(view, projection, at, true);
        switch (stage) {
            case GAS_CLOUD -> ContinuumRender.drawGasCloud(color, view, projection, at,
                    0.045f + 0.039f * Mth.clamp((radius - 0.3f) / 0.9f, 0.0f, 1.0f));
            case PROTOSTAR -> ContinuumRender.drawProtostar(color, view, projection, at,
                    0.05f + 0.025f * Mth.clamp((radius - 0.25f) / 0.3f, 0.0f, 1.0f));
            case STAR -> ContinuumRender.drawStar(null, color, view, projection, at, radius * 1.2f);
            case BLACK_HOLE -> {
                if (holeTint >= 0) {
                    int cool = ((int) (((holeTint >> 16) & 0xFF) * 0.55f) << 16) |
                            ((int) (((holeTint >> 8) & 0xFF) * 0.55f) << 8) | (int) ((holeTint & 0xFF) * 0.55f);
                    ContinuumRender.drawBlackHole(holeTint, cool, view, projection, at, radius * 0.9f);
                } else {
                    ContinuumRender.drawBlackHole(null, view, projection, at, radius * 0.9f);
                }
            }
            default -> {}
        }
        ContinuumRender.endWorldObject();
    }

    /** Rings of light thrown out when it falls in: two, one after the other, fading as they go. */
    private static void shockwave(VertexConsumer out, Matrix4f pose, Vector3f c, float t) {
        for (int ring = 0; ring < 2; ring++) {
            float local = Mth.clamp(t * 1.4f - ring * 0.25f, 0.0f, 1.0f);
            if (local <= 0.0f) continue;
            float radius = 0.3f + local * (ring == 0 ? 3.2f : 2.4f);
            float strength = (1.0f - local) * (ring == 0 ? 1.0f : 0.6f);
            float tilt = ring * 0.9f;
            int segments = 120;
            Vector3f previous = null;
            for (int i = 0; i <= segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                float px = (float) Math.cos(a) * radius;
                float pz = (float) Math.sin(a) * radius;
                Vector3f point = new Vector3f(c.x + px, c.y + pz * (float) Math.sin(tilt),
                        c.z + pz * (float) Math.cos(tilt));
                if (previous != null) {
                    beam(out, pose, previous, point, 0.05f * (1.0f - local * 0.5f), 0xcfe0ff,
                            strength);
                }
                previous = point;
            }
        }
    }

    /** A beam as light: a faint wide halo under a bright core, so it stays smooth and visible at a distance. */
    private static void beam(VertexConsumer out, Matrix4f pose, Vector3f a, Vector3f b, float width, int color,
                             float strength) {
        ContinuumRender.drawBeam(out, pose, a, b, width * 2.6f, color, strength * 0.12f);
        ContinuumRender.drawBeam(out, pose, a, b, width, color, strength);
    }

    /** Three tilted rings of field round the body, turning with the spin, brighter with the field. */
    private static void containment(VertexConsumer out, Matrix4f pose, Vector3f c, float spin, float field,
                                    float seconds, int color, float flicker, float flash) {
        float strength = (0.25f + 0.75f * field) * flicker;
        float turn = seconds * (0.4f + 2.2f * spin);
        for (int ring = 0; ring < 3; ring++) {
            float tilt = ring * 1.05f - 1.05f;
            float radius = 1.25f - ring * 0.08f + flash * 0.4f * (ring + 1);
            int segments = 96;
            Vector3f previous = null;
            for (int i = 0; i <= segments; i++) {
                double a = Math.PI * 2.0 * i / segments + turn * (ring % 2 == 0 ? 1 : -1);
                float px = (float) Math.cos(a) * radius;
                float pz = (float) Math.sin(a) * radius;
                float py = pz * (float) Math.sin(tilt);
                pz *= (float) Math.cos(tilt);
                Vector3f point = new Vector3f(c.x + px, c.y + py, c.z + pz);
                if (previous != null) {
                    float fade = 0.35f + 0.65f * (float) Math.max(0.0, Math.sin(a * 3.0 - seconds * 3.0));
                    beam(out, pose, previous, point, 0.03f, color, strength * fade * 0.8f);
                }
                previous = point;
            }
        }
        if (flash > 0.0f) {
            // a stage change: a ring of light flung out
            int segments = 96;
            Vector3f previous = null;
            float radius = 0.4f + (1.0f - flash) * 1.4f;
            for (int i = 0; i <= segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                Vector3f point = new Vector3f(c.x + (float) Math.cos(a) * radius, c.y,
                        c.z + (float) Math.sin(a) * radius);
                if (previous != null) beam(out, pose, previous, point, 0.06f, 0xffffff, flash);
                previous = point;
            }
        }
    }

    /** Beams from the floor of the chamber to the middle, with pulses running up them as fast as the feed. */
    private static void injectors(VertexConsumer out, Matrix4f pose, StellarForgeMachine machine, Vector3f c,
                                  float seconds, int color, float flicker) {
        float feed = machine.feedSetting() / 100.0f;
        if (feed <= 0.0f) return;
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 0.5 * i + Math.PI * 0.25;
            Vector3f base = new Vector3f(c.x + (float) Math.cos(a) * 1.3f, c.y - 1.45f,
                    c.z + (float) Math.sin(a) * 1.3f);
            beam(out, pose, base, c, 0.035f, color, 0.3f * feed * flicker);
            for (int k = 0; k < 2; k++) {
                float t = Mth.frac(seconds * (0.4f + 1.4f * feed) + i * 0.25f + k * 0.5f);
                ContinuumRender.drawBeamPulse(out, pose, base, c, t, 0.1f, 0.06f, flicker * 0.8f);
            }
        }
    }
}
