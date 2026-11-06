package net.phoenix.core.integration.growth;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public record GrowthStage(IntList bounds, List<ItemStack> cost, float speedBonus, float outputBonus, int batch) {

    public GrowthStage(IntList bounds, List<ItemStack> cost) {
        this(bounds, cost, 0.0f, 0.0f, 1);
    }

    public boolean hasBonus() {
        return speedBonus > 0.0f || outputBonus > 0.0f || batch > 1;
    }
}
