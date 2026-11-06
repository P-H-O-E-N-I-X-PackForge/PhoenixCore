package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.widgets.slot.ModularSlot;

public final class VaultWindowSlot extends ModularSlot {

    private final VaultSession session;
    private final int windowIndex;

    public VaultWindowSlot(VaultSession session, int windowIndex) {
        super(session.windowHandler(), windowIndex);
        this.session = session;
        this.windowIndex = windowIndex;
    }

    public int windowIndex() {
        return windowIndex;
    }

    private boolean grouped() {
        return session.server && session.view != null && session.view.window.isAggregated();
    }

    @Override
    public boolean mayPickup(Player player) {
        if (!grouped()) return super.mayPickup(player);
        return !getItem().isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        if (!grouped()) return super.getMaxStackSize();
        AggregatedStack agg = session.view.window.aggregatedStack(windowIndex);
        if (agg == null) return 64;
        var storage = session.access.storage();
        for (int backingSlot : agg.backingSlots) {
            ItemStack inSlot = storage.getStackInSlot(backingSlot);
            if (!inSlot.isEmpty()) return Math.min(inSlot.getCount(), inSlot.getMaxStackSize());
        }
        return agg.displayStack.getMaxStackSize();
    }
}
