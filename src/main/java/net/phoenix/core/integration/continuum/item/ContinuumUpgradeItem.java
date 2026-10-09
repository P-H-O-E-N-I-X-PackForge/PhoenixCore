package net.phoenix.core.integration.continuum.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.continuum.common.RocketStats;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContinuumUpgradeItem extends Item {

    private final String upgrade;
    private final int maxLevel;
    private final String effect;

    public ContinuumUpgradeItem(Properties properties, String upgrade, int maxLevel, String effect) {
        super(properties.stacksTo(16));
        this.upgrade = upgrade;
        this.maxLevel = maxLevel;
        this.effect = effect;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack module = player.getItemInHand(hand);
        ItemStack rocket = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND :
                InteractionHand.MAIN_HAND);

        if (!(rocket.getItem() instanceof ContinuumRocketItem)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Hold a Continuum Rocket in your other hand to fit this.")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(module);
        }

        int current = RocketStats.level(rocket, upgrade);
        if (current >= maxLevel) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("That upgrade is already at its highest level.")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(module);
        }

        if (!level.isClientSide) {
            RocketStats.setLevel(rocket, upgrade, current + 1);
            if (!player.getAbilities().instabuild) module.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
            player.displayClientMessage(Component.literal("Rocket upgraded to level " + (current + 1) + ".")
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResultHolder.sidedSuccess(module, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(effect).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Up to level " + maxLevel).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Hold a rocket in your other hand and use.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
