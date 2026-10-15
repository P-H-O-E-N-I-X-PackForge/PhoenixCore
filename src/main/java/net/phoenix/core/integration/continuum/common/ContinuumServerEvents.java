package net.phoenix.core.integration.continuum.common;



import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.network.S2CContinuumDataPacket;
import net.phoenix.core.integration.continuum.network.S2CContinuumStatePacket;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.utils.TeamUtils;

import java.util.UUID;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ContinuumServerEvents {

    private ContinuumServerEvents() {}

    private static int tickCounter;
    private static int upkeepCounter;

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        ContinuumData.registerServerListener(event);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ContinuumAdminCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendData(player);
            sendState(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < 20) return;
        tickCounter = 0;

        ContinuumMissions.resolveDue(event.getServer());

        if (++upkeepCounter >= 60) {
            upkeepCounter = 0;
            ContinuumTransients.get(event.getServer()).tick(event.getServer());
            for (UUID team : ContinuumTeamData.get(event.getServer()).settleOutposts(event.getServer(),
                    System.currentTimeMillis())) {
                sendStateToTeam(event.getServer(), team);
            }
        }
    }

    public static void onDataReloaded() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendData(player);
            sendState(player);
        }
    }

    public static void sendData(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new S2CContinuumDataPacket(ContinuumData.snapshot()));
    }

    public static void sendState(ServerPlayer player) {
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        if (!ContinuumTeamData.get(player.server).isEngaged(team)) return;
        sendStateAlways(player);
    }

    public static void engageAndSend(ServerPlayer player) {
        ContinuumTeamData.get(player.server).markEngaged(TeamUtils.getTeamIdOrPlayerFallback(player.getUUID()));
        sendStateAlways(player);
    }

    private static void sendStateAlways(ServerPlayer player) {
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        PacketDistributor.sendToPlayer(player, new S2CContinuumStatePacket(ContinuumStateSnapshot.build(player.server, team)));
    }

    public static void engageTeam(MinecraftServer server, UUID team) {
        ContinuumTeamData.get(server).markEngaged(team);
        sendStateToTeam(server, team);
    }

    public static void sendStateToTeam(MinecraftServer server, UUID team) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (TeamUtils.isPlayerOnTeam(player, team)) sendState(player);
        }
    }
}
