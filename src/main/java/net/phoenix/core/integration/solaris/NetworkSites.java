package net.phoenix.core.integration.solaris;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.utils.TeamUtils;
import net.phoenix_tesla_network.tesla.saveddata.TeslaTeamEnergyData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class NetworkSites {

    record Site(String kind, String name, ResourceLocation dimension, BlockPos pos, String color, String icon,
                String category) {}

    private NetworkSites() {}

    static List<Site> of(ServerPlayer player) {
        List<Site> sites = new ArrayList<>();
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        if (team == null) team = player.getUUID();

        addTesla(player, team, sites);
        return sites;
    }

    private static void addTesla(ServerPlayer player, UUID team, List<Site> sites) {
        TeslaTeamEnergyData.TeamEnergy energy = TeslaTeamEnergyData.get(player.serverLevel()).getIfPresent(team);
        if (energy == null) return;
        for (TeslaTeamEnergyData.RangeNode node : energy.rangeNodes.values()) {
            boolean tower = node.type.isTower();
            String state = node.working ? "online" : "offline";
            sites.add(new Site(tower ? "tower" : "relay", (tower ? "Tesla Tower" : "Tesla Range Extender") + " (" +
                    state + ")", node.dimension.location(), node.pos, node.working ? (tower ? "55E0FF" : "A070FF") :
                            "888888",
                    tower ? "RESOURCE" : "DOT", "Tesla"));
        }
    }
}
