package net.phoenix.core.mixin.ae2;

import net.phoenix.core.integration.ae2.ExpandedProxyContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(targets = "com.gregtechceu.gtceu.integration.ae2.machine.trait.ProxySlotRecipeHandler$ProxyRHL", remap = false)
public abstract class ProxyHandlerContextMixin {

    @Inject(method = "handleRecipe", at = @At("HEAD"))
    private void phoenix$enter(CallbackInfoReturnable<Map<?, ?>> cir) {
        ExpandedProxyContext.enter(this);
    }

    @Inject(method = "handleRecipe", at = @At("RETURN"))
    private void phoenix$exit(CallbackInfoReturnable<Map<?, ?>> cir) {
        ExpandedProxyContext.exit();
    }
}
