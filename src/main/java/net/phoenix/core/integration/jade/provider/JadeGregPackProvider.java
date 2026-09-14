package net.phoenix.core.integration.jade.provider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.block.OmniPackBlockEntity;
import net.phoenix.core.integration.gregpacks.common.upgrade.UpgradeEffects;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

public class JadeGregPackProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private static final ResourceLocation UID = new ResourceLocation(PhoenixCore.MOD_ID, "omnipack_provider");
    private static final String KEY_ENERGY = "GP_Energy";
    private static final String KEY_FLUIDS = "GP_Fluids";
    private static final String KEY_ITEMS = "GP_Items";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof OmniPackBlockEntity entity)) return;

        long stored = entity.getStoredEU();
        long capacity = new UpgradeEffects(entity.getTier(), entity.getUpgradeInventory()).totalEnergyStorage;
        CompoundTag energyTag = new CompoundTag();
        energyTag.putLong("stored", stored);
        energyTag.putLong("capacity", capacity);
        data.put(KEY_ENERGY, energyTag);

        entity.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
            if (handler.getTanks() > 0) {
                FluidStack fluid = handler.getFluidInTank(0);
                if (!fluid.isEmpty()) {
                    data.put(KEY_FLUIDS, fluid.writeToNBT(new CompoundTag()));
                }
            }
        });

        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            CompoundTag itemsTag = new CompoundTag();
            int count = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    itemsTag.put(String.valueOf(i), stack.save(new CompoundTag()));
                    count++;
                }
            }
            itemsTag.putInt("count", count);
            itemsTag.putInt("slots", handler.getSlots());
            data.put(KEY_ITEMS, itemsTag);
        });
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        IElementHelper helper = tooltip.getElementHelper();

        if (serverData.contains(KEY_ENERGY)) {
            CompoundTag tag = serverData.getCompound(KEY_ENERGY);
            long stored = tag.getLong("stored");
            long capacity = tag.getLong("capacity");
            if (capacity > 0) {
                float ratio = (float) stored / capacity;
                String label = String.format("%,d / %,d EU", stored, capacity);
                tooltip.add(helper.progress(
                        ratio,
                        Component.literal(label),
                        helper.progressStyle().color(0xFFEEE600, 0xFFEEE600).textColor(-1),
                        net.minecraft.Util.make(snownee.jade.api.ui.BoxStyle.DEFAULT,
                                style -> style.borderColor = 0xFF555555),
                        true));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
