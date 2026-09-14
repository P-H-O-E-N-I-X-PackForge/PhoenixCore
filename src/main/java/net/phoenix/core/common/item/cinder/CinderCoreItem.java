package net.phoenix.core.common.item.cinder;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CinderCoreItem extends Item {

    public static final int INVENTORY_SIZE = 64;
    private static final int MAX_MISSING_TOOLTIP_LINES = 5;

    public CinderCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> net.phoenix.core.client.gui.cinder.CinderConfiguratorScreen.open(hand));
        } else if (!player.isShiftKeyDown() && level.isClientSide) {

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> net.phoenix.core.client.cinder.CinderPreviewState.INSTANCE.cancel());
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (CinderSchemaData.getTargetId(stack) == null) return InteractionResult.PASS;
        return useOn(context);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        ItemStack stack = context.getItemInHand();
        if (player == null) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {

            CinderSchemaData.scanFromWorld(stack, level, context.getClickedPos());

            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> net.phoenix.core.client.gui.cinder.CinderConfiguratorScreen.open(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (CinderSchemaData.getTargetId(stack) == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal(
                                "This Cinder Core isn't configured yet - shift-right-click to pick a multiblock."),
                        true);
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                var state = net.phoenix.core.client.cinder.CinderPreviewState.INSTANCE;
                if (state.isActive() && state.getHand() == hand) {
                    state.commit();
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack cinderCore, @NotNull ItemStack heldItem,
                                            @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player,
                                            @NotNull net.minecraft.world.entity.SlotAccess slotAccess) {
        if (action != ClickAction.PRIMARY) return false;
        return tryStockMaterial(cinderCore, heldItem, player);
    }

    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack cinderCore, @NotNull Slot slot,
                                          @NotNull ClickAction action, @NotNull Player player) {
        if (action != ClickAction.PRIMARY) return false;
        return tryStockMaterial(cinderCore, slot.getItem(), player);
    }

    private static boolean tryStockMaterial(ItemStack cinderCore, ItemStack heldItem, Player player) {
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof BlockItem)) return false;

        return cinderCore.getCapability(ForgeCapabilities.ITEM_HANDLER).map(handler -> {
            ItemStack remainder = ItemHandlerHelper.insertItem(handler, heldItem.copy(), false);
            int inserted = heldItem.getCount() - remainder.getCount();
            if (inserted <= 0) return false;

            heldItem.shrink(inserted);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.4F,
                    1.0F + (player.level().random.nextFloat() - player.level().random.nextFloat()) * 0.4F);
            return true;
        }).orElse(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        var definition = CinderSchemaData.getTargetDefinition(stack);
        if (definition != null) {
            tooltip.add(Component.translatable("tooltip.phoenixcore.cinder_core.target",
                    definition.getBlock().getName()).withStyle(net.minecraft.ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.phoenixcore.cinder_core.unconfigured")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        var stocked = CinderSchemaData.tallyStocked(stack);
        int totalItems = stocked.values().intStream().sum();
        tooltip.add(Component.translatable("tooltip.phoenixcore.cinder_core.materials", stocked.size(), totalItems)
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        if (definition != null) {
            appendMissingMaterials(stack, stocked, tooltip);
        }

        tooltip.add(Component.translatable("tooltip.phoenixcore.cinder_core.hint")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
    }

    private static void appendMissingMaterials(ItemStack stack, Reference2IntMap<Block> stocked,
                                               List<Component> tooltip) {
        var required = CinderSchemaData.getCachedRequiredBlocks(stack);
        if (required.isEmpty()) return;

        List<Block> missing = new ArrayList<>();
        for (var entry : required.reference2IntEntrySet()) {
            if (stocked.getOrDefault(entry.getKey(), 0) < entry.getIntValue()) missing.add(entry.getKey());
        }

        if (missing.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.phoenixcore.cinder_core.ready")
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
            return;
        }

        missing.sort(Comparator.comparing(block -> block.getName().getString()));
        int shown = 0;
        for (Block block : missing) {
            if (shown >= MAX_MISSING_TOOLTIP_LINES) {
                tooltip.add(Component.translatable("tooltip.phoenixcore.cinder_core.missing_more",
                        missing.size() - shown).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                break;
            }
            tooltip.add(Component.translatable("tooltip.phoenixcore.cinder_core.missing_entry", block.getName(),
                    stocked.getOrDefault(block, 0), required.getInt(block))
                    .withStyle(net.minecraft.ChatFormatting.RED));
            shown++;
        }
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {

            private final LazyOptional<ItemStackHandler> holder = LazyOptional
                    .of(() -> new StockedMaterialsHandler(stack));

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == ForgeCapabilities.ITEM_HANDLER) {
                    return holder.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    private static class StockedMaterialsHandler extends ItemStackHandler {

        private static final String TAG_KEY = "StockedMaterials";

        private final ItemStack owner;

        private StockedMaterialsHandler(ItemStack owner) {
            super(INVENTORY_SIZE);
            this.owner = owner;
            CompoundTag saved = owner.getTagElement(TAG_KEY);
            if (saved != null) deserializeNBT(saved);
        }

        @Override
        protected void onContentsChanged(int slot) {
            owner.getOrCreateTag().put(TAG_KEY, serializeNBT());
        }
    }
}
