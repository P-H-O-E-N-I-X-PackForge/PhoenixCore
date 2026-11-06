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
import net.phoenix.core.integration.drone.network.S2CDroneSearchPacket;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public final class DroneSearchRenderer {

    private record Entry(String query, java.util.List<S2CDroneSearchPacket.Hit> hits, long tick) {}

    private static final long EXPIRE_TICKS = 100;
    private static final double VIEW_DISTANCE = 64;
    private static final Map<BlockPos, Entry> ENTRIES = new HashMap<>();

    private DroneSearchRenderer() {}

    public static void accept(S2CDroneSearchPacket msg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        if (msg.hits().isEmpty()) ENTRIES.remove(msg.controller());
        else ENTRIES.put(msg.controller(), new Entry(msg.query(), msg.hits(), level.getGameTime()));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || ENTRIES.isEmpty()) return;
        long now = mc.level.getGameTime();
        ENTRIES.values().removeIf(e -> now - e.tick() > EXPIRE_TICKS);
        if (ENTRIES.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float time = (now + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 4f;
        float alpha = 0.6f + 0.4f * (float) Math.sin(time);

        var lines = buffers.getBuffer(RenderType.lines());
        for (Entry e : ENTRIES.values()) {
            for (var hit : e.hits()) {
                if (hit.pos().distToCenterSqr(cam.x, cam.y, cam.z) > VIEW_DISTANCE * VIEW_DISTANCE) continue;
                AABB box = new AABB(hit.pos()).inflate(0.04).move(-cam.x, -cam.y, -cam.z);
                LevelRenderer.renderLineBox(pose, lines, box, 1f, 0.35f, 1f, alpha);
            }
        }
        buffers.endBatch(RenderType.lines());

        Font font = mc.font;
        for (Entry e : ENTRIES.values()) {
            for (var hit : e.hits()) {
                double dx = hit.pos().getX() + 0.5 - cam.x, dy = hit.pos().getY() + 1.25 - cam.y,
                        dz = hit.pos().getZ() + 0.5 - cam.z;
                double d2 = dx * dx + dy * dy + dz * dz;
                if (d2 > VIEW_DISTANCE * VIEW_DISTANCE) continue;
                pose.pushPose();
                pose.translate(dx, dy, dz);
                pose.mulPose(event.getCamera().rotation());
                float scale = 0.025f * (float) Math.max(1.0, Math.sqrt(d2) / 12.0);
                pose.scale(scale, -scale, scale);
                Matrix4f m = pose.last().pose();
                String text = hit.label();
                font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFF77FF, true, m, buffers,
                        Font.DisplayMode.SEE_THROUGH, 0x40000000, LightTexture.FULL_BRIGHT);
                pose.popPose();
            }
        }
        buffers.endBatch();
    }
}
