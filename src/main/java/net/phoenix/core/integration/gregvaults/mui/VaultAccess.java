package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

public interface VaultAccess {

    @Nullable
    ItemStackHandler storage();

    int contentVersion();

    boolean stillValid(Player player);

    ItemStack[] savedGrid();

    void saveGrid(ItemStack[] grid);

    default void opened(ServerPlayer player) {}

    default void closed(ServerPlayer player) {}

    VaultAccess CLIENT = new VaultAccess() {

        @Override
        public @Nullable ItemStackHandler storage() {
            return null;
        }

        @Override
        public int contentVersion() {
            return 0;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public ItemStack[] savedGrid() {
            return new ItemStack[9];
        }

        @Override
        public void saveGrid(ItemStack[] grid) {}
    };
}
