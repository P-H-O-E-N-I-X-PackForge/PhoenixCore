package net.phoenix.core.integration.conflux.signature;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class SignatureEvents {

    private static final long SECOND = 20;
    private static final double TICKS_PER_MINUTE = 1200.0;

    private static final long MAX_CATCH_UP_TICKS = 24L * 60 * 60 * 20;

    private SignatureEvents() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld.getGameTime() % SECOND != 0) return;

        WorldResearchData research = WorldResearchData.get(overworld);
        SignatureData data = SignatureData.get(overworld);
        long now = overworld.getGameTime();
        for (Map.Entry<UUID, String> entry : research.disciplines().entrySet()) {
            UUID team = entry.getKey();
            long since = Math.min(MAX_CATCH_UP_TICKS, Math.max(0, now - data.lastAdvanced(team, now)));
            data.setLastAdvanced(team, now);
            for (SignatureDefinition def : SignatureRegistry.INSTANCE.forDiscipline(entry.getValue())) {
                if (since > 0 && (def.gainPerMinute() != 0 || def.decayPerMinute() != 0)) {
                    double before = data.get(team, def);
                    if (data.set(team, def, def.advance(before, since / TICKS_PER_MINUTE))) {
                        Signatures.takeDirtyAdd(team);
                    }
                }
                for (SignatureMechanic mechanic : SignatureMechanics.of(def.id())) {
                    mechanic.onSecond(overworld, team, def);
                }
            }
        }

        for (UUID team : Signatures.takeDirty()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ResearchTeamHelper.getTeamId(player).equals(team)) sendTo(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    public static void sendTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID team = ResearchTeamHelper.getTeamId(player);
        String discipline = WorldResearchData.get(level).getDiscipline(team);
        List<S2CSignaturePacket.Entry> entries = new ArrayList<>();
        if (discipline != null) {
            SignatureData data = SignatureData.get(level);
            for (SignatureDefinition def : SignatureRegistry.INSTANCE.forDiscipline(discipline)) {
                entries.add(new S2CSignaturePacket.Entry(def, data.get(team, def)));
            }
        }
        PacketDistributor.sendToPlayer(player, new S2CSignaturePacket(entries));
    }
}
