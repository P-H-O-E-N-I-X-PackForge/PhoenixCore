package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.machine.multiblock.cortex.SculkCortexMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SculkCortexRender extends DynamicRender<SculkCortexMachine, SculkCortexRender> {

    public static final SculkCortexRender INSTANCE = new SculkCortexRender();

    private static final net.phoenix.core.client.fx.FxRender FX = net.phoenix.core.client.fx.FxHook
            .standard("sculk_cortex", "Sculk Cortex").machine("phoenixcore:sculk_cortex");
    public static final MapCodec<SculkCortexRender> CODEC = MapCodec.unit(INSTANCE);
    public static final DynamicRenderType<SculkCortexMachine, SculkCortexRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final int VEINS = 72;
    private static final int POINTS = 9;
    private static final float HALF = 1.53f;

    private static final class State {

        final float[][][] veins = new float[VEINS][POINTS][3];
        int lastDigested = -1;
        float pulseAt = -10f;
        int pulseVein;
    }

    private static final Map<Long, State> STATES = new HashMap<>();

    private SculkCortexRender() {
        net.phoenix.core.client.fx.FxStage.register(SculkCortexMachine.class, 64.0,
                (m, partial, ps, buf) -> INSTANCE.render((SculkCortexMachine) m, partial, ps, buf, 0, 0));
    }

    @Override
    public @NotNull DynamicRenderType<SculkCortexMachine, SculkCortexRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(SculkCortexMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() || net.phoenix.core.client.fx.FxHook.preview(machine);
    }

    private static State state(long key) {
        State s = STATES.get(key);
        if (s != null) return s;
        s = new State();
        Random r = new Random(key * 31L + 7L);

        float[][] normals = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        float[][] axisA = { { 0, 1, 0 }, { 0, 1, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 } };
        float[][] axisB = { { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 1, 0 }, { 0, 1, 0 } };
        for (int vIdx = 0; vIdx < VEINS; vIdx++) {
            int face = vIdx % 6;
            float u = (r.nextFloat() - 0.5f) * 2.6f;
            float w = (r.nextFloat() - 0.5f) * 2.6f;
            float ang = r.nextFloat() * Mth.TWO_PI;
            for (int k = 0; k < POINTS; k++) {
                for (int d = 0; d < 3; d++) {
                    s.veins[vIdx][k][d] = normals[face][d] * HALF + axisA[face][d] * u + axisB[face][d] * w;
                }
                ang += (r.nextFloat() - 0.5f) * 1.3f;
                u = Mth.clamp(u + Mth.cos(ang) * 0.36f, -1.45f, 1.45f);
                w = Mth.clamp(w + Mth.sin(ang) * 0.36f, -1.45f, 1.45f);
            }
        }
        STATES.put(key, s);
        if (STATES.size() > 64) STATES.clear();
        return s;
    }

    @Override
    public void render(SculkCortexMachine machine, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean pv = net.phoenix.core.client.fx.FxHook.preview(machine);
        if (!machine.isFormed() && !pv) return;
        float t = net.phoenix.core.client.fx.FxHook.time(FX, (machine.getOffsetTimer() + partialTick) / 20f);
        Vec3 c = GlowDraw.chamber(machine.getFrontFacing(), machine.getBlockPos(), 2.0, 2.5);
        State s = state(machine.getBlockPos().asLong());
        boolean on = machine.lit() || pv;
        boolean starving = machine.starving();
        float fed = (float) (machine.hunger() / 100.0);
        int level = machine.level();
        int shown = Math.min(VEINS, 6 + level * 8);
        int nodes = machine.engramCount();

        if (s.lastDigested >= 0 && machine.digested() != s.lastDigested) {
            s.pulseAt = t;
            s.pulseVein = (int) (t * 31f) % VEINS;
        }
        s.lastDigested = machine.digested();

        float[] base = starving ? new float[] { 1f, 0.35f, 0.35f } : new float[] { 0.21f, 0.79f, 0.91f };
        float dim = on ? (0.55f + 0.45f * fed) : 0.18f;

        poseStack.pushPose();
        net.phoenix.core.client.fx.FxHook.scale(FX, poseStack, c);
        Matrix4f m = poseStack.last().pose();
        GlowDraw.Cam cam = GlowDraw.Cam.now(machine.getBlockPos());
        float cx = (float) c.x;
        float cy = (float) c.y;
        float cz = (float) c.z;

        net.minecraft.client.renderer.ShaderInstance sh = net.phoenix.core.client.worldfx.WorldFXShaders.FX_CORTEX;
        if (sh != null) {
            if (!net.phoenix.core.client.fx.FxStage.active()) {
                poseStack.popPose();
                return;
            }
            float since = t - s.pulseAt;
            var look = new FxVolume.Look(t, FX.get("power", 1f), (machine.getBlockPos().asLong() & 1023L) / 100f,
                    net.phoenix.core.client.fx.FxHook.tint(FX, base[0], base[1], base[2], 1f, 0.3f, t),
                    net.phoenix.core.client.fx.FxHook.tint(FX, 0.85f, 1f, 1f, 1f, 0.7f, t));
            FxVolume.draw(sh, m, FX.get("detail", 1f), cx, cy, cz, 1.8f, 1.8f, 1.8f, FX.get("size", 1f), look,
                    new float[] { dim, starving ? 1f : 0f, (shown - 6f) / (VEINS - 6f), 0f },
                    new float[] { on && since >= 0f && since < 1.6f ? since / 1.6f : -1f,
                            Math.min(1f, nodes / 12f), 0f, 0f });
            poseStack.popPose();
            return;
        }
        VertexConsumer vc = net.phoenix.core.client.fx.FxHook.glow(FX, m, buffer.getBuffer(GlowDraw.GLOW), c, t);

        for (int i = 0; i < shown; i++) {
            float[][] pts = s.veins[i];
            float travel = (t * 0.45f + i * 0.137f) % 1f;
            for (int k = 0; k < POINTS - 1; k++) {
                float along = (k + 0.5f) / (POINTS - 1);
                float near = 1f - Math.min(1f, Math.abs(along - travel) * 3.2f);
                float flicker = starving ? 0.55f + 0.45f * Mth.sin(t * 9f + i) : 1f;
                float a = (0.22f + 0.5f * near) * dim * flicker;
                GlowDraw.ribbon(vc, m, cam, cx + pts[k][0], cy + pts[k][1], cz + pts[k][2], cx + pts[k + 1][0],
                        cy + pts[k + 1][1], cz + pts[k + 1][2], 0.05f, 0.05f, base[0], base[1], base[2], a, a);
            }
        }

        for (int n = 0; n < Math.min(nodes, shown); n++) {
            float[] p = s.veins[n][POINTS / 2];
            float tw = 0.7f + 0.3f * Mth.sin(t * 2.2f + n);
            GlowDraw.disc(vc, m, cam, cx + p[0], cy + p[1], cz + p[2], 0.11f, 0.85f, 1f, 1f, 0.85f * dim * tw, 0f);
        }

        float since = t - s.pulseAt;
        if (since >= 0f && since < 2.2f && on) {
            int stage = (int) (since / 0.7f);
            float u = (since - stage * 0.7f) / 0.7f;
            float[][] pts = s.veins[(s.pulseVein + stage * 5) % Math.max(1, shown)];
            float fp = u * (POINTS - 1);
            int k = Math.min(POINTS - 2, (int) fp);
            float f = fp - k;
            float px = Mth.lerp(f, pts[k][0], pts[k + 1][0]);
            float py = Mth.lerp(f, pts[k][1], pts[k + 1][1]);
            float pz = Mth.lerp(f, pts[k][2], pts[k + 1][2]);
            GlowDraw.disc(vc, m, cam, cx + px, cy + py, cz + pz, 0.2f, 1f, 1f, 1f, 0.9f, 0f);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SculkCortexMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(SculkCortexMachine m) {
        return new AABB(m.getBlockPos()).inflate(24);
    }
}
