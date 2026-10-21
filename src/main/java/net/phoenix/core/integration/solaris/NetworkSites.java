package net.phoenix.core.integration.solaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.continuum.machine.LaunchPadMachine;
import net.phoenix.core.utils.TeamUtils;
import net.phoenix_tesla_network.tesla.saveddata.TeslaTeamEnergyData;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class NetworkSites {

    static final String LAUNCH_COMPLEX = "launch_complex";

    record Site(String kind, String name, ResourceLocation dimension, BlockPos pos, String color, String icon,
                String category) {}

    private NetworkSites() {}

    static List<Site> of(ServerPlayer player) {
        List<Site> sites = new ArrayList<>();
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        if (team == null) team = player.getUUID();

        addTesla(player, team, sites);
        addLaunchComplexes(player, team, sites);
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

    private static void addLaunchComplexes(ServerPlayer player, UUID team, List<Site> sites) {
        TeamSiteData data = TeamSiteData.get(player.serverLevel());
        for (Map.Entry<String, TeamSiteData.Site> entry : new ArrayList<>(data.forTeam(team).entrySet())) {
            if (!entry.getKey().startsWith(LAUNCH_COMPLEX + "/")) continue;
            TeamSiteData.Site site = entry.getValue();
            ResourceLocation dimension = ResourceLocation.parse(site.dimension());
            ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level != null && level.isLoaded(site.pos()) &&
                    !(MetaMachine.getMachine(level, site.pos()) instanceof LaunchPadMachine)) {
                data.remove(team, entry.getKey());
                continue;
            }
            sites.add(new Site(LAUNCH_COMPLEX, "Launch Complex", dimension, site.pos(), "FFB040", "HOME",
                    "Continuum"));
        }
    }
}
