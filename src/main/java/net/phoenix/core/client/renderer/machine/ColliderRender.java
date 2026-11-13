package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.machine.multiblock.collider.AntimatterColliderMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class ColliderRender extends DynamicRender<AntimatterColliderMachine, ColliderRender> {

    public static final ColliderRender INSTANCE = new ColliderRender();

    private static final net.phoenix.core.client.fx.FxRender FX = net.phoenix.core.client.fx.FxHook
            .standard("antimatter_collider", "Antimatter Collider")
            .machine("phoenixcore:antimatter_collider");
    public static final MapCodec<ColliderRender> CODEC = MapCodec.unit(INSTANCE);
    public static final DynamicRenderType<AntimatterColliderMachine, ColliderRender> TYPE = new DynamicRenderType<>(
            CODEC);

    private static final float[] MATTER = { 0.35f, 0.91f, 1f };
    private static final float[] ANTI = { 1f, 0.3f, 0.85f };

    private static final float CROSS = 0.625f;

    private static final class State {

        int collisions = -1;
        float flashAt = -10f;
        int point;
    }

    private static final Map<Long, State> STATES = new HashMap<>();

    private ColliderRender() {}

    @Override
    public @NotNull DynamicRenderType<AntimatterColliderMachine, ColliderRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(AntimatterColliderMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() || net.phoenix.core.client.fx.FxHook.preview(machine);
    }

    @Override
    public void render(AntimatterColliderMachine machine, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean pv = net.phoenix.core.client.fx.FxHook.preview(machine);
        if (!machine.isFormed() && !pv) return;
        float t = net.phoenix.core.client.fx.FxHook.time(FX, (machine.getOffsetTimer() + partialTick) / 20f);
        Vec3 c = GlowDraw.chamber(machine.getFrontFacing(), machine.getBlockPos(), 3.0, 1.5);
        State s = STATES.computeIfAbsent(machine.getBlockPos().asLong(), k -> new State());
        if (STATES.size() > 64) STATES.clear();
        boolean on = machine.lit() || pv;
        float beam = on ? (pv ? 0.8f : machine.beam() / 100f) : 0f;
        float fill = Mth.clamp(machine.trap() / (float) AntimatterColliderMachine.TRAP_CAP, 0f, 1f);
        boolean powerMode = machine.mode() == AntimatterColliderMachine.POWER;

        if (s.collisions >= 0 && machine.collisions() != s.collisions && on) {
            s.flashAt = t;
            s.point = (int) (t * 7f) & 3;
        }
        s.collisions = machine.collisions();

        float cx = (float) c.x;
        float cy = (float) c.y;
        float cz = (float) c.z;
        float major = 2.0f;
        float minor = 1.6f;
        float dim = on ? 1f : 0.25f;

        poseStack.pushPose();
        net.phoenix.core.client.fx.FxHook.scale(FX, poseStack, c);
        Matrix4f m = poseStack.last().pose();
        GlowDraw.Cam cam = GlowDraw.Cam.now(machine.getBlockPos());
        VertexConsumer vc = net.phoenix.core.client.fx.FxHook.glow(FX, m, buffer.getBuffer(GlowDraw.GLOW), c, t);

        int segs = 48;
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs;
            float a1 = Mth.TWO_PI * (i + 1) / segs;
            GlowDraw.ribbon(vc, m, cam, cx + Mth.cos(a0) * major, cy, cz + Mth.sin(a0) * minor,
                    cx + Mth.cos(a1) * major, cy, cz + Mth.sin(a1) * minor, 0.04f, 0.04f, MATTER[0], MATTER[1],
                    MATTER[2], 0.3f * dim, 0.3f * dim);
            GlowDraw.ribbon(vc, m, cam, cx + Mth.cos(a0) * minor, cy, cz + Mth.sin(a0) * major,
                    cx + Mth.cos(a1) * minor, cy, cz + Mth.sin(a1) * major, 0.04f, 0.04f, ANTI[0], ANTI[1], ANTI[2],
                    0.3f * dim, 0.3f * dim);
        }

        int bunches = on ? 3 + Math.round(beam * 7f) : 1;
        float speed = 0.25f + 1.9f * beam;
        for (int i = 0; i < bunches; i++) {
            float base = i * Mth.TWO_PI / bunches;
            float ta = t * speed + base;
            float tb = -t * speed + base + 0.5f;
            for (int tail = 0; tail < 5; tail++) {
                float back = tail * 0.07f;
                float fade = 1f - tail / 5f;
                GlowDraw.disc(vc, m, cam, cx + Mth.cos(ta - back) * major, cy, cz + Mth.sin(ta - back) * minor,
                        0.1f * fade + 0.02f, MATTER[0], MATTER[1], MATTER[2], 0.7f * fade * dim, 0f);
                GlowDraw.disc(vc, m, cam, cx + Mth.cos(tb + back) * minor, cy,
                        cz + Mth.sin(tb + back) * major, 0.1f * fade + 0.02f, ANTI[0], ANTI[1], ANTI[2],
                        0.7f * fade * dim, 0f);
            }
        }

        for (int q = 0; q < 4; q++) {
            float px = cx + (q % 2 == 0 ? 1f : -1f) * CROSS * major * 1.0f;
            float pz = cz + (q < 2 ? 1f : -1f) * CROSS * major * 1.0f;
            GlowDraw.ring(vc, m, px, cy, pz, GlowDraw.X, GlowDraw.Z, 0.16f, 0.02f, 0f, Mth.TWO_PI, 16, 0.95f, 0.95f,
                    1f, 0.25f * dim);
        }
        float since = t - s.flashAt;
        if (since >= 0f && since < 0.7f) {
            float f = since / 0.7f;
            float px = cx + (s.point % 2 == 0 ? 1f : -1f) * CROSS * major;
            float pz = cz + (s.point < 2 ? 1f : -1f) * CROSS * major;
            GlowDraw.disc(vc, m, cam, px, cy, pz, 0.5f * (1f - f) + 0.1f, 1f, 1f, 1f, 0.9f * (1f - f), 0f);
            for (int i = 0; i < 14; i++) {
                float a = i * 0.449f + s.point;
                float len = (0.15f + 0.9f * f) * (0.5f + GlowDraw.noise(i, 5));
                float[] col = i % 2 == 0 ? MATTER : ANTI;
                GlowDraw.ribbon(vc, m, cam, px, cy, pz, px + Mth.cos(a) * len, cy + (GlowDraw.noise(i, 9) - 0.5f) * len,
                        pz + Mth.sin(a) * len, 0.03f, 0.005f, col[0], col[1], col[2], 0.8f * (1f - f), 0f);
            }
        }

        float r = 0.1f + 0.3f * (float) Math.sqrt(fill);
        GlowDraw.disc(vc, m, cam, cx, cy, cz, r * 2.2f, ANTI[0], ANTI[1], ANTI[2], 0.25f * dim * (0.4f + fill), 0f);
        GlowDraw.disc(vc, m, cam, cx, cy, cz, r, 1f, 0.95f, 1f, 0.9f * dim, 0.3f * dim);
        if (on && machine.emitting() > 0 && powerMode) {
            for (int k = 0; k < 10; k++) {
                float a = k * Mth.TWO_PI / 10f + t * 0.4f;
                float pulse = 0.5f + 0.5f * Mth.sin(t * 6f + k);
                GlowDraw.ribbon(vc, m, cam, cx, cy, cz, cx + Mth.cos(a) * 1.3f, cy, cz + Mth.sin(a) * 1.3f, 0.07f,
                        0.01f, 1f, 0.88f, 0.55f, 0.6f * pulse, 0f);
            }
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AntimatterColliderMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 80;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(AntimatterColliderMachine m) {
        return new AABB(m.getBlockPos()).inflate(32);
    }
}
