package net.phoenix.core.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import org.jetbrains.annotations.Nullable;

/**
 * 1.20-style access to a stack's custom NBT, backed by the CUSTOM_DATA component.
 */
public final class StackNbt {

    private StackNbt() {}

    /** Returns a tag owned by this stack; edits to it are edits to the stack. */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!stack.isEmpty()) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return tag;
    }

    /** A copy of the stack's tag, or null when it has none. Use {@link #getOrCreateTag} to edit. */
    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void removeTagKey(ItemStack stack, String key) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(key));
    }
}
