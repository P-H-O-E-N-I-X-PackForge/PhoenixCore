package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.machine.multiblock.nullwell.NullWellMachine;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class NullWellRender extends DynamicRender<NullWellMachine, NullWellRender> {

    public static final NullWellRender INSTANCE = new NullWellRender();

    private static final net.phoenix.core.client.fx.FxRender FX = net.phoenix.core.client.fx.FxHook
            .standard("null_well", "Null Well").machine("phoenixcore:null_well");
    public static final MapCodec<NullWellRender> CODEC = MapCodec.unit(INSTANCE);
    public static final DynamicRenderType<NullWellMachine, NullWellRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final float[] LILAC = { 0.79f, 0.55f, 1.0f };
    private static final float[] AMBER = { 1.0f, 0.77f, 0.30f };
    private static final float[] RED = { 1.0f, 0.29f, 0.35f };
    private static final float[] HOT = { 1.0f, 0.93f, 0.78f };

    private NullWellRender() {
        net.phoenix.core.client.fx.FxStage.register(NullWellMachine.class, 96.0,
                (m, partial, ps, buf) -> INSTANCE.render((NullWellMachine) m, partial, ps, buf, 0, 0));
    }

    @Override
    public @NotNull DynamicRenderType<NullWellMachine, NullWellRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(NullWellMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() || net.phoenix.core.client.fx.FxHook.preview(machine);
    }

    private static float[] fieldColour(float stability) {
        if (stability >= 66f) return LILAC;
        if (stability >= 33f) return GlowDraw.lerp3(AMBER, LILAC, (stability - 33f) / 33f);
        return GlowDraw.lerp3(RED, AMBER, stability / 33f);
    }

    @Override
    public void render(NullWellMachine machine, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean pv = net.phoenix.core.client.fx.FxHook.preview(machine);
        if (!machine.isFormed() && !pv) return;
        boolean on = machine.lit() || pv;

        float dim = on ? 1f : 0.2f;
        float t = net.phoenix.core.client.fx.FxHook.time(FX, (machine.getOffsetTimer() + partialTick) / 20f);
        Vec3 c = GlowDraw.chamber(machine.getFrontFacing(), machine.getBlockPos(), 3.0, 3.5);
        float mass = pv ? 0.45f : (float) (machine.mass() / NullWellMachine.LIMIT);
        float stability = pv ? 90f : machine.stability();
        float feed = machine.feedSetting() / 100f;
        float draw = machine.drawSetting() / 100f;
        float[] col = fieldColour(stability);
        float shake = stability < 40f ? (40f - stability) / 40f * 0.05f : 0f;
        float cx = (float) c.x + Mth.sin(t * 43f) * shake;
        float cy = (float) c.y + Mth.cos(t * 37f) * shake;
        float cz = (float) c.z;
        float radius = 0.16f + 0.5f * (float) Math.cbrt(Math.max(0f, mass));

        poseStack.pushPose();
        net.phoenix.core.client.fx.FxHook.scale(FX, poseStack, c);
        Matrix4f m = poseStack.last().pose();
        GlowDraw.Cam cam = GlowDraw.Cam.now(machine.getBlockPos());

        if (net.phoenix.core.client.worldfx.WorldFXShaders.FX_DISC != null) {
            if (net.phoenix.core.client.fx.FxStage.active()) {
                drawDisc(m, machine.getFrontFacing(), cx, cy, cz, radius, col, t, energyOf(feed, draw, dim), feed,
                        on && draw > 0.01f && machine.emitting() > 0 ? draw : 0f, dim,
                        machine.getBlockPos().asLong());
            }
            poseStack.popPose();
            return;
        }

        float toCamX = cam.px() - cx;
        float toCamY = cam.py() - cy;
        float toCamZ = cam.pz() - cz;
        float camDist = Math.max(radius * 1.5f, Mth.sqrt(toCamX * toCamX + toCamY * toCamY + toCamZ * toCamZ));
        float shadowR = radius * camDist / Mth.sqrt(camDist * camDist + radius * radius);
        VertexConsumer dark = buffer.getBuffer(GlowDraw.DARK);
        GlowDraw.sphere(dark, m, cx, cy, cz, shadowR * 0.995f, 18, 36, 0f, 0f, 0f, 1f);

        VertexConsumer vc = net.phoenix.core.client.fx.FxHook.glow(FX, m, buffer.getBuffer(GlowDraw.GLOW), c, t);

        GlowDraw.disc(vc, m, cam, cx, cy, cz, radius * 4.2f, col[0], col[1], col[2], 0.16f * (0.5f + mass) * dim, 0f);

        GlowDraw.ring(vc, m, cx, cy, cz, new float[] { cam.rx(), cam.ry(), cam.rz() },
                new float[] { cam.ux(), cam.uy(), cam.uz() }, radius * 1.14f, radius * 0.24f, 0f, Mth.TWO_PI, 48, 1f,
                0.9f, 1f, 0.12f * dim);

        GlowDraw.ring(vc, m, cx, cy, cz, new float[] { cam.rx(), cam.ry(), cam.rz() },
                new float[] { cam.ux(), cam.uy(), cam.uz() }, radius * 1.1f, radius * 0.06f, 0f, Mth.TWO_PI, 64, 1f,
                0.95f, 1f, 0.85f * dim);

        float p = t * 0.25f;
        float tilt = 1.05f;
        float[] u = { Mth.cos(p), 0f, Mth.sin(p) };
        float[] w = { -Mth.sin(p), 0f, Mth.cos(p) };
        float[] v = { w[0] * Mth.cos(tilt), Mth.sin(tilt), w[2] * Mth.cos(tilt) };
        float spin = t * (0.6f + 2.2f * feed);
        float energy = (0.55f + 0.45f * Math.min(1f, feed + draw + 0.3f)) * dim;
        GlowDraw.annulus(vc, m, cx, cy, cz, u, v, radius * 1.45f, radius * 3.6f, 0f, Mth.TWO_PI, 14, 96,
                (angle, out) -> {
                    float arm = 0.5f + 0.5f * Mth.sin(2f * angle - 7f * out + t * 2.5f);
                    float dop = 0.5f + 0.5f * Mth.cos(angle - spin);
                    float heat = (float) Math.pow(1f - out, 1.3f);
                    float a = (0.2f + 0.5f * arm + 0.3f * dop) * (1f - 0.85f * out) * energy * 0.55f;
                    float[] rgb = GlowDraw.lerp3(col, HOT, heat * 0.85f);
                    return new float[] { rgb[0], rgb[1], rgb[2], a };
                });

        float nrmX = u[1] * v[2] - u[2] * v[1];
        float nrmY = u[2] * v[0] - u[0] * v[2];
        float nrmZ = u[0] * v[1] - u[1] * v[0];
        float onScreenX = nrmX * cam.rx() + nrmY * cam.ry() + nrmZ * cam.rz();
        float onScreenY = nrmX * cam.ux() + nrmY * cam.uy() + nrmZ * cam.uz();
        float lensAxis = (float) Math.atan2(onScreenY, onScreenX);
        float lensEdgeOn = Math.min(1f, Mth.sqrt(onScreenX * onScreenX + onScreenY * onScreenY));
        GlowDraw.annulus(vc, m, cx, cy, cz, new float[] { cam.rx(), cam.ry(), cam.rz() },
                new float[] { cam.ux(), cam.uy(), cam.uz() }, radius * 1.12f, radius * 1.8f, 0f, Mth.TWO_PI, 6, 96,
                (angle, out) -> {
                    float along = Mth.cos(angle - lensAxis);
                    float lensW = Mth.lerp(lensEdgeOn, 1f, along * along);
                    float fall = (float) Math.pow(1f - out, 2.2f);
                    float[] rgb = GlowDraw.lerp3(col, HOT, 0.35f + 0.5f * fall);
                    return new float[] { rgb[0], rgb[1], rgb[2], 0.55f * lensW * fall * energy };
                });

        int falling = on ? 6 + Math.round(feed * 14f) : 0;
        for (int i = 0; i < falling; i++) {
            float u0 = (t * (0.18f + 0.3f * feed) + i * 0.381966f) % 1f;
            float a = i * 2.399f + u0 * 5f;
            float d = Mth.lerp(u0 * u0, radius * 3.4f, radius * 1.15f);
            float px = cx + (u[0] * Mth.cos(a) + v[0] * Mth.sin(a)) * d;
            float py = cy + (u[1] * Mth.cos(a) + v[1] * Mth.sin(a)) * d;
            float pz = cz + (u[2] * Mth.cos(a) + v[2] * Mth.sin(a)) * d;
            GlowDraw.disc(vc, m, cam, px, py, pz, 0.035f + 0.03f * (1f - u0), 1f, 0.95f, 0.85f, 0.8f * (1f - u0 * 0.4f),
                    0f);
        }

        if (on && draw > 0.01f && machine.emitting() > 0) {
            for (int s = -1; s <= 1; s += 2) {
                float len = 0.6f + 1.6f * draw;
                GlowDraw.ribbon(vc, m, cam, cx, cy + s * radius * 0.9f, cz, cx, cy + s * (radius + len), cz,
                        radius * 0.25f, 0.01f, 0.5f, 0.9f, 1f, 0.55f * draw, 0f);
                for (int i = 0; i < 6; i++) {
                    float u1 = (t * 0.9f + i / 6f) % 1f;
                    GlowDraw.disc(vc, m, cam, cx + Mth.sin(t * 5f + i) * 0.04f, cy + s * (radius + len * u1), cz,
                            0.05f * (1f - u1), 0.8f, 0.95f, 1f, 0.7f * (1f - u1), 0f);
                }
            }
        }
        poseStack.popPose();
    }

    private static float energyOf(float feed, float draw, float dim) {
        return (0.55f + 0.45f * Math.min(1f, feed + draw + 0.3f)) * dim;
    }

    private static boolean drawDisc(Matrix4f m, net.minecraft.core.Direction facing, float cx, float cy, float cz,
                                    float radius,
                                    float[] col, float t, float energy, float feed, float draw, float dim, long seed) {
        net.minecraft.client.renderer.ShaderInstance shader = net.phoenix.core.client.worldfx.WorldFXShaders.FX_DISC;
        if (shader == null) return false;

        float half = radius * 4.2f;
        float[] a = net.phoenix.core.client.fx.FxHook.tint(FX, col[0], col[1], col[2], 1f, 0.5f, t);
        float[] b = net.phoenix.core.client.fx.FxHook.tint(FX, HOT[0], HOT[1], HOT[2], 1f, 0.8f, t);

        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("Time").set(t);
        shader.safeGetUniform("Horizon").set(radius / half);
        shader.safeGetUniform("Energy").set(energy);
        shader.safeGetUniform("Feed").set(feed * dim);
        shader.safeGetUniform("Draw").set(draw);
        shader.safeGetUniform("Power").set(FX.get("power", 1f) * (0.35f + 0.65f * dim));
        shader.safeGetUniform("Seed").set((seed & 1023L) / 100f);
        shader.safeGetUniform("ColorA").set(a[0], a[1], a[2]);
        shader.safeGetUniform("ColorB").set(b[0], b[1], b[2]);

        float rx = facing.getStepZ() * half, ry = 0f, rz = -facing.getStepX() * half;
        float ux = 0f, uy = half, uz = 0f;
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        corner(bb, m, cx - rx - ux, cy - ry - uy, cz - rz - uz, 0f, 0f);
        corner(bb, m, cx + rx - ux, cy + ry - uy, cz + rz - uz, 1f, 0f);
        corner(bb, m, cx + rx + ux, cy + ry + uy, cz + rz + uz, 1f, 1f);
        corner(bb, m, cx - rx + ux, cy - ry + uy, cz - rz + uz, 0f, 1f);
        FxVolume.volumeBlended(shader, bb.buildOrThrow(), com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        return true;
    }

    private static void corner(BufferBuilder bb, Matrix4f m, float x, float y, float z, float u, float v) {
        bb.addVertex(m, x, y, z).setUv(u, v);
    }

    @Override
    public boolean shouldRenderOffScreen(NullWellMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(NullWellMachine m) {
        return new AABB(m.getBlockPos()).inflate(24);
    }
}
