package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;

public class ChunkProgressionState {
    private static final String PROGRESSION_TAG = "disciplineProgress";
    private static final String APPLIED_MILESTONES = "appliedMilestones";

    private final Set<String> appliedMilestones;
    private boolean dirty;

    public ChunkProgressionState() {
        this.appliedMilestones = new HashSet<>();
        this.dirty = false;
    }

    public boolean hasMilestoneApplied(String milestoneName) {
        return appliedMilestones.contains(milestoneName);
    }

    public void applyMilestone(String milestoneName) {
        if (appliedMilestones.add(milestoneName)) {
            dirty = true;
        }
    }

    public Set<String> getAppliedMilestones() {
        return new HashSet<>(appliedMilestones);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public void save(CompoundTag chunkTag) {
        CompoundTag progressionTag = new CompoundTag();
        ListTag milestonesTag = new ListTag();

        for (String milestone : appliedMilestones) {
            milestonesTag.add(net.minecraft.nbt.StringTag.valueOf(milestone));
        }

        progressionTag.put(APPLIED_MILESTONES, milestonesTag);
        chunkTag.put(PROGRESSION_TAG, progressionTag);
    }

    public static ChunkProgressionState load(CompoundTag chunkTag) {
        ChunkProgressionState state = new ChunkProgressionState();

        if (chunkTag.contains(PROGRESSION_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag progressionTag = chunkTag.getCompound(PROGRESSION_TAG);
            if (progressionTag.contains(APPLIED_MILESTONES, Tag.TAG_LIST)) {
                ListTag milestonesTag = progressionTag.getList(APPLIED_MILESTONES, Tag.TAG_STRING);
                for (int i = 0; i < milestonesTag.size(); i++) {
                    state.appliedMilestones.add(milestonesTag.getString(i));
                }
            }
        }

        return state;
    }

    public static ChunkProgressionState getOrCreate(LevelChunk chunk) {
        return chunk.getCapability(ChunkProgressionProvider.CHUNK_PROGRESSION_CAP)
                .orElse(new ChunkProgressionState());
    }
}
