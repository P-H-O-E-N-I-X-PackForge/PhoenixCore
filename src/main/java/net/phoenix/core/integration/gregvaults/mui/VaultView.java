package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VaultView {

    private final VaultAccess access;
    private final ItemStackHandler storage;
    public final VaultWindow window;

    private String query = "";
    private VaultSortMode sortMode = VaultSortMode.NAME;
    private boolean sortReversed = true;
    private VaultDisplayMode displayMode = VaultDisplayMode.SLOTS;
    private int scrollRow = 0;
    private int builtVersion = Integer.MIN_VALUE;

    public VaultView(VaultAccess access) {
        this.access = access;
        this.storage = access.storage();
        this.window = new VaultWindow(storage);
    }

    public VaultDisplayMode displayMode() {
        return displayMode;
    }

    public int scrollRow() {
        return scrollRow;
    }

    public int totalRows() {
        return (int) Math.ceil(window.entryCount() / (double) VaultWindow.COLS);
    }

    public int maxScroll() {
        return Math.max(0, totalRows() - VaultWindow.ROWS);
    }

    public int activeSlots() {
        return Math.max(0, Math.min(VaultWindow.SIZE, window.entryCount() - scrollRow * VaultWindow.COLS));
    }

    public void setScroll(int row) {
        scrollRow = Math.max(0, Math.min(maxScroll(), row));
        window.setOffset(scrollRow * VaultWindow.COLS);
    }

    public void setQuery(String text) {
        query = text == null ? "" : text;
        scrollRow = 0;
        rebuild();
    }

    public void setDisplayMode(VaultDisplayMode mode) {
        displayMode = mode;
        scrollRow = 0;
        rebuild();
    }

    public void setSort(VaultSortMode mode, boolean reversed) {
        sortMode = mode;
        sortReversed = reversed;
        applySort();
        rebuild();
    }

    public void organize() {
        organizeStorage();
        rebuild();
    }

    public boolean tick() {
        int version = access.contentVersion();
        if (version == builtVersion) return false;
        builtVersion = version;
        if (query.isEmpty() && displayMode != VaultDisplayMode.STACKED) {
            setScroll(scrollRow);
            return true;
        }
        rebuild();
        return true;
    }

    public void rebuild() {
        builtVersion = access.contentVersion();
        int[] filtered = null;
        if (!query.isEmpty()) {
            String needle = query.toLowerCase(Locale.ROOT);
            List<Integer> matching = new ArrayList<>();
            for (int i = 0; i < storage.getSlots(); i++) {
                ItemStack stack = storage.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)) {
                    matching.add(i);
                }
            }
            filtered = matching.stream().mapToInt(Integer::intValue).toArray();
        }
        window.setFilteredIndices(filtered);
        window.setAggregatedView(displayMode == VaultDisplayMode.STACKED ? aggregate(filtered) : null);
        setScroll(scrollRow);
    }

    private List<AggregatedStack> aggregate(int[] filtered) {
        List<AggregatedStack> out = new ArrayList<>();
        Map<Item, List<AggregatedStack>> byItem = new HashMap<>();
        int count = filtered != null ? filtered.length : storage.getSlots();
        for (int n = 0; n < count; n++) {
            int slot = filtered != null ? filtered[n] : n;
            ItemStack stack = storage.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            AggregatedStack found = null;
            List<AggregatedStack> group = byItem.computeIfAbsent(stack.getItem(), k -> new ArrayList<>());
            for (AggregatedStack candidate : group) {
                if (ItemStack.isSameItemSameComponents(candidate.displayStack, stack)) {
                    found = candidate;
                    break;
                }
            }
            if (found == null) {
                List<Integer> slots = new ArrayList<>();
                slots.add(slot);
                AggregatedStack created = new AggregatedStack(stack.copy(), slots);
                group.add(created);
                out.add(created);
            } else {
                found.backingSlots.add(slot);
                found.addCount(stack.getCount());
            }
        }
        return out;
    }

    private void applySort() {
        int slots = storage.getSlots();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = storage.getStackInSlot(i);
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
            if (ItemStack.matches(storage.getStackInSlot(i), next)) continue;
            storage.setStackInSlot(i, next);
        }
    }

    private void organizeStorage() {
        int slots = storage.getSlots();

        ItemStack[] arr = new ItemStack[slots];
        for (int i = 0; i < slots; i++) arr[i] = storage.getStackInSlot(i).copy();

        for (int i = 0; i < slots; i++) {
            if (arr[i].isEmpty()) continue;
            int limit = storage.getSlotLimit(i);
            if (arr[i].getCount() >= limit) continue;
            for (int j = i + 1; j < slots && arr[i].getCount() < limit; j++) {
                if (arr[j].isEmpty() || !ItemStack.isSameItemSameComponents(arr[i], arr[j])) continue;
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
            if (ItemStack.matches(arr[i], next) && ItemStack.matches(storage.getStackInSlot(i), next)) continue;
            storage.setStackInSlot(i, next);
        }
    }

    public static String registryName(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
