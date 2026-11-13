package net.phoenix.core.common.machine.multiblock;

import com.gregtechceu.gtceu.api.item.tool.GTToolItem;

import net.minecraft.world.item.ItemStack;
import net.phoenix.core.rebirth.RebirthBinderItem;

public final class ScreenUse {

    private ScreenUse() {}

    public static boolean opens(ItemStack held) {
        if (held.isEmpty()) return true;
        return !(held.getItem() instanceof GTToolItem) && !(held.getItem() instanceof RebirthBinderItem);
    }
}
