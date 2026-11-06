package net.phoenix.core.mixin.modularui;

import brachy.modularui.overlay.OverlayStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(value = OverlayStack.class, remap = false)
public abstract class OverlayStackMixin {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private static Iterator phoenix$snapshotOverlaysForDraw(List overlay) {
        return new ArrayList(overlay).iterator();
    }
}
