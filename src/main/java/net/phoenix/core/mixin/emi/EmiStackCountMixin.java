package net.phoenix.core.mixin.emi;

import net.phoenix.core.utils.CompactCount;

import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiStack.class, remap = false)
public abstract class EmiStackCountMixin {

    @Inject(method = "toString", at = @At("RETURN"), cancellable = true)
    private void phoenix$compactToString(CallbackInfoReturnable<String> cir) {
        EmiStack self = (EmiStack) (Object) this;
        long amount = self.getAmount();

        if (amount > 9999) {
            String original = cir.getReturnValue();
            // EMI toString usually looks like "item:stone x64"
            // We replace the part after the 'x'
            if (original.contains(" x")) {
                String base = original.split(" x")[0];
                cir.setReturnValue(base + " x" + CompactCount.compactIfNumeric(String.valueOf(amount)));
            }
        }
    }
}
