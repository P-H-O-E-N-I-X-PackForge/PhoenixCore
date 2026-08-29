package net.phoenix.core.integration.gregpacks.common.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class OmniPackInventory extends SimpleContainer {

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_UPGRADES = "Upgrades";
    private static final String TAG_SLOT = "Slot";

    public OmniPackInventory(int slots) {
        super(slots);
    }

    public void saveToItem(ItemStack stack) {
        stack.getOrCreateTag().put(TAG_ITEMS, serializeToTag());
    }

    public void loadFromItem(ItemStack stack) {
        if (!stack.hasTag()) return;
        deserializeFromTag(stack.getTag().getList(TAG_ITEMS, Tag.TAG_COMPOUND));
    }

    public static OmniPackInventory fromItem(ItemStack stack, int slots) {
        OmniPackInventory inv = new OmniPackInventory(slots);
        inv.loadFromItem(stack);
        return inv;
    }

    public void saveUpgradesToItem(ItemStack stack) {
        stack.getOrCreateTag().put(TAG_UPGRADES, serializeToTag());
    }

    public void loadUpgradesFromItem(ItemStack stack) {
        if (!stack.hasTag()) return;
        deserializeFromTag(stack.getTag().getList(TAG_UPGRADES, Tag.TAG_COMPOUND));
    }

    public static OmniPackInventory fromUpgradeItem(ItemStack stack, int slots) {
        OmniPackInventory inv = new OmniPackInventory(slots);
        inv.loadUpgradesFromItem(stack);
        return inv;
    }

    public ListTag serializeToTag() {
        ListTag list = new ListTag();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack s = getItem(i);
            if (!s.isEmpty()) {
                CompoundTag t = new CompoundTag();
                t.putByte(TAG_SLOT, (byte) i);
                s.save(t);
                list.add(t);
            }
        }
        return list;
    }

    public void deserializeFromTag(ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int slot = t.getByte(TAG_SLOT) & 0xFF;
            if (slot < getContainerSize()) setItem(slot, ItemStack.of(t));
        }
    }
}
