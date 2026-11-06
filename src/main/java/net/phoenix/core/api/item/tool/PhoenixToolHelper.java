package net.phoenix.core.api.item.tool;

import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.data.item.PhoenixItems;

import java.util.function.Supplier;

public class PhoenixToolHelper {

    public static final Supplier<ItemStack> SUPPLY_POWER_UNIT_LUV = () -> PhoenixItems.POWER_UNIT_LUV.get()
            .getDefaultInstance();
    public static final Supplier<ItemStack> SUPPLY_POWER_UNIT_ZPM = () -> PhoenixItems.POWER_UNIT_ZPM.get()
            .getDefaultInstance();
}
