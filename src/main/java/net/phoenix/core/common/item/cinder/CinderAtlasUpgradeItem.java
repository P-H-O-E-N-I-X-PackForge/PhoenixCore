package net.phoenix.core.common.item.cinder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CinderAtlasUpgradeItem extends Item {

    private final CinderAtlasUpgradeType upgradeType;

    public CinderAtlasUpgradeItem(Properties properties, CinderAtlasUpgradeType upgradeType) {
        super(properties);
        this.upgradeType = upgradeType;
    }

    public CinderAtlasUpgradeType getUpgradeType() {
        return upgradeType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        Level level = context.level();
        tooltip.add(Component.literal(switch (upgradeType) {
            case LOADOUT_EXPANSION -> "Adds +1 loadout slot to a Cinder Atlas (stacks with more copies).";
            case RANGE_EXTENDER_I -> "Extends a Cinder Atlas's wireless range to " +
                    (int) CinderAtlasUpgrades.RANGE_TIER_1 + " blocks.";
            case RANGE_EXTENDER_II -> "Extends a Cinder Atlas's wireless range to " +
                    (int) CinderAtlasUpgrades.RANGE_TIER_2 + " blocks.";
            case RANGE_EXTENDER_III -> "Extends a Cinder Atlas's wireless range to unlimited.";
        }).withStyle(ChatFormatting.GRAY));
    }
}
