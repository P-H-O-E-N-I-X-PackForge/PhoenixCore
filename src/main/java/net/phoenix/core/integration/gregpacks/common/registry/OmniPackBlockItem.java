package net.phoenix.core.integration.gregpacks.common.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlock;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlockEntity;
import net.phoenix.core.integration.gregpacks.common.inventory.OpenPackHelper;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class OmniPackBlockItem extends BlockItem implements ICurioItem {

    public OmniPackBlockItem(OmniPackBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("back");
    }

    @Override
    public OmniPackBlock getBlock() {
        return (OmniPackBlock) super.getBlock();
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        if (!context.getPlayer().isShiftKeyDown()) return InteractionResult.PASS;
        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
                                                           @NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ItemStack stack = player.getItemInHand(hand);
                OmniPackTier tier = getBlock().getTier();
                int slotIndex = OpenPackHelper.findSlot(player, stack);
                OpenPackHelper.open(serverPlayer, stack, tier, slotIndex);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level,
                                                 @Nullable Player player, ItemStack stack, BlockState state) {
        boolean result = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OmniPackBlockEntity be) {
            be.loadFromItemStack(stack);
        }
        return result;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        var tier = getBlock().getTier();
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.slots", tier.defaultSlots));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.fluid", tier.defaultFluidStorage));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.energy", tier.defaultEnergyStorage));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.upgrades", tier.defaultMaxUpgrades));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.place.0"));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.place.1"));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.place.2"));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.place.3"));
        tooltip.add(Component.translatable("item.gregpacks.omnipack.tooltip.place.4"));
    }
}
