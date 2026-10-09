package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.phoenix.core.integration.conflux.dimension.network.S2CDisciplineProgressionSyncPacket;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class WorldProgressionApplier {

    public static void applyWorldStageToTeam(ServerLevel level, UUID teamId, String newStage) {
        DisciplineProgressionData.ProgressionState progression = DisciplineProgressionData.get(level)
                .getProgression(teamId);

        if (progression == null) {
            return;
        }

        progression.currentStage = newStage;
        DisciplineProgressionData.get(level).unlockWorldStage(teamId, newStage);

        markChunksForUpdate(level, teamId, newStage);
    }

    private static void markChunksForUpdate(ServerLevel level, UUID teamId, String newStage) {
        int viewDistance = level.getServer().getPlayerList().getViewDistance();

        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();

            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                    LevelChunk levelChunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                    if (levelChunk == null) continue;

                    ChunkProgressionState state = ChunkProgressionState.getOrCreate(levelChunk);

                    if (state.hasMilestoneApplied(newStage)) continue;

                    state.applyMilestone(newStage);

                    levelChunk.setUnsaved(true);

                    level.getChunkSource().blockChanged(
                            new BlockPos(levelChunk.getPos().getMinBlockX(),
                                    level.getMinBuildHeight(),
                                    levelChunk.getPos().getMinBlockZ()));
                }
            }
        }
    }

    public static void applyBiomeColorTransition(ServerLevel level, UUID teamId, String disciplineId,
                                                 String fromStage, String toStage) {
        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(disciplineId);
        if (theme == null) return;

        DisciplineTheme.ColorProgression fromColor = findColorProgression(theme, fromStage);
        DisciplineTheme.ColorProgression toColor = findColorProgression(theme, toStage);
        if (fromColor == null || toColor == null) return;

        syncProgressionToClients(level, teamId, disciplineId, toStage);
    }

    public static void applyStructureRetheming(ServerLevel level, UUID teamId, String disciplineId, String newStage) {
        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(disciplineId);
        if (theme == null) return;
    }

    public static void applySkyboxTransition(ServerLevel level, UUID teamId, String disciplineId, String newStage) {
        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(disciplineId);
        if (theme == null) return;

        syncProgressionToClients(level, teamId, disciplineId, newStage);
    }

    private static void syncProgressionToClients(ServerLevel level, UUID teamId, String disciplineId, String stage) {
        Set<String> unlockedStages = Set.of();
        DisciplineProgressionData.ProgressionState progression = DisciplineProgressionData.get(level)
                .getProgression(teamId);
        if (progression != null) unlockedStages = progression.unlockedStages;

        CompoundTag entry = new CompoundTag();
        entry.putUUID("teamId", teamId);
        entry.putString("disciplineId", disciplineId);
        entry.putString("currentStage", stage);
        ListTag stagesTag = new ListTag();
        for (String unlocked : unlockedStages) stagesTag.add(StringTag.valueOf(unlocked));
        entry.put("unlockedStages", stagesTag);

        ListTag progressions = new ListTag();
        progressions.add(entry);

        CompoundTag data = new CompoundTag();
        data.put("progressions", progressions);
        data.putString("currentDiscipline", disciplineId);
        data.putString("currentStage", stage);

        for (ServerPlayer player : level.players()) {
            S2CDisciplineProgressionSyncPacket.send(player, data);
        }
    }

    @Nullable
    private static DisciplineTheme.ColorProgression findColorProgression(DisciplineTheme theme, String milestone) {
        for (DisciplineTheme.ColorProgression progression : theme.colorProgression) {
            if (milestone.equals(progression.milestone)) {
                return progression;
            }
        }
        return null;
    }
}
