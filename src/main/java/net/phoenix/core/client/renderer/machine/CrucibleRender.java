package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.machine.multiblock.electric.AetherCrucibleMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class CrucibleRender extends DynamicRender<AetherCrucibleMachine, CrucibleRender> {

    public static final CrucibleRender INSTANCE = new CrucibleRender();

    private static final net.phoenix.core.client.fx.FxRender FX = net.phoenix.core.client.fx.FxHook
            .standard("aether_crucible", "Aether Crucible").machine("phoenixcore:aether_crucible");
    public static final MapCodec<CrucibleRender> CODEC = MapCodec.unit(INSTANCE);
    public static final DynamicRenderType<AetherCrucibleMachine, CrucibleRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final float[] AETHER = { 0.43f, 0.94f, 0.78f };
    private static final float[] GOLD = { 1f, 0.82f, 0.29f };

    private static final Map<Long, float[]> WORK = new HashMap<>();

    private CrucibleRender() {
        net.phoenix.core.client.fx.FxStage.register(AetherCrucibleMachine.class, 64.0,
                (m, partial, ps, buf) -> INSTANCE.render((AetherCrucibleMachine) m, partial, ps, buf, 0, 0));
    }

    @Override
    public @NotNull DynamicRenderType<AetherCrucibleMachine, CrucibleRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(AetherCrucibleMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() || net.phoenix.core.client.fx.FxHook.preview(machine);
    }

    @Override
    public void render(AetherCrucibleMachine machine, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean pv = net.phoenix.core.client.fx.FxHook.preview(machine);
        if (!machine.isFormed() && !pv) return;
        float t = net.phoenix.core.client.fx.FxHook.time(FX, (machine.getOffsetTimer() + partialTick) / 20f);
        Vec3 c = GlowDraw.chamber(machine.getFrontFacing(), machine.getBlockPos(), 1.0, 0.0);
        float[] w = WORK.computeIfAbsent(machine.getBlockPos().asLong(), k -> new float[] { 0f, t });
        if (WORK.size() > 64) WORK.clear();
        float dt = Math.min(0.2f, Math.max(0f, t - w[1]));
        w[1] = t;
        w[0] = Mth.clamp(
                w[0] + (((pv || machine.getRecipeLogic().isWorking()) ? 1f : 0f) - w[0]) * Math.min(1f, dt * 1.8f), 0f,
                1f);
        float work = w[0];
        float[] col = GlowDraw.lerp3(AETHER, GOLD, work);

        float cx = (float) c.x;
        float cz = (float) c.z;
        float surface = -0.68f + 0.02f * Mth.sin(t * 2f);

        poseStack.pushPose();
        net.phoenix.core.client.fx.FxHook.scale(FX, poseStack, c);
        Matrix4f m = poseStack.last().pose();
        GlowDraw.Cam cam = GlowDraw.Cam.now(machine.getBlockPos());
        net.minecraft.client.renderer.ShaderInstance sh = net.phoenix.core.client.worldfx.WorldFXShaders.FX_CRUCIBLE;
        if (sh != null) {
            if (!net.phoenix.core.client.fx.FxStage.active()) {
                poseStack.popPose();
                return;
            }
            var look = new FxVolume.Look(t, FX.get("power", 1f), (machine.getBlockPos().asLong() & 1023L) / 100f,
                    net.phoenix.core.client.fx.FxHook.tint(FX, AETHER[0], AETHER[1], AETHER[2], 1f, 0.3f, t),
                    net.phoenix.core.client.fx.FxHook.tint(FX, GOLD[0], GOLD[1], GOLD[2], 1f, 0.7f, t));
            float[] p0 = { work, 1f, 0f, 0f };
            float[] p1 = { 0f, 0f, 0f, 0f };
            FxVolume.draw(sh, m, FX.get("detail", 1f), cx, -0.05f, cz, 1.0f, 1.45f, 1.0f, FX.get("size", 1f), look, p0,
                    p1);
            poseStack.popPose();
            return;
        }
        VertexConsumer vc = net.phoenix.core.client.fx.FxHook.glow(FX, m, buffer.getBuffer(GlowDraw.GLOW), c, t);

        GlowDraw.flatDisc(vc, m, cx, surface, cz, 0.55f, col[0], col[1], col[2], 0.55f + 0.3f * work, 0.2f);
        for (int k = 0; k < 3; k++) {
            float f = (t * 0.5f + k / 3f) % 1f;
            GlowDraw.ring(vc, m, cx, surface + 0.01f, cz, GlowDraw.X, GlowDraw.Z, 0.1f + 0.45f * f, 0.03f, 0f,
                    Mth.TWO_PI, 24, 1f, 1f, 0.95f, 0.5f * (1f - f));
        }

        for (int i = 0; i < 7; i++) {
            float life = (t * (0.25f + 0.3f * work) + i * 0.173f) % 1f;
            float a = i * 2.399f;
            GlowDraw.disc(vc, m, cam, cx + Mth.cos(a) * 0.3f * (1f - 0.4f * life), surface + life * 1.6f,
                    cz + Mth.sin(a) * 0.3f * (1f - 0.4f * life), 0.03f + 0.02f * Mth.sin(i), 1f, 1f, 0.95f,
                    0.8f * (1f - life), 0f);
        }

        float oy = 0.55f + 0.06f * Mth.sin(t * 1.7f);
        float orb = 0.2f + 0.1f * work + 0.02f * Mth.sin(t * 5f);
        GlowDraw.disc(vc, m, cam, cx, oy, cz, orb * 3f, col[0], col[1], col[2], 0.22f + 0.2f * work, 0f);
        GlowDraw.disc(vc, m, cam, cx, oy, cz, orb, 1f, 1f, 1f, 0.9f, 0.35f);
        int motes = 6 + Math.round(8f * work);
        for (int i = 0; i < motes; i++) {
            float a = t * (0.8f + 0.7f * work) + i * Mth.TWO_PI / motes;
            float r = 0.38f + 0.12f * Mth.sin(i * 1.7f + t);
            float y = oy + Mth.sin(a * 2f + i) * 0.35f;
            GlowDraw.disc(vc, m, cam, cx + Mth.cos(a) * r, y, cz + Mth.sin(a) * r * 0.9f, 0.04f, col[0], col[1], col[2],
                    0.9f, 0f);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AetherCrucibleMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(AetherCrucibleMachine m) {
        return new AABB(m.getBlockPos()).inflate(16);
    }
}
