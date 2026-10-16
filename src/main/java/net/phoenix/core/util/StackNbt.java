package net.phoenix.core.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import org.jetbrains.annotations.Nullable;

public final class StackNbt {

    private StackNbt() {}

    /**
     * A tag that writes through to the stack, like the 1.20 getOrCreateTag. CustomData is immutable and copies on the
     * way in and out, so mutating a copy silently dropped every edit; here a fresh CustomData (never shared with
     * another stack) is installed and its own backing tag is handed out.
     */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag copy = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (stack.isEmpty()) return copy;
        CustomData fresh = CustomData.of(copy);
        stack.set(DataComponents.CUSTOM_DATA, fresh);
        return fresh.getUnsafe();
    }

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

    public static CompoundTag saveStack(ItemStack stack) {
        if (stack.isEmpty()) return new CompoundTag();
        return (CompoundTag) stack.save(NbtRegistries.PROVIDER);
    }

    public static void saveInto(ItemStack stack, CompoundTag into) {
        if (!stack.isEmpty()) stack.save(NbtRegistries.PROVIDER, into);
    }

    public static ItemStack loadStack(CompoundTag tag) {
        return ItemStack.parseOptional(NbtRegistries.PROVIDER, tag);
    }

    public static CompoundTag saveFluid(net.neoforged.neoforge.fluids.FluidStack fluid) {
        if (fluid.isEmpty()) return new CompoundTag();
        return (CompoundTag) fluid.save(NbtRegistries.PROVIDER);
    }

    public static void saveFluidInto(net.neoforged.neoforge.fluids.FluidStack fluid, CompoundTag into) {
        if (!fluid.isEmpty()) fluid.save(NbtRegistries.PROVIDER, into);
    }

    public static net.neoforged.neoforge.fluids.FluidStack loadFluid(CompoundTag tag) {
        return net.neoforged.neoforge.fluids.FluidStack.parseOptional(NbtRegistries.PROVIDER, tag);
    }
}
