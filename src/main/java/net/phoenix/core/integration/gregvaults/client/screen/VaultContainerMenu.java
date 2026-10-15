package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

@SuppressWarnings("all")
public class VaultContainerMenu extends AbstractVaultMenu {

    public VaultContainerMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler) {
        this(VaultRegistry.VAULT_MENU.get(), windowId, playerInv, vaultHandler, null);
    }

    public VaultContainerMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler, VaultMachine machine) {
        this(VaultRegistry.VAULT_MENU.get(), windowId, playerInv, vaultHandler, machine);
    }

    protected VaultContainerMenu(MenuType<?> menuType, int windowId, Inventory playerInv,
                                 IItemHandler vaultHandler, VaultMachine machine) {
        super(menuType, windowId, playerInv, vaultHandler, machine);
    }

    public VaultContainerMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, new ItemStackHandler(buf.readInt()));
    }

    @Override
    protected void addPlayerSlots(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        SLOTS_X + col * SLOT_SIZE,
                        playerY + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col,
                    SLOTS_X + col * SLOT_SIZE,
                    hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine == null) return true;
        return machine.isFormed() && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64.0;
    }
}
