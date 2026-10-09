package net.phoenix.core.integration.conflux.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.mixin.accessor.GameRendererAccessor;
import net.phoenix.core.mixin.accessor.PostChainAccessor;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AxiomShaderManager {

    private static @Nullable ResourceLocation activeLocation = null;
    private static float elapsed = 0f;

    private static final Map<ResourceLocation, PostChain> CHAINS = new HashMap<>();
    private static @Nullable Object chainsOwner;

    private AxiomShaderManager() {}

    private static void checkOwner() {
        Object owner = Minecraft.getInstance().getResourceManager();
        if (chainsOwner != owner) {

            release();
            chainsOwner = owner;
        }
    }

    public static boolean preload(ResourceLocation location) {
        checkOwner();
        if (CHAINS.containsKey(location)) return true;
        Minecraft mc = Minecraft.getInstance();
        try {
            CHAINS.put(location, new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                    mc.getMainRenderTarget(), location));
            return true;
        } catch (Exception e) {
            return false;
        } finally {

            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    public static void release() {
        deactivate();
        CHAINS.values().forEach(chain -> {
            try {
                chain.close();
            } catch (Exception ignored) {}
        });
        CHAINS.clear();
    }

    public static void activate(@Nullable ResourceLocation location) {
        if (location == null) {
            deactivate();
            return;
        }
        if (location.equals(activeLocation)) return;

        Minecraft mc = Minecraft.getInstance();
        if (!preload(location)) {
            activeLocation = null;
            return;
        }
        PostChain chain = CHAINS.get(location);
        chain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        mc.getMainRenderTarget().bindWrite(true);

        GameRendererAccessor renderer = (GameRendererAccessor) mc.gameRenderer;
        renderer.phoenix$setPostEffect(chain);
        renderer.phoenix$setEffectActive(true);
        activeLocation = location;
        elapsed = 0f;
    }

    public static void tick(float dt) {
        if (activeLocation == null) return;
        elapsed += dt;
        pushUniform1f("Time", elapsed);
    }

    public static void pushFrameData(
                                     float screenW, float screenH,
                                     float cursorX, float cursorY,
                                     float[] nodeScreenXY, int nodeCount,
                                     float[] nodeStrength,
                                     float[] rippleOriginXY, float[] rippleAge, int rippleCount) {
        if (activeLocation == null) return;
        Minecraft mc = Minecraft.getInstance();
        PostChain effect = mc.gameRenderer.currentEffect();
        if (effect == null) return;

        List<PostPass> passes;
        try {
            passes = ((PostChainAccessor) effect).getPasses();
        } catch (Exception e) {
            return;
        }

        float invW = screenW > 0 ? 1f / screenW : 0f;
        float invH = screenH > 0 ? 1f / screenH : 0f;

        int safeCount = Math.min(nodeCount, 8);
        float[] nodeUV = new float[16];
        for (int i = 0; i < safeCount; i++) {
            nodeUV[i * 2] = nodeScreenXY[i * 2] * invW;
            nodeUV[i * 2 + 1] = nodeScreenXY[i * 2 + 1] * invH;
        }

        int safeRipples = Math.min(rippleCount, 4);
        float[] rippleUV = new float[8];
        for (int i = 0; i < safeRipples; i++) {
            rippleUV[i * 2] = rippleOriginXY[i * 2] * invW;
            rippleUV[i * 2 + 1] = rippleOriginXY[i * 2 + 1] * invH;
        }

        float cursorU = cursorX * invW;
        float cursorV = cursorY * invH;

        for (PostPass pass : passes) {
            var eff = pass.getEffect();
            try {

                var ssSampler = eff.getUniform("ScreenSize");
                if (ssSampler != null) ssSampler.set(screenW, screenH);

                var cursor = eff.getUniform("CursorUV");
                if (cursor != null) cursor.set(cursorU, cursorV);

                var heatCount = eff.getUniform("HeatCount");
                if (heatCount != null) heatCount.set(safeCount);

                var heatSrc = eff.getUniform("HeatSources");
                if (heatSrc != null) heatSrc.set(nodeUV);

                var heatStr = eff.getUniform("HeatStrength");
                if (heatStr != null) {
                    float[] str = new float[8];
                    for (int i = 0; i < safeCount; i++)
                        str[i] = (nodeStrength != null && i < nodeStrength.length) ? nodeStrength[i] : 1f;
                    heatStr.set(str);
                }

                var nodeCountU = eff.getUniform("NodeCount");
                if (nodeCountU != null) nodeCountU.set(safeCount);

                var nodePos = eff.getUniform("NodePositions");
                if (nodePos != null) nodePos.set(nodeUV);

                var ripCnt = eff.getUniform("RippleCount");
                if (ripCnt != null) ripCnt.set(safeRipples);

                var ripOrig = eff.getUniform("RippleOrigins");
                if (ripOrig != null) ripOrig.set(rippleUV);

                var ripA = eff.getUniform("RippleAge");
                if (ripA != null) {
                    float[] ages = new float[4];
                    for (int i = 0; i < safeRipples; i++)
                        ages[i] = rippleAge != null && i < rippleAge.length ? rippleAge[i] : 0f;
                    ripA.set(ages);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void applyPostChain() {}

    public static void deactivate() {
        if (activeLocation == null) return;

        GameRendererAccessor renderer = (GameRendererAccessor) Minecraft.getInstance().gameRenderer;
        renderer.phoenix$setEffectActive(false);
        renderer.phoenix$setPostEffect(null);
        activeLocation = null;
    }

    public static boolean isActive() {
        return activeLocation != null;
    }

    private static void pushUniform1f(String name, float value) {
        Minecraft mc = Minecraft.getInstance();
        PostChain effect = mc.gameRenderer.currentEffect();
        if (effect == null) return;
        try {
            for (PostPass pass : ((PostChainAccessor) effect).getPasses()) {
                var u = pass.getEffect().getUniform(name);
                if (u != null) u.set(value);
            }
        } catch (Exception ignored) {}
    }
}
