package net.phoenix.core.integration.gregvaults.common.blocks;


import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.phoenix.core.configs.PhoenixConfigs;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VaultCoreBlock extends Block {

    private final CoreTier tier;

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int slots = PhoenixConfigs.getSlotValue(this.tier);
        tooltip.add(Component.translatable(
                "tooltip.gregtechvaults.vault_core_" + tier.name().toLowerCase(),
                slots));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public VaultCoreBlock(CoreTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CoreTier getTier() {
        return tier;
    }
}
