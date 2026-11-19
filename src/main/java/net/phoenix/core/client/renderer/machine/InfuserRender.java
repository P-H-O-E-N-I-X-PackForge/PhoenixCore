package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.common.machine.multiblock.infuser.PhoenixInfuserMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The Phoenix Infuser: a column of flame between two wings of fire that open as the heat climbs, with embers rising,
 * ash
 * heaped on the floor, a burst of sparks when something is burned and a golden ring when something is renewed.
 */
public class InfuserRender extends DynamicRender<PhoenixInfuserMachine, InfuserRender> {

    public static final InfuserRender INSTANCE = new InfuserRender();

    /** The settings of /pftfx for this render. */
    private static final net.phoenix.core.client.fx.FxRender FX = net.phoenix.core.client.fx.FxHook
            .standard("phoenix_infuser", "Phoenix Infuser").machine("phoenixcore:phoenix_infuser");
    public static final MapCodec<InfuserRender> CODEC = MapCodec.unit(INSTANCE);
    public static final DynamicRenderType<PhoenixInfuserMachine, InfuserRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final class State {

        int burned = -1;
        int renewed = -1;
        float burstAt = -10f;
        float ringAt = -10f;
    }

    private static final Map<Long, State> STATES = new HashMap<>();

    private InfuserRender() {
        net.phoenix.core.client.fx.FxStage.register(PhoenixInfuserMachine.class, 64.0,
                (m, partial, ps, buf) -> INSTANCE.render((PhoenixInfuserMachine) m, partial, ps, buf, 0, 0));
    }

    @Override
    public @NotNull DynamicRenderType<PhoenixInfuserMachine, InfuserRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(PhoenixInfuserMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() || net.phoenix.core.client.fx.FxHook.preview(machine);
    }

    @Override
    public void render(PhoenixInfuserMachine machine, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean pv = net.phoenix.core.client.fx.FxHook.preview(machine);
        if (!machine.isFormed() && !pv) return;
        float t = net.phoenix.core.client.fx.FxHook.time(FX, (machine.getOffsetTimer() + partialTick) / 20f);
        Vec3 c = GlowDraw.chamber(machine.getFrontFacing(), machine.getBlockPos(), 2.0, 2.5);
        State s = STATES.computeIfAbsent(machine.getBlockPos().asLong(), k -> new State());
        if (STATES.size() > 64) STATES.clear();
        boolean on = machine.lit() || pv;
        float heat = pv ? 0.7f : (float) (machine.heat() / 100.0);
        float flame = on ? 0.25f + 0.75f * heat : 0.05f;
        float ash = Mth.clamp(machine.ash() / (float) PhoenixInfuserMachine.MAX_ASH, 0f, 1f);

        if (s.burned >= 0 && machine.burned() != s.burned) s.burstAt = t;
        if (s.renewed >= 0 && machine.renewed() != s.renewed) s.ringAt = t;
        s.burned = machine.burned();
        s.renewed = machine.renewed();

        float cx = (float) c.x;
        float cz = (float) c.z;
        float floor = 1.03f;

        poseStack.pushPose();
        net.phoenix.core.client.fx.FxHook.scale(FX, poseStack, c);
        Matrix4f m = poseStack.last().pose();
        GlowDraw.Cam cam = GlowDraw.Cam.now(machine.getBlockPos());

        net.minecraft.client.renderer.ShaderInstance sh = net.phoenix.core.client.worldfx.WorldFXShaders.FX_FLAME;
        // with the shader, the stage hook draws everything, so the block-entity pass draws none of it
        if (sh != null && !net.phoenix.core.client.fx.FxStage.active()) {
            poseStack.popPose();
            return;
        }

        // the ash, heaped
        VertexConsumer dark = buffer.getBuffer(GlowDraw.DARK);
        GlowDraw.flatDisc(dark, m, cx, floor + 0.04f * ash, cz, 0.35f + 0.8f * ash, 0.36f, 0.33f, 0.31f, 1f, 1f);
        GlowDraw.flatDisc(dark, m, cx, floor + 0.12f * ash + 0.01f, cz, 0.2f + 0.45f * ash, 0.44f, 0.4f, 0.37f, 1f, 1f);

        if (sh != null) {
            float bAge = t - s.burstAt;
            float rAge = t - s.ringAt;
            var look = new FxVolume.Look(t, FX.get("power", 1f), (machine.getBlockPos().asLong() & 1023L) / 100f,
                    net.phoenix.core.client.fx.FxHook.tint(FX, 1f, 0.5f, 0.15f, 1f, 0.3f, t),
                    net.phoenix.core.client.fx.FxHook.tint(FX, 1f, 0.95f, 0.75f, 1f, 0.7f, t));
            FxVolume.draw(sh, m, FX.get("detail", 1f), cx, floor + 1.5f, cz, 1.9f, 1.8f, 1.9f, FX.get("size", 1f), look,
                    new float[] { flame, on ? 1f : 0.3f, bAge >= 0f && bAge < 0.7f ? bAge / 0.7f : -1f,
                            rAge >= 0f && rAge < 1.1f ? rAge / 1.1f : -1f },
                    new float[] { heat, 0f, 0f, 0f });
            poseStack.popPose();
            return;
        }

        VertexConsumer vc = net.phoenix.core.client.fx.FxHook.glow(FX, m, buffer.getBuffer(GlowDraw.GLOW), c, t);
        if (on || flame > 0.1f) {
            // the flame, in layers rising from the ash, flickering
            float h = 0.5f + 1.7f * flame;
            for (int i = 0; i < 9; i++) {
                float f = i / 8f;
                float flick = 0.88f + 0.12f * Mth.sin(t * 9f + i * 1.3f) + 0.05f * Mth.sin(t * 17f + i);
                float y = floor + 0.1f + f * h;
                float radius = (0.42f - 0.34f * f) * (0.6f + 0.7f * flame) * flick;
                float sway = Mth.sin(t * 3f + f * 3f) * 0.07f * f;
                float[] col = f < 0.35f ? new float[] { 1f, 0.45f, 0.12f } :
                        f < 0.7f ? new float[] { 1f, 0.72f, 0.28f } : new float[] { 1f, 0.95f, 0.75f };
                GlowDraw.disc(vc, m, cam, cx + sway, y, cz, radius * 1.4f, col[0], col[1], col[2],
                        0.5f * (on ? 1f : 0.3f), 0f);
            }
            GlowDraw.disc(vc, m, cam, cx, floor + 0.25f, cz, 0.3f * (0.5f + flame), 1f, 0.98f, 0.9f,
                    0.8f * (on ? 1f : 0.3f), 0f);

            // two wings of fire, turning slowly, opening with the heat
            float open = 0.35f + 0.65f * heat;
            float phi = t * 0.3f;
            float[] u = { Mth.cos(phi), 0f, Mth.sin(phi) };
            for (int side = -1; side <= 1; side += 2) {
                float start = side > 0 ? 0.25f : Mth.PI - 0.25f - 1.1f * open;
                float radius = 0.55f + 0.85f * heat;
                for (int layer = 0; layer < 3; layer++) {
                    float lr = radius + layer * 0.14f;
                    float width = 0.1f + 0.05f * layer + 0.08f * heat;
                    float a = (0.32f - 0.08f * layer) * (on ? 1f : 0.25f) * (0.75f + 0.25f * Mth.sin(t * 6f + layer));
                    float[] col = layer == 0 ? new float[] { 1f, 0.75f, 0.3f } : new float[] { 1f, 0.45f, 0.15f };
                    GlowDraw.ring(vc, m, cx, floor + 0.15f, cz, u, GlowDraw.Y, lr, width, start, 1.1f * open, 24,
                            col[0], col[1], col[2], a);
                }
            }
        }

        // embers rising
        int embers = on ? 6 + Math.round(heat * 28f) : 3;
        for (int i = 0; i < embers; i++) {
            float life = (t * (0.18f + 0.22f * GlowDraw.noise(i, 3)) + GlowDraw.noise(i, 5)) % 1f;
            float ang = GlowDraw.noise(i, 7) * Mth.TWO_PI;
            float rad = 0.15f + 0.45f * GlowDraw.noise(i, 9);
            float x = cx + Mth.cos(ang + life * 2f) * rad * (1f - 0.3f * life);
            float z = cz + Mth.sin(ang + life * 2f) * rad * (1f - 0.3f * life);
            float y = floor + 0.2f + life * (1.4f + 1.2f * flame);
            GlowDraw.disc(vc, m, cam, x, y, z, 0.03f * (1f - life * 0.6f), 1f, life < 0.5f ? 0.8f : 0.55f, 0.25f,
                    0.9f * (1f - life), 0f);
        }

        // a burst of sparks when it burns something
        float sinceBurst = t - s.burstAt;
        if (sinceBurst >= 0f && sinceBurst < 0.7f) {
            float f = sinceBurst / 0.7f;
            for (int i = 0; i < 18; i++) {
                float a = GlowDraw.noise(i, 11) * Mth.TWO_PI;
                float el = (GlowDraw.noise(i, 13) - 0.3f) * 1.4f;
                float d = (0.2f + 1.2f * f) * (0.5f + GlowDraw.noise(i, 15));
                GlowDraw.disc(vc, m, cam, cx + Mth.cos(a) * d, floor + 0.4f + el * d, cz + Mth.sin(a) * d, 0.04f, 1f,
                        0.9f, 0.5f, 1f - f, 0f);
            }
        }
        // and a golden ring when it renews something
        float sinceRing = t - s.ringAt;
        if (sinceRing >= 0f && sinceRing < 1.1f) {
            float f = sinceRing / 1.1f;
            GlowDraw.ring(vc, m, cx, floor + 0.5f + 0.4f * f, cz, GlowDraw.X, GlowDraw.Z, 0.3f + 1.1f * f,
                    0.06f * (1f - f) + 0.02f, 0f, Mth.TWO_PI, 40, 1f, 0.83f, 0.3f, 0.9f * (1f - f));
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(PhoenixInfuserMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(PhoenixInfuserMachine m) {
        return new AABB(m.getBlockPos()).inflate(24);
    }
}
