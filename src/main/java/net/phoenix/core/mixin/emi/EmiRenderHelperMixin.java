package net.phoenix.core.mixin.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.phoenix.core.utils.CompactCount;

import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.runtime.EmiDrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows big amounts compactly (1500 as 1.5k) in EMI's slots. EMI draws a stack's amount itself ({@code renderAmount}), not
 * through vanilla's item decorations (it asks vanilla for an empty count), so this is where the counts of GT recipes in EMI come
 * from. EMI right-aligns the text at 14 pixels wide at most, so a long number started inside the slot and ran into the next one;
 * the compact text is drawn smaller and right-aligned to the slot's edge.
 */
@Mixin(value = EmiRenderHelper.class, remap = false)
public abstract class EmiRenderHelperMixin {

    @Inject(method = "renderAmount(Ldev/emi/emi/runtime/EmiDrawContext;IILnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void phoenix$compactAmount(EmiDrawContext context, int x, int y, Component amount, CallbackInfo ci) {
        String text = amount.getString();
        String compact = CompactCount.compactIfNumeric(text, CompactCount.COUNT_THRESHOLD);
        if (compact == null || compact.equals(text)) return;

        Font font = Minecraft.getInstance().font;
        float scale = 0.7f;
        float width = font.width(compact) * scale;

        context.push();
        context.matrices().translate(x + 17 - width, y + 11, 200);
        context.matrices().scale(scale, scale, 1.0f);
        context.drawTextWithShadow(Component.literal(compact), 0, 0, -1);
        context.pop();
        ci.cancel();
    }
}
