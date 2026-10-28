package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.utils.CompactCount;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsCountSuffixMixin {

    private static final String DECORATIONS = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V";

    @Unique
    private int phoenix$decorationDepth;

    @Inject(method = DECORATIONS, at = @At("HEAD"))
    private void phoenix$enterDecorations(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        phoenix$decorationDepth++;
    }

    @Inject(method = DECORATIONS, at = @At("RETURN"))
    private void phoenix$leaveDecorations(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        phoenix$decorationDepth = Math.max(0, phoenix$decorationDepth - 1);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true)
    private void phoenix$compactString(Font font, String text, int x, int y, int color, boolean dropShadow,
                                       CallbackInfoReturnable<Integer> cir) {
        if (phoenix$decorationDepth <= 0 || text == null) return;

        String compacted = CompactCount.compactIfNumeric(text, CompactCount.COUNT_THRESHOLD);
        if (compacted != null && !compacted.equals(text)) {
            cir.setReturnValue(phoenix$renderScaled(font, compacted, font.width(text), x, y, color, dropShadow));
        }
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true)
    private void phoenix$compactSequence(Font font, FormattedCharSequence text, int x, int y, int color,
                                         boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (phoenix$decorationDepth <= 0 || text == null) return;

        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });

        String original = sb.toString();
        String compacted = CompactCount.compactIfNumeric(original, CompactCount.COUNT_THRESHOLD);
        if (compacted != null && !compacted.equals(original)) {
            cir.setReturnValue(phoenix$renderScaled(font, compacted, font.width(text), x, y, color, dropShadow));
        }
    }

    @Unique
    private int phoenix$renderScaled(Font font, String compacted, int originalWidth, int x, int y, int color,
                                     boolean shadow) {
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        float scale = 0.70f;
        float rightEdge = x + originalWidth;
        float left = rightEdge - font.width(compacted) * scale;

        graphics.pose().pushPose();
        graphics.pose().translate(left, y + 2, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        int result = graphics.drawString(font, compacted, 0, 0, color, shadow);
        graphics.pose().popPose();
        return result;
    }
}
