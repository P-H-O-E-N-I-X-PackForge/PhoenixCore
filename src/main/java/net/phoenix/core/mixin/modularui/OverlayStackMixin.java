package net.phoenix.core.mixin.modularui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import brachy.modularui.overlay.OverlayStack;

/**
 * ModularUI-Modern's {@code OverlayStack.draw()} iterates its static overlay list directly. If any
 * overlay's own render/drawForeground call triggers {@code OverlayStack.open()}/{@code close()}
 * re-entrantly (e.g. an overlay that dismisses itself or opens another mid-render), that mutates the
 * very list {@code draw()} is iterating, throwing a {@link java.util.ConcurrentModificationException} -
 * observed crashing on plain vanilla screens (the overlay stack is global, tracked across every screen,
 * not just ModularUI ones). Confirmed via decompiled bytecode of the actual
 * {@code modularui-mc1.20.1-3.3.1-SNAPSHOT.jar}: {@code draw()}'s only {@code List.iterator()} call is
 * on the live static field, with nothing else in the method touching a list iterator. Redirecting that
 * one call to iterate a defensive snapshot copy instead eliminates the crash regardless of what
 * {@code open()}/{@code close()} do mid-draw, with zero change to {@code draw()}'s actual rendering
 * logic - upstream issue: https://github.com/brachy84/ModularUI-Modern/issues.
 */
@Mixin(value = OverlayStack.class, remap = false)
public abstract class OverlayStackMixin {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private static Iterator phoenix$snapshotOverlaysForDraw(List overlay) {
        return new ArrayList(overlay).iterator();
    }
}
