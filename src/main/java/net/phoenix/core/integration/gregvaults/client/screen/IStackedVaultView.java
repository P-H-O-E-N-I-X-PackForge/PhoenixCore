package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

public interface IStackedVaultView {

    boolean isStackedMode();

    int getSlotIndex(Slot slot);

    void applyOptimisticPickup(AggregatedStack agg, boolean half);

    void renderStackedCountLabel(GuiGraphics g, Slot slot, long total);

    boolean renderStackedTooltip(GuiGraphics g, AggregatedStack agg, int x, int y);
}
