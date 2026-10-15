package net.phoenix.core.integration.gregpacks.common.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlock;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlockEntity;
import net.phoenix.core.integration.gregpacks.common.energy.OmniPackEnergyHandler;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackItem;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import net.phoenix.core.integration.gregpacks.common.registry.OmniPackBlockItem;

public class GregPacksCapabilityHandler {

    public static void register(RegisterCapabilitiesEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            OmniPackTier tier = getTier(item);
            if (tier == null) continue;
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new OmniPackFluidHandler(stack, tier),
                    item);
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new OmniPackEnergyHandler(stack, tier),
                    item);
        }
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof OmniPackBlock)) continue;
            event.registerBlock(Capabilities.ItemHandler.BLOCK,
                    (level, pos, state, be, side) -> be instanceof OmniPackBlockEntity pack ?
                            pack.getItemHandler() : null,
                    block);
            event.registerBlock(Capabilities.FluidHandler.BLOCK,
                    (level, pos, state, be, side) -> be instanceof OmniPackBlockEntity pack ?
                            pack.getFluidHandler() : null,
                    block);
            event.registerBlock(Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, be, side) -> be instanceof OmniPackBlockEntity pack ?
                            pack.getEnergyHandler() : null,
                    block);
        }
    }

    private static OmniPackTier getTier(Item item) {
        if (item instanceof OmniPackItem pack) return pack.getTier();
        if (item instanceof OmniPackBlockItem pack) return pack.getBlock().getTier();
        return null;
    }
}
