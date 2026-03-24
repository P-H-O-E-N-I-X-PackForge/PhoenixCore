package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.client.renderer.PhoenixRenderTypes;
import net.phoenix.core.common.machine.multiblock.electric.TeslaTowerMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;

public class TeslaTowerRenderer extends DynamicRender<TeslaTowerMachine, TeslaTowerRenderer> {

    public static final TeslaTowerRenderer INSTANCE = new TeslaTowerRenderer();
    public static final Codec<TeslaTowerRenderer> CODEC = Codec.unit(INSTANCE);
    public static final DynamicRenderType<TeslaTowerMachine, TeslaTowerRenderer> TYPE = new DynamicRenderType<>(CODEC);

    private TeslaTowerRenderer() {}

    @Override
    public @NotNull DynamicRenderType<TeslaTowerMachine, TeslaTowerRenderer> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRenderOffScreen(TeslaTowerMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(TeslaTowerMachine m) {
        // Inflated to ensure arcs don't disappear when the controller is off-screen
        return new AABB(m.getPos()).inflate(40);
    }

    @Override
    public boolean shouldRender(TeslaTowerMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() && machine.isActive();
    }

    @Override
    public void render(TeslaTowerMachine machine, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        long time = machine.getLevel().getGameTime();
        VertexConsumer vc = buffer.getBuffer(PhoenixRenderTypes.LIGHT_RING());

        // Settings
        float RING_RADIUS = 6.5f;
        float TEEPEE_DROP = 2.5f;
        int ARC_POINTS = 30;

        // Folded translate(-1.5, 0.5, -3.5) into positions to avoid
        // singleplayer vs dedicated server origin discrepancy.
        // xPositions: 8.0 - 1.5 = 6.5
        // zPositions: 4.0 - 3.5 = 0.5
        float[] yPositions = new float[] { 5.5f, 14.5f, 22.5f }; // y + 0.5
        float[] xPositions = new float[] { 1.0f, 1.0f, 1.0f };
        float[] zPositions = new float[] { 6f, 6.0f, 6.0f };

        poseStack.pushPose();

        for (int ringIndex = 0; ringIndex < yPositions.length; ringIndex++) {
            float xBase = xPositions[ringIndex];
            float yBase = yPositions[ringIndex];
            float zBase = zPositions[ringIndex];
            Vec3 currentTopCenter = new Vec3(xBase, yBase, zBase);

            for (int i = 0; i < ARC_POINTS; i++) {
                double rotationSpeed = time * (0.02 + (ringIndex * 0.01));
                double angle = (2 * Math.PI * i / ARC_POINTS) + rotationSpeed;

                double x = xBase + Math.cos(angle) * RING_RADIUS;
                double z = zBase + Math.sin(angle) * RING_RADIUS;
                double pulse = Math.sin((time + ringIndex * 10 + i) * 0.1) * 0.2;
                double y = yBase - TEEPEE_DROP + pulse;

                Vec3 targetPos = new Vec3(x, y, z);

                if ((time + i * 7 + ringIndex * 13) % 12 < 5) {
                    renderArc(poseStack, vc, currentTopCenter, targetPos, time);

                    if (machine.getLevel().isClientSide && machine.getLevel().random.nextFloat() < 0.05f) {
                        machine.getLevel().addParticle(
                                PhoenixParticles.TESLA_SPARK.get(),
                                targetPos.x + machine.getPos().getX() + 0.5,
                                targetPos.y + machine.getPos().getY() + 0.5,
                                targetPos.z + machine.getPos().getZ() + 0.5,
                                0.0, 0.0, 0.0);
                    }
                }
            }
        }
        poseStack.popPose();
    }

    private void generateLightningPath(List<Vec3> points, Vec3 start, Vec3 end, int depth, float jitter, long time) {
        if (depth == 0) {
            points.add(end);
            return;
        }

        Vec3 mid = start.add(end).scale(0.5);
        // Use the sinNoise we made earlier to keep it high-performance
        float seed = (float) (time + start.x + start.y + start.z);

        mid = mid.add(
                (sinNoise(seed) - 0.5) * jitter * depth,
                (sinNoise(seed + 1) - 0.5) * jitter * depth,
                (sinNoise(seed + 2) - 0.5) * jitter * depth);

        generateLightningPath(points, start, mid, depth - 1, jitter, time);
        generateLightningPath(points, mid, end, depth - 1, jitter, time);
    }

    private void drawVolumetricArc(VertexConsumer vc, Matrix4f pose, List<Vec3> path, float width, float r, float g,
                                   float b, float a) {
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 s = path.get(i);
            Vec3 e = path.get(i + 1);

            // Calculate a 'normal' to the line to give it width
            // In a perfect world, this would face the camera, but for a
            // high-performance render, a fixed offset or Y-offset works well.
            float dx = (float) (e.x - s.x);
            float dy = (float) (e.y - s.y);
            float dz = (float) (e.z - s.z);

            // Perpendicular vector for thickness
            float nx = -dz * width;
            float nz = dx * width;

            // Quad Vertex 1
            vc.vertex(pose, (float) s.x - nx, (float) s.y, (float) s.z - nz).color(r, g, b, a).endVertex();
            // Quad Vertex 2
            vc.vertex(pose, (float) s.x + nx, (float) s.y, (float) s.z + nz).color(r, g, b, a).endVertex();
            // Quad Vertex 3
            vc.vertex(pose, (float) e.x + nx, (float) e.y, (float) e.z + nz).color(r, g, b, a).endVertex();
            // Quad Vertex 4
            vc.vertex(pose, (float) e.x - nx, (float) e.y, (float) e.z - nz).color(r, g, b, a).endVertex();
        }
    }

    private void drawTeslaLine(VertexConsumer vc, Matrix4f pose, Vec3 s, Vec3 e,
                               int depth, boolean isCore, long time) {
        if (depth == 0) {
            float r, g, b, a;
            if (isCore) {
                r = 160 / 255f;
                g = 32 / 255f;
                b = 240 / 255f;
                a = 1.0f;
            } else {
                r = 255 / 255f;
                g = 183 / 255f;
                b = 197 / 255f;
                a = 0.5f;
            }
            vc.vertex(pose, (float) s.x, (float) s.y, (float) s.z).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) e.x, (float) e.y, (float) e.z).color(r, g, b, a).endVertex();
            return;
        }

        Vec3 mid = s.add(e).scale(0.5);

        // --- THE PERFORMANCE FIX ---
        // Instead of RandomSource.create(), we use a deterministic "Pseudo-random" math function.
        // This is much faster and uses 0 memory allocations.
        float seed = (float) (time + s.x * 31 + s.y * 17 + s.z);
        float jitter = 0.25f * depth;

        double jX = (sinNoise(seed) - 0.5) * jitter;
        double jY = (sinNoise(seed * 1.2f) - 0.5) * jitter;
        double jZ = (sinNoise(seed * 1.5f) - 0.5) * jitter;

        mid = mid.add(jX, jY, jZ);

        drawTeslaLine(vc, pose, s, mid, depth - 1, isCore, time);
        drawTeslaLine(vc, pose, mid, e, depth - 1, isCore, time);
    }

    private static final Vec3[] POINT_CACHE = new Vec3[64];
    static {
        for (int i = 0; i < 64; i++) POINT_CACHE[i] = Vec3.ZERO;
    }

    private void renderArc(PoseStack stack, VertexConsumer vc, Vec3 start, Vec3 end, long time) {
        Matrix4f pose = stack.last().pose();

        // Smooth time for drifting, integer time for snapping
        float smoothTime = time * 0.05f;
        long snapTime = time / 4; // Change the "base" shape every 4 ticks

        POINT_CACHE[0] = start;
        // We pass both to the path generator
        int totalPoints = generatePath(start, end, 1, 5, 0.18f, snapTime, smoothTime);
        POINT_CACHE[totalPoints - 1] = end;

        // Inside renderArc
        drawRibbon(vc, pose, totalPoints, 0.08f, 1.0f, 0.7f, 0.8f, 0.3f, time); // Glow
        drawRibbon(vc, pose, totalPoints, 0.02f, 0.6f, 0.1f, 0.9f, 1.0f, time); // Core
    }

    private int generatePath(Vec3 s, Vec3 e, int index, int depth, float jitter, long snapTime, float smoothTime) {
        if (depth == 0) {
            POINT_CACHE[index] = e;
            return index + 1;
        }

        Vec3 mid = s.add(e).scale(0.5);

        // combine snapTime (big jumps) with smoothTime (gentle vibration)
        float seed = (float) (snapTime + (index * 1.5f) + smoothTime);

        // This creates the "Arcing" look:
        // The bolt structure is stable, but the midpoints "vibrate"
        float currentJitter = jitter * (float) Math.sqrt(depth);

        mid = mid.add(
                (sinNoise(seed) - 0.5) * currentJitter,
                (sinNoise(seed + 12) - 0.5) * currentJitter,
                (sinNoise(seed + 24) - 0.5) * currentJitter);

        int nextIndex = generatePath(s, mid, index, depth - 1, jitter, snapTime, smoothTime);
        return generatePath(mid, e, nextIndex, depth - 1, jitter, snapTime, smoothTime);
    }

    private void drawRibbon(VertexConsumer vc, Matrix4f pose, int count, float width, float r, float g, float b,
                            float a, long time) {
        for (int i = 0; i < count - 1; i++) {
            Vec3 s = POINT_CACHE[i];
            Vec3 e = POINT_CACHE[i + 1];

            // 1. Direction of the segment
            float dx = (float) (e.x - s.x);
            float dy = (float) (e.y - s.y);
            float dz = (float) (e.z - s.z);

            // 2. The "Twist" Math
            // We calculate a 'roll' angle that changes per segment and over time
            float twistSpeed = time * 0.1f;
            float roll = (i * 0.5f) + twistSpeed;

            // 3. Calculate the Perpendicular "Width" Vectors
            // We use Sin/Cos to rotate the offset around the segment's axis
            float angleX = (float) Math.cos(roll) * width;
            float angleY = (float) Math.sin(roll) * width;

            // Using a basic billboarding-style offset that rotates
            float nx = -dz * angleX;
            float ny = angleY;
            float nz = dx * angleX;

            // 4. Draw the Quad
            // Vertices at S (Start)
            vc.vertex(pose, (float) s.x - nx, (float) s.y - ny, (float) s.z - nz).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) s.x + nx, (float) s.y + ny, (float) s.z + nz).color(r, g, b, a).endVertex();

            // Vertices at E (End) - Note: To be perfectly professional,
            // the End vertices of segment I should use the same 'roll' as the Start of I+1
            float nextRoll = ((i + 1) * 0.5f) + twistSpeed;
            float nnx = -dz * (float) Math.cos(nextRoll) * width;
            float nny = (float) Math.sin(nextRoll) * width;
            float nnz = dx * (float) Math.cos(nextRoll) * width;

            vc.vertex(pose, (float) e.x + nnx, (float) e.y + nny, (float) e.z + nnz).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) e.x - nnx, (float) e.y - nny, (float) e.z - nnz).color(r, g, b, a).endVertex();
        }
    }

    private float sinNoise(float n) {
        return (float) (Math.sin(n * 2137.123) * 43758.5453) % 1.0f;
    }
}
