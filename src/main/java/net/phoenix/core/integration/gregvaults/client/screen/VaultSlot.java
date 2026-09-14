package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("all")
public class VaultSlot extends SlotItemHandler {

    private final RemappingHandler remapping;

    public VaultSlot(RemappingHandler remapping, int visibleIndex, int x, int y) {
        super(remapping, visibleIndex, x, y);
        this.remapping = remapping;
    }

    @Override
    public boolean isActive() {
        return remapping.isIndexActive(this.getSlotIndex());
    }

    public boolean isAggregated() {
        return remapping.isAggregated();
    }

    public AggregatedStack getAggregatedStack() {
        return remapping.getAggregatedStack(this.getSlotIndex());
    }

    @Override
    public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
        if (!remapping.isAggregated()) return super.mayPickup(player);
        return !getItem().isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        if (!remapping.isAggregated()) return super.getMaxStackSize();
        AggregatedStack agg = remapping.getAggregatedStack(this.getSlotIndex());
        if (agg == null) return 64;
        for (int backingSlot : agg.backingSlots) {
            ItemStack inSlot = remapping.getRealStackPublic(backingSlot);
            if (!inSlot.isEmpty()) return Math.min(inSlot.getCount(), inSlot.getMaxStackSize());
        }
        return agg.displayStack.getMaxStackSize();
    }

    public static class RemappingHandler implements IItemHandlerModifiable {

        private final IItemHandler real;
        private int offset = 0;
        private final int windowSize;

        private int[] filteredIndices = null;
        private int[] sortedIndices = null;
        private ItemStack[] clientCache = null;
        private List<AggregatedStack> aggregatedView = null;

        public RemappingHandler(IItemHandler real, int windowSize) {
            this.real = real;
            this.windowSize = windowSize;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public void setFilteredIndices(int[] indices) {
            this.filteredIndices = indices;
        }

        public void setSortedIndices(int[] indices) {
            this.sortedIndices = indices;
        }

        public void setClientCache(ItemStack[] cache) {
            this.clientCache = cache;
        }

        public void setAggregatedView(List<AggregatedStack> view) {
            this.aggregatedView = view;
        }

        public boolean isAggregated() {
            return aggregatedView != null;
        }

        public int getAggregatedIndex(int visibleSlot) {
            if (aggregatedView == null) return -1;
            int absolute = offset + visibleSlot;
            if (absolute < 0 || absolute >= aggregatedView.size()) return -1;
            return absolute;
        }

        public AggregatedStack getAggregatedStack(int visibleSlot) {
            int index = getAggregatedIndex(visibleSlot);
            return index >= 0 ? aggregatedView.get(index) : null;
        }

        public ItemStack getRealStackPublic(int backingSlot) {
            if (clientCache != null && backingSlot >= 0 && backingSlot < clientCache.length) {
                return clientCache[backingSlot] != null ? clientCache[backingSlot] : ItemStack.EMPTY;
            }
            return real.getStackInSlot(backingSlot);
        }

        private int realIndex(int visibleSlot) {
            int absolute = offset + visibleSlot;
            if (filteredIndices != null) {
                if (absolute < 0 || absolute >= filteredIndices.length) return -1;
                return filteredIndices[absolute];
            } else if (sortedIndices != null) {
                if (absolute < 0 || absolute >= sortedIndices.length) return -1;
                return sortedIndices[absolute];
            } else {
                if (absolute < 0 || absolute >= real.getSlots()) return -1;
                return absolute;
            }
        }

        public boolean isIndexActive(int visibleSlot) {
            if (aggregatedView != null) {
                int absolute = offset + visibleSlot;
                return absolute >= 0 && absolute < aggregatedView.size();
            }
            return realIndex(visibleSlot) >= 0;
        }

        @Override
        public int getSlots() {
            return windowSize;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (aggregatedView != null) {
                int absolute = offset + slot;
                if (absolute < 0 || absolute >= aggregatedView.size()) return ItemStack.EMPTY;
                AggregatedStack agg = aggregatedView.get(absolute);
                for (int backingSlot : agg.backingSlots) {
                    ItemStack inSlot = getRealStackPublic(backingSlot);
                    if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, agg.displayStack)) {
                        return agg.displayStack.copyWithCount(1);
                    }
                }
                return ItemStack.EMPTY;
            }
            int ri = realIndex(slot);
            if (ri < 0) return ItemStack.EMPTY;
            if (clientCache != null && ri < clientCache.length) {
                return clientCache[ri] != null ? clientCache[ri] : ItemStack.EMPTY;
            }
            return real.getStackInSlot(ri);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (aggregatedView != null) {
                int absolute = offset + slot;
                if (absolute >= 0 && absolute < aggregatedView.size()) {
                    AggregatedStack agg = aggregatedView.get(absolute);
                    for (int backingSlot : agg.backingSlots) {
                        ItemStack existing = real.getStackInSlot(backingSlot);
                        if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
                            ItemStack remainder = real.insertItem(backingSlot, stack, simulate);
                            if (remainder.getCount() < stack.getCount()) return remainder;
                        }
                    }
                    for (int backingSlot : agg.backingSlots) {
                        if (real.getStackInSlot(backingSlot).isEmpty()) {
                            return real.insertItem(backingSlot, stack, simulate);
                        }
                    }
                }
                ItemStack remaining = stack.copy();
                for (int i = 0; i < real.getSlots() && !remaining.isEmpty(); i++) {
                    ItemStack existing = real.getStackInSlot(i);
                    if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining)) {
                        remaining = real.insertItem(i, remaining, simulate);
                    }
                }
                for (int i = 0; i < real.getSlots() && !remaining.isEmpty(); i++) {
                    if (real.getStackInSlot(i).isEmpty()) {
                        return real.insertItem(i, remaining, simulate);
                    }
                }
                return remaining;
            }
            int ri = realIndex(slot);
            return ri < 0 ? stack : real.insertItem(ri, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (aggregatedView != null) {
                int absolute = offset + slot;
                if (absolute < 0 || absolute >= aggregatedView.size()) return ItemStack.EMPTY;
                AggregatedStack agg = aggregatedView.get(absolute);
                for (int backingSlot : agg.backingSlots) {
                    ItemStack inSlot = getRealStackPublic(backingSlot);
                    if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, agg.displayStack)) {
                        ItemStack result = real.extractItem(backingSlot, amount, simulate);
                        if (!result.isEmpty()) return result;
                    }
                }
                return ItemStack.EMPTY;
            }
            int ri = realIndex(slot);
            return ri < 0 ? ItemStack.EMPTY : real.extractItem(ri, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (aggregatedView != null) {
                int absolute = offset + slot;
                if (absolute < 0 || absolute >= aggregatedView.size()) return 0;
                AggregatedStack agg = aggregatedView.get(absolute);
                int total = 0;
                for (int backingSlot : agg.backingSlots) {
                    ItemStack inSlot = real.getStackInSlot(backingSlot);
                    if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, agg.displayStack)) {
                        total += Math.min(real.getSlotLimit(backingSlot), inSlot.getMaxStackSize());
                    }
                }
                return Math.max(agg.displayStack.getMaxStackSize(), total);
            }
            int ri = realIndex(slot);
            return ri < 0 ? 0 : real.getSlotLimit(ri);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (aggregatedView != null) {
                int absolute = offset + slot;
                if (absolute < 0 || absolute >= aggregatedView.size()) return false;
                return ItemStack.isSameItemSameTags(aggregatedView.get(absolute).displayStack, stack);
            }
            int ri = realIndex(slot);
            return ri >= 0 && real.isItemValid(ri, stack);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (aggregatedView != null) return;
            int ri = realIndex(slot);
            if (ri >= 0 && real instanceof IItemHandlerModifiable m) {
                m.setStackInSlot(ri, stack);
            }
        }
    }
}
