package net.phoenix.core.client.renderer.phoenix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.forge.PhoenixForgeMachine;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class PhoenixForgeWorldRenderer {

    private PhoenixForgeWorldRenderer() {}

    private static final class Types extends RenderType {

        private Types(String n, VertexFormat f, VertexFormat.Mode m, int b, boolean a, boolean s, Runnable st,
                      Runnable cl) {
            super(n, f, m, b, a, s, st, cl);
        }

        static final RenderType GLOW = create("phoenixcore_phoenix_glow", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES, 1 << 16, false, false,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .createCompositeState(false));
    }

    private static final double RANGE = 320.0;
    private static final float SCALE = 5.2f;
    private static final Set<PhoenixForgeMachine> FORGES = new LinkedHashSet<>();
    private static final Map<PhoenixForgeMachine, long[]> PHASE_CHANGE = new IdentityHashMap<>();

    public static synchronized void add(PhoenixForgeMachine forge) {
        FORGES.add(forge);
    }

    public static synchronized void remove(PhoenixForgeMachine forge) {
        FORGES.remove(forge);
        PHASE_CHANGE.remove(forge);
    }

    private static synchronized List<PhoenixForgeMachine> all() {
        return new ArrayList<>(FORGES);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        synchronized (PhoenixForgeWorldRenderer.class) {
            FORGES.clear();
            PHASE_CHANGE.clear();
        }
    }

    private static synchronized float sincePhaseChange(PhoenixForgeMachine forge, int phase) {
        long[] last = PHASE_CHANGE.computeIfAbsent(forge,
                f -> new long[] { phase, System.nanoTime() - 60_000_000_000L });
        if (last[0] != phase) {
            last[0] = phase;
            last[1] = System.nanoTime();
        }
        return (System.nanoTime() - last[1]) / 1_000_000_000f;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float clock = (mc.level.getGameTime() + partial) / 20f;

        for (PhoenixForgeMachine forge : all()) {
            if (!forge.isFormed() || forge.getLevel() != mc.level) continue;
            int phase = forge.getPhoenixPhase();
            if (phase <= 0) continue;
            BlockPos pos = forge.getBlockPos();
            if (pos.distToCenterSqr(camera) > RANGE * RANGE) continue;
            Direction front = forge.getFrontFacing();
            BlockPos center = pos.relative(front.getOpposite(), 5);
            AABB reach = new AABB(center).inflate(16, 0, 16).expandTowards(0, 24, 0);
            if (!event.getFrustum().isVisible(reach)) continue;

            float since = sincePhaseChange(forge, phase);
            PoseStack ps = event.getPoseStack();
            ps.pushPose();
            ps.translate(center.getX() + 0.5 - camera.x, center.getY() - camera.y, center.getZ() + 0.5 - camera.z);
            Matrix4f m = ps.last().pose();
            VertexConsumer vc = buffers.getBuffer(Types.GLOW);
            Vector3f left = event.getCamera().getLeftVector();
            Vector3f up = event.getCamera().getUpVector();
            if (phase == 1) {
                drawEmber(vc, m, left, up, clock, 12.5f);
            } else {
                drawPhoenix(vc, m, left, up, front, clock, since);
            }
            ps.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch(Types.GLOW);
    }

    private static void drawEmber(VertexConsumer vc, Matrix4f m, Vector3f left, Vector3f up, float t, float height) {
        float pulse = 0.85f + 0.15f * Mth.sin(t * 3f);
        halo(vc, m, left, up, 0f, height, 0f, 2.6f * pulse, 0xFFB040, 0.18f);
        halo(vc, m, left, up, 0f, height, 0f, 1.3f * pulse, 0xFFE0A0, 0.4f);
        halo(vc, m, left, up, 0f, height, 0f, 0.55f, 0xFFFFFF, 0.8f);
        for (int i = 0; i < 9; i++) {
            float a = t * (0.6f + 0.1f * i) + i * 0.7f;
            float r = 1.6f + 0.7f * Mth.sin(t + i);
            halo(vc, m, left, up, Mth.cos(a) * r, height + Mth.sin(a * 1.3f) * 0.8f, Mth.sin(a) * r, 0.17f, 0xFFD060,
                    0.85f);
        }
    }

    private static void drawPhoenix(VertexConsumer vc, Matrix4f m, Vector3f left, Vector3f up, Direction front, float t,
                                    float since) {
        float rise = Mth.clamp(since / 7f, 0f, 1f);
        float eased = rise * rise * (3f - 2f * rise);
        float scale = SCALE * (0.2f + 0.8f * eased);
        float height = 11f + 3f * eased + 0.45f * Mth.sin(t * 2.2f);
        float fx = front.getStepX();
        float fz = front.getStepZ();

        float yaw = 0.14f * Mth.sin(t * 0.35f);
        float cs = Mth.cos(yaw);
        float sn = Mth.sin(yaw);
        float dirX = fx * cs - fz * sn;
        float dirZ = fx * sn + fz * cs;
        float rightX = -dirZ;
        float rightZ = dirX;

        float roll = 0.08f * Mth.cos(t * 2.2f);

        halo(vc, m, left, up, 0f, height, 0f, 6.5f * scale / SCALE, 0xFF9A30, 0.07f * (0.3f + eased));
        halo(vc, m, left, up, 0f, height, 0f, 3.2f * scale / SCALE, 0xFFD890, 0.12f * (0.3f + eased));

        final float s = scale;
        final float h = height;
        PhoenixModel.Sink sink = (ax, ay, az, ac, bx, by, bz, bc, cx, cy, cz, cc) -> {
            float[] a = place(ax, ay, az, roll, s, h, rightX, rightZ, dirX, dirZ);
            float[] b = place(bx, by, bz, roll, s, h, rightX, rightZ, dirX, dirZ);
            float[] c = place(cx, cy, cz, roll, s, h, rightX, rightZ, dirX, dirZ);
            vc.addVertex(m, a[0], a[1], a[2]).setColor(ac);
            vc.addVertex(m, b[0], b[1], b[2]).setColor(bc);
            vc.addVertex(m, c[0], c[1], c[2]).setColor(cc);
        };
        PhoenixModel.build(sink, t, Mth.clamp(0.4f + 0.6f * eased, 0f, 1f));

        for (int i = 0; i < 26; i++) {
            float life = (t * (0.35f + 0.05f * (i % 5)) + i * 0.137f) % 1f;
            float side = i % 2 == 0 ? 1f : -1f;
            float spread = 2.2f + 4.6f * ((i * 0.37f) % 1f);
            float wx = side * spread * scale / SCALE;
            float wz = -0.6f - 2.5f * ((i * 0.53f) % 1f);
            float ox = rightX * wx + dirX * wz;
            float oz = rightZ * wx + dirZ * wz;
            halo(vc, m, left, up, ox, height - 0.4f - life * 6f * scale / SCALE, oz, 0.13f * (1f - life), 0xFFC050,
                    0.9f * (1f - life));
        }
    }

    private static float[] place(float x, float y, float z, float roll, float scale, float height, float rightX,
                                 float rightZ, float dirX, float dirZ) {
        float cr = Mth.cos(roll);
        float sr = Mth.sin(roll);
        float rx = x * cr - y * sr;
        float ry = x * sr + y * cr;
        return new float[] { (rightX * rx + dirX * z) * scale, height + ry * scale, (rightZ * rx + dirZ * z) * scale };
    }

    private static void halo(VertexConsumer vc, Matrix4f m, Vector3f left, Vector3f up, float x, float y, float z,
                             float r, int rgb, float alpha) {
        int inner = ((int) (alpha * 255f) << 24) | (rgb & 0xFFFFFF);
        int outer = rgb & 0xFFFFFF;
        int n = 14;
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n;
            float a1 = Mth.TWO_PI * (i + 1) / n;
            float c0 = Mth.cos(a0) * r;
            float s0 = Mth.sin(a0) * r;
            float c1 = Mth.cos(a1) * r;
            float s1 = Mth.sin(a1) * r;
            vc.addVertex(m, x, y, z).setColor(inner);
            vc.addVertex(m, x - left.x * c0 + up.x * s0, y - left.y * c0 + up.y * s0, z - left.z * c0 + up.z * s0)
                    .setColor(outer);
            vc.addVertex(m, x - left.x * c1 + up.x * s1, y - left.y * c1 + up.y * s1, z - left.z * c1 + up.z * s1)
                    .setColor(outer);
        }
    }
}
