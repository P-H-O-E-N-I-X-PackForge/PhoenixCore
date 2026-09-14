package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AggregatedStack {

    public final ItemStack displayStack;
    public final List<Integer> backingSlots;
    private long totalCount;

    public AggregatedStack(ItemStack displayStack, List<Integer> backingSlots) {
        this.displayStack = displayStack;
        this.backingSlots = backingSlots;
        this.totalCount = displayStack.getCount();
    }

    public void addCount(long count) {
        this.totalCount += count;
    }

    public long totalCount() {
        return totalCount;
    }
}
