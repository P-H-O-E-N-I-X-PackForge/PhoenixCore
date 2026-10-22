package net.phoenix.core.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.phoenix.core.PhoenixCore;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TEMPORARY debugging: what the creative screen does with a click. Remove with CreativeClickDebug. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenDebugMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void phoenix$dbgSlotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        var screen = (AbstractContainerScreen<?>) (Object) this;
        PhoenixCore.LOGGER.info("[CreativeClickDebug] creative.slotClicked slot={} slotId={} button={} type={} carried={}",
                slot == null ? "null" : slot.index + ":" + slot.getItem(), slotId, mouseButton, type,
                screen.getMenu().getCarried());
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void phoenix$dbgMouseClicked(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        PhoenixCore.LOGGER.info("[CreativeClickDebug] creative.mouseClicked returned {}", cir.getReturnValue());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void phoenix$dbgMouseClickedHead(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        PhoenixCore.LOGGER.info("[CreativeClickDebug] creative.mouseClicked enter at {},{} button {}", (int) x, (int) y, button);
    }
}
