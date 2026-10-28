package net.phoenix.core.client.stellar;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
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

            int color = StellarForgeState.colorOf(anim.heat);
            Vector3f at = new Vector3f((float) centre.x, (float) centre.y, (float) centre.z);
            float radius = Math.max(0.02f, anim.radius);
            float field = machine.fieldSetting() / 100.0f;
            float flicker = machine.powerOk() ? 1.0f : 0.5f + 0.5f * (float) Math.sin(seconds * 31.0f);
            // once the body is all there and nothing is happening the light settles and breathes; a push flares it
            float calm = 0.16f + 0.05f * (float) Math.sin(seconds * 1.1f);
            float live = Math.max(anim.surge, machine.feedSetting() / 100.0f * 0.7f);
            float level = Mth.lerp(anim.settle, 1.0f, calm + (1.0f - calm) * live);

            VertexConsumer lines = buffers.getBuffer(RenderType.lightning());
            containment(lines, pose, at, machine.spinSetting() / 100.0f, field, seconds, color, flicker * level, anim.flash);
            injectors(lines, pose, machine, at, seconds, color, flicker * level);
            buffers.endBatch(RenderType.lightning());

            switch (stage) {
                case GAS_CLOUD -> ContinuumRender.drawGasCloud(color, view, projection, at, radius);
                case PROTOSTAR -> ContinuumRender.drawProtostar(color, view, projection, at, radius);
                case STAR -> ContinuumRender.drawStar(null, color, view, projection, at, radius);
                case BLACK_HOLE -> {
                    ContinuumRender.beginWorldObject(view, projection, at);
                    ContinuumRender.drawBlackHole(null, view, projection, at, radius);
                    ContinuumRender.endWorldObject();
                }
                default -> {}
            }
        }
    }

    /** Three tilted rings of field round the body, turning with the spin, brighter with the field. */
    private static void containment(VertexConsumer out, Matrix4f pose, Vector3f c, float spin, float field,
                                    float seconds, int color, float flicker, float flash) {
        float strength = (0.25f + 0.75f * field) * flicker;
        float turn = seconds * (0.4f + 2.2f * spin);
        for (int ring = 0; ring < 3; ring++) {
            float tilt = ring * 1.05f - 1.05f;
            float radius = 1.25f - ring * 0.08f + flash * 0.4f * (ring + 1);
            int segments = 56;
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
                    ContinuumRender.drawBeam(out, pose, previous, point, 0.025f, color, strength * fade);
                }
                previous = point;
            }
        }
        if (flash > 0.0f) {
            // a stage change: a ring of light flung out
            int segments = 48;
            Vector3f previous = null;
            float radius = 0.4f + (1.0f - flash) * 1.4f;
            for (int i = 0; i <= segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                Vector3f point = new Vector3f(c.x + (float) Math.cos(a) * radius, c.y,
                        c.z + (float) Math.sin(a) * radius);
                if (previous != null) ContinuumRender.drawBeam(out, pose, previous, point, 0.06f, 0xffffff, flash);
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
            ContinuumRender.drawBeam(out, pose, base, c, 0.03f, color, 0.35f * feed * flicker);
            for (int k = 0; k < 2; k++) {
                float t = Mth.frac(seconds * (0.4f + 1.4f * feed) + i * 0.25f + k * 0.5f);
                ContinuumRender.drawBeamPulse(out, pose, base, c, t, 0.08f, 0.05f, flicker);
            }
        }
    }
}
