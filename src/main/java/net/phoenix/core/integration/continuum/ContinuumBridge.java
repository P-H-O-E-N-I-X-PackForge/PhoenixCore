package net.phoenix.core.integration.continuum;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.phoenix.continuum.api.ContinuumHost;
import net.phoenix.continuum.client.ContinuumNodeNotes;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.client.ConfluxNodeNotes;
import net.phoenix.core.api.event.ConfluxResearchEvent;
import net.phoenix.core.api.event.ConfluxTeamChangedEvent;
import net.phoenix.core.integration.conflux.dimension.ConfluxDimensionFactory;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.utils.TeamUtils;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ContinuumBridge {

    private static final String SNAPSHOT = "continuum_bridge_before";

    private ContinuumBridge() {}

    public static void init() {
        ContinuumHost.setTeams(new ContinuumHost.Teams() {

            @Override
            public UUID teamOf(UUID player) {
                UUID team = TeamUtils.getTeamIdOrPlayerFallback(player);
                return team == null ? player : team;
            }

            @Override
            public boolean onTeam(Player player, UUID team) {
                return TeamUtils.isPlayerOnTeam(player, team);
            }
        });

        ContinuumHost.setResearch(new ContinuumHost.Research() {

            private @Nullable WorldResearchData data() {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                return server == null ? null : WorldResearchData.get(server.overworld());
            }

            @Override
            public boolean isUnlocked(UUID team, ResourceLocation node) {
                WorldResearchData data = data();
                return data != null && data.isUnlocked(team, node);
            }

            @Override
            public boolean hasFlag(UUID team, String flag) {
                WorldResearchData data = data();
                return data != null && data.hasFlag(team, flag);
            }

            @Override
            public @Nullable String getDiscipline(UUID team) {
                WorldResearchData data = data();
                return data == null ? null : data.getDiscipline(team);
            }
        });

        ContinuumHost.setTravel(new ContinuumHost.Travel() {

            @Override
            public ResourceKey<Level> dimensionOf(String discipline) {
                return ConfluxDimensionFactory.getDimensionKey(discipline);
            }

            @Override
            public boolean visit(ServerPlayer player, String discipline) {
                return ConfluxDimensionFactory.visitDisciplineDimension(player, discipline);
            }
        });
    }

    public static void initClient() {
        ConfluxNodeNotes.register(node -> ContinuumNodeNotes.notes(node.id));
    }

    @SubscribeEvent
    public static void onResearchPre(ConfluxResearchEvent.Pre event) {
        event.getContext().put(SNAPSHOT, ContinuumHost.beforeResearch(event.getServer(), event.getTeam()));
    }

    @SubscribeEvent
    public static void onResearchPost(ConfluxResearchEvent.Post event) {
        Object before = event.getContext().get(SNAPSHOT);
        if (before != null) ContinuumHost.afterResearch(event.getServer(), event.getTeam(), before);
        else ContinuumHost.teamChanged(event.getServer(), event.getTeam());
    }

    @SubscribeEvent
    public static void onTeamChanged(ConfluxTeamChangedEvent event) {
        ContinuumHost.teamChanged(event.getServer(), event.getTeam());
    }
}
