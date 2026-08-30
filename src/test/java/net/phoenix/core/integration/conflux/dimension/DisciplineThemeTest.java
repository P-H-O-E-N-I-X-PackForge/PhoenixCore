package net.phoenix.core.integration.conflux.dimension;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DisciplineThemeTest {

    private DisciplineTheme phoenixTheme;

    @BeforeEach
    public void setUp() {
        
        JsonObject json = new JsonObject();
        json.addProperty("display_name", "The Phoenix");

        JsonObject biomePaletteJson = new JsonObject();
        JsonArray stagesArray = new JsonArray();

        JsonObject stageJson = new JsonObject();
        stageJson.addProperty("stage", "initial");
        JsonArray biomesArray = new JsonArray();
        biomesArray.add("minecraft:plains");
        stageJson.add("biomes", biomesArray);
        stagesArray.add(stageJson);

        biomePaletteJson.add("stages", stagesArray);
        json.add("biome_palette", biomePaletteJson);

        JsonObject structuresJson = new JsonObject();
        structuresJson.addProperty("particle_density", 1.2f);
        json.add("structures", structuresJson);

        JsonObject skyboxJson = new JsonObject();
        skyboxJson.addProperty("sky_color", "#FF8C00");
        skyboxJson.addProperty("fog_color", "#FF6347");
        skyboxJson.addProperty("fog_density", 0.7f);
        skyboxJson.addProperty("render_mode", "sunset");
        json.add("skybox", skyboxJson);

        JsonArray colorProgArray = new JsonArray();
        JsonObject colorJson = new JsonObject();
        colorJson.addProperty("milestone", "initial");
        colorJson.addProperty("grass_color", "#7CB342");
        colorJson.addProperty("water_color", "#3D87CF");
        colorJson.addProperty("foliage_color", "#558B2F");
        colorProgArray.add(colorJson);
        json.add("color_progression", colorProgArray);

        phoenixTheme = DisciplineTheme.fromJson(json, "phoenix");
    }

    @Test
    public void testThemeLoading() {
        assertNotNull(phoenixTheme);
        assertEquals("phoenix", phoenixTheme.disciplineId);
        assertEquals("The Phoenix", phoenixTheme.displayName);
    }

    @Test
    public void testBiomePalette() {
        assertNotNull(phoenixTheme.biomePalette);
        assertFalse(phoenixTheme.biomePalette.stages.isEmpty());

        DisciplineTheme.BiomePalette.StageEntry firstStage = phoenixTheme.biomePalette.stages.get(0);
        assertEquals("initial", firstStage.stageName);
        assertFalse(firstStage.biomes.isEmpty());
    }

    @Test
    public void testSkyboxProfile() {
        assertNotNull(phoenixTheme.skybox);
        assertEquals("sunset", phoenixTheme.skybox.renderMode);
        assertEquals(0.7f, phoenixTheme.skybox.fogDensity);
        assertTrue(phoenixTheme.skybox.skyColor > 0);
        assertTrue(phoenixTheme.skybox.fogColor > 0);
    }

    @Test
    public void testColorProgression() {
        assertNotNull(phoenixTheme.colorProgression);
        assertTrue(phoenixTheme.colorProgression.length > 0);

        DisciplineTheme.ColorProgression color = phoenixTheme.colorProgression[0];
        assertEquals("initial", color.milestone);
        assertTrue(color.grassColor > 0);
        assertTrue(color.waterColor > 0);
        assertTrue(color.foliageColor > 0);
    }

    @Test
    public void testStructureThemeSet() {
        assertNotNull(phoenixTheme.structures);
        assertEquals(1.2f, phoenixTheme.structures.particleDensity);
    }
}
