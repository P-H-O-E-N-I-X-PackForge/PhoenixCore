package net.phoenix.core.mixin.modularui;

import net.minecraft.world.item.ItemStack;

import brachy.modularui.widgets.ItemDisplayWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * ModularUI's item display widget (what recipe viewers such as EMI's GT recipe pages use for their slots) draws an item's count
 * twice when it shows amounts: {@code GuiDraw.drawItem} draws the item with vanilla's decorations, which include the stack count,
 * and then the widget draws its own formatted amount on top. ModularUI's normal slots avoid that by drawing the decorations of a
 * one-item copy; do the same here, so only the widget's own amount shows (and big numbers read as 15K, not 15375 over 15K).
 */
@Mixin(value = ItemDisplayWidget.class, remap = false)
public abstract class ItemDisplayWidgetMixin {

    @Shadow
    private boolean displayAmount;

    @ModifyArg(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lbrachy/modularui/drawable/GuiDraw;drawItem(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;IIFFI)V",
                    remap = false),
            index = 1,
            remap = false)
    private ItemStack phoenix$dropVanillaCount(ItemStack stack) {
        return displayAmount && stack.getCount() > 1 ? stack.copyWithCount(1) : stack;
    }
}
