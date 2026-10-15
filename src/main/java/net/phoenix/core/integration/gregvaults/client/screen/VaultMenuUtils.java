package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VaultMenuUtils {

    private VaultMenuUtils() {}

    public static void applySortToStorage(ItemStackHandler handler, VaultSortMode sortMode,
                                          boolean sortReversed, VaultSlot.RemappingHandler remapping) {
        int slots = handler.getSlots();

        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) stacks.add(s.copy());
        }

        Comparator<ItemStack> cmp = switch (sortMode) {
            case NAME -> Comparator.comparing(s -> s.getHoverName().getString());
            case COUNT -> Comparator.comparingInt((ItemStack s) -> s.getCount()).reversed();
        };
        if (sortReversed) cmp = cmp.reversed();
        stacks.sort(cmp);

        for (int i = 0; i < slots; i++) {
            ItemStack next = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            if (ItemStack.matches(handler.getStackInSlot(i), next)) continue;
            handler.setStackInSlot(i, next);
        }
        remapping.setSortedIndices(null);
    }

    public static void organize(ItemStackHandler handler, VaultSlot.RemappingHandler remapping) {
        int slots = handler.getSlots();

        ItemStack[] arr = new ItemStack[slots];
        for (int i = 0; i < slots; i++) {
            arr[i] = handler.getStackInSlot(i).copy();
        }

        for (int i = 0; i < slots; i++) {
            if (arr[i].isEmpty()) continue;
            int limit = handler.getSlotLimit(i);
            if (arr[i].getCount() >= limit) continue;
            for (int j = i + 1; j < slots && arr[i].getCount() < limit; j++) {
                if (arr[j].isEmpty() || !ItemStack.isSameItemSameTags(arr[i], arr[j])) continue;
                int canTake = Math.min(arr[j].getCount(), limit - arr[i].getCount());
                arr[i].grow(canTake);
                arr[j].shrink(canTake);
                if (arr[j].isEmpty()) arr[j] = ItemStack.EMPTY;
            }
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack s : arr) {
            if (!s.isEmpty()) stacks.add(s);
        }
        stacks.sort(Comparator.comparing(s -> s.getHoverName().getString()));

        for (int i = 0; i < slots; i++) {
            ItemStack next = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            if (ItemStack.matches(arr[i], next) && ItemStack.matches(handler.getStackInSlot(i), next)) continue;
            handler.setStackInSlot(i, next);
        }
        remapping.setSortedIndices(null);
    }
}
