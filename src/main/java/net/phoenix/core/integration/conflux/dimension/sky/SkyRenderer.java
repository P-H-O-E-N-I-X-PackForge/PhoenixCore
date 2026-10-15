package net.phoenix.core.integration.conflux.dimension.sky;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.vertex.PoseStack;

public abstract class SkyRenderer {

    protected final String dimensionId;
    protected final Level level;
    protected float skyBrightness;
    protected long worldTime;

    public SkyRenderer(String dimensionId, Level level) {
        this.dimensionId = dimensionId;
        this.level = level;
        this.skyBrightness = 1.0f;
        this.worldTime = 0;
    }

    public abstract void render(PoseStack poseStack, MultiBufferSource bufferSource,
                                float partialTick, float skyBrightness);

    public void update() {
        if (level != null) {
            worldTime = level.getGameTime();
            skyBrightness = level.getSkyDarken() == 0 ? 1.0f : 0.5f;
        }
    }

    public float getCameraYaw(float partialTick) {
        if (level != null) {
            return level.getGameTime() * 0.005f;
        }
        return 0.0f;
    }

    public void cleanup() {}

    public String getDimensionId() {
        return dimensionId;
    }

    public boolean shouldRender() {
        return true;
    }
}
