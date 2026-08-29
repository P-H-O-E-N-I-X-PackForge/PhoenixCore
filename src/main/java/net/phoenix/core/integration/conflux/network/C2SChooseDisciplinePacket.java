package net.phoenix.core.integration.conflux.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SChooseDisciplinePacket {

    private final String disciplineId;

    public C2SChooseDisciplinePacket(String disciplineId) {
        this.disciplineId = disciplineId;
    }

    public static void encode(C2SChooseDisciplinePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.disciplineId);
    }

    public static C2SChooseDisciplinePacket decode(FriendlyByteBuf buf) {
        return new C2SChooseDisciplinePacket(buf.readUtf());
    }

    public static void handle(C2SChooseDisciplinePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            UUID teamId = ResearchTeamHelper.getTeamId(player);
            if (teamId == null) return;

            boolean success = WorldResearchData.get(level)
                    .chooseDiscipline(teamId, pkt.disciplineId, ResearchTreeRegistry.INSTANCE);
            if (success) {
                ConfluxNetwork.syncResearchToPlayer(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
