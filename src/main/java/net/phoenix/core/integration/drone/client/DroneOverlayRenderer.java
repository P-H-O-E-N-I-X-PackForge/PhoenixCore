package net.phoenix.core.integration.drone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.phoenix.core.integration.drone.DroneItems;
import net.phoenix.core.integration.drone.network.DroneTargetView;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public final class DroneOverlayRenderer {

    private static final double LABEL_DISTANCE = 48;

    private DroneOverlayRenderer() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return;
        if (!player.getMainHandItem().is(DroneItems.DRONE_WAND.get()) &&
                !player.getOffhandItem().is(DroneItems.DRONE_WAND.get())) {
            return;
        }
        var entries = DroneOverlayCache.live();
        if (entries.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        float time = (mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 6f;
        float pulse = 0.55f + 0.45f * (float) Math.sin(time);

        for (var entry : entries.entrySet()) {

            AABB self = new AABB(entry.getKey()).inflate(0.03).move(-cam.x, -cam.y, -cam.z);
            LevelRenderer.renderLineBox(pose, lines, self, 0.35f, 0.9f, 1f, 1f);

            for (DroneTargetView view : entry.getValue().targets()) {
                int argb = colour(view);
                float a = problem(view) ? pulse : 1f;
                AABB box = new AABB(view.pos()).inflate(0.02).move(-cam.x, -cam.y, -cam.z);
                LevelRenderer.renderLineBox(pose, lines, box, (argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f,
                        (argb & 255) / 255f, a);
            }
        }
        buffers.endBatch(RenderType.lines());

        Font font = mc.font;
        for (var entry : entries.entrySet()) {
            for (DroneTargetView view : entry.getValue().targets()) {
                BlockPos p = view.pos();
                double dx = p.getX() + 0.5 - cam.x, dy = p.getY() + 1.4 - cam.y, dz = p.getZ() + 0.5 - cam.z;
                if (dx * dx + dy * dy + dz * dz > LABEL_DISTANCE * LABEL_DISTANCE) continue;
                String label = label(view);
                String second = view.groupName().isEmpty() ? "P" + view.priority() :
                        view.groupName() + " \u00b7 P" + view.priority();
                drawLabel(pose, buffers, font, event, dx, dy, dz, label, colour(view), 0);
                drawLabel(pose, buffers, font, event, dx, dy - 0.28, dz, second, 0xFFAAAAAA, 0);
            }
        }
        buffers.endBatch();
    }

    private static void drawLabel(PoseStack pose, MultiBufferSource buffers, Font font, RenderLevelStageEvent event,
                                  double x, double y, double z, String text, int colour, int unused) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(event.getCamera().rotation());
        float scale = 0.025f * (float) Math.max(1.0, Math.sqrt(x * x + y * y + z * z) / 12.0);
        pose.scale(scale, -scale, scale);
        Matrix4f m = pose.last().pose();
        float w = -font.width(text) / 2f;
        font.drawInBatch(text, w, 0, colour, true, m, buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000,
                LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    private static boolean problem(DroneTargetView v) {
        if (!v.controllable()) return false;
        return !v.workingEnabled() || v.status().equals("WAITING") || v.status().equals("SUSPEND");
    }

    static int colour(DroneTargetView v) {
        return net.phoenix.core.integration.drone.network.DroneStatus.colour(v);
    }

    static String label(DroneTargetView v) {
        if (!v.controllable()) return "N/A";
        if (!v.workingEnabled()) return "DISABLED";
        return switch (v.status()) {
            case "WORKING" -> v.duration() > 0 ? "RUNNING " + (v.progress() * 100 / v.duration()) + "%" : "RUNNING";
            case "WAITING" -> "WAITING";
            case "SUSPEND" -> "SUSPENDED";
            default -> "IDLE";
        };
    }
}
