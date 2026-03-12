package net.phoenix.core.mixin.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.phoenix.core.api.PhoenixColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextColor.class)
public class MixinTextColor {
    @Inject(method = "fromLegacyFormat", at = @At("HEAD"), cancellable = true)
    private static void phoenix$applyCustomHex(ChatFormatting formatting, CallbackInfoReturnable<TextColor> cir) {
        char lastCode = PhoenixColors.LAST_CODE.get();
        Integer customColor = PhoenixColors.CUSTOM_FORMATTING.get(lastCode);

        if (customColor != null && formatting == ChatFormatting.LIGHT_PURPLE) {
            cir.setReturnValue(TextColor.fromRgb(customColor));
        }
    }
}