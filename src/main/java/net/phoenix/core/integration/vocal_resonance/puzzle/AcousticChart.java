package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The rhythm chart a Harmonic Lock plays the first time it is started. Each lane is a note, and each beat is a time
 * (in ticks from the start) and a lane.
 *
 * <pre>
 * {"bpm": 120, "lanes": ["C4", "E4", "G4", "B4"], "beats": [[20, 0], [30, 1], [40, 2], [50, 3]]}
 * </pre>
 */
public record AcousticChart(ResourceLocation id, int bpm, List<Integer> laneNotes, List<Beat> beats) {

    public record Beat(int tick, int lane) {}

    public static final int MS_PER_TICK = 50;

    public static AcousticChart fromJson(ResourceLocation id, JsonObject json) {
        int bpm = json.has("bpm") ? json.get("bpm").getAsInt() : 120;
        List<Integer> lanes = new ArrayList<>();
        for (JsonElement lane : json.getAsJsonArray("lanes")) {
            lanes.add(AcousticSoundEntry.parseNote(lane.getAsString()));
        }
        if (lanes.isEmpty() || lanes.size() > 8) throw new IllegalArgumentException("a chart needs 1 to 8 lanes");
        List<Beat> beats = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("beats")) {
            JsonArray pair = element.getAsJsonArray();
            int lane = pair.get(1).getAsInt();
            if (lane < 0 || lane >= lanes.size()) throw new IllegalArgumentException("beat lane out of range");
            beats.add(new Beat(pair.get(0).getAsInt(), lane));
        }
        beats.sort(Comparator.comparingInt(Beat::tick));
        return new AcousticChart(id, bpm, List.copyOf(lanes), List.copyOf(beats));
    }

    public int lastBeatTick() {
        return beats.isEmpty() ? 0 : beats.get(beats.size() - 1).tick();
    }
}
