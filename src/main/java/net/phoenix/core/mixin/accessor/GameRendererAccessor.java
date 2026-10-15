package net.phoenix.core.mixin.accessor;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("postEffect")
    void phoenix$setPostEffect(@Nullable PostChain chain);

    @Accessor("effectActive")
    void phoenix$setEffectActive(boolean active);
}
