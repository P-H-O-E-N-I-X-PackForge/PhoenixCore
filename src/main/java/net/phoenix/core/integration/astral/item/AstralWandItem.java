package net.phoenix.core.integration.astral.item;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;
import net.phoenix.core.integration.astral.puzzle.AstralSurveyItem;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlockEntity;
import net.phoenix.core.util.StackNbt;

import org.jetbrains.annotations.NotNull;

public class AstralWandItem extends Item {

    private static final String CHANNEL_TAG = "ChannelPedestal";

    public AstralWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                net.phoenix.core.integration.astral.client.AstralCodexScreen.open();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof AstralRitualPedestalBlockEntity pedestal) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (pedestal.canBegin(player)) {
                StackNbt.getOrCreateTag(context.getItemInHand()).putLong(CHANNEL_TAG, pos.asLong());
                player.startUsingItem(context.getHand());
            }
            return InteractionResult.CONSUME;
        }

        if (MetaMachine.getMachine(level, pos) instanceof AstralCalibratedMachine machine) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!machine.isFormed()) {
                player.displayClientMessage(Component.literal("§cBuild the machine first."), true);
                return InteractionResult.CONSUME;
            }
            for (ItemStack held : player.getInventory().items) {
                if (AstralSurveyItem.isSurvey(held) &&
                        AstralSurveyItem.isFor(held, level, machine.getBlockPos(), machine.getChartSeed())) {
                    player.displayClientMessage(Component.literal("§eYou already hold a survey of this machine."),
                            true);
                    return InteractionResult.CONSUME;
                }
            }
            ItemStack survey = AstralSurveyItem.create(machine, level);
            if (!player.getInventory().add(survey)) player.drop(survey, false);
            player.displayClientMessage(Component.literal(
                    "§bYou survey the machine. Channel the Ritual Pedestal with the survey in your inventory."), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull net.minecraft.world.entity.LivingEntity entity) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player) || !StackNbt.hasTag(stack) ||
                !StackNbt.getTag(stack).contains(CHANNEL_TAG)) {
            return;
        }
        BlockPos pos = BlockPos.of(StackNbt.getTag(stack).getLong(CHANNEL_TAG));
        int ticks = getUseDuration(stack, entity) - remaining;
        if (!(level.getBlockEntity(pos) instanceof AstralRitualPedestalBlockEntity pedestal) ||
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36) {
            StackNbt.getTag(stack).remove(CHANNEL_TAG);
            player.stopUsingItem();
            return;
        }
        if (ticks >= AstralRitualPedestalBlockEntity.CHANNEL_TICKS) {
            StackNbt.getTag(stack).remove(CHANNEL_TAG);
            pedestal.complete(player);
            player.stopUsingItem();
        } else {
            pedestal.channelTick(player, ticks);
        }
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity,
                             int timeLeft) {
        if (level.isClientSide || !StackNbt.hasTag(stack) || !StackNbt.getTag(stack).contains(CHANNEL_TAG)) return;
        StackNbt.getTag(stack).remove(CHANNEL_TAG);
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("The channel breaks."), true);
        }
    }
}
