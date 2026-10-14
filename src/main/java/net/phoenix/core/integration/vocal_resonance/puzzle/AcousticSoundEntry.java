package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * What the server knows about one library sound: its pitch, tempo and tags. The puzzle logic runs on this table, so it
 * never has to trust what a client heard.
 *
 * <pre>
 * {"sound": "phoenixcore:effect/chime_c", "note": "C4", "bpm": 120, "tags": ["bright"]}
 * </pre>
 */
public record AcousticSoundEntry(ResourceLocation sound, int note, int bpm, Set<String> tags) {

    public static final int NO_NOTE = -1;

    public static AcousticSoundEntry fromJson(JsonObject json) {
        ResourceLocation sound = new ResourceLocation(json.get("sound").getAsString());
        int note = json.has("note") ? parseNote(json.get("note").getAsString()) : NO_NOTE;
        int bpm = json.has("bpm") ? json.get("bpm").getAsInt() : 0;
        Set<String> tags = new HashSet<>();
        if (json.has("tags")) {
            JsonArray array = json.getAsJsonArray("tags");
            for (JsonElement tag : array) tags.add(tag.getAsString());
        }
        return new AcousticSoundEntry(sound, note, bpm, Set.copyOf(tags));
    }

    /** "C4", "F#3", "Bb2" to a MIDI number (C4 = 60), or {@link #NO_NOTE} when it cannot be read. */
    public static int parseNote(@Nullable String text) {
        if (text == null || text.length() < 2) return NO_NOTE;
        int semitone = switch (Character.toUpperCase(text.charAt(0))) {
            case 'C' -> 0;
            case 'D' -> 2;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 7;
            case 'A' -> 9;
            case 'B' -> 11;
            default -> -100;
        };
        if (semitone < 0) return NO_NOTE;
        int i = 1;
        if (text.charAt(i) == '#') {
            semitone++;
            i++;
        } else if (text.charAt(i) == 'b') {
            semitone--;
            i++;
        }
        try {
            return 12 * (Integer.parseInt(text.substring(i)) + 1) + semitone;
        } catch (NumberFormatException e) {
            return NO_NOTE;
        }
    }
}
