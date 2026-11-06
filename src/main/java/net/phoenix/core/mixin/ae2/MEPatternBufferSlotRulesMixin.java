package net.phoenix.core.mixin.ae2;

import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import net.phoenix.core.integration.ae2.ExpandedProxyContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = MEPatternBufferPartMachine.class, remap = false)
public abstract class MEPatternBufferSlotRulesMixin {

    @Inject(method = "couldSlotMatchContents", at = @At("HEAD"), cancellable = true)
    private static void phoenix$rules(MEPatternBufferPartMachine.InternalSlot slot, Map<?, ?> contents,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (ExpandedProxyContext.blocked(slot)) cir.setReturnValue(false);
    }

    @Inject(method = "couldSlotMatchContents", at = @At("RETURN"))
    private static void phoenix$wanted(MEPatternBufferPartMachine.InternalSlot slot, Map<?, ?> contents,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) ExpandedProxyContext.wanted(slot);
    }
}
