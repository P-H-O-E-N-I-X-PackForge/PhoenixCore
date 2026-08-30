package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkProgressionStateTest {

    private ChunkProgressionState chunkState;

    @BeforeEach
    public void setUp() {
        chunkState = new ChunkProgressionState();
    }

    @Test
    public void testInitialState() {
        assertFalse(chunkState.hasMilestoneApplied("thermal_recursion"));
        assertTrue(chunkState.getAppliedMilestones().isEmpty());
        assertFalse(chunkState.isDirty());
    }

    @Test
    public void testApplyMilestone() {
        chunkState.applyMilestone("thermal_recursion");

        assertTrue(chunkState.hasMilestoneApplied("thermal_recursion"));
        assertTrue(chunkState.isDirty());
    }

    @Test
    public void testMultipleMilestones() {
        chunkState.applyMilestone("thermal_recursion");
        chunkState.applyMilestone("molten_path");
        chunkState.applyMilestone("solar_recursion");

        assertTrue(chunkState.hasMilestoneApplied("thermal_recursion"));
        assertTrue(chunkState.hasMilestoneApplied("molten_path"));
        assertTrue(chunkState.hasMilestoneApplied("solar_recursion"));
        assertEquals(3, chunkState.getAppliedMilestones().size());
    }

    @Test
    public void testDuplicateMilestoneDoesNotChangeDirty() {
        chunkState.applyMilestone("thermal_recursion");
        chunkState.clearDirty();
        assertFalse(chunkState.isDirty());

        chunkState.applyMilestone("thermal_recursion");
        assertFalse(chunkState.isDirty()); 
    }

    @Test
    public void testNBTSerialization() {
        chunkState.applyMilestone("thermal_recursion");
        chunkState.applyMilestone("molten_path");

        CompoundTag chunkTag = new CompoundTag();
        chunkState.save(chunkTag);

        assertTrue(chunkTag.contains("disciplineProgress"));

        CompoundTag progressionTag = chunkTag.getCompound("disciplineProgress");
        assertTrue(progressionTag.contains("appliedMilestones"));
    }

    @Test
    public void testNBTDeserialization() {
        
        chunkState.applyMilestone("thermal_recursion");
        chunkState.applyMilestone("molten_path");

        CompoundTag chunkTag = new CompoundTag();
        chunkState.save(chunkTag);

        ChunkProgressionState loaded = ChunkProgressionState.load(chunkTag);

        assertTrue(loaded.hasMilestoneApplied("thermal_recursion"));
        assertTrue(loaded.hasMilestoneApplied("molten_path"));
        assertEquals(2, loaded.getAppliedMilestones().size());
    }

    @Test
    public void testClearDirty() {
        chunkState.applyMilestone("thermal_recursion");
        assertTrue(chunkState.isDirty());

        chunkState.clearDirty();
        assertFalse(chunkState.isDirty());

        assertTrue(chunkState.hasMilestoneApplied("thermal_recursion"));
    }
}
