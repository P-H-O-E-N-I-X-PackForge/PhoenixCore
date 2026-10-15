package net.phoenix.core.shop.reward;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class ShopRewardTypes {

    private static final Map<String, Function<CompoundTag, ShopReward>> LOADERS = new HashMap<>();

    static {
        register(ItemShopReward.TYPE, ItemShopReward::load);
        register(CommandShopReward.TYPE, CommandShopReward::load);
        register(ResearchUnlockShopReward.TYPE, ResearchUnlockShopReward::load);
        register(ThreadShopReward.TYPE, ThreadShopReward::load);
    }

    public static void register(String typeId, Function<CompoundTag, ShopReward> loader) {
        LOADERS.put(typeId, loader);
    }

    public static ShopReward load(CompoundTag tag) {
        String type = tag.getString("type");
        Function<CompoundTag, ShopReward> loader = LOADERS.get(type);
        if (loader == null) throw new IllegalArgumentException("Unknown shop reward type: " + type);
        return loader.apply(tag.getCompound("data"));
    }

    public static CompoundTag save(ShopReward reward) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", reward.typeId());
        tag.put("data", reward.save());
        return tag;
    }

    public static java.util.Set<String> ids() {
        return LOADERS.keySet();
    }

    private ShopRewardTypes() {}
}
