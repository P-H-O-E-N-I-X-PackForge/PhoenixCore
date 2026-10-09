package net.phoenix.core.common.item.cinder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public final class CinderAtlasData {

    private CinderAtlasData() {}

    public static final int BASE_LOADOUT_COUNT = 4;
    public static final int MAX_LOADOUT_COUNT = 8;
    public static final int SLOT_COUNT = 8;
    public static final int UPGRADE_SLOT_COUNT = 4;

    private static final String ACTIVE_LOADOUT = "ActiveLoadout";
    private static final String ACTIVE_SLOT = "ActiveSlot";
    private static final String LOADOUTS = "Loadouts";
    private static final String LOADOUT_NAME = "Name";
    private static final String SLOTS = "Slots";
    private static final String UPGRADES = "Upgrades";

    public static int getActiveLoadout(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        int index = tag != null ? tag.getInt(ACTIVE_LOADOUT) : 0;
        return clamp(index, MAX_LOADOUT_COUNT);
    }

    public static void setActiveLoadout(ItemStack atlas, int loadoutIndex) {
        atlas.getOrCreateTag().putInt(ACTIVE_LOADOUT, clamp(loadoutIndex, MAX_LOADOUT_COUNT));
    }

    public static int getActiveSlot(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        int index = tag != null ? tag.getInt(ACTIVE_SLOT) : 0;
        return clamp(index, SLOT_COUNT);
    }

    public static void setActiveSlot(ItemStack atlas, int slotIndex) {
        atlas.getOrCreateTag().putInt(ACTIVE_SLOT, clamp(slotIndex, SLOT_COUNT));
    }

    public static String getLoadoutName(ItemStack atlas, int loadoutIndex) {
        CompoundTag loadout = peekLoadoutTag(atlas, loadoutIndex);
        if (loadout != null && loadout.contains(LOADOUT_NAME)) {
            return loadout.getString(LOADOUT_NAME);
        }
        return "Loadout " + (loadoutIndex + 1);
    }

    public static void setLoadoutName(ItemStack atlas, int loadoutIndex, String name) {
        getOrCreateLoadoutTag(atlas, loadoutIndex).putString(LOADOUT_NAME, name);
    }

    public static CompoundTag getOrCreateSlotTag(ItemStack atlas, int loadoutIndex, int slotIndex) {
        CompoundTag loadout = getOrCreateLoadoutTag(atlas, loadoutIndex);
        ListTag slots = loadout.getList(SLOTS, Tag.TAG_COMPOUND);
        slotIndex = clamp(slotIndex, SLOT_COUNT);

        while (slots.size() <= slotIndex) {
            slots.add(new CompoundTag());
        }
        CompoundTag slot = slots.getCompound(slotIndex);
        loadout.put(SLOTS, slots);
        return slot;
    }

    public static @Nullable CompoundTag peekSlotTag(ItemStack atlas, int loadoutIndex, int slotIndex) {
        CompoundTag loadout = peekLoadoutTag(atlas, loadoutIndex);
        if (loadout == null) return null;
        ListTag slots = loadout.getList(SLOTS, Tag.TAG_COMPOUND);
        slotIndex = clamp(slotIndex, SLOT_COUNT);
        return slotIndex < slots.size() ? slots.getCompound(slotIndex) : null;
    }

    public static boolean isSlotConfigured(ItemStack atlas, int loadoutIndex, int slotIndex) {
        CompoundTag slot = peekSlotTag(atlas, loadoutIndex, slotIndex);
        return slot != null && CinderSchemaData.getTargetId(slot) != null;
    }

    public static void clearSlot(ItemStack atlas, int loadoutIndex, int slotIndex) {
        CompoundTag slot = peekSlotTag(atlas, loadoutIndex, slotIndex);
        if (slot != null) CinderSchemaData.clearTarget(slot);
    }

    private static CompoundTag getOrCreateLoadoutTag(ItemStack atlas, int loadoutIndex) {
        CompoundTag tag = atlas.getOrCreateTag();
        ListTag loadouts = tag.getList(LOADOUTS, Tag.TAG_COMPOUND);
        loadoutIndex = clamp(loadoutIndex, MAX_LOADOUT_COUNT);

        while (loadouts.size() <= loadoutIndex) {
            loadouts.add(new CompoundTag());
        }
        CompoundTag loadout = loadouts.getCompound(loadoutIndex);
        tag.put(LOADOUTS, loadouts);
        return loadout;
    }

    private static @Nullable CompoundTag peekLoadoutTag(ItemStack atlas, int loadoutIndex) {
        CompoundTag tag = atlas.getTag();
        if (tag == null) return null;
        ListTag loadouts = tag.getList(LOADOUTS, Tag.TAG_COMPOUND);
        loadoutIndex = clamp(loadoutIndex, MAX_LOADOUT_COUNT);
        return loadoutIndex < loadouts.size() ? loadouts.getCompound(loadoutIndex) : null;
    }

    public static ItemStack getUpgradeSlot(ItemStack atlas, int slotIndex) {
        CompoundTag tag = atlas.getTag();
        if (tag == null) return ItemStack.EMPTY;
        ListTag upgrades = tag.getList(UPGRADES, Tag.TAG_COMPOUND);
        slotIndex = clamp(slotIndex, UPGRADE_SLOT_COUNT);
        if (slotIndex >= upgrades.size()) return ItemStack.EMPTY;
        return ItemStack.of(upgrades.getCompound(slotIndex));
    }

    public static void setUpgradeSlot(ItemStack atlas, int slotIndex, ItemStack upgrade) {
        CompoundTag tag = atlas.getOrCreateTag();
        ListTag upgrades = tag.getList(UPGRADES, Tag.TAG_COMPOUND);
        slotIndex = clamp(slotIndex, UPGRADE_SLOT_COUNT);

        while (upgrades.size() <= slotIndex) {
            upgrades.add(new CompoundTag());
        }
        CompoundTag saved = new CompoundTag();
        if (!upgrade.isEmpty()) upgrade.save(saved);
        upgrades.set(slotIndex, saved);
        tag.put(UPGRADES, upgrades);
    }

    private static int clamp(int index, int count) {
        return Math.max(0, Math.min(count - 1, index));
    }
}
