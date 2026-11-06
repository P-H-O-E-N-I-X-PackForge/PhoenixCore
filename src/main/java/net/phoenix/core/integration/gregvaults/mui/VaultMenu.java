package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import brachy.modularui.screen.ModularContainerMenu;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

public final class VaultMenu extends ModularContainerMenu {

    private final VaultSession session;

    public VaultMenu(int containerId, VaultSession session) {
        super(containerId);
        this.session = session;
    }

    public VaultSession session() {
        return session;
    }

    @Override
    public boolean stillValid(Player player) {
        return session.stillValid(player) && super.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide || !session.server || session.view == null) return ItemStack.EMPTY;
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem().copy();
        ItemStack original = stack.copy();

        boolean inWindow = session.windowSlots.contains(slot);
        boolean inGrid = session.gridSlots.contains(slot);

        if (inWindow && session.view.window.isAggregated()) {
            return quickMoveGrouped(slot);
        }
        if (slot == session.resultSlot) {
            return quickCraft(player, slot);
        }

        if (inWindow) {
            if (!mergeInto(session.playerSlots, stack, true)) return ItemStack.EMPTY;
        } else if (inGrid) {
            stack = session.insertIntoVault(stack);
            if (!stack.isEmpty()) mergeInto(session.playerSlots, stack, true);
        } else {
            ItemStack remaining = session.insertIntoVault(stack.copy());
            int moved = stack.getCount() - remaining.getCount();
            stack.shrink(moved);
            if (!stack.isEmpty()) mergeInto(session.gridSlots, stack, false);
        }

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        return original;
    }

    private ItemStack quickMoveGrouped(Slot slot) {
        ItemStack visible = slot.getItem();
        if (visible.isEmpty()) return ItemStack.EMPTY;

        int toMove = Math.min(capacityFor(visible), visible.getMaxStackSize());
        if (toMove <= 0) return ItemStack.EMPTY;

        ItemStack extracted = slot.remove(toMove);
        if (extracted.isEmpty()) return ItemStack.EMPTY;

        if (extracted.getCount() > toMove) {
            session.insertIntoVault(extracted.split(extracted.getCount() - toMove));
        }

        ItemStack remaining = extracted.copy();
        mergeInto(session.playerSlots, remaining, true);
        if (!remaining.isEmpty()) session.insertIntoVault(remaining);

        session.view.rebuild();
        return ItemStack.EMPTY;
    }

    private ItemStack quickCraft(Player player, Slot slot) {
        ItemStack result = slot.getItem();
        if (result.isEmpty()) return ItemStack.EMPTY;
        int outputPerCraft = result.getCount();
        if (outputPerCraft <= 0) return ItemStack.EMPTY;

        Level level = player.level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, session.grid.asCraftInput(), level)
                .map(RecipeHolder::value)
                .orElse(null);
        if (recipe == null) return ItemStack.EMPTY;

        int maxByStack = result.getMaxStackSize() / outputPerCraft;

        int maxByDurability = Integer.MAX_VALUE;
        for (int gi = 0; gi < session.grid.getContainerSize(); gi++) {
            ItemStack gridItem = session.grid.getItem(gi);
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
        ItemStack collected = ItemStack.EMPTY;

        session.beginGridBulkUpdate();
        try {
            Map<net.minecraft.world.item.Item, ArrayDeque<Integer>> vaultSlotMap = session.buildVaultSlotMap();
            session.setSkipRefill(true);
            for (int craft = 0; craft < maxCrafts; craft++) {
                ItemStack current = slot.getItem();
                if (current.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(current, itemAtStart)) break;
                if (capacityFor(result) < outputPerCraft) break;

                ItemStack crafted = current.copyWithCount(outputPerCraft);
                if (!collected.isEmpty() && !ItemStack.isSameItemSameComponents(collected, crafted)) break;

                session.snapshotGridIngredients();
                slot.onTake(player, current);
                boolean refilled = session.refillGridFromVault(vaultSlotMap, false);

                if (collected.isEmpty()) collected = crafted.copy();
                else collected.grow(crafted.getCount());

                if (!refilled) break;
            }
            if (!collected.isEmpty()) {
                mergeInto(session.playerSlots, collected, true);
                if (!collected.isEmpty()) session.insertIntoVault(collected);
            }
        } finally {
            session.setSkipRefill(false);
            session.endGridBulkUpdate();
        }
        return ItemStack.EMPTY;
    }

    private int capacityFor(ItemStack template) {
        int space = 0;
        for (Slot s : session.playerSlots) {
            if (!s.mayPlace(template)) continue;
            ItemStack inSlot = s.getItem();
            if (inSlot.isEmpty()) {
                space += Math.min(s.getMaxStackSize(), template.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(inSlot, template)) {
                space += Math.min(s.getMaxStackSize(), inSlot.getMaxStackSize()) - inSlot.getCount();
            }
        }
        return space;
    }

    private boolean mergeInto(List<? extends Slot> targets, ItemStack stack, boolean fromEnd) {
        boolean moved = false;
        int n = targets.size();

        for (int k = 0; k < n && !stack.isEmpty(); k++) {
            Slot target = targets.get(fromEnd ? n - 1 - k : k);
            ItemStack inSlot = target.getItem();
            if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(stack, inSlot) || !target.mayPlace(stack)) {
                continue;
            }
            int limit = Math.min(target.getMaxStackSize(stack), inSlot.getMaxStackSize());
            int room = limit - inSlot.getCount();
            if (room <= 0) continue;
            int move = Math.min(room, stack.getCount());
            inSlot.grow(move);
            stack.shrink(move);
            target.setChanged();
            moved = true;
        }

        for (int k = 0; k < n && !stack.isEmpty(); k++) {
            Slot target = targets.get(fromEnd ? n - 1 - k : k);
            if (!target.getItem().isEmpty() || !target.mayPlace(stack)) continue;
            int move = Math.min(target.getMaxStackSize(stack), stack.getCount());
            if (move <= 0) continue;
            target.set(stack.split(move));
            moved = true;
        }
        return moved;
    }
}
