package net.phoenix.core.integration.conflux.signature;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureDefinitionTest {

    private static SignatureDefinition load(String id) throws Exception {
        try (InputStream in = SignatureDefinitionTest.class
                .getResourceAsStream("/data/phoenixcore/conflux/signatures/" + id + ".json")) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return SignatureDefinition.fromJson(id, json);
        }
    }

    @Test
    void theShippedMeterFilesParseAndBelongToTheirDisciplines() throws Exception {
        assertEquals("phoenix", load("ember").discipline());
        assertEquals("sculk", load("loop").discipline());
        assertEquals("void", load("patience").discipline());
    }

    @Test
    void levelsAreFoundByValue() throws Exception {
        SignatureDefinition ember = load("ember");
        assertEquals(0, ember.levelIndex(0));
        assertEquals(0, ember.levelIndex(99.9));
        assertEquals(1, ember.levelIndex(100));
        assertEquals(3, ember.levelIndex(1000));
        assertEquals("Blazing", ember.levelName(400));
    }

    @Test
    void aMeterMovesByItsOwnRateAndStaysBetweenZeroAndItsCeiling() throws Exception {
        SignatureDefinition ember = load("ember");
        assertEquals(91.0, ember.advance(100, 3), 1e-9);
        assertEquals(0.0, ember.advance(5, 60), 1e-9);
        SignatureDefinition patience = load("patience");
        assertEquals(160.0, patience.advance(100, 60), 1e-9);
        assertEquals(patience.max(), patience.advance(patience.max() - 1, 600), 1e-9);
        assertEquals(50.0, load("loop").advance(50, 10_000), 1e-9);
    }

    @Test
    void aLevelFlagNamesTheMeterAndTheLevel() throws Exception {
        assertEquals("sig:ember:2", load("ember").flag(2));
    }

    @Test
    void theDefinitionSurvivesBeingSentAsJson() throws Exception {
        SignatureDefinition original = load("patience");
        SignatureDefinition copy = SignatureDefinition.fromJson("patience", original.toJson());
        assertEquals(original, copy);
        assertTrue(copy.levels().size() >= 2);
    }
}
