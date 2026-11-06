package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class WindowTitleMixin {

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void phoenix$title(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Phoenix Forge Technologies");
    }
}
