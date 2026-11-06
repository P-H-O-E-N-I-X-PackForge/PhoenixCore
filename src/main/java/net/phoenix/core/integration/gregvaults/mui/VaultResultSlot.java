package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.widgets.slot.ModularSlot;

public final class VaultResultSlot extends ModularSlot {

    private final ResultSlot inner;

    public VaultResultSlot(VaultSession session) {
        super(session.resultHandler(), 0);
        accessibility(false, true);
        if (session.server && session.grid != null) {
            this.inner = new ResultSlot(session.player, session.grid, session.result, 0, 0, 0) {

                @Override
                public void onTake(Player player, ItemStack stack) {
                    session.snapshotGridIngredients();
                    super.onTake(player, stack);
                    session.afterCraftTaken(player);
                }
            };
        } else {
            this.inner = null;
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return inner != null ? inner.mayPickup(player) : super.mayPickup(player);
    }

    @Override
    public ItemStack remove(int amount) {
        return inner != null ? inner.remove(amount) : super.remove(amount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        if (inner != null) inner.onTake(player, stack);
        else super.onTake(player, stack);
    }
}
