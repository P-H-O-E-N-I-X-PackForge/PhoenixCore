package net.phoenix.core.integration.jade.provider;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@SuppressWarnings("all")
public class VaultProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("vault_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof VaultMachine)) {
            return;
        }

        CompoundTag data = accessor.getServerData();
        if (!data.contains("formed") || !data.getBoolean("formed")) return;

        int totalSlots = data.getInt("totalSlots");
        int availableSlots = data.getInt("availableSlots");

        tooltip.add(Component.translatable("tooltip.gregtechvaults.jade.total_slots", totalSlots)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gregtechvaults.jade.available_slots", availableSlots)
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof VaultMachine machine)) {
            return;
        }

        tag.putBoolean("formed", machine.isFormed());

        if (machine.isFormed()) {
            tag.putInt("totalSlots", machine.getTotalSlots());
            tag.putInt("availableSlots", machine.getAvailableSlots());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
