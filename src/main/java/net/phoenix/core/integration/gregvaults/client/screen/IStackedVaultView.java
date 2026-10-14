package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

/**
 * What the container screen mixin needs from a vault screen. The mixin targets AbstractContainerScreen, and
 * AbstractVaultScreen extends it, so the mixin must not name the screen class itself: Mixin cannot load a subclass of the
 * class it is in the middle of transforming. This interface has no such link.
 */
public interface IStackedVaultView {

    boolean isStackedMode();

    int getSlotIndex(Slot slot);

    void applyOptimisticPickup(AggregatedStack agg, boolean half);

    void renderStackedCountLabel(GuiGraphics g, Slot slot, long total);

    boolean renderStackedTooltip(GuiGraphics g, AggregatedStack agg, int x, int y);
}
