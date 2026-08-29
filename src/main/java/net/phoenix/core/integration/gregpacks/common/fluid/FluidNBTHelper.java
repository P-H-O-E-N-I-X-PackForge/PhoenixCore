package net.phoenix.core.integration.gregpacks.common.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class FluidNBTHelper {

    private static final String ROOT = "gregpacks";
    private static final String FLUID = "fluid";

    public static FluidStack getFluid(ItemStack stack) {
        if (!stack.hasTag()) return FluidStack.EMPTY;
        CompoundTag root = stack.getTag().getCompound(ROOT);
        if (!root.contains(FLUID)) return FluidStack.EMPTY;
        return FluidStack.loadFluidStackFromNBT(root.getCompound(FLUID));
    }

    public static int getAmount(ItemStack stack) {
        return getFluid(stack).getAmount();
    }

    public static void setFluid(ItemStack stack, FluidStack fluid) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag root = tag.contains(ROOT) ? tag.getCompound(ROOT) : new CompoundTag();

        if (fluid.isEmpty()) {
            root.remove(FLUID);
        } else {
            root.put(FLUID, fluid.writeToNBT(new CompoundTag()));
        }

        tag.put(ROOT, root);
    }

    public static void clear(ItemStack stack) {
        setFluid(stack, FluidStack.EMPTY);
    }
}
