package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

public class PlanetOrbit {

    public enum PlanetType {
        PLANET,
        MOON,
        STAR,
        NEBULA
    }

    private final String name;
    private final PlanetType type;
    private final float orbitRadius;
    private final float orbitSpeed;
    private final float bodyRadius;
    private final Vec3 orbitCenter;
    private final int color;
    private final boolean hasGlow;
    private final float glowIntensity;

    public PlanetOrbit(String name, PlanetType type, float orbitRadius, float orbitSpeed,
                       float bodyRadius, Vec3 orbitCenter, int color, boolean hasGlow,
                       float glowIntensity) {
        this.name = name;
        this.type = type;
        this.orbitRadius = orbitRadius;
        this.orbitSpeed = orbitSpeed;
        this.bodyRadius = bodyRadius;
        this.orbitCenter = orbitCenter;
        this.color = color;
        this.hasGlow = hasGlow;
        this.glowIntensity = glowIntensity;
    }

    public Vec3 getPosition(long worldTime) {
        float angle = (worldTime * orbitSpeed) % 360f;
        float radians = (float) Math.toRadians(angle);

        float x = (float) Math.cos(radians) * orbitRadius;
        float z = (float) Math.sin(radians) * orbitRadius;
        float y = (float) Math.sin(radians * 0.5f) * (orbitRadius * 0.3f); 

        return orbitCenter.add(x, y, z);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, long worldTime) {
        Vec3 position = getPosition(worldTime);

        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);

        renderPlanetSphere(poseStack, bufferSource);

        if (hasGlow) {
            renderGlow(poseStack, bufferSource);
        }

        poseStack.popPose();
    }

    private static final Vec3 LIGHT_DIR = new Vec3(1.0, 0.25, -0.15).normalize();

    private static final double AMBIENT_FLOOR = 0.15;

    private void renderPlanetSphere(PoseStack poseStack, MultiBufferSource bufferSource) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder consumer = tesselator.getBuilder();
        consumer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        int baseR = (color >> 16) & 0xFF;
        int baseG = (color >> 8) & 0xFF;
        int baseB = color & 0xFF;

        int latSegments = 16;
        int lonSegments = 32;

        for (int lat = 0; lat < latSegments; lat++) {
            for (int lon = 0; lon < lonSegments; lon++) {

                float lat1 = ((lat / (float) latSegments) - 0.5f) * (float) Math.PI;
                float lat2 = (((lat + 1) / (float) latSegments) - 0.5f) * (float) Math.PI;
                float lon1 = (lon / (float) lonSegments) * 2f * (float) Math.PI;
                float lon2 = ((lon + 1) / (float) lonSegments) * 2f * (float) Math.PI;

                float x1 = (float) (bodyRadius * Math.cos(lat1) * Math.cos(lon1));
                float y1 = (float) (bodyRadius * Math.sin(lat1));
                float z1 = (float) (bodyRadius * Math.cos(lat1) * Math.sin(lon1));

                float x2 = (float) (bodyRadius * Math.cos(lat1) * Math.cos(lon2));
                float y2 = (float) (bodyRadius * Math.sin(lat1));
                float z2 = (float) (bodyRadius * Math.cos(lat1) * Math.sin(lon2));

                float x3 = (float) (bodyRadius * Math.cos(lat2) * Math.cos(lon2));
                float y3 = (float) (bodyRadius * Math.sin(lat2));
                float z3 = (float) (bodyRadius * Math.cos(lat2) * Math.sin(lon2));

                float x4 = (float) (bodyRadius * Math.cos(lat2) * Math.cos(lon1));
                float y4 = (float) (bodyRadius * Math.sin(lat2));
                float z4 = (float) (bodyRadius * Math.cos(lat2) * Math.sin(lon1));

                int[] c1 = litColor(x1, y1, z1, lat1, baseR, baseG, baseB);
                int[] c2 = litColor(x2, y2, z2, lat1, baseR, baseG, baseB);
                int[] c3 = litColor(x3, y3, z3, lat2, baseR, baseG, baseB);
                int[] c4 = litColor(x4, y4, z4, lat2, baseR, baseG, baseB);

                addVertex(consumer, poseStack, x1, y1, z1, c1[0], c1[1], c1[2], 255);
                addVertex(consumer, poseStack, x2, y2, z2, c2[0], c2[1], c2[2], 255);
                addVertex(consumer, poseStack, x3, y3, z3, c3[0], c3[1], c3[2], 255);

                addVertex(consumer, poseStack, x1, y1, z1, c1[0], c1[1], c1[2], 255);
                addVertex(consumer, poseStack, x3, y3, z3, c3[0], c3[1], c3[2], 255);
                addVertex(consumer, poseStack, x4, y4, z4, c4[0], c4[1], c4[2], 255);
            }
        }

        BufferUploader.drawWithShader(consumer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static int[] litColor(float x, float y, float z, float latitude, int baseR, int baseG, int baseB) {
        double len = Math.sqrt(x * x + y * y + z * z);
        double nx = x / len;
        double ny = y / len;
        double nz = z / len;

        double dot = nx * LIGHT_DIR.x + ny * LIGHT_DIR.y + nz * LIGHT_DIR.z;
        double brightness = AMBIENT_FLOOR + (1.0 - AMBIENT_FLOOR) * Math.max(0.0, dot);
        double band = 0.92 + 0.08 * Math.sin(latitude * 7.0);
        double factor = brightness * band;

        return new int[] {
                clamp255(baseR * factor),
                clamp255(baseG * factor),
                clamp255(baseB * factor)
        };
    }

    private static int clamp255(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    private void renderGlow(PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder consumer = tesselator.getBuilder();
        consumer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        int r = (int) ((color >> 16 & 0xFF) * glowIntensity);
        int g = (int) ((color >> 8 & 0xFF) * glowIntensity);
        int b = (int) ((color & 0xFF) * glowIntensity);
        int alpha = 60;

        float glowRadius = bodyRadius * 1.35f;
        int latSegments = 8;
        int lonSegments = 16;

        for (int lat = 0; lat < latSegments; lat++) {
            for (int lon = 0; lon < lonSegments; lon++) {
                float lat1 = ((lat / (float) latSegments) - 0.5f) * (float) Math.PI;
                float lat2 = (((lat + 1) / (float) latSegments) - 0.5f) * (float) Math.PI;
                float lon1 = (lon / (float) lonSegments) * 2f * (float) Math.PI;
                float lon2 = ((lon + 1) / (float) lonSegments) * 2f * (float) Math.PI;

                float x1 = (float) (glowRadius * Math.cos(lat1) * Math.cos(lon1));
                float y1 = (float) (glowRadius * Math.sin(lat1));
                float z1 = (float) (glowRadius * Math.cos(lat1) * Math.sin(lon1));

                float x2 = (float) (glowRadius * Math.cos(lat1) * Math.cos(lon2));
                float y2 = y1;
                float z2 = (float) (glowRadius * Math.cos(lat1) * Math.sin(lon2));

                float x3 = (float) (glowRadius * Math.cos(lat2) * Math.cos(lon2));
                float y3 = (float) (glowRadius * Math.sin(lat2));
                float z3 = (float) (glowRadius * Math.cos(lat2) * Math.sin(lon2));

                float x4 = (float) (glowRadius * Math.cos(lat2) * Math.cos(lon1));
                float y4 = y3;
                float z4 = (float) (glowRadius * Math.cos(lat2) * Math.sin(lon1));

                addVertex(consumer, poseStack, x1, y1, z1, r, g, b, alpha);
                addVertex(consumer, poseStack, x2, y2, z2, r, g, b, alpha);
                addVertex(consumer, poseStack, x3, y3, z3, r, g, b, alpha);

                addVertex(consumer, poseStack, x1, y1, z1, r, g, b, alpha);
                addVertex(consumer, poseStack, x3, y3, z3, r, g, b, alpha);
                addVertex(consumer, poseStack, x4, y4, z4, r, g, b, alpha);
            }
        }

        BufferUploader.drawWithShader(consumer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private void addVertex(VertexConsumer consumer, PoseStack poseStack,
                          float x, float y, float z, int r, int g, int b, int a) {
        consumer.vertex(poseStack.last().pose(), x, y, z)
                .color(r, g, b, a)
                .endVertex();
    }

    public String getName() { return name; }
    public PlanetType getType() { return type; }
    public float getOrbitRadius() { return orbitRadius; }
    public float getBodyRadius() { return bodyRadius; }
    public Vec3 getOrbitCenter() { return orbitCenter; }

    @Override
    public String toString() {
        return String.format("PlanetOrbit[%s type=%s orbit=%.1f body=%.1f glow=%b]",
            name, type, orbitRadius, bodyRadius, hasGlow);
    }
}
