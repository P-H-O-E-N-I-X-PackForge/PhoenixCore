package net.phoenix.core.integration.conflux.dimension.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.dimension.ConfluxDimensionFactory;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.UUID;

public class C2SDimensionTeleportPacket implements CustomPacketPayload {

    public static final Type<C2SDimensionTeleportPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_dimension_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDimensionTeleportPacket> STREAM_CODEC = StreamCodec
            .of((buf, pkt) -> buf.writeUtf(pkt.action), buf -> new C2SDimensionTeleportPacket(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final String action;

    public C2SDimensionTeleportPacket(String action) {
        this.action = action;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ServerLevel overworld = player.getServer().overworld();
            WorldResearchData researchData = WorldResearchData.get(overworld);

            UUID teamId = ResearchTeamHelper.getTeamId(player);
            if (teamId == null) return;

            String discipline = researchData.getDiscipline(teamId);
            if (discipline == null) return;

            if ("enter".equals(action)) {
                ConfluxDimensionFactory.enterDisciplineDimension(player, teamId, discipline);
            } else if ("exit".equals(action)) {
                overworld = player.getServer().overworld();
                player.teleportTo(overworld, 0.5, 64, 0.5, 0, 0);
            }
        });
    }
}
