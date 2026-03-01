package net.phoenix.core.integration.jade.provider;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.electric.TeslaTowerMachine;
import net.phoenix.core.common.machine.multiblock.part.special.TeslaEnergyHatchPartMachine;
import net.phoenix.core.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.UUID;

public class TeslaNetworkProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("tesla_network_info");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MetaMachineBlockEntity metaBE) {
            UUID team = null;
            BlockPos pos = accessor.getPosition();
            long transferRate = 0;
            int mode = -1;

            if (accessor.getLevel() instanceof ServerLevel sl) {
                MinecraftServer server = sl.getServer();
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (overworld == null) return;

                TeslaTeamEnergyData data = TeslaTeamEnergyData.get(overworld);
                var machine = metaBE.getMetaMachine();

                if (machine instanceof TeslaEnergyHatchPartMachine hatch) {
                    team = hatch.getOwnerTeamUUID();
                    if (team != null) {
                        mode = hatch.isUplink() ? 0 : 1;
                        transferRate = data.getOrCreate(team).machineDisplayFlow.getOrDefault(pos, 0L);
                    }
                }// ADD THIS BLOCK:
                else if (machine instanceof TeslaTowerMachine tower) {
                    // Assuming your Tower class has a way to get the Team UUID
                    // (similar to how the Hatch does it)
                    team = tower.getOwnerUUID();
                    mode = -1; // We don't want "Providing/Taking" on the main controller
                }
                else {
                    for (var entry : data.getNetworksView().entrySet()) {
                        TeslaTeamEnergyData.TeamEnergy teamData = entry.getValue();
                        if (teamData.soulLinkedMachines.contains(pos)) {
                            team = entry.getKey();
                            mode = 1;
                            transferRate = teamData.machineDisplayFlow.getOrDefault(pos, 0L);
                            break;
                        } else if (teamData.activeChargers.contains(pos)) {
                            team = entry.getKey();
                            mode = 2; // Charger Mode
                            transferRate = teamData.machineDisplayFlow.getOrDefault(pos, 0L);
                            break;
                        }
                    }
                }

                if (team != null) {
                    TeslaTeamEnergyData.TeamEnergy teamData = data.getOrCreate(team);
                    tag.putUUID("TeslaTeam", team);
                    tag.putString("TeamName", TeamUtils.getTeamName(team));
                    tag.putString("Stored", FormattingUtil.formatNumbers(teamData.stored));
                    tag.putString("Capacity", FormattingUtil.formatNumbers(teamData.capacity));
                    tag.putLong("LocalTransfer", transferRate);
                    tag.putInt("TransferMode", mode);

                    int physicalHatches = teamData.getLiveHatchCount(sl.getGameTime());
                    int wiredMachines = teamData.soulLinkedMachines.size();
                    int wirelessChargers = teamData.activeChargers.size();

                    tag.putInt("TotalConnections", physicalHatches + wiredMachines + wirelessChargers);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("TeslaTeam")) return;

        tooltip.add(Component.literal("Network: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.getString("TeamName")).withStyle(ChatFormatting.AQUA)));

        tooltip.add(Component.literal("Cloud: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.getString("Stored")).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" / " + data.getString("Capacity") + " EU")
                        .withStyle(ChatFormatting.YELLOW)));

        int connections = data.contains("TotalConnections") ? data.getInt("TotalConnections") :
                data.getInt("ActiveHatches");
        tooltip.add(Component.literal("Connections: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(connections)).withStyle(ChatFormatting.WHITE)));

        if (data.contains("TransferMode") && data.getInt("TransferMode") != -1) {
            long rate = data.getLong("LocalTransfer");
            int mode = data.getInt("TransferMode");

            MutableComponent label;
            ChatFormatting color;
            String icon = "";

            switch (mode) {
                case 0 -> { // Uplink (Providing to Cloud)
                    label = Component.literal("Providing: ");
                    color = ChatFormatting.GREEN;
                }
                case 2 -> { // Wireless Charger (Broadcasting to Players)
                    label = Component.literal("Broadcasting: ");
                    color = ChatFormatting.AQUA;
                    icon = "§3波 "; // Matching the Binder UI icon
                }
                default -> { // Downlink / Machine (Taking from Cloud)
                    label = Component.literal("Taking: ");
                    color = ChatFormatting.RED;
                }
            }

            // Only show rate if it's actually transferring, otherwise show IDLE
            if (rate > 0) {
                tooltip.add(label.withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(icon + FormattingUtil.formatNumbers(rate) + " EU/t")
                                .withStyle(color)));
            } else {
                tooltip.add(label.withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("IDLE").withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
