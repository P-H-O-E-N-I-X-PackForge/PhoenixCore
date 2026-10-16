package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record AcousticChart(ResourceLocation id, int bpm, List<Integer> laneNotes, List<Beat> beats) {

    public record Beat(int tick, int lane, int len) {

        public Beat(int tick, int lane) {
            this(tick, lane, 0);
        }

        public boolean isHold() {
            return len > 0;
        }

        public int endTick() {
            return tick + len;
        }
    }

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
            beats.add(
                    new Beat(pair.get(0).getAsInt(), lane, pair.size() > 2 ? Math.max(0, pair.get(2).getAsInt()) : 0));
        }
        beats.sort(Comparator.comparingInt(Beat::tick));
        return new AcousticChart(id, bpm, List.copyOf(lanes), List.copyOf(beats));
    }

    public int lastBeatTick() {
        int last = 0;
        for (Beat beat : beats) last = Math.max(last, beat.endTick());
        return last;
    }
}
