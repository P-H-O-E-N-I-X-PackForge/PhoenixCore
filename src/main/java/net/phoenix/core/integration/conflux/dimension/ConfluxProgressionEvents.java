package net.phoenix.core.integration.conflux.dimension;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.integration.conflux.network.C2SResearchUnlockPacket;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchUnlock;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class ConfluxProgressionEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WorldResearchData researchData = WorldResearchData.get(level);
            ResearchTreeRegistry registry = ResearchTreeRegistry.INSTANCE;

            UUID teamId = getTeamIdForPlayer(player);
            if (teamId != null) {
                String discipline = researchData.getDiscipline(teamId);
                if (discipline != null) {
                    DisciplineProgressionData.get(level).getProgression(teamId);
                }
            }
        }
    }

    public static void onResearchUnlock(ServerPlayer player, ResearchNode node, UUID teamId) {
        ServerLevel level = player.serverLevel();
        WorldResearchData researchData = WorldResearchData.get(level);
        String discipline = researchData.getDiscipline(teamId);

        if (discipline == null) return;

        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(discipline);
        if (theme == null) return;

        for (ResearchUnlock unlock : node.unlocks) {
            if ("world_stage".equals(unlock.type())) {
                String stageName = unlock.value();

                WorldProgressionApplier.applyWorldStageToTeam(level, teamId, stageName);

                WorldProgressionApplier.applyBiomeColorTransition(level, discipline, getPreviousStage(theme), stageName);
                WorldProgressionApplier.applyStructureRetheming(level, discipline, stageName);
                WorldProgressionApplier.applySkyboxTransition(level, discipline, stageName);
            }
        }
    }

    private static String getPreviousStage(DisciplineTheme theme) {
        if (theme.colorProgression.length > 0) {
            return theme.colorProgression[0].milestone;
        }
        return "initial";
    }

    @Nullable
    private static UUID getTeamIdForPlayer(ServerPlayer player) {

        if (net.minecraftforge.fml.ModList.get().isLoaded("ftbteams")) {
            try {
                return getTeamFromFTB(player);
            } catch (Exception e) {
                
            }
        }
        return null;
    }

    private static UUID getTeamFromFTB(ServerPlayer player) {
        
        return null;
    }
}
