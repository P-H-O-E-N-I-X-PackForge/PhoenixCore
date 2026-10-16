package net.phoenix.core.integration.continuum.common;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;
import net.phoenix.core.utils.TeamUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tells a team when their research has just detected or surveyed something. A body's stage follows from the research a
 * team has unlocked, so there is no event to hook: take a snapshot before an unlock, another after, and announce whatever
 * moved up.
 */
public final class ContinuumDiscoveryNotifier {

    private ContinuumDiscoveryNotifier() {}

    public static Map<ResourceLocation, DiscoveryStage> snapshot(MinecraftServer server, UUID team) {
        ContinuumTeamData data = ContinuumTeamData.get(server);
        Map<ResourceLocation, DiscoveryStage> out = new HashMap<>();
        for (ContinuumSystem system : ContinuumData.systems()) {
            out.put(system.id(), data.effectiveStage(server, team, system.id()));
        }
        for (ContinuumBody body : ContinuumData.allBodies()) {
            out.put(body.id(), data.effectiveStage(server, team, body.id()));
        }
        return out;
    }

    public static void announce(MinecraftServer server, UUID team, Map<ResourceLocation, DiscoveryStage> before) {
        Map<ResourceLocation, DiscoveryStage> after = snapshot(server, team);

        List<ContinuumBody> surveyed = new ArrayList<>();
        List<ContinuumBody> detected = new ArrayList<>();
        List<ContinuumSystem> systems = new ArrayList<>();
        java.util.Set<ResourceLocation> systemsWithBodies = new java.util.HashSet<>();

        for (ContinuumBody body : ContinuumData.allBodies()) {
            DiscoveryStage was = before.getOrDefault(body.id(), DiscoveryStage.UNKNOWN);
            DiscoveryStage now = after.getOrDefault(body.id(), DiscoveryStage.UNKNOWN);
            if (now.atLeast(was) && now != was) {
                systemsWithBodies.add(body.system());
                if (now == DiscoveryStage.SURVEYED) surveyed.add(body);
                else if (now == DiscoveryStage.DETECTED) detected.add(body);
            }
        }
        for (ContinuumSystem system : ContinuumData.systems()) {
            DiscoveryStage was = before.getOrDefault(system.id(), DiscoveryStage.UNKNOWN);
            DiscoveryStage now = after.getOrDefault(system.id(), DiscoveryStage.UNKNOWN);
            if (now != was && now.atLeast(was) && !systemsWithBodies.contains(system.id())) systems.add(system);
        }
        if (surveyed.isEmpty() && detected.isEmpty() && systems.isEmpty()) return;

        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (TeamUtils.isPlayerOnTeam(p, team)) players.add(p);
        if (players.isEmpty()) return;

        List<Component> lines = new ArrayList<>();
        for (ContinuumBody b : surveyed) {
            lines.add(Component.literal("§6[Continuum] §bSurvey complete: §f" + b.name() + "§7. " +
                    "It can now be travelled to."));
        }
        for (ContinuumBody b : detected) {
            lines.add(Component.literal("§6[Continuum] §eSignal detected: §f" + b.name() + "§7."));
        }
        for (ContinuumSystem s : systems) {
            lines.add(Component.literal("§6[Continuum] §eNew system on the charts: §f" + s.name() + "§7."));
        }

        boolean survey = !surveyed.isEmpty();
        String first = survey ? surveyed.get(0).name() : !detected.isEmpty() ? detected.get(0).name() :
                systems.get(0).name();
        int total = surveyed.size() + detected.size() + systems.size();
        Component title = Component.literal(survey ? "§bSurvey complete" : "§eSignal detected");
        Component subtitle = Component.literal("§f" + first + (total > 1 ? " §7(+" + (total - 1) + " more)" : ""));

        for (ServerPlayer p : players) {
            for (Component line : lines) p.sendSystemMessage(line);
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f,
                    survey ? 1.2f : 1.0f);
        }
    }
}
