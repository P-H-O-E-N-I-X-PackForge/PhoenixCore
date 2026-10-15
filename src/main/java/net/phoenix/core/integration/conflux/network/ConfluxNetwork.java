package net.phoenix.core.integration.conflux.network;



import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.research.DisciplineInfo;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.UUID;

public final class ConfluxNetwork {

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(C2SResearchUnlockPacket.TYPE, C2SResearchUnlockPacket.STREAM_CODEC, C2SResearchUnlockPacket::handle);

        registrar.playToServer(C2SAbandonDisciplinePacket.TYPE, C2SAbandonDisciplinePacket.STREAM_CODEC, C2SAbandonDisciplinePacket::handle);

        registrar.playToServer(C2SChooseDisciplinePacket.TYPE, C2SChooseDisciplinePacket.STREAM_CODEC, C2SChooseDisciplinePacket::handle);

        registrar.playToClient(S2CResearchSyncPacket.TYPE, S2CResearchSyncPacket.STREAM_CODEC, S2CResearchSyncPacket::handle);

        registrar.playToServer(net.phoenix.core.integration.conflux.dimension.network.C2SDimensionTeleportPacket.TYPE, net.phoenix.core.integration.conflux.dimension.network.C2SDimensionTeleportPacket.STREAM_CODEC, (msg, ctx) -> msg.handle(ctx));

        registrar.playToClient(net.phoenix.core.integration.conflux.dimension.network.S2CDisciplineProgressionSyncPacket.TYPE, net.phoenix.core.integration.conflux.dimension.network.S2CDisciplineProgressionSyncPacket.STREAM_CODEC, (msg, ctx) -> msg.handle(ctx));
    }

    public static void syncResearchToPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        UUID teamId = ResearchTeamHelper.getTeamId(player);
        WorldResearchData data = WorldResearchData.get(level);
        DisciplineInfo disc = data.getDisciplineInfo(teamId, ResearchTreeRegistry.INSTANCE);
        PacketDistributor.sendToPlayer(player, new S2CResearchSyncPacket(
                        data.getUnlocked(teamId), data.getLockedOut(teamId), data.getFlags(teamId),
                        disc.disciplineId(), disc.disciplineTitle(), disc.committed(), disc.switchCost(),
                        net.phoenix.core.integration.conflux.terminal.ConfluxDataStore.get(level).snapshot(teamId)));
    }
}
