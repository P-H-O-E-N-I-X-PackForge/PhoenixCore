package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Material.class, remap = false)
public abstract class MixinMaterial {

    @Inject(method = "getLocalizedName", at = @At("RETURN"), cancellable = true)
    private void phoenix$injectHexColor(CallbackInfoReturnable<Object> cir) {
        Material self = (Material) (Object) this;
        Object original = cir.getReturnValue();

        String nameText;
        if (original instanceof String s) {
            nameText = s;
        } else if (original instanceof Component c) {
            nameText = c.getString();
        } else {
            return; // Safety exit
        }

        // Logic to get color
        int color = self.getMaterialRGB();
        String hex = String.format("%06X", (0xFFFFFF & color));

        // Create the tagged string
        String taggedName = "[#" + hex + "]" + nameText;

        // Return as the original type to avoid another ClassCastException
        if (original instanceof Component) {
            cir.setReturnValue(Component.literal(taggedName));
        } else {
            cir.setReturnValue(taggedName);
        }
    }
}
