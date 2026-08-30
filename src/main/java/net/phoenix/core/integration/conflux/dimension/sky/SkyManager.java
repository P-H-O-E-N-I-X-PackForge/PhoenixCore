package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class SkyManager {

    private static final SkyManager INSTANCE = new SkyManager();

    private final Map<String, SkyRenderer> skyRenderers = new HashMap<>();
    private String currentDimension = "";
    private SkyRenderer activeSkyRenderer = null;
    private boolean skyRenderingEnabled = true;

    public static SkyManager getInstance() {
        return INSTANCE;
    }

    public void init(Level level) {
        
        skyRenderers.put("phoenix", new PhoenixSkyRenderer("phoenix", level));
        skyRenderers.put("sculk", new SculkSkyRenderer("sculk", level));
        skyRenderers.put("void", new VoidSkyRenderer("void", level));
        skyRenderers.put("sealed_a", new SealedASkyRenderer("sealed_a", level));
        skyRenderers.put("sealed_b", new SealedBSkyRenderer("sealed_b", level));

        System.out.println("[PhoenixCore Sky] Initialized " + skyRenderers.size() + " sky renderers");
    }

    public void onDimensionChange(String dimensionId) {
        if (dimensionId.equals(currentDimension)) {
            return;
        }

        currentDimension = dimensionId;
        activeSkyRenderer = skyRenderers.get(dimensionId);

        if (activeSkyRenderer != null) {
            System.out.println("[PhoenixCore Sky] Switched to " + dimensionId + " sky");
        }
    }

    public void update() {
        if (activeSkyRenderer != null) {
            activeSkyRenderer.update();
        }
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, float skyBrightness) {
        if (!skyRenderingEnabled || activeSkyRenderer == null) {
            return;
        }

        if (!activeSkyRenderer.shouldRender()) {
            return;
        }

        try {
            poseStack.pushPose();

            float yaw = activeSkyRenderer.getCameraYaw(partialTick);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));

            activeSkyRenderer.render(poseStack, bufferSource, partialTick, skyBrightness);

            poseStack.popPose();
        } catch (Exception e) {
            System.err.println("[PhoenixCore Sky] Error rendering sky: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void toggleSkyRendering() {
        skyRenderingEnabled = !skyRenderingEnabled;
        System.out.println("[PhoenixCore Sky] Sky rendering: " + (skyRenderingEnabled ? "ENABLED" : "DISABLED"));
    }

    public boolean isSkyRenderingEnabled() {
        return skyRenderingEnabled;
    }

    public SkyRenderer getActiveSkyRenderer() {
        return activeSkyRenderer;
    }

    public SkyRenderer getSkyRenderer(String dimensionId) {
        return skyRenderers.get(dimensionId);
    }

    public String getCurrentDimension() {
        return currentDimension;
    }

    public void cleanup() {
        for (SkyRenderer renderer : skyRenderers.values()) {
            renderer.cleanup();
        }
        skyRenderers.clear();
        activeSkyRenderer = null;
    }

    public void logStatistics() {
        System.out.println("[PhoenixCore Sky] Statistics:");
        System.out.println("  Active Sky Renderers: " + skyRenderers.size());
        System.out.println("  Current Dimension: " + (currentDimension.isEmpty() ? "None" : currentDimension));
        System.out.println("  Sky Rendering: " + (skyRenderingEnabled ? "Enabled" : "Disabled"));
    }
}
