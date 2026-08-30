package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.integration.ars_nouveau.client.SoulVisionManager;
import net.phoenix.core.mixin.accessor.PostChainAccessor;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
    private void phoenixcore$injectUniforms(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (!SoulVisionManager.hasDensityData()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer.currentEffect() instanceof PostChainAccessor accessor) {
            for (PostPass pass : accessor.getPasses()) {
                if (!pass.getEffect().getName().equals(SoulVisionManager.SOUL_VISION_EFFECT_NAME)) continue;

                var effect = pass.getEffect();

                // Read the actual matrices the just-finished 3D pass used, straight off
                // RenderSystem, rather than trying to capture them mid-frame via an event -
                // nothing touches these globals between the level render ending and this
                // PostChain.process() call, so they're guaranteed to still hold that frame's
                // real camera transform here.
                Matrix4f invProjMat = new Matrix4f(RenderSystem.getProjectionMatrix()).invert();
                Matrix4f invViewMat = new Matrix4f(RenderSystem.getModelViewStack().last().pose()).invert();
                Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

                Uniform invProj = effect.getUniform("InvProjMat");
                if (invProj != null) invProj.set(invProjMat);

                Uniform invView = effect.getUniform("InvViewMat");
                if (invView != null) invView.set(invViewMat);

                Uniform camUniform = effect.getUniform("CameraPos");
                if (camUniform != null) {
                    camUniform.set((float) camPos.x, (float) camPos.y, (float) camPos.z);
                }

                Uniform center = effect.getUniform("CenterChunk");
                if (center != null) {
                    center.set(SoulVisionManager.getCenterChunkX(), SoulVisionManager.getCenterChunkZ());
                }

                Uniform radius = effect.getUniform("GridRadius");
                if (radius != null) radius.set(SoulVisionManager.getGridRadius());

                effect.setSampler("DensitySampler", SoulVisionManager::getDensityTextureId);
            }
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/PostChain;process(F)V",
                     shift = At.Shift.AFTER))
    private void phoenixcore$runScreenEffects(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (renderLevel) {
            WorldFXManager.applyScreenEffects(partialTicks);
        }
    }
}
