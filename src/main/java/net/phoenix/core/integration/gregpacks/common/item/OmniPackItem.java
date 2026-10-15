package net.phoenix.core.integration.gregpacks.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.gregpacks.common.OmniPackTickHandler;
import net.phoenix.core.integration.gregpacks.common.inventory.OpenPackHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class OmniPackItem extends Item implements ICurioItem {

    private final OmniPackTier tier;
    private ItemStack sourceStack = null;

    public OmniPackItem(OmniPackTier tier) {
        super(new Properties().stacksTo(1));
        this.tier = tier;
    }

    public OmniPackTier getTier() {
        return tier;
    }

    public ItemStack getSourceStack() {
        return sourceStack;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("back");
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
                                                           @NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            int slotIndex = OpenPackHelper.findSlot(player, stack);
            OpenPackHelper.open(serverPlayer, stack, tier, slotIndex);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.gregpacks.omnipack.tooltip.slots", tier.defaultSlots));
        tooltipComponents
                .add(Component.translatable("item.gregpacks.omnipack.tooltip.fluid", tier.defaultFluidStorage));
        tooltipComponents
                .add(Component.translatable("item.gregpacks.omnipack.tooltip.energy", tier.defaultEnergyStorage));
        tooltipComponents
                .add(Component.translatable("item.gregpacks.omnipack.tooltip.upgrades", tier.defaultMaxUpgrades));
        long storedEU = OmniPackTickHandler.getStoredEU(stack);
        if (storedEU > 0) {
            tooltipComponents.add(Component.literal("§aEU: " + storedEU + " / " + tier.defaultEnergyStorage));
        }
        tooltipComponents.add(Component.literal("§7Shift-click to equip as chestplate"));
    }
}
