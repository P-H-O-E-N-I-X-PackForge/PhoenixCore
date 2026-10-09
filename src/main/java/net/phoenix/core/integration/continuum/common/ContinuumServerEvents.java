package net.phoenix.core.integration.continuum.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.network.S2CContinuumDataPacket;
import net.phoenix.core.integration.continuum.network.S2CContinuumStatePacket;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.utils.TeamUtils;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < 20) return;
        tickCounter = 0;

        ContinuumMissions.resolveDue(event.getServer());

        if (++upkeepCounter >= 60) {
            upkeepCounter = 0;
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
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CContinuumDataPacket(ContinuumData.snapshot()));
    }

    public static void sendState(ServerPlayer player) {
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CContinuumStatePacket(ContinuumStateSnapshot.build(player.server, team)));
    }

    public static void sendStateToTeam(MinecraftServer server, UUID team) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (TeamUtils.isPlayerOnTeam(player, team)) sendState(player);
        }
    }
}
