package net.phoenix.core.integration.conflux.dimension.physics;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PhysicsDebugRenderer {

    private static boolean debugEnabled = false;

    public static void toggleDebug() {
        debugEnabled = !debugEnabled;
        System.out.println("[PhoenixCore] Physics debug visualization: " + (debugEnabled ? "ENABLED" : "DISABLED"));
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!debugEnabled) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) {
            return;
        }

        String dimensionId = getDimensionId(level);
        PhysicsRegistry registry = PhysicsRegistry.getInstance();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0F);

        for (GravityZone zone : registry.getGravityZones(dimensionId)) {
            renderGravityZone(event.getPoseStack(), zone, mc);
        }

        for (MovingPlatform platform : registry.getPlatforms(dimensionId)) {
            renderPlatform(event.getPoseStack(), platform, mc);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);
    }

    private static void renderGravityZone(PoseStack poseStack, GravityZone zone, Minecraft mc) {
        poseStack.pushPose();

        var center = zone.getCenter();
        double playerX = mc.gameRenderer.getMainCamera().getPosition().x;
        double playerY = mc.gameRenderer.getMainCamera().getPosition().y;
        double playerZ = mc.gameRenderer.getMainCamera().getPosition().z;

        poseStack.translate(center.x - playerX, center.y - playerY, center.z - playerZ);

        float r, g, b;
        float multiplier = zone.getGravityMultiplier();

        if (multiplier < 0.3f) {
            r = 0.0f; g = 1.0f; b = 1.0f;
        } else if (multiplier < 0.7f) {
            r = 0.5f; g = 0.5f; b = 1.0f;
        } else {
            r = 1.0f; g = 1.0f; b = 0.0f;
        }

        renderSphere(poseStack, zone.getRadius(), r, g, b, 0.5f);

        poseStack.popPose();
    }

    private static void renderPlatform(PoseStack poseStack, MovingPlatform platform, Minecraft mc) {
        poseStack.pushPose();

        var pos = platform.getPosition();
        double playerX = mc.gameRenderer.getMainCamera().getPosition().x;
        double playerY = mc.gameRenderer.getMainCamera().getPosition().y;
        double playerZ = mc.gameRenderer.getMainCamera().getPosition().z;

        poseStack.translate(pos.x - playerX, pos.y - playerY, pos.z - playerZ);

        float r = 1.0f, g = 1.0f, b = 1.0f;

        switch (platform.getType()) {
            case CONVEYOR_BELT -> { r = 1.0f; g = 0.5f; b = 0.0f; }
            case ELEVATOR_UP -> { r = 0.0f; g = 1.0f; b = 0.0f; }
            case ELEVATOR_DOWN -> { r = 1.0f; g = 0.0f; b = 0.0f; }
            case SPIRAL -> { r = 1.0f; g = 0.0f; b = 1.0f; }
        }

        var size = platform.getSize();
        renderBox(poseStack, size.x, size.y, size.z, r, g, b, 0.5f);

        var velocity = platform.getVelocity();
        if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
            renderArrow(poseStack, velocity.x * 5, velocity.y * 5, velocity.z * 5, r, g, b);
        }

        poseStack.popPose();
    }

    private static void renderSphere(PoseStack poseStack, double radius, float r, float g, float b, float alpha) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();

        for (int lat = 0; lat <= 8; lat++) {
            double theta = (lat * Math.PI) / 8.0;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            double lastX = 0, lastY = 0, lastZ = 0;
            for (int lon = 0; lon <= 16; lon++) {
                double phi = (lon * 2 * Math.PI) / 16.0;
                double x = radius * sinTheta * Math.cos(phi);
                double y = radius * cosTheta;
                double z = radius * sinTheta * Math.sin(phi);

                if (lon > 0) {
                    addLine(builder, matrix, lastX, lastY, lastZ, x, y, z, r, g, b, alpha);
                }
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
        tesselator.end();
    }

    private static void renderBox(PoseStack poseStack, double width, double height, double depth,
                                  float r, float g, float b, float alpha) {
        double w = width / 2.0;
        double h = height / 2.0;
        double d = depth / 2.0;

        AABB box = new AABB(-w, -h, -d, w, h, d);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        LevelRenderer.renderLineBox(poseStack, builder, box, r, g, b, alpha);

        tesselator.end();
    }

    private static void renderArrow(PoseStack poseStack, double x, double y, double z, float r, float g, float b) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        addLine(builder, matrix, 0, 0, 0, x, y, z, r, g, b, 1.0f);

        tesselator.end();
    }

    private static void addLine(VertexConsumer consumer, Matrix4f matrix, double x1, double y1, double z1,
                                double x2, double y2, double z2, float r, float g, float b, float a) {
        consumer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a).endVertex();
    }

    private static String getDimensionId(Level level) {

        String path = level.dimension().location().getPath();
        String discipline = path.startsWith("conflux/") ? path.substring("conflux/".length()) : path;

        if (discipline.startsWith("phoenix")) return "phoenix";
        if (discipline.startsWith("sculk")) return "sculk";
        if (discipline.startsWith("void")) return "void";
        if (discipline.startsWith("sealed_a")) return "sealed_a";
        if (discipline.startsWith("sealed_b")) return "sealed_b";

        return "";
    }
}