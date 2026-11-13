package net.phoenix.core.rebirth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.conflux.ending.Ending;
import net.phoenix.conflux.research.ResearchTeamHelper;

public final class RebirthReplay {

    private RebirthReplay() {}

    public static void play(ServerPlayer player, Ending ending) {
        PacketDistributor.sendToPlayer(player, S2CEndingPacket.of(player.server, ending));
        var team = RebirthData.get(player.serverLevel()).team(ResearchTeamHelper.getTeamId(player));
        BlockPos forge = team.forgePos == RebirthData.NONE ? player.blockPosition() : BlockPos.of(team.forgePos);
        S2CRebirthStatePacket.send(player, S2CRebirthStatePacket.of(player, forge, true, true, ""));
    }
}
