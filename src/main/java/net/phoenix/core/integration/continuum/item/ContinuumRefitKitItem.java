package net.phoenix.core.integration.continuum.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.continuum.common.RocketStats;

import java.util.List;

public class ContinuumRefitKitItem extends Item {

    private final int tier;

    public ContinuumRefitKitItem(Properties properties, int tier) {
        super(properties.stacksTo(4));
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("Hold a rocket and sneak + use it to open the refit screen.")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        Level level = context.level();
        tooltip.add(Component.literal("Raises a rocket's hull from " + RocketStats.tierName(tier - 1) + " to " +
                RocketStats.tierName(tier) + ".").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("A better hull flies faster, wears less and fails less, carries more upgrades " +
                "and reaches higher-tier bodies.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + use a rocket to open the refit screen.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
