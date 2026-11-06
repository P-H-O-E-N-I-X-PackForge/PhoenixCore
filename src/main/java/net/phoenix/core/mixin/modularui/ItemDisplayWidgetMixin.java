package net.phoenix.core.mixin.modularui;

import net.minecraft.world.item.ItemStack;

import brachy.modularui.widgets.ItemDisplayWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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
