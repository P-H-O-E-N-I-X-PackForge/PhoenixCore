package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchUnlock;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class ConfluxProgressionEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WorldResearchData researchData = WorldResearchData.get(level);
            ResearchTreeRegistry registry = ResearchTreeRegistry.INSTANCE;

            // Used to resolve through a local getTeamIdForPlayer()/getTeamFromFTB() pair that always
            // returned null (the FTB branch's body was never filled in), so `discipline` was always
            // null here and this whole handler quietly did nothing for every player, every login.
            // ResearchTeamHelper is the same resolver every other part of Conflux already uses.
            UUID teamId = ResearchTeamHelper.getTeamId(player);
            String discipline = researchData.getDiscipline(teamId);
            if (discipline != null) {
                DisciplineProgressionData.get(level).getProgression(teamId);
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

                WorldProgressionApplier.applyBiomeColorTransition(level, teamId, discipline, getPreviousStage(theme),
                        stageName);
                WorldProgressionApplier.applyStructureRetheming(level, teamId, discipline, stageName);
                WorldProgressionApplier.applySkyboxTransition(level, teamId, discipline, stageName);
            }
        }
    }

    private static String getPreviousStage(DisciplineTheme theme) {
        if (theme.colorProgression.length > 0) {
            return theme.colorProgression[0].milestone;
        }
        return "initial";
    }
}
