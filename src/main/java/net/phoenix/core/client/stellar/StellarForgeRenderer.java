package net.phoenix.core.client.stellar;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.phoenix.continuum.api.client.ContinuumRender;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.gui.hud.GuiDraw;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachine;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws what a Stellar Forge is making inside its chamber: a gas cloud, a protostar, a star or a black hole, held by
 * rings of containment field that turn with the spin and glow with the field, and fed by beams from the floor that
 * run with the feed. It is drawn before the translucent blocks, so the glass of the chamber is in front of it.
 *
 * <p>
 * A black hole grows with the mass it has swallowed and its disk gets fiercer with it and with the feed. When a star falls
 * in it is a scene of about nine seconds: the star's heartbeat quickens and it shudders and brightens, the field tightens
 * round it as it is squeezed to a point, the screen whites out in the flash, and rings of light are thrown out as a black
 * hole opens, furious at first, then settling.
 */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class StellarForgeRenderer {

    private static final double RANGE = 64.0;
    private static long lastReport;

    private static final float INSTABILITY = 0.25f;
    private static final float FLASH = StellarForgeClient.Anim.COLLAPSE_FLASH_AT;

    private static final int DONE_FLASH = 1;
    private static final int DONE_OPEN = 2;

    /** What a black hole looks like right now. */
    private record HoleLook(float radius, int hot, int cool, float intensity, float feeding) {}

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
        StellarCinematic.frame();
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
            float p = falling ? Mth.clamp(anim.collapse / StellarForgeClient.Anim.COLLAPSE_SECONDS, 0.0f, 1.0f) : 1.0f;
            boolean before = falling && p < FLASH;
            float pA = before ? Mth.clamp(p / INSTABILITY, 0.0f, 1.0f) : falling ? 1.0f : 0.0f;
            float pB = before ? Mth.clamp((p - INSTABILITY) / (FLASH - INSTABILITY), 0.0f, 1.0f) : falling ? 1.0f : 0.0f;
            float after = falling && !before ? Mth.clamp((p - FLASH) / (1.0f - FLASH), 0.0f, 1.0f) : falling ? 0.0f : 1.0f;
            float squeeze = (float) Math.pow(pB, 2.2);
            float distance = (float) centre.length();
            Vec3 world = machine.chamberCentre();

            int holeTint = machine.holeTint();
            var shownStage = before ? anim.collapseFrom : stage;
            int color = shownStage == StellarForgeState.Stage.STAR && machine.starTint() >= 0 ? machine.starTint() :
                    shownStage == StellarForgeState.Stage.BLACK_HOLE && holeTint >= 0 ? holeTint :
                            StellarForgeState.colorOf(anim.heat);
            if (before) {
                // it brightens and goes blue-white as it tears in
                int blue = GuiDraw.mix(0xFF_000000 | color, 0xFF_A8C8FF, 0.55f * pB) & 0xFFFFFF;
                color = GuiDraw.mix(0xFF_000000 | blue, 0xFF_FFFFFF, 0.3f * pA + 0.7f * squeeze) & 0xFFFFFF;
            }

            Vector3f at = new Vector3f((float) centre.x, (float) centre.y, (float) centre.z);
            if (before) {
                float shake = 0.02f * (0.4f * pA + pB);
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
            float ringScale = 1.0f;
            if (falling) {
                if (before) {
                    // the field winds up, shivers, then draws tight round the star
                    spin = Math.min(1.0f, spin + 0.4f * pA + 1.2f * pB);
                    field = Math.min(1.0f, field + 0.5f * pA + 0.5f * pB);
                    level = Math.max(level, 0.6f + 0.4f * pA + 0.8f * pB);
                    ringScale = 1.0f + 0.06f * pA * (float) Math.sin(seconds * 14.0f) - 0.65f * squeeze;
                } else {
                    // and springs out past where it was when it lets go, then settles
                    float wind = (1.0f - after) * (1.0f - after);
                    spin = Math.min(1.0f, spin + wind * 1.2f);
                    field = Math.min(1.0f, field + wind);
                    level = Math.max(level, 1.4f * wind);
                    ringScale = 0.35f + 0.65f * backOut(Mth.clamp(after / 0.5f, 0.0f, 1.0f));
                }
            }

            if (falling) collapseSounds(mc, anim, machine, world, seconds, before, pA, pB, after);
            reportToPlayer(distance, before, pA, pB, p, after);

            VertexConsumer lines = buffers.getBuffer(StellarBeamRenderType.BEAMS);
            containment(lines, pose, at, spin, field, seconds, color, flicker * level, anim.flash, ringScale);
            injectors(lines, pose, machine, at, seconds, color, flicker * level);
            if (before && pB > 0.0f) inwardStreaks(lines, pose, at, seconds, pB, color);
            if (falling && !before) shockwave(lines, pose, at, after);
            buffers.endBatch(StellarBeamRenderType.BEAMS);

            if (falling) {
                if (before) {
                    // the old body: shuddering, then squeezed down to a point and whitened
                    float shudder = 1.0f + 0.10f * pA * (float) Math.sin(seconds * (10.0f + 30.0f * pA));
                    float r = anim.collapseRadius * shudder * (1.0f - 0.95f * squeeze);
                    drawBody(anim.collapseFrom, Math.max(0.015f, r), color, null, view, projection, at);
                } else if (after < 0.07f) {
                    // the flash: a point of white where it was
                    drawBody(StellarForgeState.Stage.STAR, 0.05f, 0xFFFFFF, null, view, projection, at);
                } else {
                    // the black hole opening: it swells from nothing and its disk is furious, then settles
                    float open = Mth.clamp((after - 0.07f) / 0.65f, 0.0f, 1.0f);
                    float grow = 1.0f - (1.0f - open) * (1.0f - open) * (1.0f - open);
                    float boost = (float) Math.pow(1.0f - Mth.clamp((after - 0.07f) / 0.8f, 0.0f, 1.0f), 1.5);
                    drawBody(StellarForgeState.Stage.BLACK_HOLE, 0.0f, color, holeLook(machine, anim, holeTint, grow, boost),
                            view, projection, at);
                }
            } else if (stage == StellarForgeState.Stage.BLACK_HOLE) {
                drawBody(stage, 0.0f, color, holeLook(machine, anim, holeTint, 1.0f, 0.0f), view, projection, at);
            } else {
                drawBody(stage, radius, color, null, view, projection, at);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ the black hole

    /**
     * A black hole's look: it grows with the mass it has swallowed (a log scale, so it keeps growing visibly), and its disk
     * gets fiercer with that mass and with the feed: brighter, bluer-white in the middle, reaching further and flaring.
     * {@code grow} scales the size (0 to 1, for it opening) and {@code boost} (0 to 1) is extra fury, for the first moments.
     */
    private static HoleLook holeLook(StellarForgeMachine machine, StellarForgeClient.Anim anim, int tint, float grow,
                                     float boost) {
        float g = StellarForgeClient.Anim.holeGrowth(machine.mass());
        float feedNow = machine.powerOk() ? machine.feedSetting() / 100.0f : 0.0f;
        float feeding = Mth.clamp(0.2f + 0.55f * g + 0.35f * feedNow + 0.3f * anim.surge + boost, 0.0f, 1.0f);
        float intensity = 0.9f + 1.4f * g + 0.5f * feedNow + anim.surge + 2.5f * boost;
        int hot;
        int cool;
        if (tint >= 0) {
            hot = tint;
            cool = darken(tint, 0.55f);
        } else {
            hot = GuiDraw.mix(0xFF_FFDB99, 0xFF_DCEAFF, g) & 0xFFFFFF;
            cool = GuiDraw.mix(0xFF_F25214, 0xFF_FF8A3A, g) & 0xFFFFFF;
        }
        // the disk reaches 35% further at full feeding: draw the body smaller to match, so it stays inside the chamber
        float radius = Math.max(0.02f, anim.hole * 0.9f * grow / (1.0f + 0.35f * feeding));
        return new HoleLook(radius, hot, cool, intensity, feeding);
    }

    private static int darken(int rgb, float keep) {
        return ((int) (((rgb >> 16) & 0xFF) * keep) << 16) | ((int) (((rgb >> 8) & 0xFF) * keep) << 8) |
                (int) ((rgb & 0xFF) * keep);
    }

    /**
     * One body of a stage at a size the chamber can hold. The cloud's and protostar's glow and the black hole's disk are
     * flat pictures that cannot be cut off by the walls, so they are kept inside them (about 1.35 blocks from the middle,
     * the chamber being 1.5): bigger and they draw over the casing. The star is a real sphere and can be larger.
     */
    private static void drawBody(StellarForgeState.Stage stage, float radius, int color, HoleLook hole, Matrix4f view,
                                 Matrix4f projection, Vector3f at) {
        // the beams just drawn leave the depth test off, and the bodies' spheres need it on to hide behind blocks
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        // drawn before the translucent blocks: write depth where lit, so water and glass behind do not show over it
        ContinuumRender.beginWorldObject(view, projection, at, true);
        switch (stage) {
            case GAS_CLOUD -> ContinuumRender.drawGasCloud(color, view, projection, at,
                    0.045f + 0.039f * Mth.clamp((radius - 0.3f) / 0.9f, 0.0f, 1.0f));
            case PROTOSTAR -> ContinuumRender.drawProtostar(color, view, projection, at,
                    0.05f + 0.025f * Mth.clamp((radius - 0.25f) / 0.3f, 0.0f, 1.0f));
            case STAR -> ContinuumRender.drawStar(null, color, view, projection, at, radius * 1.2f);
            case BLACK_HOLE -> {
                if (hole != null) {
                    ContinuumRender.drawBlackHole(hole.hot(), hole.cool(), 1.0f, hole.intensity(), hole.feeding(), view,
                            projection, at, hole.radius());
                }
            }
            default -> {}
        }
        ContinuumRender.endWorldObject();
    }

    // ------------------------------------------------------------------------------------------------ the collapse

    private static float backOut(float t) {
        float c = 1.70158f;
        float u = t - 1.0f;
        return 1.0f + (c + 1.0f) * u * u * u + c * u * u;
    }

    /** The heartbeat that quickens, the blast, the thunder and the low note as the hole opens; and the particles. */
    private static void collapseSounds(Minecraft mc, StellarForgeClient.Anim anim, StellarForgeMachine machine, Vec3 world,
                                       float seconds, boolean before, float pA, float pB, float after) {
        var level = mc.level;
        if (level == null) return;
        if (before) {
            float dread = Mth.clamp(0.4f * pA + pB, 0.0f, 1.0f);
            if (seconds >= anim.nextBeat) {
                level.playLocalSound(world.x, world.y, world.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS,
                        2.0f + 2.0f * dread, 0.7f + 0.7f * dread, false);
                anim.nextBeat = seconds + 1.0f - 0.8f * dread;
            }
            if (pB > 0.0f) {
                // dust pouring into the point
                int count = 1 + (int) (8.0f * pB);
                for (int i = 0; i < count; i++) {
                    double a = level.random.nextDouble() * Math.PI * 2.0;
                    double b = (level.random.nextDouble() - 0.5) * Math.PI;
                    double r = 1.4;
                    double dx = Math.cos(a) * Math.cos(b) * r;
                    double dy = Math.sin(b) * r;
                    double dz = Math.sin(a) * Math.cos(b) * r;
                    level.addParticle(ParticleTypes.REVERSE_PORTAL, world.x + dx, world.y + dy, world.z + dz, -dx * 0.5,
                            -dy * 0.5, -dz * 0.5);
                }
            }
            return;
        }
        if ((anim.collapseDone & DONE_FLASH) == 0) {
            anim.collapseDone |= DONE_FLASH;
            level.playLocalSound(world.x, world.y, world.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f,
                    0.55f, false);
            level.playLocalSound(world.x, world.y, world.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 5.0f,
                    0.45f, false);
            level.addParticle(ParticleTypes.FLASH, world.x, world.y, world.z, 0.0, 0.0, 0.0);
            for (int i = 0; i < 60; i++) {
                double a = level.random.nextDouble() * Math.PI * 2.0;
                double b = (level.random.nextDouble() - 0.5) * Math.PI;
                double speed = 0.15 + level.random.nextDouble() * 0.5;
                level.addParticle(ParticleTypes.END_ROD, world.x, world.y, world.z, Math.cos(a) * Math.cos(b) * speed,
                        Math.sin(b) * speed, Math.sin(a) * Math.cos(b) * speed);
            }
        }
        if (after > 0.1f && (anim.collapseDone & DONE_OPEN) == 0) {
            anim.collapseDone |= DONE_OPEN;
            level.playLocalSound(world.x, world.y, world.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2.5f, 0.4f,
                    false);
        }
    }

    /** The camera shakes and the screen whites out for a player near the collapse, less the further away. */
    private static void reportToPlayer(float distance, boolean before, float pA, float pB, float p, float after) {
        float near = Mth.clamp(1.0f - distance / 14.0f, 0.0f, 1.0f);
        float close = Mth.clamp(1.0f - distance / 10.0f, 0.0f, 1.0f);
        float shake;
        float white;
        if (before) {
            shake = 0.08f * pA + 0.5f * pB;
            white = 0.12f * pB * pB * pB;
        } else {
            float blast = Mth.clamp(1.0f - after / 0.35f, 0.0f, 1.0f);
            shake = Math.max(0.9f * (1.0f - after) * (1.0f - after), blast);
            white = Mth.clamp(1.0f - (p - FLASH) / 0.05f, 0.0f, 1.0f);
        }
        StellarCinematic.report(shake * near, white * close);
    }

    /** Streaks of light drawn in from the walls to the point as the star is squeezed. */
    private static void inwardStreaks(VertexConsumer out, Matrix4f pose, Vector3f c, float seconds, float pB, int color) {
        int n = 14;
        for (int i = 0; i < n; i++) {
            // evenly spread directions, turning slowly
            double y = 1.0 - 2.0 * (i + 0.5) / n;
            double r = Math.sqrt(1.0 - y * y);
            double a = i * 2.399963 + seconds * 0.6;
            Vector3f from = new Vector3f(c.x + (float) (Math.cos(a) * r) * 1.35f, c.y + (float) y * 1.35f,
                    c.z + (float) (Math.sin(a) * r) * 1.35f);
            beam(out, pose, from, c, 0.03f, color, 0.5f * pB);
            float t = Mth.frac(seconds * (2.5f + 4.0f * pB) + i * 0.071f);
            ContinuumRender.drawBeamPulse(out, pose, from, c, t, 0.14f, 0.07f, pB);
        }
    }

    /** Rings of light thrown out when it falls in: three, one after the other, fading as they go. */
    private static void shockwave(VertexConsumer out, Matrix4f pose, Vector3f c, float t) {
        for (int ring = 0; ring < 3; ring++) {
            float local = Mth.clamp((t - 0.01f * ring * 10.0f * 0.8f) * 1.7f, 0.0f, 1.0f);
            if (local <= 0.0f) continue;
            float radius = 0.3f + local * (ring == 0 ? 4.5f : ring == 1 ? 3.4f : 2.5f);
            float strength = (1.0f - local) * (ring == 0 ? 1.0f : ring == 1 ? 0.7f : 0.5f);
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
                    beam(out, pose, previous, point, 0.06f * (1.0f - local * 0.5f), 0xcfe0ff, strength);
                }
                previous = point;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ the beams

    /** A beam as light: a faint wide halo under a bright core, so it stays smooth and visible at a distance. */
    private static void beam(VertexConsumer out, Matrix4f pose, Vector3f a, Vector3f b, float width, int color,
                             float strength) {
        ContinuumRender.drawBeam(out, pose, a, b, width * 2.6f, color, strength * 0.12f);
        ContinuumRender.drawBeam(out, pose, a, b, width, color, strength);
    }

    /** Three tilted rings of field round the body, turning with the spin, brighter with the field. */
    private static void containment(VertexConsumer out, Matrix4f pose, Vector3f c, float spin, float field,
                                    float seconds, int color, float flicker, float flash, float scale) {
        float strength = (0.25f + 0.75f * field) * flicker;
        float turn = seconds * (0.4f + 2.2f * spin);
        for (int ring = 0; ring < 3; ring++) {
            float tilt = ring * 1.05f - 1.05f;
            float radius = (1.25f - ring * 0.08f) * scale + flash * 0.4f * (ring + 1);
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
