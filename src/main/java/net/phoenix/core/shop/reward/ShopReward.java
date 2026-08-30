package net.phoenix.core.shop.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.shop.RewardSpec;

public interface ShopReward {

    void grant(ServerPlayer player);

    Component describe();

    String typeId();

    CompoundTag save();

    RewardSpec toSpec();

    static ShopReward load(CompoundTag tag) {
        return ShopRewardTypes.load(tag);
    }
}
