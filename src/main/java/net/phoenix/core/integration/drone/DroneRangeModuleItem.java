package net.phoenix.core.integration.drone;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DroneRangeModuleItem extends Item {

    private final int bonus;

    public DroneRangeModuleItem(Properties properties, int bonus) {
        super(properties);
        this.bonus = bonus;
    }

    public int bonus() {
        return bonus;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Range §a+" + bonus + " §7blocks for a Drone Controller"));
        tooltip.add(Component.literal("§8Right-click a Drone Controller with it. Up to " +
                DroneControllerMachine.MODULE_SLOTS + " modules."));
    }
}
