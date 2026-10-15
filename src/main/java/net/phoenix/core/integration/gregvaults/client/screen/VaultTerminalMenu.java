package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

@SuppressWarnings("all")
public class VaultTerminalMenu extends AbstractVaultMenu {

    private int lockedMenuSlot = -1;

    public VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler) {
        this(windowId, playerInv, vaultHandler, null);
    }

    public VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler, VaultMachine machine) {
        super(VaultRegistry.VAULT_TERMINAL_MENU.get(), windowId, playerInv, vaultHandler, machine);
    }

    public VaultTerminalMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, new ItemStackHandler(buf.readInt()));
    }

    @Override
    protected void addPlayerSlots(Inventory playerInv) {
        ItemStack terminalStack = findTerminalStack(playerInv);
        int terminalInvSlot = findTerminalInvSlot(playerInv, terminalStack);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int invSlot = col + row * 9 + 9;
                int menuSlot = slots.size();
                addSlot(invSlot == terminalInvSlot ?
                        new LockedSlot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, playerY + row * SLOT_SIZE) :
                        new Slot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, playerY + row * SLOT_SIZE));
                if (invSlot == terminalInvSlot) lockedMenuSlot = menuSlot;
            }
        }
        for (int col = 0; col < 9; col++) {
            int invSlot = col;
            int menuSlot = slots.size();
            addSlot(invSlot == terminalInvSlot ?
                    new LockedSlot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, hotbarY) :
                    new Slot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, hotbarY));
            if (invSlot == terminalInvSlot) lockedMenuSlot = menuSlot;
        }
    }

    private static ItemStack findTerminalStack(Inventory playerInv) {
        ItemStack main = playerInv.player.getMainHandItem();
        if (main.getItem() instanceof WirelessTerminalItem) return main;
        ItemStack off = playerInv.player.getOffhandItem();
        if (off.getItem() instanceof WirelessTerminalItem) return off;
        return ItemStack.EMPTY;
    }

    private static int findTerminalInvSlot(Inventory playerInv, ItemStack terminalStack) {
        if (terminalStack.isEmpty()) return -1;
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            if (playerInv.getItem(i) == terminalStack) return i;
        }
        return -1;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null || machine.isFormed();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot.index == lockedMenuSlot) return false;
        return super.canTakeItemForPickAll(stack, slot);
    }

    private static final class LockedSlot extends Slot {

        LockedSlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
