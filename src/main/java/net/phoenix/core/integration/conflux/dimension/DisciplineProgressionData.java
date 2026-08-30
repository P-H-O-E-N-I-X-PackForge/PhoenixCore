package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DisciplineProgressionData extends SavedData {
    private static final String ID = "conflux_dimension_progression";

    private final Map<UUID, ProgressionState> progressions = new HashMap<>();

    public static DisciplineProgressionData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(DisciplineProgressionData::load, DisciplineProgressionData::new, ID);
    }

    public @Nullable ProgressionState getProgression(UUID teamId) {
        return progressions.get(teamId);
    }

    public void setProgression(UUID teamId, String disciplineId, String currentStage) {
        ProgressionState state = progressions.computeIfAbsent(teamId, k -> new ProgressionState(disciplineId));
        state.currentStage = currentStage;
        setDirty();
    }

    public void unlockWorldStage(UUID teamId, String stageName) {
        ProgressionState state = progressions.get(teamId);
        if (state != null) {
            state.unlockedStages.add(stageName);
            setDirty();
        }
    }

    public boolean hasStageUnlocked(UUID teamId, String stageName) {
        ProgressionState state = progressions.get(teamId);
        return state != null && state.unlockedStages.contains(stageName);
    }

    public static class ProgressionState {
        public String disciplineId;
        public String currentStage;
        public final Set<String> unlockedStages;
        public long lastProgression;

        public ProgressionState(String disciplineId) {
            this.disciplineId = disciplineId;
            this.currentStage = "initial";
            this.unlockedStages = new HashSet<>();
            this.lastProgression = System.currentTimeMillis();
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("discipline", disciplineId);
            tag.putString("currentStage", currentStage);
            tag.putLong("lastProgression", lastProgression);

            ListTag stagesTag = new ListTag();
            for (String stage : unlockedStages) {
                stagesTag.add(net.minecraft.nbt.StringTag.valueOf(stage));
            }
            tag.put("unlockedStages", stagesTag);
            return tag;
        }

        public static ProgressionState load(CompoundTag tag) {
            ProgressionState state = new ProgressionState(tag.getString("discipline"));
            state.currentStage = tag.getString("currentStage");
            state.lastProgression = tag.getLong("lastProgression");

            if (tag.contains("unlockedStages", Tag.TAG_LIST)) {
                ListTag stagesTag = tag.getList("unlockedStages", Tag.TAG_STRING);
                for (int i = 0; i < stagesTag.size(); i++) {
                    state.unlockedStages.add(stagesTag.getString(i));
                }
            }
            return state;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag progressionsTag = new ListTag();
        for (Map.Entry<UUID, ProgressionState> entry : progressions.entrySet()) {
            CompoundTag progressTag = entry.getValue().save();
            progressTag.putUUID("teamId", entry.getKey());
            progressionsTag.add(progressTag);
        }
        tag.put("progressions", progressionsTag);
        return tag;
    }

    public static DisciplineProgressionData load(CompoundTag tag) {
        DisciplineProgressionData data = new DisciplineProgressionData();
        if (tag.contains("progressions", Tag.TAG_LIST)) {
            ListTag progressionsTag = tag.getList("progressions", Tag.TAG_COMPOUND);
            for (int i = 0; i < progressionsTag.size(); i++) {
                CompoundTag progressTag = progressionsTag.getCompound(i);
                UUID teamId = progressTag.getUUID("teamId");
                ProgressionState state = ProgressionState.load(progressTag);
                data.progressions.put(teamId, state);
            }
        }
        return data;
    }
}
