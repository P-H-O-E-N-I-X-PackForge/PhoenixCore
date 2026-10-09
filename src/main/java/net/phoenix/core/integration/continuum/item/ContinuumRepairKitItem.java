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
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.common.RocketStats;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContinuumRepairKitItem extends Item {

    public ContinuumRepairKitItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        ItemStack rocket = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND :
                InteractionHand.MAIN_HAND);

        if (!(rocket.getItem() instanceof ContinuumRocketItem)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("Hold a Continuum Rocket in your other hand to repair it.")
                                .withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResultHolder.fail(kit);
        }

        float wear = RocketStats.wear(rocket);
        if (wear <= 0.0f) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("The rocket is in perfect condition.")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(kit);
        }

        if (!level.isClientSide) {
            float repaired = (float) PhoenixConfigs.INSTANCE.continuum.repairKitWear;
            RocketStats.setWear(rocket, wear - repaired);
            if (!player.getAbilities().instabuild) kit.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 1.8f);
            player.displayClientMessage(Component.literal("Rocket wear is now " +
                    Math.round(RocketStats.wear(rocket) * 100.0f) + "%.").withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResultHolder.sidedSuccess(kit, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Removes " + Math.round(PhoenixConfigs.INSTANCE.continuum.repairKitWear * 100) +
                "% wear from a rocket.").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Hold a rocket in your other hand and use.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
