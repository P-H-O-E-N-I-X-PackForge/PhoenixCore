package net.phoenix.core.integration.drone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public final class DroneFleetRenderer {

    private static final double VIEW_DISTANCE = 64;

    private static final int DWELL = 50;

    private DroneFleetRenderer() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var entries = DroneFleetCache.live();
        if (entries.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        float time = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer solid = buffers.getBuffer(RenderType.debugFilledBox());

        record Beam(Vec3 from, Vec3 to, int colour, float phase) {}
        java.util.List<Beam> beams = new java.util.ArrayList<>();

        for (var entry : entries.entrySet()) {
            BlockPos c = entry.getKey();
            if (c.distSqr(BlockPos.containing(cam)) > VIEW_DISTANCE * VIEW_DISTANCE) continue;
            var fleet = entry.getValue();
            int n = Math.max(1, fleet.drones());
            for (int i = 0; i < n; i++) {
                double angle = time * 0.035 + i * (Math.PI * 2 / n);
                double bob = Math.sin(time * 0.11 + i * 1.7) * 0.15;
                double x = c.getX() + 0.5 + Math.cos(angle) * 1.7;
                double y = c.getY() + 2.5 + bob;
                double z = c.getZ() + 0.5 + Math.sin(angle) * 1.7;

                int beamColour = 0xFF59E6FF;
                var targets = fleet.targets();
                if (!targets.isEmpty()) {
                    int slot = (int) ((time / DWELL) + i * 3) % targets.size();
                    var t = targets.get(slot);
                    beamColour = t.colour();
                    float phase = (time % DWELL) / DWELL;
                    beams.add(new Beam(new Vec3(x, y - 0.08, z),
                            new Vec3(t.pos().getX() + 0.5, t.pos().getY() + 0.5, t.pos().getZ() + 0.5), beamColour,
                            phase));
                }
                pose.pushPose();
                pose.translate(x - cam.x, y - cam.y, z - cam.z);
                pose.mulPose(Axis.YP.rotation((float) -angle + (float) Math.PI / 2));
                pose.mulPose(Axis.ZP.rotation((float) Math.sin(time * 0.11 + i) * 0.08f));
                drawDrone(pose, solid, time + i * 7, beamColour);
                pose.popPose();
            }
        }
        buffers.endBatch(RenderType.debugFilledBox());

        if (!beams.isEmpty()) {
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            for (Beam b : beams) {
                line(pose, lines, b.from.subtract(cam), b.to.subtract(cam), b.colour);
            }
            buffers.endBatch(RenderType.lines());

            VertexConsumer spark = buffers.getBuffer(RenderType.debugFilledBox());
            for (Beam b : beams) {
                Vec3 p = b.from.lerp(b.to, b.phase).subtract(cam);
                float r = (b.colour >> 16 & 255) / 255f, g = (b.colour >> 8 & 255) / 255f, bl = (b.colour & 255) / 255f;
                LevelRenderer.addChainedFilledBoxVertices(pose, spark, p.x - 0.07, p.y - 0.07, p.z - 0.07, p.x + 0.07,
                        p.y + 0.07, p.z + 0.07, r, g, bl, 0.9f);
            }
            buffers.endBatch(RenderType.debugFilledBox());
        }
    }

    private static void line(PoseStack pose, VertexConsumer consumer, Vec3 a, Vec3 b, int argb) {
        Vec3 d = b.subtract(a).normalize();
        var last = pose.last();
        consumer.addVertex(last, (float) a.x, (float) a.y, (float) a.z).setColor(argb)
                .setNormal(last, (float) d.x, (float) d.y, (float) d.z);
        consumer.addVertex(last, (float) b.x, (float) b.y, (float) b.z).setColor(argb)
                .setNormal(last, (float) d.x, (float) d.y, (float) d.z);
    }

    private static void drawDrone(PoseStack pose, VertexConsumer c, float time, int glow) {
        box(pose, c, 0.2, 0.05, 0.2, 0.17f, 0.19f, 0.23f, 1f);
        float gr = (glow >> 16 & 255) / 255f, gg = (glow >> 8 & 255) / 255f, gb = (glow & 255) / 255f;
        LevelRenderer.addChainedFilledBoxVertices(pose, c, -0.07, 0.05, -0.07, 0.07, 0.11, 0.07, gr, gg, gb, 0.95f);
        LevelRenderer.addChainedFilledBoxVertices(pose, c, -0.05, -0.1, -0.05, 0.05, -0.05, 0.05, gr, gg, gb, 0.9f);
        for (int arm = 0; arm < 2; arm++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(45 + arm * 90));
            box(pose, c, 0.36, 0.014, 0.022, 0.1f, 0.11f, 0.14f, 1f);
            pose.popPose();
        }
        for (int r = 0; r < 4; r++) {
            double ang = Math.PI / 4 + r * Math.PI / 2;
            pose.pushPose();
            pose.translate(Math.cos(ang) * 0.255, 0.05, Math.sin(ang) * 0.255);
            pose.mulPose(Axis.YP.rotation(time * 1.6f * (r % 2 == 0 ? 1 : -1)));
            box(pose, c, 0.12, 0.006, 0.012, 0.8f, 0.85f, 0.9f, 0.75f);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            box(pose, c, 0.12, 0.006, 0.012, 0.8f, 0.85f, 0.9f, 0.75f);
            pose.popPose();
        }
    }

    private static void box(PoseStack pose, VertexConsumer c, double hx, double hy, double hz, float r, float g,
                            float b, float a) {
        LevelRenderer.addChainedFilledBoxVertices(pose, c, -hx, -hy, -hz, hx, hy, hz, r, g, b, a);
    }
}
