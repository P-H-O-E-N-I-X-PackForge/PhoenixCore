package net.phoenix.core.shop.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.shop.RewardSpec;

public class ItemShopReward implements ShopReward {

    public static final String TYPE = "item";

    private final ItemStack stack;

    public ItemShopReward(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void grant(ServerPlayer player) {
        ItemStack give = stack.copy();
        if (!player.getInventory().add(give)) {
            player.drop(give, false);
        }
    }

    @Override
    public Component describe() {
        return Component.literal(stack.getCount() + "x ").append(stack.getHoverName());
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("stack", stack.save(new CompoundTag()));
        return tag;
    }

    public static ItemShopReward load(CompoundTag tag) {
        return new ItemShopReward(ItemStack.of(tag.getCompound("stack")));
    }

    @Override
    public RewardSpec toSpec() {
        return new RewardSpec(TYPE, "", stack.copy());
    }
}
