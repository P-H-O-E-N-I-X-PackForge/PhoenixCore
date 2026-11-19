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

    private static final net.phoenix.core.client.fx.FxRender FX = net.phoenix.core.client.fx.FxRender
            .builder("phoenix_forge", "Phoenix Forge phoenix")
            .param("speed", 0f, 10f, "how fast it animates, 1 being as it is")
            .param("size", 0.1f, 4f, "how big it is, 1 being as it is")
            .param("power", 0f, 4f, "how bright it is, 1 being as it is")
            .param("detail", 0.3f, 1f, "how many steps the 3D shader takes: lower is faster")
            .param("classic", 0f, 1f, "1 draws the model-built phoenix, as it is unless set; 0 draws the shader one")
            .build()
            .spawner(PhoenixForgeWorldRenderer::spawnPreview);

    private record Preview(double x, double y, double z, Direction front, long until) {}

    private static volatile Preview preview;

    private static String spawnPreview(int seconds) {
        var player = Minecraft.getInstance().player;
        if (player == null) return "not in a world";
        preview = new Preview(player.getX(), player.getY(), player.getZ(), player.getDirection(),
                System.currentTimeMillis() + seconds * 1000L);
        return "Phoenix put down for " + seconds + " seconds, flying the way you face. /pftfx clear takes it away.";
    }

    public static void clearPreview() {
        preview = null;
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
        RenderLevelStageEvent.Stage stage = event.getStage();
        if (stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES &&
                stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float clock = net.phoenix.core.client.fx.FxHook.time(FX, (mc.level.getGameTime() + partial) / 20f);
        net.minecraft.client.renderer.ShaderInstance shader = net.phoenix.core.client.worldfx.WorldFXShaders.FX_PHOENIX;
        boolean useShader = shader != null && FX.get("classic", 1f) < 0.5f;

        if (useShader && stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            if (!all().isEmpty() || preview != null) net.phoenix.core.client.fx.SceneDepth.capture();
            return;
        }
        if (!useShader && stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Runnable restore = useShader ?
                net.phoenix.core.client.renderer.machine.FxVolume.stageMatrices(
                        new Matrix4f(event.getModelViewMatrix()), event.getProjectionMatrix()) :
                null;
        if (useShader) {
            net.phoenix.core.client.renderer.machine.FxVolume.adapt(mc.getFps());
        }

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
            if (useShader) {
                drawWithShader(shader, m, phase, front, clock, since);
            } else if (phase == 1) {
                drawEmber(vc, m, left, up, clock, 12.5f);
            } else {
                drawPhoenix(vc, m, left, up, front, clock, since);
            }
            ps.popPose();
            drew = true;
        }
        Preview spawned = preview;
        if (spawned != null) {
            if (System.currentTimeMillis() > spawned.until()) {
                preview = null;
            } else {
                PoseStack ps = event.getPoseStack();
                ps.pushPose();
                ps.translate(spawned.x() - camera.x, spawned.y() - camera.y, spawned.z() - camera.z);
                Matrix4f m = ps.last().pose();
                if (useShader) {
                    drawWithShader(shader, m, 2, spawned.front(), clock, 100f);
                } else {
                    drawPhoenix(buffers.getBuffer(Types.GLOW), m, event.getCamera().getLeftVector(),
                            event.getCamera().getUpVector(), spawned.front(), clock, 100f);
                }
                ps.popPose();
                drew = true;
            }
        }
        if (restore != null) restore.run();
        if (useShader) net.phoenix.core.client.fx.SceneDepth.release();
        if (drew) buffers.endBatch(Types.GLOW);
    }

    private static void drawWithShader(net.minecraft.client.renderer.ShaderInstance shader, Matrix4f m, int phase,
                                       Direction front, float t, float since) {
        float rise = Mth.clamp(since / 7f, 0f, 1f);
        float eased = rise * rise * (3f - 2f * rise);
        float size = FX.get("size", 1f);
        float scale = (phase == 1 ? SCALE : SCALE * (0.2f + 0.8f * eased)) * size;
        float height = phase == 1 ? 12.5f : 11f + 3f * eased + 0.45f * Mth.sin(t * 2.2f);
        float yaw = 0.14f * Mth.sin(t * 0.35f);
        float cs = Mth.cos(yaw);
        float sn = Mth.sin(yaw);
        float dirX = front.getStepX() * cs - front.getStepZ() * sn;
        float dirZ = front.getStepX() * sn + front.getStepZ() * cs;

        float cx = dirX * -0.55f * scale;
        float cz = dirZ * -0.55f * scale;
        float cy = height + 0.2f * scale;
        var look = new net.phoenix.core.client.renderer.machine.FxVolume.Look(t, FX.get("power", 1f), 3.7f,
                net.phoenix.core.client.fx.FxHook.tint(FX, 1f, 0.76f, 0.29f, 1f, 0.3f, t),
                net.phoenix.core.client.fx.FxHook.tint(FX, 1f, 0.957f, 0.847f, 1f, 0.7f, t));
        net.phoenix.core.client.renderer.machine.FxVolume.draw(shader, m, FX.get("detail", 1f), cx, cy, cz,
                2.3f * scale, 1.5f * scale, 2.3f * scale, scale, look,
                new float[] { Mth.clamp(0.4f + 0.6f * eased, 0f, 1f), 0f, phase == 1 ? 0f : 1f, eased },
                new float[] { dirX, dirZ, 0f, 0f });
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
