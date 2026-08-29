package net.phoenix.core.shop;

import net.minecraft.world.item.ItemStack;

public record RewardSpec(String type, String param, ItemStack itemParam) {

    public RewardSpec(String type, String param) {
        this(type, param, ItemStack.EMPTY);
    }
}
