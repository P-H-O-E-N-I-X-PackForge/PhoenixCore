package net.phoenix.core.integration.gregvaults.common.blocks;

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
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        int slots = PhoenixConfigs.getSlotValue(this.tier);
        tooltip.add(Component.translatable(
                "tooltip.gregtechvaults.vault_core_" + tier.name().toLowerCase(),
                slots));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public VaultCoreBlock(CoreTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CoreTier getTier() {
        return tier;
    }
}
