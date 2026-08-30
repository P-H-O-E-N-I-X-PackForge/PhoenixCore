package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DisciplineProgressionDataTest {

    private DisciplineProgressionData progressionData;
    private UUID testTeamId;

    @BeforeEach
    public void setUp() {
        progressionData = new DisciplineProgressionData();
        testTeamId = UUID.randomUUID();
    }

    @Test
    public void testProgressionStateCreation() {
        String disciplineId = "phoenix";
        progressionData.setProgression(testTeamId, disciplineId, "initial");

        DisciplineProgressionData.ProgressionState state = progressionData.getProgression(testTeamId);
        assertNotNull(state);
        assertEquals("phoenix", state.disciplineId);
        assertEquals("initial", state.currentStage);
    }

    @Test
    public void testUnlockWorldStage() {
        progressionData.setProgression(testTeamId, "phoenix", "initial");
        progressionData.unlockWorldStage(testTeamId, "thermal_recursion");

        assertTrue(progressionData.hasStageUnlocked(testTeamId, "thermal_recursion"));
        assertFalse(progressionData.hasStageUnlocked(testTeamId, "nonexistent"));
    }

    @Test
    public void testNBTSerialization() {
        progressionData.setProgression(testTeamId, "phoenix", "initial");
        progressionData.unlockWorldStage(testTeamId, "thermal_recursion");
        progressionData.unlockWorldStage(testTeamId, "molten_path");

        CompoundTag tag = new CompoundTag();
        progressionData.save(tag);

        assertTrue(tag.contains("progressions", Tag.TAG_LIST));

        ListTag progressionsTag = tag.getList("progressions", Tag.TAG_COMPOUND);
        assertEquals(1, progressionsTag.size());

        CompoundTag progTag = progressionsTag.getCompound(0);
        assertEquals(testTeamId, progTag.getUUID("teamId"));
        assertEquals("phoenix", progTag.getString("discipline"));
    }

    @Test
    public void testNBTDeserialization() {
        
        progressionData.setProgression(testTeamId, "sculk", "initial");
        progressionData.unlockWorldStage(testTeamId, "sculk_resonance");

        CompoundTag tag = new CompoundTag();
        progressionData.save(tag);

        DisciplineProgressionData loadedData = DisciplineProgressionData.load(tag);

        DisciplineProgressionData.ProgressionState loadedState = loadedData.getProgression(testTeamId);
        assertNotNull(loadedState);
        assertEquals("sculk", loadedState.disciplineId);
        assertEquals("initial", loadedState.currentStage);
        assertTrue(loadedState.unlockedStages.contains("sculk_resonance"));
    }

    @Test
    public void testMultipleTeams() {
        UUID team1 = UUID.randomUUID();
        UUID team2 = UUID.randomUUID();

        progressionData.setProgression(team1, "phoenix", "initial");
        progressionData.setProgression(team2, "sculk", "initial");

        DisciplineProgressionData.ProgressionState state1 = progressionData.getProgression(team1);
        DisciplineProgressionData.ProgressionState state2 = progressionData.getProgression(team2);

        assertEquals("phoenix", state1.disciplineId);
        assertEquals("sculk", state2.disciplineId);
    }

    @Test
    public void testProgressionStateNBT() {
        DisciplineProgressionData.ProgressionState state = new DisciplineProgressionData.ProgressionState("void");
        state.currentStage = "dimensional_fracture";
        state.unlockedStages.add("void_convergence");
        state.unlockedStages.add("dimensional_fracture");

        CompoundTag tag = state.save();

        DisciplineProgressionData.ProgressionState loaded = DisciplineProgressionData.ProgressionState.load(tag);

        assertEquals("void", loaded.disciplineId);
        assertEquals("dimensional_fracture", loaded.currentStage);
        assertTrue(loaded.unlockedStages.contains("void_convergence"));
        assertTrue(loaded.unlockedStages.contains("dimensional_fracture"));
    }
}
