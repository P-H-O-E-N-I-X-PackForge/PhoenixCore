package net.phoenix.core.integration.solaris;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.api.ConfluxApi;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenixvine.solaris.api.SolarisAPI;
import net.phoenixvine.solaris.api.SolarisGatesAPI;

import java.util.List;
import java.util.UUID;

final class SolarisGatesBridge {

    static final List<String> DISCIPLINES = List.of("phoenix", "sculk", "void", "sealed_a", "sealed_b");

    private static boolean defaultsBound = false;

    private SolarisGatesBridge() {}

    static void registerGates() {
        SolarisGatesAPI.registerGate("conflux:discipline", player -> ConfluxApi.discipline(player) != null);
        for (String discipline : DISCIPLINES) {
            SolarisGatesAPI.registerGate("conflux:discipline/" + discipline,
                    player -> discipline.equals(ConfluxApi.discipline(player)));
        }

        for (ResearchTree tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            if (tree.discipline == null) continue;
            tree.getCommitmentNode().ifPresent(core -> SolarisGatesAPI.registerGate("conflux:core/" + tree.discipline,
                    player -> ConfluxApi.isUnlocked(player, core.id)));
        }

        for (ResearchNode node : ResearchTreeRegistry.INSTANCE.getAllNodes()) {
            ResourceLocation id = node.id;
            SolarisGatesAPI.registerGate("conflux:node/" + id, player -> ConfluxApi.isUnlocked(player, id));
        }
    }

    static void bindDefaults() {
        if (defaultsBound) return;
        defaultsBound = true;

        for (String discipline : DISCIPLINES) {
            ResourceLocation dimension = ResourceLocation.fromNamespaceAndPath("phoenixcore",
                    "conflux/" + discipline);
            for (String feature : List.of(SolarisAPI.FEATURE_MINIMAP, SolarisAPI.FEATURE_WORLD_MAP,
                    SolarisAPI.FEATURE_FULLSCREEN_MAP)) {
                SolarisGatesAPI.bind(feature, dimension, "conflux:discipline");
            }
            for (String feature : List.of(SolarisAPI.FEATURE_UNDERGROUND_MAP, SolarisAPI.FEATURE_GLOBE_VIEW,
                    SolarisAPI.FEATURE_SHOW_MOBS)) {
                SolarisGatesAPI.bind(feature, dimension, "conflux:core/" + discipline);
            }
        }
    }

    static void refreshTeam(MinecraftServer server, UUID team) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (team.equals(ResearchTeamHelper.getTeamId(player))) SolarisGatesAPI.refresh(player);
        }
    }

    static void refreshAll(MinecraftServer server) {
        SolarisGatesAPI.refreshAll(server);
    }
}
