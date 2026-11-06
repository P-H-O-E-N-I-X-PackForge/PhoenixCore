package net.phoenix.core.integration.gregvaults.common.items;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

public interface IVaultLinkableHandler {

    boolean canLink(ItemStack stack);

    void link(ItemStack stack, GlobalPos pos);

    void unlink(ItemStack stack);
}
