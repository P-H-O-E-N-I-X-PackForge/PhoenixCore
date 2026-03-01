
package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.renderer.PhoenixRenderTypes;
import net.phoenix.core.common.machine.multiblock.electric.TeslaTowerMachine;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;

public class TeslaTowerRenderer extends DynamicRender<TeslaTowerMachine, TeslaTowerRenderer> {

    // --- GTCEu Required Boilerplate ---
    public static final TeslaTowerRenderer INSTANCE = new TeslaTowerRenderer();
    public static final Codec<TeslaTowerRenderer> CODEC = Codec.unit(INSTANCE);
    public static final DynamicRenderType<TeslaTowerMachine, TeslaTowerRenderer> TYPE = new DynamicRenderType<>(CODEC);

    private TeslaTowerRenderer() {}

    @Override
    public @NotNull DynamicRenderType<TeslaTowerMachine, TeslaTowerRenderer> getType() {
        return TYPE;
    }

    // --- Optimization & Visibility ---
    @Override
    public boolean shouldRenderOffScreen(TeslaTowerMachine m) {
        return true; // Keeps the arcs visible even if the main controller block is behind you
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(TeslaTowerMachine m) {
        // Inflate by 40 so the game doesn't stop rendering the arcs if you're standing at the bottom
        return new AABB(m.getPos()).inflate(40);
    }

    @Override
    public boolean shouldRender(TeslaTowerMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() && machine.isActive();
    }

    @Override
    public void render(TeslaTowerMachine machine, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {

        long time = machine.getLevel().getGameTime();
        VertexConsumer vc = buffer.getBuffer(PhoenixRenderTypes.LIGHT_RING());

        // Define your "Special Areas" relative to the controller
        // Example: Arcs jumping between four insulators at the top corners
        Vec3 topCenter = new Vec3(0.5, 15.0, 0.5);
        Vec3[] insulators = {
                new Vec3(-2, 12, -2),
                new Vec3(2, 12, -2),
                new Vec3(2, 12, 2),
                new Vec3(-2, 12, 2)
        };

        poseStack.pushPose();
        // Shift the poseStack so 0,0,0 is the center of the controller block
        // This makes the math way easier

        for (int i = 0; i < insulators.length; i++) {
            // Flicker logic: different arcs fire at different times
            if ((time + i * 7) % 10 < 4) {
                renderArc(poseStack, vc, topCenter, insulators[i], time);
            }
        }

        poseStack.popPose();
    }

    private void renderArc(PoseStack stack, VertexConsumer vc, Vec3 start, Vec3 end, long time) {
        Matrix4f pose = stack.last().pose();

        // White core
        drawTeslaLine(vc, pose, start, end, 5, 255, 255, 255, 0.9f, time);
        // Blue outer glow
        drawTeslaLine(vc, pose, start, end, 4, 100, 150, 255, 0.4f, time);
    }

    private void drawTeslaLine(VertexConsumer vc, Matrix4f pose, Vec3 s, Vec3 e,
                               int depth, int r, int g, int b, float a, long time) {
        if (depth == 0) {
            vc.vertex(pose, (float)s.x, (float)s.y, (float)s.z).color(r/255f, g/255f, b/255f, a).endVertex();
            vc.vertex(pose, (float)e.x, (float)e.y, (float)e.z).color(r/255f, g/255f, b/255f, a).endVertex();
            return;
        }

        Vec3 mid = s.add(e).scale(0.5);

        // Use time in the seed so the lightning "crawls" or changes every tick
        // But use s and e in the seed so the arcs between different points look unique
        RandomSource rand = RandomSource.create(time / 2 + s.hashCode() + e.hashCode());

        float jitter = 0.3f * depth;
        mid = mid.add(
                (rand.nextFloat() - 0.5) * jitter,
                (rand.nextFloat() - 0.5) * jitter,
                (rand.nextFloat() - 0.5) * jitter
        );

        drawTeslaLine(vc, pose, s, mid, depth - 1, r, g, b, a, time);
        drawTeslaLine(vc, pose, mid, e, depth - 1, r, g, b, a, time);
    }
}