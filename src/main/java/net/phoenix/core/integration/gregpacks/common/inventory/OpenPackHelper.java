package net.phoenix.core.integration.gregpacks.common.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import net.phoenix.core.integration.gregpacks.common.upgrade.UpgradeEffects;
import org.jetbrains.annotations.NotNull;

public class OpenPackHelper {

    public static void open(ServerPlayer player, ItemStack stack, OmniPackTier tier, int slotIndex) {
        OmniPackInventory upgradeInv = OmniPackInventory.fromUpgradeItem(stack, tier.defaultMaxUpgrades);
        UpgradeEffects effects = new UpgradeEffects(tier, upgradeInv);

        OmniPackInventory inv = OmniPackInventory.fromItem(stack, effects.totalSlots);

        NetworkHooks.openScreen(player, new MenuProvider() {

            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("container.gregpacks.omnipack");
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInv, @NotNull Player p) {
                OmniPackMenu menu = new OmniPackMenu(windowId, playerInv, inv, upgradeInv, tier);
                menu.setPackSlotIndex(slotIndex);
                menu.setSourceStack(stack);
                menu.setServerPlayer((ServerPlayer) p);
                return menu;
            }
        }, buf -> {
            buf.writeShort(effects.totalSlots);          
            buf.writeByte(tier.defaultMaxUpgrades);      
            buf.writeByte(tier.ordinal());               
        });
    }

    public static int findSlot(Player player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == stack) return i;
        }
        return -1;
    }
}
