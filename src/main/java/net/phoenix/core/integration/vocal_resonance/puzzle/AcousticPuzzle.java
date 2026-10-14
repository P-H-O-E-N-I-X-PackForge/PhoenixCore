package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A sequence of sounds the jukebox has to be played, in order. Every step lists what a sound must have to count; a
 * step may ask for an exact sound, a note, a tag, or any mix of them.
 *
 * <pre>
 * {"steps": [{"note": "C4"}, {"note": "E4"}, {"tag": "low"}, {"sound": "phoenixcore:effect/gong"}]}
 * </pre>
 */
public record AcousticPuzzle(ResourceLocation id, List<Step> steps) {

    public record Step(@Nullable ResourceLocation sound, int note, @Nullable String tag) {

        public boolean matches(AcousticSoundEntry entry) {
            if (sound != null && !sound.equals(entry.sound())) return false;
            if (note != AcousticSoundEntry.NO_NOTE && note != entry.note()) return false;
            return tag == null || entry.tags().contains(tag);
        }
    }

    public static AcousticPuzzle fromJson(ResourceLocation id, JsonObject json) {
        List<Step> steps = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("steps");
        for (JsonElement element : array) {
            JsonObject step = element.getAsJsonObject();
            ResourceLocation sound = step.has("sound") ? new ResourceLocation(step.get("sound").getAsString()) : null;
            int note = step.has("note") ? AcousticSoundEntry.parseNote(step.get("note").getAsString()) :
                    AcousticSoundEntry.NO_NOTE;
            String tag = step.has("tag") ? step.get("tag").getAsString() : null;
            steps.add(new Step(sound, note, tag));
        }
        return new AcousticPuzzle(id, List.copyOf(steps));
    }
}
