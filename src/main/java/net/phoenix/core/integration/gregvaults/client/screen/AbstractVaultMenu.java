package net.phoenix.core.integration.gregvaults.client.screen;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("all")
public abstract class AbstractVaultMenu extends AbstractContainerMenu {

    public static final int COLS      = 9;
    public static final int SLOT_SIZE = 18;
    public static final int MAX_ROWS  = 6;
    public static final int SLOTS_X   = 8;
    public static final int SLOTS_Y   = 18;

    public final IItemHandler vaultHandler;
    public final int totalSlots;
    public final int visibleRows;

    @Nullable
    public final VaultMachine machine;

    public final int craftSectionY;
    public final int craftGridY;
    public final int craftGridX;
    public final int craftOutX;
    public final int craftOutY;
    public final int playerY;
    public final int hotbarY;

    private final VaultSlot.RemappingHandler remapping;

    public final CraftingContainer craftingGrid = new TransientCraftingContainer(this, 3, 3);
    public final ResultContainer   craftingResult = new ResultContainer();

    public final int playerSlotsStart;
    public final int craftingSlotsStart;
    public final int craftingOutputStart;
    public final List<Slot> vaultInputSlots;

    private int[]        filteredIndices  = null;
    private VaultSortMode sortMode        = VaultSortMode.NAME;
    private boolean      sortReversed     = true;
    private String       lastSearchQuery  = "";
    private VaultDisplayMode displayMode    = VaultDisplayMode.SLOTS;
    private List<AggregatedStack> aggregatedView = null;
    public  ItemStack[]  clientCache        = null;
    public  int          clientCacheVersion = 0;

    private boolean refilling  = false;
    private boolean skipRefill = false;
    private boolean suppressCraftingResultUpdates = false;

    private Consumer<ItemStack[]> onGridClose = null;

    private final ItemStack[] lastGridIngredients = new ItemStack[9];

    protected AbstractVaultMenu(MenuType<?> menuType, int windowId, Inventory playerInv,
                                IItemHandler vaultHandler, @Nullable VaultMachine machine) {
        super(menuType, windowId);
        this.vaultHandler = vaultHandler;
        this.machine      = machine;
        this.totalSlots   = vaultHandler.getSlots();

        int usedRows    = Math.max(1, (int) Math.ceil(totalSlots / (double) COLS));
        this.visibleRows = Math.min(usedRows, MAX_ROWS);
        int visibleSlots = visibleRows * COLS;

        this.craftSectionY = SLOTS_Y + visibleRows * SLOT_SIZE - 1;
        this.craftGridY    = craftSectionY + SLOT_SIZE;
        this.craftGridX    = SLOTS_X;
        this.craftOutX     = craftGridX + 3 * SLOT_SIZE + 27;
        this.craftOutY     = craftGridY + SLOT_SIZE;
        this.playerY       = craftGridY + 3 * SLOT_SIZE + 14;
        this.hotbarY       = playerY + 3 * SLOT_SIZE + 4;

        this.remapping = new VaultSlot.RemappingHandler(vaultHandler, visibleSlots);
        for (int i = 0; i < visibleSlots; i++) {
            addSlot(new VaultSlot(remapping, i,
                    SLOTS_X + (i % COLS) * SLOT_SIZE,
                    SLOTS_Y + (i / COLS) * SLOT_SIZE));
        }

        this.playerSlotsStart = slots.size();
        addPlayerSlots(playerInv);

        this.craftingSlotsStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftingGrid, col + row * 3,
                        craftGridX + col * SLOT_SIZE,
                        craftGridY + row * SLOT_SIZE));
            }
        }

        this.craftingOutputStart = slots.size();
        addSlot(new VaultCraftingResultSlot(playerInv.player, craftingGrid, craftingResult, 0,
                craftOutX, craftOutY));

        List<Slot> vaultSlots = new ArrayList<>(vaultHandler.getSlots());
        for (int i = 0; i < vaultHandler.getSlots(); i++) {
            vaultSlots.add(new CacheAwareVaultSlot(i, -10000, -10000));
        }
        this.vaultInputSlots = java.util.Collections.unmodifiableList(vaultSlots);

        updateCraftingResult();
    }

    private class CacheAwareVaultSlot extends SlotItemHandler {

        private final int vaultSlotIndex;

        CacheAwareVaultSlot(int vaultSlotIndex, int x, int y) {
            super(vaultHandler, vaultSlotIndex, x, y);
            this.vaultSlotIndex = vaultSlotIndex;
        }

        @Override
        public ItemStack getItem() {
            if (clientCache != null && vaultSlotIndex < clientCache.length) {
                ItemStack cached = clientCache[vaultSlotIndex];
                return cached != null ? cached : ItemStack.EMPTY;
            }
            return super.getItem();
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }
    }

    protected abstract void addPlayerSlots(Inventory playerInv);

    private class VaultCraftingResultSlot extends ResultSlot {

        VaultCraftingResultSlot(Player player, CraftingContainer grid, ResultContainer result,
                                int slot, int x, int y) {
            super(player, grid, result, slot, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            snapshotGridIngredients();
            super.onTake(player, stack);

            applyCraftingToolDamageToGrid(player);

            if (!skipRefill) {
                if (machine != null) machine.beginBatch(VaultMachine.BatchSyncMode.DELTA_ONLY);
                try {
                    refillGridFromVaultVoid(buildVaultSlotMap());
                } finally {
                    if (machine != null) machine.endBatch();
                }
            }
        }
    }

    public void setClientCache(ItemStack[] cache) {
        this.clientCache = cache;
        this.clientCacheVersion++;
        remapping.setClientCache(cache);
        if (displayMode == VaultDisplayMode.STACKED) {
            rebuildAggregatedView();
            updateScroll(0);
        }
        refreshVisibleSlots();
    }

    public void updateClientCacheSlot(int slot, ItemStack stack) {
        if (clientCache != null && slot >= 0 && slot < clientCache.length) {
            clientCache[slot] = stack;
            this.clientCacheVersion++;
            remapping.setClientCache(clientCache);
            if (displayMode == VaultDisplayMode.STACKED) rebuildAggregatedView();
            refreshVisibleSlots();
        }
    }

    public void applyDeltaUpdate() {
        this.clientCacheVersion++;
        remapping.setClientCache(clientCache);
        if (displayMode == VaultDisplayMode.STACKED) rebuildAggregatedView();
        refreshVisibleSlots();
    }

    public void refreshCraftingResult() {
        updateCraftingResult();
        broadcastChanges();
    }

    public void beginCraftingGridBulkUpdate() {
        suppressCraftingResultUpdates = true;
    }

    public void endCraftingGridBulkUpdate() {
        suppressCraftingResultUpdates = false;
        lastGridHash = gridHash();
        updateCraftingResult();
        broadcastChanges();
    }

    public void refreshVisibleSlots() {
        int visibleCount = getVisibleSlotCount();
        for (int i = 0; i < visibleCount; i++) {
            Slot slot = slots.get(i);
            if (slot instanceof VaultSlot) slot.set(remapping.getStackInSlot(i));
        }
    }

    public void updateScroll(int scrollRow) {
        remapping.setOffset(scrollRow * COLS);
        refreshVisibleSlots();
    }

    private ItemStack getSearchStack(int i) {
        if (clientCache != null && i >= 0 && i < clientCache.length) {
            return clientCache[i] == null ? ItemStack.EMPTY : clientCache[i];
        }
        return vaultHandler.getStackInSlot(i);
    }

    public void updateSearch(String query) {
        this.lastSearchQuery = query == null ? "" : query;
        int size = clientCache != null ? clientCache.length : vaultHandler.getSlots();
        if (query == null || query.isEmpty()) {
            filteredIndices = null;
            remapping.setFilteredIndices(null);
        } else {
            String q = query.toLowerCase();
            ArrayList<Integer> matching = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                ItemStack stack = getSearchStack(i);
                if (!stack.isEmpty() && stack.getHoverName().getString().toLowerCase().contains(q))
                    matching.add(i);
            }
            filteredIndices = matching.stream().mapToInt(Integer::intValue).toArray();
            remapping.setFilteredIndices(filteredIndices);
        }
    }

    public int[] getFilteredIndices() { return filteredIndices; }

    public VaultDisplayMode getDisplayMode() { return displayMode; }

    public void setDisplayMode(VaultDisplayMode mode) {
        this.displayMode = mode;
        remapping.setOffset(0);
        if (mode == VaultDisplayMode.STACKED) {
            rebuildAggregatedView();
        } else {
            aggregatedView = null;
            remapping.setAggregatedView(null);
        }
        refreshVisibleSlots();
    }

    private void rebuildAggregatedView() {
        int size = clientCache != null ? clientCache.length : vaultHandler.getSlots();
        java.util.LinkedHashMap<String, AggregatedStack> byKey = new java.util.LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            ItemStack stack = clientCache != null && clientCache[i] != null
                    ? clientCache[i]
                    : vaultHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            final int slot = i;
            final long count = stack.getCount();
            String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
                    + (stack.hasTag() ? Integer.toHexString(stack.getTag().hashCode()) : "");
            byKey.compute(key, (k, existing) -> {
                if (existing == null) {
                    List<Integer> slots = new ArrayList<>();
                    slots.add(slot);
                    return new AggregatedStack(stack.copy(), slots);
                }
                existing.backingSlots.add(slot);
                existing.addCount(count);
                return existing;
            });
        }

        aggregatedView = new ArrayList<>(byKey.values());
        remapping.setAggregatedView(aggregatedView);
    }

    public int getVisibleSlotCount() { return visibleRows * COLS; }

    public int getTotalFilteredRows() {
        if (displayMode == VaultDisplayMode.STACKED && aggregatedView != null)
            return (int) Math.ceil(aggregatedView.size() / (double) COLS);
        int count = filteredIndices != null ? filteredIndices.length : totalSlots;
        return (int) Math.ceil(count / (double) COLS);
    }

    public VaultSortMode getSortMode()    { return sortMode; }
    public boolean isSortReversed()       { return sortReversed; }

    public void setSortMode(VaultSortMode mode) { this.sortMode = mode; }
    public void setSortReversed(boolean reversed) { this.sortReversed = reversed; }

    public void setSort(VaultSortMode mode, boolean reversed) {
        this.sortMode     = mode;
        this.sortReversed = reversed;
        applySortToStorage();
        if (displayMode == VaultDisplayMode.STACKED) rebuildAggregatedView();
        refreshVisibleSlots();
    }

    private void applySortToStorage() {
        if (!(vaultHandler instanceof ItemStackHandler handler)) return;
        if (machine != null) machine.beginBatch(VaultMachine.BatchSyncMode.FULL_SNAPSHOT);
        VaultMenuUtils.applySortToStorage(handler, sortMode, sortReversed, remapping);
        if (machine != null) machine.endBatch();
    }

    public void organize() {
        if (!(vaultHandler instanceof ItemStackHandler handler)) return;
        if (machine != null) machine.beginBatch(VaultMachine.BatchSyncMode.FULL_SNAPSHOT);
        VaultMenuUtils.organize(handler, remapping);
        if (machine != null) machine.endBatch();
    }

    public void setOnGridClose(Consumer<ItemStack[]> callback) {
        this.onGridClose = callback;
    }

    public void initCraftingGrid(ItemStack[] saved) {
        if (saved == null) return;
        for (int i = 0; i < Math.min(saved.length, craftingGrid.getContainerSize()); i++) {
            craftingGrid.setItem(i, saved[i] == null ? ItemStack.EMPTY : saved[i].copy());
        }
        updateCraftingResult();
    }

    private int lastGridHash = 0;

    @Override
    public void slotsChanged(Container container) {
        if (container == craftingGrid) {
            if (suppressCraftingResultUpdates) return;
            int hash = gridHash();
            if (hash != lastGridHash) {
                lastGridHash = hash;
                updateCraftingResult();
            }
        }
    }

    private int gridHash() {
        int h = 1;
        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            ItemStack s = craftingGrid.getItem(i);
            h = 31 * h + (s.isEmpty() ? 0 : (net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(s.getItem()) * 31 + s.getCount()));
        }
        return h;
    }

    private void updateCraftingResult() {
        Level level = getLevel();
        if (level == null) return;
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftingGrid, level)
                .orElse(null);
        craftingResult.setItem(0,
                recipe == null ? ItemStack.EMPTY : recipe.assemble(craftingGrid, level.registryAccess()));
    }

    public void snapshotGridIngredients() {
        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            ItemStack s = craftingGrid.getItem(i);
            lastGridIngredients[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
    }

    private void applyCraftingToolDamageToGrid(Player player) {
        if (player.level().isClientSide) return;

        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            ItemStack before = lastGridIngredients[i];
            if (!isCraftingToolLike(before)) continue;

            ItemStack after = craftingGrid.getItem(i);

            if (after.isEmpty()) {
                ItemStack damaged = before.copy();
                damaged.setCount(1);
                damageCraftingTool(damaged, player);
                if (!damaged.isEmpty()) {
                    craftingGrid.setItem(i, damaged);
                }
                continue;
            }

            if (!ItemStack.isSameItemSameTags(before, after)) continue;

            ItemStack damaged = after.copy();
            damaged.setCount(1);
            damageCraftingTool(damaged, player);
            craftingGrid.setItem(i, damaged.isEmpty() ? ItemStack.EMPTY : damaged);
        }
    }

    private boolean isCraftingToolLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof IGTTool) return true;
        if (!ToolHelper.getCraftingToolTypes(stack).isEmpty()) return true;
        return stack.isDamageableItem() && stack.getMaxStackSize() == 1;
    }

    private void damageCraftingTool(ItemStack stack, Player player) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof IGTTool || !ToolHelper.getCraftingToolTypes(stack).isEmpty()) {
            ToolHelper.damageItemWhenCrafting(stack, player);
        } else if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, p -> {});
        }
    }

    private Map<net.minecraft.world.item.Item, ArrayDeque<Integer>> buildVaultSlotMap() {
        Map<net.minecraft.world.item.Item, ArrayDeque<Integer>> map = new HashMap<>();
        for (int v = 0; v < vaultHandler.getSlots(); v++) {
            ItemStack s = vaultHandler.getStackInSlot(v);
            if (!s.isEmpty()) map.computeIfAbsent(s.getItem(), k -> new ArrayDeque<>()).add(v);
        }
        return map;
    }

    private void refillGridFromVaultVoid(Map<net.minecraft.world.item.Item, ArrayDeque<Integer>> vaultSlotMap) {
        refillGridFromVault(vaultSlotMap, true);
    }

    private boolean refillGridFromVault(Map<net.minecraft.world.item.Item, ArrayDeque<Integer>> vaultSlotMap, boolean updateResult) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return false;
        if (!(vaultHandler instanceof ItemStackHandler handler)) return false;

        boolean anyRefilled = false;
        boolean gridBroken = false;
        refilling = true;
        try {
            for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
                ItemStack needed = lastGridIngredients[i];
                if (needed == null || needed.isEmpty()) continue;

                ItemStack inGrid = craftingGrid.getItem(i);

                if (!inGrid.isEmpty()) {
                    if (needed.isDamageableItem() && inGrid.getDamageValue() != needed.getDamageValue()) {
                        ArrayDeque<Integer> deque = vaultSlotMap.get(needed.getItem());
                        if (deque != null) {
                            while (!deque.isEmpty()) {
                                int v = deque.peek();
                                ItemStack vaultStack = handler.getStackInSlot(v);
                                if (vaultStack.isEmpty()) { deque.poll(); continue; }
                                if (!ItemStack.isSameItemSameTags(vaultStack, needed)) { deque.poll(); continue; }
                                vaultStack.shrink(1);
                                handler.setStackInSlot(v, vaultStack.isEmpty() ? ItemStack.EMPTY : vaultStack);
                                craftingGrid.setItem(i, needed.copyWithCount(1));
                                if (vaultStack.isEmpty()) deque.poll();
                                anyRefilled = true;
                                break;
                            }
                        }
                    }
                    continue;
                }

                ArrayDeque<Integer> deque = vaultSlotMap.get(needed.getItem());
                if (deque == null || deque.isEmpty()) {
                    gridBroken = true;
                    continue;
                }

                boolean filled = false;
                while (!deque.isEmpty()) {
                    int v = deque.peek();
                    ItemStack vaultStack = handler.getStackInSlot(v);
                    if (vaultStack.isEmpty()) { deque.poll(); continue; }
                    if (!ItemStack.isSameItemSameTags(vaultStack, needed)) { deque.poll(); continue; }
                    vaultStack.shrink(1);
                    handler.setStackInSlot(v, vaultStack.isEmpty() ? ItemStack.EMPTY : vaultStack);
                    craftingGrid.setItem(i, needed.copyWithCount(1));
                    if (vaultStack.isEmpty()) deque.poll();
                    anyRefilled = true;
                    filled = true;
                    break;
                }

                if (!filled) gridBroken = true;
            }
        } finally {
            refilling = false;
        }

        if (updateResult && !suppressCraftingResultUpdates) {
            updateCraftingResult();
        }
        return anyRefilled && !gridBroken;
    }

    protected ItemStack insertIntoFullVault(ItemStack stack) {
        if (machine != null) machine.beginBatch(VaultMachine.BatchSyncMode.DELTA_ONLY);
        try {
            return insertIntoFullVaultInternal(stack);
        } finally {
            if (machine != null) machine.endBatch();
        }
    }

    protected ItemStack insertIntoFullVaultInternal(ItemStack stack) {
        if (!(vaultHandler instanceof ItemStackHandler handler)) {
            moveItemStackTo(stack, 0, getVisibleSlotCount(), false);
            return stack;
        }
        int size = handler.getSlots();
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            ItemStack existing = handler.getStackInSlot(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack)) continue;
            int limit  = Math.min(handler.getSlotLimit(i), existing.getMaxStackSize());
            int canFit = limit - existing.getCount();
            if (canFit <= 0) continue;
            int moved = Math.min(canFit, stack.getCount());
            existing.grow(moved);
            stack.shrink(moved);
            handler.setStackInSlot(i, existing);
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) continue;
            int limit = Math.min(handler.getSlotLimit(i), stack.getMaxStackSize());
            int moved = Math.min(limit, stack.getCount());
            handler.setStackInSlot(i, stack.copyWithCount(moved));
            stack.shrink(moved);
        }
        return stack;
    }

    public void doStackedPickup(Slot slot, int amount) {
        if (!(slot instanceof VaultSlot vs) || !vs.isAggregated()) return;
        if (machine != null) machine.beginBatch(VaultMachine.BatchSyncMode.DELTA_ONLY);
        try {
            ItemStack extracted = slot.remove(amount);
            if (extracted.isEmpty()) return;
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                setCarried(extracted);
            } else if (ItemStack.isSameItemSameTags(carried, extracted)) {
                carried.grow(extracted.getCount());
            } else {
                insertIntoFullVaultInternal(extracted);
            }
            slot.setChanged();
            if (displayMode == VaultDisplayMode.STACKED) rebuildAggregatedView();
        } finally {
            if (machine != null) machine.endBatch();
        }
    }

    private ItemStack quickMoveAggregatedVaultSlot(Slot slot, int invStart, int invEnd) {
        ItemStack visible = slot.getItem();
        if (visible.isEmpty()) return ItemStack.EMPTY;

        int toMove = computePlayerCapacity(visible, invStart, invEnd);
        toMove = Math.min(toMove, visible.getMaxStackSize());
        if (toMove <= 0) return ItemStack.EMPTY;

        if (machine != null) machine.beginBatch(VaultMachine.BatchSyncMode.DELTA_ONLY);
        try {
            ItemStack extracted = slot.remove(toMove);
            if (extracted.isEmpty()) return ItemStack.EMPTY;

            if (extracted.getCount() > toMove) {
                ItemStack excess = extracted.split(extracted.getCount() - toMove);
                insertIntoFullVaultInternal(excess);
            }

            ItemStack remaining = extracted.copy();
            moveItemStackTo(remaining, invStart, invEnd, true);
            if (!remaining.isEmpty()) insertIntoFullVaultInternal(remaining);

            slot.setChanged();
            if (displayMode == VaultDisplayMode.STACKED) rebuildAggregatedView();
            return ItemStack.EMPTY;
        } finally {
            if (machine != null) machine.endBatch();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack    = slot.getItem().copy();
        ItemStack original = stack.copy();
        int vaultEnd   = getVisibleSlotCount();
        int invStart   = playerSlotsStart;
        int invEnd     = craftingSlotsStart;
        int craftStart = craftingSlotsStart;
        int craftEnd   = craftingOutputStart;
        int craftOut   = craftingOutputStart;

        if (index < vaultEnd && displayMode == VaultDisplayMode.STACKED && slot instanceof VaultSlot vaultSlot && vaultSlot.isAggregated()) {
            return quickMoveAggregatedVaultSlot(slot, invStart, invEnd);
        }

        if (index == craftOut) {
            ItemStack result = slot.getItem();
            if (result.isEmpty()) return ItemStack.EMPTY;
            int outputPerCraft = result.getCount();
            if (outputPerCraft <= 0) return ItemStack.EMPTY;

            Level level = getLevel();
            if (level == null) return ItemStack.EMPTY;

            var recipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftingGrid, level)
                    .orElse(null);
            if (recipe == null) return ItemStack.EMPTY;

            int maxByStack = result.getMaxStackSize() / outputPerCraft;

            int maxByDurability = Integer.MAX_VALUE;
            for (int gi = 0; gi < craftingGrid.getContainerSize(); gi++) {
                ItemStack gridItem = craftingGrid.getItem(gi);
                if (gridItem.isEmpty() || !gridItem.isDamageableItem()) continue;
                int uses = Math.max(0, gridItem.getMaxDamage() - gridItem.getDamageValue());
                maxByDurability = Math.min(maxByDurability, uses);
            }
            if (result.isDamageableItem()) {
                int uses = Math.max(0, (result.getMaxDamage() - result.getDamageValue()) / Math.max(1, outputPerCraft));
                maxByDurability = Math.min(maxByDurability, uses);
            }

            int maxCrafts = Math.min(maxByStack, maxByDurability);
            if (maxCrafts <= 0) return ItemStack.EMPTY;

            ItemStack itemAtStart = result.copy();
            ItemStack collected   = ItemStack.EMPTY;

            if (machine != null) machine.beginBatch(VaultMachine.BatchSyncMode.DELTA_ONLY);
            beginCraftingGridBulkUpdate();
            try {
                Map<net.minecraft.world.item.Item, ArrayDeque<Integer>> vaultSlotMap = buildVaultSlotMap();
                skipRefill = true;
                for (int craft = 0; craft < maxCrafts; craft++) {
                    ItemStack current = slot.getItem();
                    if (current.isEmpty()) break;
                    if (!ItemStack.isSameItemSameTags(current, itemAtStart)) break;
                    if (computePlayerCapacity(result, invStart, invEnd) < outputPerCraft) break;

                    ItemStack crafted = current.copyWithCount(outputPerCraft);
                    if (!collected.isEmpty() && !ItemStack.isSameItemSameTags(collected, crafted)) break;

                    snapshotGridIngredients();
                    slot.onTake(player, current);
                    boolean refilled = refillGridFromVault(vaultSlotMap, false);

                    if (collected.isEmpty()) collected = crafted.copy();
                    else collected.grow(crafted.getCount());

                    if (!refilled) break;
                }
                if (!collected.isEmpty()) {
                    if (!moveItemStackTo(collected, invStart, invEnd, true))
                        insertIntoFullVaultInternal(collected);
                }
            } finally {
                skipRefill = false;
                endCraftingGridBulkUpdate();
                if (machine != null) machine.endBatch();
            }
            return ItemStack.EMPTY;

        } else if (index < vaultEnd) {
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else if (index >= craftStart && index < craftEnd) {
            stack = insertIntoFullVault(stack);
            if (!stack.isEmpty()) moveItemStackTo(stack, invStart, invEnd, true);
        } else {
            ItemStack remaining = insertIntoFullVault(stack.copy());
            int moved = stack.getCount() - remaining.getCount();
            stack.shrink(moved);
            if (!stack.isEmpty()) moveItemStackTo(stack, craftStart, craftEnd, false);
        }

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private int computePlayerCapacity(ItemStack template, int invStart, int invEnd) {
        int space = 0;
        for (int i = invStart; i < invEnd; i++) {
            Slot s = slots.get(i);
            ItemStack inSlot = s.getItem();
            if (inSlot.isEmpty()) {
                space += Math.min(s.getMaxStackSize(), template.getMaxStackSize());
            } else if (ItemStack.isSameItemSameTags(inSlot, template)) {
                space += Math.min(s.getMaxStackSize(), inSlot.getMaxStackSize()) - inSlot.getCount();
            }
        }
        return space;
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        int limit        = Math.min(items.size(), slots.size());
        int vaultVisible = getVisibleSlotCount();
        for (int i = 0; i < limit; i++) {
            if (i < vaultVisible) continue;
            slots.get(i).set(items.get(i));
        }
        setCarried(carried);
    }

    @Override
    public void setItem(int slotId, int stateId, ItemStack stack) {
        if (slotId >= 0 && slotId < getVisibleSlotCount()) return;
        super.setItem(slotId, stateId, stack);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            if (machine != null && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                machine.removeViewer(sp);
            }
            if (onGridClose != null) {
                ItemStack[] grid = new ItemStack[craftingGrid.getContainerSize()];
                for (int i = 0; i < grid.length; i++) grid[i] = craftingGrid.getItem(i).copy();
                onGridClose.accept(grid);
            }
        }
    }

    protected Level getLevel() {
        for (Slot slot : slots) {
            if (slot.container instanceof Inventory inv) return inv.player.level();
        }
        return null;
    }
}