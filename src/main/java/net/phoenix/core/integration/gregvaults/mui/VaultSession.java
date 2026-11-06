package net.phoenix.core.integration.gregvaults.mui;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import brachy.modularui.widgets.slot.ModularSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VaultSession {

    public final boolean server;
    public final Player player;
    public final VaultAccess access;

    @Nullable
    public final VaultView view;

    @Nullable
    public final TransientCraftingContainer grid;

    @Nullable
    public final ResultContainer result;

    private final IItemHandlerModifiable windowHandler;
    private final IItemHandlerModifiable gridHandler;
    private final IItemHandlerModifiable resultHandler;

    public final List<ModularSlot> windowSlots = new ArrayList<>();
    public final List<ModularSlot> playerSlots = new ArrayList<>();
    public final List<ModularSlot> gridSlots = new ArrayList<>();
    public ModularSlot resultSlot;

    public VaultSyncHandler sync;

    private final boolean[] windowEnabled = new boolean[VaultWindow.SIZE];

    public int totalRows = 0;
    public int storageSlots = 0;
    public int activeSlots = VaultWindow.SIZE;
    public VaultDisplayMode displayMode = VaultDisplayMode.SLOTS;
    public int scrollRow = 0;

    private boolean suppressResultUpdates = false;
    private boolean skipRefill = false;
    private int lastGridHash = 0;
    private final ItemStack[] lastGridIngredients = new ItemStack[9];

    public VaultSession(Player player, VaultAccess access) {
        this.player = player;
        this.access = access;
        this.server = !player.level().isClientSide;
        java.util.Arrays.fill(windowEnabled, true);
        java.util.Arrays.fill(lastGridIngredients, ItemStack.EMPTY);

        if (server && access.storage() != null) {
            this.view = new VaultView(access);
            this.grid = new TransientCraftingContainer(new GridWatcher(), 3, 3);
            this.result = new ResultContainer();
            this.windowHandler = view.window;
            this.gridHandler = new InvWrapper(grid);
            this.resultHandler = new InvWrapper(result);
        } else {
            this.view = null;
            this.grid = null;
            this.result = null;
            this.windowHandler = new ItemStackHandler(VaultWindow.SIZE);
            this.gridHandler = new ItemStackHandler(9);
            this.resultHandler = new ItemStackHandler(1);
        }
    }

    private final class GridWatcher extends AbstractContainerMenu {

        GridWatcher() {
            super(null, -1);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void slotsChanged(Container container) {
            if (container == grid) onGridChanged();
        }
    }

    public IItemHandlerModifiable windowHandler() {
        return windowHandler;
    }

    public IItemHandlerModifiable gridHandler() {
        return gridHandler;
    }

    public IItemHandlerModifiable resultHandler() {
        return resultHandler;
    }

    public boolean isStacked() {
        return displayMode == VaultDisplayMode.STACKED;
    }

    public boolean stillValid(Player player) {
        return access.stillValid(player);
    }

    public void tick() {
        if (view == null) return;
        view.tick();
        refreshWindowSlots();
    }

    public void refreshWindowSlots() {
        if (view == null) return;
        for (int i = 0; i < windowSlots.size() && i < VaultWindow.SIZE; i++) {
            boolean active = view.window.isIndexActive(i);
            if (active == windowEnabled[i]) continue;
            windowEnabled[i] = active;
            ModularSlot slot = windowSlots.get(i);
            slot.setEnabled(active);
            if (slot.getSyncHandler() != null) slot.getSyncHandler().setEnabled(active, true);
        }
    }

    public void onGridChanged() {
        if (!server || suppressResultUpdates) return;
        int hash = gridHash();
        if (hash != lastGridHash) {
            lastGridHash = hash;
            updateCraftingResult();
        }
    }

    private int gridHash() {
        int h = 1;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.getItem(i);
            h = 31 * h + (s.isEmpty() ? 0 : (net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(s.getItem()) *
                    31 + s.getCount()));
        }
        return h;
    }

    public void updateCraftingResult() {
        Level level = player.level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), level)
                .map(RecipeHolder::value)
                .orElse(null);
        result.setItem(0, recipe == null ? ItemStack.EMPTY : recipe.assemble(grid.asCraftInput(),
                level.registryAccess()));
    }

    public void beginGridBulkUpdate() {
        suppressResultUpdates = true;
    }

    public void endGridBulkUpdate() {
        suppressResultUpdates = false;
        lastGridHash = gridHash();
        updateCraftingResult();
    }

    public void initGrid(ItemStack[] saved) {
        if (!server || grid == null || saved == null) return;
        for (int i = 0; i < Math.min(saved.length, grid.getContainerSize()); i++) {
            grid.setItem(i, saved[i] == null ? ItemStack.EMPTY : saved[i].copy());
        }
        updateCraftingResult();
    }

    public ItemStack[] copyGrid() {
        if (grid == null) return access.savedGrid();
        ItemStack[] out = new ItemStack[grid.getContainerSize()];
        for (int i = 0; i < out.length; i++) out[i] = grid.getItem(i).copy();
        return out;
    }

    public void snapshotGridIngredients() {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.getItem(i);
            lastGridIngredients[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
    }

    public void setSkipRefill(boolean skip) {
        skipRefill = skip;
    }

    public void afterCraftTaken(Player taker) {
        applyCraftingToolDamageToGrid(taker);
        if (!skipRefill) refillGridFromVault(buildVaultSlotMap(), true);
    }

    private void applyCraftingToolDamageToGrid(Player taker) {
        if (taker.level().isClientSide) return;

        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack before = lastGridIngredients[i];
            if (!isCraftingToolLike(before)) continue;

            ItemStack after = grid.getItem(i);

            if (after.isEmpty()) {
                ItemStack damaged = before.copy();
                damaged.setCount(1);
                damageCraftingTool(damaged, taker);
                if (!damaged.isEmpty()) grid.setItem(i, damaged);
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(before, after)) continue;

            ItemStack damaged = after.copy();
            damaged.setCount(1);
            damageCraftingTool(damaged, taker);
            grid.setItem(i, damaged.isEmpty() ? ItemStack.EMPTY : damaged);
        }
    }

    private boolean isCraftingToolLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof IGTTool) return true;
        if (!ToolHelper.getCraftingToolTypes(stack).isEmpty()) return true;
        return stack.isDamageableItem() && stack.getMaxStackSize() == 1;
    }

    private void damageCraftingTool(ItemStack stack, Player taker) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof IGTTool || !ToolHelper.getCraftingToolTypes(stack).isEmpty()) {
            ToolHelper.damageItemWhenCrafting(stack, taker);
        } else if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, taker, EquipmentSlot.MAINHAND);
        }
    }

    public Map<Item, ArrayDeque<Integer>> buildVaultSlotMap() {
        ItemStackHandler storage = access.storage();
        Map<Item, ArrayDeque<Integer>> map = new HashMap<>();
        for (int v = 0; v < storage.getSlots(); v++) {
            ItemStack s = storage.getStackInSlot(v);
            if (!s.isEmpty()) map.computeIfAbsent(s.getItem(), k -> new ArrayDeque<>()).add(v);
        }
        return map;
    }

    public boolean refillGridFromVault(Map<Item, ArrayDeque<Integer>> vaultSlotMap, boolean updateResult) {
        ItemStackHandler handler = access.storage();
        if (handler == null) return false;

        boolean anyRefilled = false;
        boolean gridBroken = false;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack needed = lastGridIngredients[i];
            if (needed == null || needed.isEmpty()) continue;

            ItemStack inGrid = grid.getItem(i);

            if (!inGrid.isEmpty()) {
                if (needed.isDamageableItem() && inGrid.getDamageValue() != needed.getDamageValue()) {
                    ArrayDeque<Integer> deque = vaultSlotMap.get(needed.getItem());
                    if (deque != null) {
                        while (!deque.isEmpty()) {
                            int v = deque.peek();
                            ItemStack vaultStack = handler.getStackInSlot(v);
                            if (vaultStack.isEmpty() || !ItemStack.isSameItemSameComponents(vaultStack, needed)) {
                                deque.poll();
                                continue;
                            }
                            vaultStack.shrink(1);
                            handler.setStackInSlot(v, vaultStack.isEmpty() ? ItemStack.EMPTY : vaultStack);
                            grid.setItem(i, needed.copyWithCount(1));
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
                if (vaultStack.isEmpty() || !ItemStack.isSameItemSameComponents(vaultStack, needed)) {
                    deque.poll();
                    continue;
                }
                vaultStack.shrink(1);
                handler.setStackInSlot(v, vaultStack.isEmpty() ? ItemStack.EMPTY : vaultStack);
                grid.setItem(i, needed.copyWithCount(1));
                if (vaultStack.isEmpty()) deque.poll();
                anyRefilled = true;
                filled = true;
                break;
            }

            if (!filled) gridBroken = true;
        }

        if (updateResult && !suppressResultUpdates) updateCraftingResult();
        return anyRefilled && !gridBroken;
    }

    public ItemStack insertIntoVault(ItemStack stack) {
        ItemStackHandler handler = access.storage();
        if (handler == null) return stack;
        int size = handler.getSlots();
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            ItemStack existing = handler.getStackInSlot(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) continue;
            int limit = Math.min(handler.getSlotLimit(i), existing.getMaxStackSize());
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
}
