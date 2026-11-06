package net.phoenix.core.integration.gregvaults.common.items;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;
import net.phoenix.core.integration.gregvaults.mui.VaultAccess;
import net.phoenix.core.integration.gregvaults.mui.VaultMachineAccess;
import net.phoenix.core.integration.gregvaults.mui.VaultPanel;
import net.phoenix.core.integration.gregvaults.mui.VaultSession;
import net.phoenix.core.util.StackNbt;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.factory.PlayerInventoryUIFactory;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WirelessTerminalItem extends Item implements IUIHolder<PlayerInventoryGuiData<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(WirelessTerminalItem.class);
    private static final String TAG_LINKED_POS = "linkedVault";
    private static final String TAG_EMITTER_TIER = "emitterTier";

    public static final String KEY_VAULT_NOT_FORMED = "message.gregtechvaults.vault_not_formed";
    public static final String KEY_VAULT_LINKED = "message.gregtechvaults.vault_linked";
    public static final String KEY_NOT_LINKED = "message.gregtechvaults.terminal_not_linked";
    public static final String KEY_DIMENSION_NOT_FOUND = "message.gregtechvaults.dimension_not_found";
    public static final String KEY_DIFFERENT_DIMENSION = "message.gregtechvaults.different_dimension";
    public static final String KEY_VAULT_NOT_FOUND = "message.gregtechvaults.vault_not_found";
    public static final String KEY_OUT_OF_RANGE = "message.gregtechvaults.out_of_range";
    public static final String KEY_VAULT_TERMINAL_TITLE = "gui.gregtechvaults.vault_terminal";
    public static final String KEY_TOOLTIP_LINKED = "tooltip.gregtechvaults.linked";
    public static final String KEY_TOOLTIP_NOT_LINKED = "tooltip.gregtechvaults.not_linked";
    public static final String KEY_TOOLTIP_HOW_TO_LINK = "tooltip.gregtechvaults.how_to_link";
    public static final String KEY_TOOLTIP_RANGE = "tooltip.gregtechvaults.range";
    public static final String KEY_TOOLTIP_EMITTER = "tooltip.gregtechvaults.emitter";
    public static final String KEY_WIRELESS_DISABLED = "message.gregtechvaults.wireless_disabled";

    private static final Component WIRELESS_DISABLED_MESSAGE = Component.translatable(KEY_WIRELESS_DISABLED)
            .withStyle(ChatFormatting.RED);

    public enum EmitterTier {

        NONE(0),
        LV(1),
        MV(2),
        HV(3),
        EV(4),
        IV(5),
        LUV(6),
        ZPM(7),
        UV(8);

        public final int level;

        EmitterTier(int level) {
            this.level = level;
        }

        public static EmitterTier fromLevel(int level) {
            for (EmitterTier tier : values()) {
                if (tier.level == level) return tier;
            }
            return NONE;
        }

        public double getMultiplier() {
            PhoenixConfigs.WirelessTerminal cfg = PhoenixConfigs.INSTANCE.wirelessTerminal;
            return switch (this) {
                case NONE -> 1.0;
                case LV -> cfg.lvEmitterBonus;
                case MV -> cfg.mvEmitterBonus;
                case HV -> cfg.hvEmitterBonus;
                case EV -> cfg.evEmitterBonus;
                case IV -> cfg.ivEmitterBonus;
                case LUV -> cfg.luvEmitterBonus;
                case ZPM -> cfg.zpmEmitterBonus;
                case UV -> cfg.uvEmitterBonus;
            };
        }

        public String displayName() {
            return switch (this) {
                case NONE -> "";
                case LV -> "LV";
                case MV -> "MV";
                case HV -> "HV";
                case EV -> "EV";
                case IV -> "IV";
                case LUV -> "LuV";
                case ZPM -> "ZPM";
                case UV -> "UV";
            };
        }
    }

    public static EmitterTier getEmitterTier(ItemStack stack) {
        CompoundTag tag = StackNbt.getTag(stack);
        if (tag == null || !tag.contains(TAG_EMITTER_TIER)) return EmitterTier.NONE;
        return EmitterTier.fromLevel(tag.getInt(TAG_EMITTER_TIER));
    }

    public static void setEmitterTier(ItemStack stack, EmitterTier tier) {
        StackNbt.getOrCreateTag(stack).putInt(TAG_EMITTER_TIER, tier.level);
    }

    public static double getRange(ItemStack stack) {
        PhoenixConfigs.WirelessTerminal cfg = PhoenixConfigs.INSTANCE.wirelessTerminal;
        if (cfg.infiniteRange) return Double.MAX_VALUE;

        double baseRange = Math.max(0.0, cfg.connectionDistance);
        double multiplier = Math.max(0.0, getEmitterTier(stack).getMultiplier());
        return baseRange * multiplier;
    }

    @Nullable
    public static GlobalPos getLinkedPosition(ItemStack stack) {
        CompoundTag tag = StackNbt.getTag(stack);
        if (tag != null && tag.contains(TAG_LINKED_POS, Tag.TAG_COMPOUND)) {
            return GlobalPos.CODEC
                    .decode(NbtOps.INSTANCE, tag.get(TAG_LINKED_POS))
                    .resultOrPartial(Util.prefix("Vault link", LOG::error))
                    .map(Pair::getFirst)
                    .orElse(null);
        }
        return null;
    }

    public static void link(ItemStack stack, GlobalPos pos) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                .result()
                .ifPresent(tag -> StackNbt.getOrCreateTag(stack).put(TAG_LINKED_POS, tag));
    }

    public static void unlink(ItemStack stack) {
        StackNbt.removeTagKey(stack, TAG_LINKED_POS);
    }

    public static boolean isLinked(ItemStack stack) {
        return getLinkedPosition(stack) != null;
    }

    public static final IVaultLinkableHandler LINKABLE_HANDLER = new IVaultLinkableHandler() {

        @Override
        public boolean canLink(ItemStack stack) {
            return stack.getItem() instanceof WirelessTerminalItem;
        }

        @Override
        public void link(ItemStack stack, GlobalPos pos) {
            WirelessTerminalItem.link(stack, pos);
        }

        @Override
        public void unlink(ItemStack stack) {
            WirelessTerminalItem.unlink(stack);
        }
    };

    public WirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (!(level.getBlockEntity(pos) instanceof MetaMachine mbe) ||
                !(mbe instanceof VaultMachine vault)) {
            return InteractionResult.PASS;
        }

        if (!vault.isFormed()) {
            serverPlayer.sendSystemMessage(
                    Component.translatable(KEY_VAULT_NOT_FORMED).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (!vault.getVaultTier().wirelessAllowed()) {
            serverPlayer.sendSystemMessage(WIRELESS_DISABLED_MESSAGE);
            return InteractionResult.FAIL;
        }

        ItemStack stack = ctx.getItemInHand();
        link(stack, GlobalPos.of(level.dimension(), pos));
        serverPlayer.sendSystemMessage(
                Component.translatable(KEY_VAULT_LINKED).withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);

        if (resolveVault(serverPlayer, stack, true) == null) return InteractionResultHolder.fail(stack);
        PlayerInventoryUIFactory.INSTANCE.openFromHand(serverPlayer, hand);
        return InteractionResultHolder.consume(stack);
    }

    public void openFromInventory(ServerPlayer player, int inventorySlot) {
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (resolveVault(player, stack, true) == null) return;
        PlayerInventoryUIFactory.INSTANCE.openFromPlayerInventory(player, inventorySlot);
    }

    @Nullable
    private VaultMachine resolveVault(ServerPlayer player, ItemStack stack, boolean report) {
        GlobalPos linkedPos = getLinkedPosition(stack);
        if (linkedPos == null) {
            tell(player, report, Component.translatable(KEY_NOT_LINKED).withStyle(ChatFormatting.RED));
            return null;
        }

        ServerLevel targetLevel = player.getServer().getLevel(linkedPos.dimension());
        if (targetLevel == null) {
            tell(player, report, Component.translatable(KEY_DIMENSION_NOT_FOUND).withStyle(ChatFormatting.RED));
            return null;
        }

        PhoenixConfigs.WirelessTerminal cfg = PhoenixConfigs.INSTANCE.wirelessTerminal;

        if (!cfg.infiniteRange && targetLevel != player.level()) {
            tell(player, report, Component.translatable(KEY_DIFFERENT_DIMENSION).withStyle(ChatFormatting.RED));
            return null;
        }

        BlockPos vaultPos = linkedPos.pos();
        if (!cfg.infiniteRange) {
            double range = getRange(stack);
            double distance = Math.sqrt(player.blockPosition().distSqr(vaultPos));
            if (distance > range) {
                tell(player, report, Component.translatable(KEY_OUT_OF_RANGE, (int) distance, (int) range)
                        .withStyle(ChatFormatting.RED));
                return null;
            }
        }

        if (!(targetLevel.getBlockEntity(vaultPos) instanceof MetaMachine mbe) ||
                !(mbe instanceof VaultMachine vault)) {
            tell(player, report, Component.translatable(KEY_VAULT_NOT_FOUND).withStyle(ChatFormatting.RED));
            if (report) unlink(stack);
            return null;
        }

        if (!vault.isFormed()) {
            tell(player, report, Component.translatable(KEY_VAULT_NOT_FORMED).withStyle(ChatFormatting.RED));
            return null;
        }

        if (!vault.getVaultTier().wirelessAllowed()) {
            tell(player, report, WIRELESS_DISABLED_MESSAGE);
            return null;
        }
        return vault;
    }

    private static void tell(ServerPlayer player, boolean report, Component message) {
        if (report) player.sendSystemMessage(message);
    }

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> data, PanelSyncManager syncManager, UISettings settings) {
        Player player = data.getPlayer();
        ItemStack stack = data.getUsedItemStack();

        VaultAccess access = VaultAccess.CLIENT;
        if (player instanceof ServerPlayer serverPlayer) {
            VaultMachine vault = resolveVault(serverPlayer, stack, false);
            if (vault != null) access = new VaultMachineAccess(vault, true);
        }

        int locked = -1;
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == stack) locked = i;
        }
        return VaultPanel.build(new VaultSession(player, access), syncManager, settings, locked);
    }

    @Override
    public ModularScreen createScreen(PlayerInventoryGuiData<?> data, ModularPanel<?> mainPanel) {
        return new com.gregtechceu.gtceu.api.mui.GTGuiScreen(mainPanel);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        Level level = context.level();
        EmitterTier tier = getEmitterTier(stack);
        PhoenixConfigs.WirelessTerminal cfg = PhoenixConfigs.INSTANCE.wirelessTerminal;

        GlobalPos pos = getLinkedPosition(stack);
        if (pos != null) {
            tooltip.add(Component.translatable(KEY_TOOLTIP_LINKED).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal(
                    "  " + pos.pos().getX() + " " + pos.pos().getY() + " " + pos.pos().getZ())
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable(KEY_TOOLTIP_NOT_LINKED).withStyle(ChatFormatting.GRAY));
        }

        if (tier != EmitterTier.NONE) {
            tooltip.add(Component.translatable(KEY_TOOLTIP_EMITTER, tier.displayName())
                    .withStyle(ChatFormatting.AQUA));
        }

        if (cfg.infiniteRange) {
            tooltip.add(Component.translatable(KEY_TOOLTIP_RANGE, "∞")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            int range = (int) getRange(stack);
            tooltip.add(Component.translatable(KEY_TOOLTIP_RANGE, range)
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable(KEY_TOOLTIP_HOW_TO_LINK).withStyle(ChatFormatting.DARK_GRAY));
    }
}
