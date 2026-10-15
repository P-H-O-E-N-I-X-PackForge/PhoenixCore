package net.phoenix.core.integration.conflux.network;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SChooseDisciplinePacket implements CustomPacketPayload {
    public static final Type<C2SChooseDisciplinePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_choose_discipline_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SChooseDisciplinePacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> C2SChooseDisciplinePacket.encode(msg, buf), C2SChooseDisciplinePacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


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

    public static void handle(C2SChooseDisciplinePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            UUID teamId = ResearchTeamHelper.getTeamId(player);
            if (teamId == null) return;

            boolean success = WorldResearchData.get(level)
                    .chooseDiscipline(teamId, pkt.disciplineId, ResearchTreeRegistry.INSTANCE);
            if (success) {
                ConfluxNetwork.syncResearchToPlayer(player);

                net.phoenix.core.integration.continuum.common.ContinuumServerEvents.sendStateToTeam(player.server,
                        teamId);
            }
        });
        }
}
