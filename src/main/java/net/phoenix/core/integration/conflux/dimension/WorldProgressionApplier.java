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

                    // hasMilestoneApplied used to be write-only - applyMilestone() was called
                    // unconditionally here, so every stage advance re-marked and force-re-rendered
                    // every loaded chunk in every player's view distance even if that exact chunk had
                    // already been updated for this exact stage (e.g. a second team unlocking the same
                    // stage later, or the same team re-triggering it).
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

        // The client-side payoff (BiomeColorProvider.setDisciplineProgression) reads the theme's own
        // color_progression table by stage name, so the real "transition" work is just making sure
        // every affected client knows the new stage - see syncProgressionToClients.
        syncProgressionToClients(level, teamId, disciplineId, toStage);
    }

    /**
     * Structure retheming (per-discipline texture overrides for placed structures, via
     * {@link DisciplineTheme.StructureThemeSet#structureTextures}) has no rendering consumer anywhere
     * in the codebase to hook into - nothing reads that map to actually swap a texture or model. Making
     * this do something real means deciding what it retextures (specific structure pieces? any block
     * matching a tag?) and building that lookup, which is a real feature design, not a wiring fix like
     * the other two methods here. Left as a documented no-op until that's designed.
     */
    public static void applyStructureRetheming(ServerLevel level, UUID teamId, String disciplineId, String newStage) {
        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(disciplineId);
        if (theme == null) return;
    }

    public static void applySkyboxTransition(ServerLevel level, UUID teamId, String disciplineId, String newStage) {
        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(disciplineId);
        if (theme == null) return;

        // DisciplineSkyRenderer.setSkyboxProfile is keyed by discipline only (not stage - Conflux only
        // has one skybox per discipline right now), but it's driven by the exact same client-side sync
        // as the biome colors above, so this independently ensures clients are synced too rather than
        // silently relying on applyBiomeColorTransition having already been called first.
        syncProgressionToClients(level, teamId, disciplineId, newStage);
    }

    /** Sends every player currently in this level the team's current discipline/stage/unlocked-stages,
     *  which is what actually drives BiomeColorProvider and DisciplineSkyRenderer client-side (via
     *  ClientDisciplineProgressionCache.updateProgression). This used to never happen at all -
     *  S2CDisciplineProgressionSyncPacket.send(CompoundTag) was an empty no-op with no caller anywhere. */
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
