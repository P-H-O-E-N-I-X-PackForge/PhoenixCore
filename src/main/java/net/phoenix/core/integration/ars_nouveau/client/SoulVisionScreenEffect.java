package net.phoenix.core.integration.ars_nouveau.client;

import net.minecraft.client.renderer.ShaderInstance;
import net.phoenix.core.client.worldfx.PhoenixScreenEffect;
import net.phoenix.core.client.worldfx.WorldFXShaders;

public class SoulVisionScreenEffect extends PhoenixScreenEffect {

    @Override
    public ShaderInstance getShader() {
        return WorldFXShaders.SOUL_VISION_COMPOSITE;
    }

    @Override
    public void uploadUniforms(float partialTick) {
        
    }

    @Override
    public int priority() {
        return 100;
    }
}
