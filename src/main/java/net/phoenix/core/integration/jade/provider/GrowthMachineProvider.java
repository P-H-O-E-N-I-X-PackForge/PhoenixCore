package net.phoenix.core.integration.jade.provider;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.growth.GrowthMultiblockMachine;
import net.phoenix.core.integration.growth.GrowthStage;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** What a growing machine is up to: its stage and what the stage gives, its progress, and why it may be waiting. */
public class GrowthMachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("growth_machine_info");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof GrowthMultiblockMachine machine)) return;
        GrowthStage stage = machine.currentStage();
        tag.putInt("GrowthStage", machine.getGrowthStage());
        tag.putInt("GrowthMax", machine.maxStage());
        tag.putFloat("GrowthProgress", machine.getGrowthProgress());
        tag.putFloat("GrowthSpeed", stage.speedBonus());
        tag.putFloat("GrowthOutput", stage.outputBonus());
        tag.putInt("GrowthBatch", stage.batch());
        tag.putInt("GrowthStall", machine.getGrowthStall());
        tag.putInt("Tendrils", machine.growingTendrils());
        tag.putInt("Matured", machine.getMaturedTendrils());
        tag.putBoolean("Controlled", machine.isControlled());
        tag.putBoolean("Paused", machine.isGrowthPaused());
        tag.putInt("TendrilBlocks", machine.getTendrilBlockCount());
        tag.putFloat("TendrilSpeed", machine.tendrilSpeedBonus());
        tag.putFloat("TendrilOutput", machine.tendrilOutputBonus());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains("GrowthStage")) return;

        int stage = data.getInt("GrowthStage");
        int max = data.getInt("GrowthMax");
        tooltip.add(Component.literal("Growth stage " + stage + " / " + max).withStyle(ChatFormatting.GREEN));
        if (stage < max) {
            tooltip.add(Component.literal("Next stage: " + Math.round(data.getFloat("GrowthProgress") * 100) + "%")
                    .withStyle(ChatFormatting.GRAY));
        }

        StringBuilder bonus = new StringBuilder();
        if (data.getFloat("GrowthSpeed") > 0.0f) bonus.append(Math.round(data.getFloat("GrowthSpeed") * 100)).append("% faster  ");
        if (data.getFloat("GrowthOutput") > 0.0f) bonus.append("+").append(Math.round(data.getFloat("GrowthOutput") * 100)).append("% output  ");
        if (data.getInt("GrowthBatch") > 1) bonus.append("x").append(data.getInt("GrowthBatch")).append(" batch");
        if (bonus.length() > 0) tooltip.add(Component.literal(bonus.toString().trim()).withStyle(ChatFormatting.AQUA));

        if (data.getInt("GrowthStall") == GrowthMultiblockMachine.STALL_COST) {
            tooltip.add(Component.literal("Waiting for the next stage's cost").withStyle(ChatFormatting.RED));
        } else if (data.getInt("GrowthStall") == GrowthMultiblockMachine.STALL_BLOCKED) {
            tooltip.add(Component.literal("Something is in the way").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.literal("Tendrils: " + data.getInt("Tendrils") + " growing, " + data.getInt("Matured") +
                " grown, " + data.getInt("TendrilBlocks") + " blocks (" + Math.round(data.getFloat("TendrilSpeed") * 100) +
                "% faster, +" + Math.round(data.getFloat("TendrilOutput") * 100) + "% output)")
                .withStyle(ChatFormatting.DARK_GREEN));
        if (data.getBoolean("Controlled")) {
            tooltip.add(Component.literal(data.getBoolean("Paused") ? "Growth control: PAUSED" :
                    "Growth control: fitted").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.literal("No growth control hatch: it grows wild and breaks blocks")
                    .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
