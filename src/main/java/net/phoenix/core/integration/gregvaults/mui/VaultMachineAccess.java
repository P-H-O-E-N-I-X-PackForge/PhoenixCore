package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;

import org.jetbrains.annotations.Nullable;

public final class VaultMachineAccess implements VaultAccess {

    private final VaultMachine machine;
    private final boolean remote;

    public VaultMachineAccess(VaultMachine machine, boolean remote) {
        this.machine = machine;
        this.remote = remote;
    }

    public VaultMachine machine() {
        return machine;
    }

    @Override
    public @Nullable ItemStackHandler storage() {
        return machine.getItemHandler();
    }

    @Override
    public int contentVersion() {
        return machine.contentVersion();
    }

    @Override
    public boolean stillValid(Player player) {
        if (!machine.isFormed()) return false;
        return remote || player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64.0;
    }

    @Override
    public ItemStack[] savedGrid() {
        return machine.getSavedCraftingGrid();
    }

    @Override
    public void saveGrid(ItemStack[] grid) {
        machine.setSavedCraftingGrid(grid);
    }

    @Override
    public void opened(ServerPlayer player) {
        machine.addViewer(player);
    }

    @Override
    public void closed(ServerPlayer player) {
        machine.removeViewer(player);
    }
}
