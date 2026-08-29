package net.phoenix.core.common.data.recipe.records;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("all")
public record FullBeeConfig(
                            String beeId,
                            int tier,
                            String pollinationInputId,
                            String finalOutputId,
                            int outputCount,
                            int decantingEut,
                            int decantingDuration,
                            int waxEut,
                            int waxDuration,
                            int fluidEut,
                            int fluidDuration,
                            int boostedDecantingEut,
                            int boostedDecantingDuration,
                            int boostedWaxEut,
                            int boostedWaxDuration,
                            int boostedFluidEut,
                            int boostedFluidDuration) {

    public ItemStack finalOutputItem() {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(finalOutputId));
        assert item != null;
        return new ItemStack(item, outputCount);
    }
}
