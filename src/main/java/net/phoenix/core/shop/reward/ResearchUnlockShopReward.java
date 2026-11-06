package net.phoenix.core.shop.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.shop.RewardSpec;

public class ResearchUnlockShopReward implements ShopReward {

    public static final String TYPE = "research_flag";

    private final String flag;

    public ResearchUnlockShopReward(String flag) {
        this.flag = flag;
    }

    @Override
    public void grant(ServerPlayer player) {
        WorldResearchData.get(player.serverLevel()).grantFlag(ResearchTeamHelper.getTeamId(player), flag);
    }

    @Override
    public Component describe() {
        return Component.literal("Unlocks research flag: " + flag).withStyle(ChatFormatting.AQUA);
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("flag", flag);
        return tag;
    }

    public static ResearchUnlockShopReward load(CompoundTag tag) {
        return new ResearchUnlockShopReward(tag.getString("flag"));
    }

    @Override
    public RewardSpec toSpec() {
        return new RewardSpec(TYPE, flag);
    }
}
