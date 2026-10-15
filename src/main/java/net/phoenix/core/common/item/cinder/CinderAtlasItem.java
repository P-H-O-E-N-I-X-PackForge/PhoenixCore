package net.phoenix.core.common.item.cinder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.phoenix.core.integration.ae2.CinderAtlasMenuHost;
import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.items.CraftingTermMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CinderAtlasItem extends Item implements IMenuItem {

    public CinderAtlasItem(Properties properties) {
        super(properties);
    }

    public static boolean openCraftingTerminal(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CinderAtlasItem)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;

        var accessPoint = CinderAtlasWirelessLink.getLinkedAccessPoint(stack, level);
        if (accessPoint == null || !CinderAtlasWirelessLink.isInRange(stack, accessPoint, player)) return false;

        return MenuOpener.open(CraftingTermMenu.TYPE, player, MenuLocators.forHand(player, hand));
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int slot, ItemStack stack, @Nullable BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level)) return null;

        var accessPoint = CinderAtlasWirelessLink.getLinkedAccessPoint(stack, level);
        if (accessPoint == null || !CinderAtlasWirelessLink.isInRange(stack, accessPoint, player)) return null;

        return new CinderAtlasMenuHost(player, slot, stack, accessPoint);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> net.phoenix.core.client.gui.cinder.CinderAtlasScreen.open(hand));
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
        if (!CinderDeploySource.hasConfiguredTarget(stack)) return InteractionResult.PASS;
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
            return scanIntoActiveSlot(context, level, player, hand, stack);
        }

        if (!CinderDeploySource.hasConfiguredTarget(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("This loadout slot isn't configured - shift-right-click to pick a " +
                                "design."),
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

    private InteractionResult scanIntoActiveSlot(UseOnContext context, Level level, Player player,
                                                 InteractionHand hand, ItemStack stack) {
        CompoundTag slotTag = CinderAtlasData.getOrCreateSlotTag(stack, CinderAtlasData.getActiveLoadout(stack),
                CinderAtlasData.getActiveSlot(stack));
        boolean scanned = CinderSchemaData.scanFromWorld(slotTag, level, context.getClickedPos());

        if (!scanned && !level.isClientSide) {
            player.displayClientMessage(
                    Component.literal("Nothing to scan there - aim at a formed multiblock."), true);
        }

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> net.phoenix.core.client.gui.cinder.CinderAtlasScreen.open(hand));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        int active = CinderAtlasData.getActiveLoadout(stack);
        int capacity = CinderAtlasUpgrades.getLoadoutCapacity(stack);

        for (int loadout = 0; loadout < capacity; loadout++) {
            int configured = 0;
            for (int slot = 0; slot < CinderAtlasData.SLOT_COUNT; slot++) {
                if (CinderAtlasData.isSlotConfigured(stack, loadout, slot)) configured++;
            }

            String name = CinderAtlasData.getLoadoutName(stack, loadout);
            ChatFormatting color = loadout == active ? ChatFormatting.GOLD : ChatFormatting.GRAY;
            tooltip.add(Component.literal((loadout == active ? "> " : "  ") + name + " (" + configured + "/" +
                    CinderAtlasData.SLOT_COUNT + ")").withStyle(color));
        }

        double range = CinderAtlasUpgrades.getWirelessRange(stack);
        String rangeText = Double.isInfinite(range) ? "Unlimited" : (int) range + " blocks";
        tooltip.add(Component.literal("Wireless range: " + rangeText).withStyle(ChatFormatting.DARK_AQUA));

        int installed = 0;
        for (int i = 0; i < CinderAtlasData.UPGRADE_SLOT_COUNT; i++) {
            if (!CinderAtlasData.getUpgradeSlot(stack, i).isEmpty()) installed++;
        }
        tooltip.add(Component.literal("Upgrades: " + installed + "/" + CinderAtlasData.UPGRADE_SLOT_COUNT)
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
