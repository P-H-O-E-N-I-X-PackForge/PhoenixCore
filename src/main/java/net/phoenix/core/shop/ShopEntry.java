package net.phoenix.core.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.shop.reward.ShopReward;
import net.phoenix.core.shop.reward.ShopRewardTypes;

import java.util.List;
import java.util.UUID;

public class ShopEntry {

    public static final String DEFAULT_CATEGORY = "General";

    public final UUID id;
    public final String name;
    public final ItemStack icon;
    public final int cost;
    public final List<ShopReward> rewards;
    public final String category;

    public ShopEntry(UUID id, String name, ItemStack icon, int cost, List<ShopReward> rewards, String category) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.cost = cost;
        this.rewards = rewards;
        this.category = category == null || category.isBlank() ? DEFAULT_CATEGORY : category;
    }

    public static ShopEntry create(String name, ItemStack icon, int cost, List<ShopReward> rewards,
            String category) {
        return new ShopEntry(UUID.randomUUID(), name, icon, cost, rewards, category);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.put("icon", icon.save(new CompoundTag()));
        tag.putInt("cost", cost);
        tag.putString("category", category);
        ListTag rewardList = new ListTag();
        for (ShopReward reward : rewards) rewardList.add(ShopRewardTypes.save(reward));
        tag.put("rewards", rewardList);
        return tag;
    }

    public static ShopEntry load(CompoundTag tag) {
        List<ShopReward> rewards = new java.util.ArrayList<>();
        ListTag rewardList = tag.getList("rewards", 10);
        for (int i = 0; i < rewardList.size(); i++) {
            rewards.add(ShopReward.load(rewardList.getCompound(i)));
        }
        return new ShopEntry(
                tag.getUUID("id"),
                tag.getString("name"),
                ItemStack.of(tag.getCompound("icon")),
                tag.getInt("cost"),
                rewards,
                tag.contains("category") ? tag.getString("category") : DEFAULT_CATEGORY);
    }
}
