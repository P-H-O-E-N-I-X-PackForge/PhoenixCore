package net.phoenix.core.integration.conflux.dimension.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.conflux.dimension.ConfluxDimensionFactory;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SDimensionTeleportPacket {

    private final String action;

    public C2SDimensionTeleportPacket(String action) {
        this.action = action;
    }

    public C2SDimensionTeleportPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
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

        return true;
    }
}
