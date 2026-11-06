package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VaultWindow implements IItemHandlerModifiable {

    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int SIZE = COLS * ROWS;

    private final IItemHandler real;
    private int offset = 0;
    private int[] filteredIndices = null;
    private List<AggregatedStack> aggregatedView = null;

    public VaultWindow(IItemHandler real) {
        this.real = real;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }

    public void setFilteredIndices(int[] indices) {
        this.filteredIndices = indices;
    }

    public int[] filteredIndices() {
        return filteredIndices;
    }

    public void setAggregatedView(List<AggregatedStack> view) {
        this.aggregatedView = view;
    }

    public boolean isAggregated() {
        return aggregatedView != null;
    }

    public AggregatedStack aggregatedStack(int visibleSlot) {
        if (aggregatedView == null) return null;
        int absolute = offset + visibleSlot;
        return absolute < 0 || absolute >= aggregatedView.size() ? null : aggregatedView.get(absolute);
    }

    public int entryCount() {
        if (aggregatedView != null) return aggregatedView.size();
        return filteredIndices != null ? filteredIndices.length : real.getSlots();
    }

    private int realIndex(int visibleSlot) {
        int absolute = offset + visibleSlot;
        if (absolute < 0) return -1;
        if (filteredIndices != null) return absolute < filteredIndices.length ? filteredIndices[absolute] : -1;
        return absolute < real.getSlots() ? absolute : -1;
    }

    public boolean isIndexActive(int visibleSlot) {
        if (aggregatedView != null) return aggregatedStack(visibleSlot) != null;
        return realIndex(visibleSlot) >= 0;
    }

    @Override
    public int getSlots() {
        return SIZE;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (aggregatedView != null) {
            AggregatedStack agg = aggregatedStack(slot);
            if (agg == null) return ItemStack.EMPTY;
            for (int backingSlot : agg.backingSlots) {
                ItemStack inSlot = real.getStackInSlot(backingSlot);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, agg.displayStack)) {

                    return agg.displayStack.copyWithCount((int) Math.min(agg.totalCount(), Integer.MAX_VALUE));
                }
            }
            return ItemStack.EMPTY;
        }
        int ri = realIndex(slot);
        return ri < 0 ? ItemStack.EMPTY : real.getStackInSlot(ri);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (aggregatedView != null) {
            AggregatedStack agg = aggregatedStack(slot);
            if (agg != null) {
                for (int backingSlot : agg.backingSlots) {
                    ItemStack existing = real.getStackInSlot(backingSlot);
                    if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
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
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                    remaining = real.insertItem(i, remaining, simulate);
                }
            }
            for (int i = 0; i < real.getSlots() && !remaining.isEmpty(); i++) {
                if (real.getStackInSlot(i).isEmpty()) return real.insertItem(i, remaining, simulate);
            }
            return remaining;
        }
        int ri = realIndex(slot);
        return ri < 0 ? stack : real.insertItem(ri, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (aggregatedView != null) {
            AggregatedStack agg = aggregatedStack(slot);
            if (agg == null) return ItemStack.EMPTY;
            for (int backingSlot : agg.backingSlots) {
                ItemStack inSlot = real.getStackInSlot(backingSlot);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, agg.displayStack)) {
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
            AggregatedStack agg = aggregatedStack(slot);
            if (agg == null) return 0;
            int total = 0;
            for (int backingSlot : agg.backingSlots) {
                ItemStack inSlot = real.getStackInSlot(backingSlot);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, agg.displayStack)) {
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
            AggregatedStack agg = aggregatedStack(slot);
            return agg != null && ItemStack.isSameItemSameComponents(agg.displayStack, stack);
        }
        int ri = realIndex(slot);
        return ri >= 0 && real.isItemValid(ri, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (aggregatedView != null) return;
        int ri = realIndex(slot);
        if (ri >= 0 && real instanceof IItemHandlerModifiable modifiable) modifiable.setStackInSlot(ri, stack);
    }
}
