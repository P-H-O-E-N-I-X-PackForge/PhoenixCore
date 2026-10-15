package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.phoenix.core.client.worldfx.SkyRenderContext;
import net.phoenix.core.client.worldfx.WorldFXManager;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At("RETURN"),
            require = 0)
    private void phoenixcore$onSkyRendered(Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
                                           float partialTick, Camera camera,
                                           boolean isFoggy, Runnable setupFog,
                                           CallbackInfo ci) {
        WorldFXManager.renderSkyLayers(new SkyRenderContext(modelViewMatrix, projectionMatrix, partialTick, camera));
    }

    @Inject(
            method = "renderSky",
            at = @At("RETURN"),
            require = 0)
    private void phoenixcore$onSkyRenderedFallback(Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
                                                   float partialTick, Camera camera,
                                                   boolean isFoggy, Runnable setupFog,
                                                   CallbackInfo ci) {
        WorldFXManager.renderSkyLayers(new SkyRenderContext(modelViewMatrix, projectionMatrix, partialTick, camera));
    }
}
