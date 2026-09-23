package net.phoenix.core.common.item.cinder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * NBT layout for the Cinder Atlas deployment tool - see
 * {@code docs/content/Development/Systems/cinder_atlas_deployment_tool.md} for the design this backs.
 * Where a single Cinder Core's tag directly holds one {@code TargetMultiblock}/slice/preference set
 * (see {@link CinderSchemaData}), an Atlas holds up to {@value #MAX_LOADOUT_COUNT} loadouts, each with
 * up to {@value #SLOT_COUNT} independent pattern slots - and each slot's own sub-tag is exactly what a
 * Cinder Core's root tag would be, read/written through {@link CinderSchemaData}'s
 * {@link CompoundTag}-taking overloads rather than duplicating any of that logic here. This class only
 * owns getting to the right sub-tag; it has no idea what a "target" or a "slice repeat" even is.
 * <p>
 * {@value #MAX_LOADOUT_COUNT} is the NBT's physical ceiling, not how many loadouts a given Atlas can
 * actually use - that's {@code CinderAtlasUpgrades#getLoadoutCapacity}, starting at
 * {@value #BASE_LOADOUT_COUNT} and raised by installed Loadout Expansion upgrades (see the Upgrades
 * section below). Keeping the array always sized up to the physical ceiling (rather than growing it
 * dynamically as capacity increases) means installing/removing an expansion upgrade never needs any NBT
 * migration - a "locked" loadout's tag already exists lazily the same way any other one does, it's just
 * not reachable through {@link #setActiveLoadout} or shown in the UI until unlocked.
 * <p>
 * Tag shape:
 * <pre>
 * {
 *   ActiveLoadout: int,
 *   ActiveSlot: int,
 *   Loadouts: [
 *     { Name: string (optional), Slots: [ {..CinderSchemaData fields.. }, ... up to SLOT_COUNT ] },
 *     ... up to MAX_LOADOUT_COUNT entries, lazily filled in on first touch ...
 *   ],
 *   Upgrades: [ ItemStack, ... up to UPGRADE_SLOT_COUNT entries, empty stacks for unfilled slots ]
 * }
 * </pre>
 */
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

    /** Which loadout the radial menu currently points at. Clamped defensively on read since nothing
     *  should ever write an out-of-range index, but NBT can be hand-edited or come from an
     *  older/different tool version. This clamp is only the NBT's physical ceiling - callers that care
     *  whether the loadout is actually *unlocked* (the editor screen, the radial menu, the active-loadout
     *  packet handler) must check {@code CinderAtlasUpgrades#getLoadoutCapacity} themselves. */
    public static int getActiveLoadout(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        int index = tag != null ? tag.getInt(ACTIVE_LOADOUT) : 0;
        return clamp(index, MAX_LOADOUT_COUNT);
    }

    public static void setActiveLoadout(ItemStack atlas, int loadoutIndex) {
        atlas.getOrCreateTag().putInt(ACTIVE_LOADOUT, clamp(loadoutIndex, MAX_LOADOUT_COUNT));
    }

    /** Which slot *within* the active loadout actually deploys when you right-click in the world - see
     *  {@code CinderDeploySource}. Separate from "which slot the editor screen currently has selected"
     *  in spirit, but the screen sets this to match its own selection since there's only one sensible
     *  reason to select a slot right now; a dedicated "deploy vs. edit" distinction can be added later
     *  if a real use for it shows up (e.g. the radial menu jumping to a slot without opening the full
     *  screen). */
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

    /** The sub-tag for one pattern slot - hand this directly to {@link CinderSchemaData}'s
     *  {@code CompoundTag} overloads ({@code setTarget}, {@code resolveSchema}, {@code resolvePlacement},
     *  etc.) exactly as if it were a Cinder Core's own root tag. Creates the loadout/slot structure on
     *  first touch rather than requiring it to be pre-populated. */
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

    /** Read-only counterpart of {@link #getOrCreateSlotTag} - returns {@code null} instead of creating
     *  structure, for UI code that just wants to know "is this slot configured" without mutating an
     *  item sitting in someone's inventory on every render frame. */
    public static @Nullable CompoundTag peekSlotTag(ItemStack atlas, int loadoutIndex, int slotIndex) {
        CompoundTag loadout = peekLoadoutTag(atlas, loadoutIndex);
        if (loadout == null) return null;
        ListTag slots = loadout.getList(SLOTS, Tag.TAG_COMPOUND);
        slotIndex = clamp(slotIndex, SLOT_COUNT);
        return slotIndex < slots.size() ? slots.getCompound(slotIndex) : null;
    }

    /** Whether a slot has an actual target configured - a freshly-created empty slot tag has no
     *  {@code TargetMultiblock} key yet, same "unconfigured" meaning a fresh Cinder Core has. */
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

    /**
     * Design doc feature #7 - the upgrade system. A fixed {@value #UPGRADE_SLOT_COUNT}-entry list of
     * real {@link ItemStack}s (not just type markers), the same "store the real stack" choice
     * {@code OmniPackInventory} makes for its own upgrade slots elsewhere in this codebase - keeps this
     * generically correct (stack count, any future NBT on the upgrade item itself) rather than reducing
     * an upgrade to a bare enum name.
     */
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
