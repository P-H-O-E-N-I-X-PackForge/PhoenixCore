package net.phoenix.core.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.machine.PlasmaArcFurnaceRender;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.common.machine.multiblock.electric.HighPressurePlasmaArcFurnaceMachine;
import net.phoenixvine.wiki.fx.WorldFXManager;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class RiftFX {

    private static final Set<HighPressurePlasmaArcFurnaceMachine> TRACKED = ConcurrentHashMap.newKeySet();
    private static final Map<HighPressurePlasmaArcFurnaceMachine, Float> SHOWN = new WeakHashMap<>();

    private static final float[] BLUE = { 0.35f, 0.62f, 1.0f };
    private static final float[] WHITE = { 0.88f, 0.95f, 1.0f };

    private static final float NEAR_RANGE = 70f;

    private RiftFX() {}

    public static void add(HighPressurePlasmaArcFurnaceMachine machine) {
        TRACKED.add(machine);
    }

    public static void remove(HighPressurePlasmaArcFurnaceMachine machine) {
        TRACKED.remove(machine);
        synchronized (SHOWN) {
            SHOWN.remove(machine);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TRACKED.clear();
        synchronized (SHOWN) {
            SHOWN.clear();
        }
    }

    public static boolean ready() {
        return WorldFXShaders.FX_RIFT != null;
    }

    public static boolean active(HighPressurePlasmaArcFurnaceMachine m) {
        return FxHook.preview(m) || (m.isFormed() && m.getShieldType() == Shield.ShieldTypes.NORMAL);
    }

    public static Vec3 centre(HighPressurePlasmaArcFurnaceMachine m) {
        double x = m.getBlockPos().getX() + 0.5;
        double y = m.getBlockPos().getY() + 2.5;
        double z = m.getBlockPos().getZ() + 0.5;
        switch (m.getFrontFacing()) {
            case NORTH -> z -= 20.0;
            case SOUTH -> z += 20.0;
            case WEST -> x -= 20.0;
            case EAST -> x += 20.0;
            default -> {}
        }
        return new Vec3(x, y, z);
    }

    private static float shown(HighPressurePlasmaArcFurnaceMachine m) {
        float target = FxHook.preview(m) ? 0.7f : 0.12f + 0.88f * Mth.clamp(m.riftGrowth(), 0f, 1f);
        synchronized (SHOWN) {
            float now = SHOWN.getOrDefault(m, 0f);
            now += (target - now) * 0.03f;
            SHOWN.put(m, now);
            return now;
        }
    }

    private static float nearness(Vec3 camera, Vec3 centre) {
        float d = (float) camera.distanceTo(centre);
        float k = 1f - Mth.clamp((d - 6f) / (NEAR_RANGE - 6f), 0f, 1f);
        return k * k;
    }

    private static float[] project(Vec3 rel, Matrix4f view, Matrix4f proj) {
        Vector4f clip = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1f).mul(view).mul(proj);
        if (clip.w <= 0.05f) return null;
        return new float[] { clip.x / clip.w, clip.y / clip.w, clip.z / clip.w };
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        ShaderInstance shader = WorldFXShaders.FX_RIFT;
        if (shader == null || TRACKED.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        FxRender fx = PlasmaArcFurnaceRender.fx();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        Matrix4f view = new Matrix4f(event.getModelViewMatrix());
        Matrix4f proj = new Matrix4f(event.getProjectionMatrix());
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        float seconds = FxHook.time(fx, (System.currentTimeMillis() % 1000000L) / 1000f);
        float size = fx.get("size", 1f);
        float power = fx.get("power", 1f);

        float[] a = BLUE;
        float[] b = WHITE;
        float[] set = fx.colour();
        if (set == null && fx.theme() != null) set = fx.theme().colourAt(0.5f, seconds);
        if (set != null) {
            a = set;
            b = new float[] { Mth.lerp(0.6f, set[0], 1f), Mth.lerp(0.6f, set[1], 1f), Mth.lerp(0.6f, set[2], 1f) };
        }

        int texture = -1;
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f(proj), VertexSorting.DISTANCE_TO_ORIGIN);
        for (HighPressurePlasmaArcFurnaceMachine m : TRACKED) {
            if (!active(m) || m.getLevel() != mc.level) continue;
            Vec3 centre = centre(m);
            Vec3 rel = centre.subtract(camPos);
            float growth = shown(m);

            float halfHeight = (2.5f + 8.5f * growth) * size;
            float widthRatio = 0.08f + 0.17f * growth;

            float[] c = project(rel, view, proj);
            if (c == null) continue;
            float[] top = project(rel.add(0, halfHeight, 0), view, proj);
            float[] bottom = project(rel.subtract(0, halfHeight, 0), view, proj);
            if (top == null || bottom == null) continue;
            float halfScreen = Math.abs(top[1] - bottom[1]) / 4f;
            if (halfScreen < 0.002f) continue;

            if (texture < 0) texture = WorldFXManager.captureSkyToTexture();
            mc.getMainRenderTarget().bindWrite(false);

            float near = nearness(camPos, centre);
            float glitch = near * Mth.clamp(growth * 1.2f, 0.25f, 1f);

            RenderSystem.setShader(() -> shader);
            shader.setSampler("InSampler", texture);
            shader.safeGetUniform("ScreenSize").set((float) w, (float) h);
            shader.safeGetUniform("Time").set(seconds);
            shader.safeGetUniform("RiftPos").set(c[0] * 0.5f + 0.5f, c[1] * 0.5f + 0.5f);
            shader.safeGetUniform("RiftSize").set(halfScreen * widthRatio, halfScreen);
            shader.safeGetUniform("Centre").set((float) rel.x, (float) rel.y, (float) rel.z);
            shader.safeGetUniform("Normal").set(m.getFrontFacing().getStepX(), 0f, m.getFrontFacing().getStepZ());
            shader.safeGetUniform("HalfH").set(halfHeight);
            shader.safeGetUniform("WidthRatio").set(widthRatio);
            shader.safeGetUniform("Growth").set(growth);
            shader.safeGetUniform("Lens").set(0.7f + 1.1f * growth);
            shader.safeGetUniform("Glitch").set(glitch);
            shader.safeGetUniform("ColorA").set(a[0], a[1], a[2]);
            shader.safeGetUniform("ColorB").set(b[0], b[1], b[2]);
            shader.safeGetUniform("Seed").set((m.getBlockPos().asLong() & 1023L) / 100f);
            shader.safeGetUniform("Power").set(power);

            float ex = 3.7f * halfHeight;
            float ey = 6.6f * halfHeight;
            float flat = Mth.sqrt(left.x * left.x + left.z * left.z);
            float rx = flat < 1e-4f ? 1f : -left.x / flat, ry = 0f, rz = flat < 1e-4f ? 0f : -left.z / flat;
            float cx = (float) rel.x, cy = (float) rel.y, cz = (float) rel.z;

            Matrix4fStack stack = RenderSystem.getModelViewStack();
            stack.pushMatrix();
            stack.set(view);
            RenderSystem.applyModelViewMatrix();

            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bb.addVertex(cx - rx * ex, cy - ey, cz - rz * ex);
            bb.addVertex(cx + rx * ex, cy - ey, cz + rz * ex);
            bb.addVertex(cx + rx * ex, cy + ey, cz + rz * ex);
            bb.addVertex(cx - rx * ex, cy + ey, cz - rz * ex);
            net.phoenix.core.client.renderer.machine.FxVolume.blended(shader, bb.buildOrThrow(),
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            stack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
    }

    private static float closeness(Vec3 camera, Vec3 centre, float range) {
        float d = (float) camera.distanceTo(centre);
        return 1f - Mth.clamp((d - 3f) / (range - 3f), 0f, 1f);
    }

    private static float shake(Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        float strongest = 0f;
        for (HighPressurePlasmaArcFurnaceMachine m : TRACKED) {
            if (!active(m) || m.getLevel() != mc.level) continue;
            float g = 0.12f + 0.88f * Mth.clamp(m.riftGrowth(), 0f, 1f);
            strongest = Math.max(strongest,
                    (float) Math.pow(closeness(camPos, centre(m), 90f), 2.4) * (0.4f + 0.6f * g));
        }
        return strongest;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (TRACKED.isEmpty() || Minecraft.getInstance().level == null) return;
        float s = shake(event.getCamera().getPosition());
        if (s < 0.004f) return;
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        float amp = s * 5.5f;
        float jolt = Math.max(0f, Mth.sin(t * 2.3f) * Mth.sin(t * 5.1f + 1f) - 0.45f) * 9f * s * s;
        float fast = 1f + 2.5f * s;
        event.setPitch(event.getPitch() + (Mth.sin(t * 31f * fast) + Mth.sin(t * 17.3f * fast)) * 0.5f * amp +
                jolt * Mth.sin(t * 80f));
        event.setYaw(event.getYaw() + (Mth.sin(t * 27f * fast + 2f) + Mth.sin(t * 13.1f * fast)) * 0.5f * amp +
                jolt * 0.6f * Mth.sin(t * 71f));
        event.setRoll(event.getRoll() + Mth.sin(t * 23f * fast + 4f) * amp * 1.1f);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (TRACKED.isEmpty() || Minecraft.getInstance().level == null) return;
        float s = shake(event.getCamera().getPosition());
        if (s < 0.02f) return;
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        event.setFOV(event.getFOV() + (Mth.sin(t * 19f) * 0.6f + Mth.sin(t * 7.3f) * 0.4f) * s * 6.0f);
    }
}
