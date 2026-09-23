package net.phoenix.core.common.item.cinder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A single-purpose upgrade item for the Cinder Atlas - see {@link CinderAtlasUpgradeType} for the axis
 * it affects. Installed via {@link CinderAtlasScreen}'s Upgrades panel, not consumed by any recipe of
 * its own.
 */
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
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
