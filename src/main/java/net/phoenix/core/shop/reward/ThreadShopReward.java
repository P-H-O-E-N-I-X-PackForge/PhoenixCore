package net.phoenix.core.shop.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.integration.astral.item.AstralThreadCellItem;
import net.phoenix.core.shop.RewardSpec;

public class ThreadShopReward implements ShopReward {

    public static final String TYPE = "astral_thread";

    private final int amount;

    public ThreadShopReward(int amount) {
        this.amount = amount;
    }

    @Override
    public void grant(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AstralThreadCellItem) {
                AstralThreadCellItem.addThread(stack, amount);
                return;
            }
        }
        player.displayClientMessage(
                Component.literal("No Astral Thread Cell carried - Thread reward was lost.")
                        .withStyle(ChatFormatting.RED),
                false);
    }

    @Override
    public Component describe() {
        return Component.literal("+" + amount + " Astral Thread").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("amount", amount);
        return tag;
    }

    public static ThreadShopReward load(CompoundTag tag) {
        return new ThreadShopReward(tag.getInt("amount"));
    }

    @Override
    public RewardSpec toSpec() {
        return new RewardSpec(TYPE, String.valueOf(amount));
    }
}
