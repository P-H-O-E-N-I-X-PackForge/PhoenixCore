package net.phoenix.core.integration.conflux.signature;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record SignatureDefinition(String id, String discipline, String title, String description, int color,
                                  String icon, double max, double start, double gainPerMinute,
                                  double decayPerMinute, List<Level> levels) {

    public record Level(double at, String name) {}

    public static SignatureDefinition fromJson(String id, JsonObject obj) {
        List<Level> levels = new ArrayList<>();
        if (obj.has("levels")) {
            for (var element : obj.getAsJsonArray("levels")) {
                JsonObject l = element.getAsJsonObject();
                levels.add(new Level(l.get("at").getAsDouble(), l.has("name") ? l.get("name").getAsString() : ""));
            }
        }
        levels.sort(java.util.Comparator.comparingDouble(Level::at));
        if (levels.isEmpty()) levels.add(new Level(0, ""));
        String color = obj.has("color") ? obj.get("color").getAsString().replace("#", "") : "FFFFFF";
        int rgb;
        try {
            rgb = Integer.parseInt(color, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            rgb = 0xFFFFFF;
        }
        double max = obj.has("max") ? Math.max(1.0, obj.get("max").getAsDouble()) : 1000.0;
        return new SignatureDefinition(id, obj.has("discipline") ? obj.get("discipline").getAsString() : "",
                obj.has("title") ? obj.get("title").getAsString() : id,
                obj.has("description") ? obj.get("description").getAsString() : "", 0xFF000000 | rgb,
                obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:nether_star", max,
                clamp(obj.has("start") ? obj.get("start").getAsDouble() : 0.0, 0.0, max),
                obj.has("gain_per_minute") ? obj.get("gain_per_minute").getAsDouble() : 0.0,
                obj.has("decay_per_minute") ? obj.get("decay_per_minute").getAsDouble() : 0.0, List.copyOf(levels));
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("discipline", discipline);
        obj.addProperty("title", title);
        obj.addProperty("description", description);
        obj.addProperty("color", String.format("%06X", color & 0xFFFFFF));
        obj.addProperty("icon", icon);
        obj.addProperty("max", max);
        obj.addProperty("start", start);
        obj.addProperty("gain_per_minute", gainPerMinute);
        obj.addProperty("decay_per_minute", decayPerMinute);
        JsonArray array = new JsonArray();
        for (Level level : levels) {
            JsonObject l = new JsonObject();
            l.addProperty("at", level.at());
            l.addProperty("name", level.name());
            array.add(l);
        }
        obj.add("levels", array);
        return obj;
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public int levelIndex(double value) {
        int index = 0;
        for (int i = 0; i < levels.size(); i++) {
            if (value >= levels.get(i).at()) index = i;
        }
        return index;
    }

    public String levelName(double value) {
        return levels.get(levelIndex(value)).name();
    }

    public double advance(double value, double minutes) {
        return clamp(value + (gainPerMinute - decayPerMinute) * minutes, 0.0, max);
    }

    public String flag(int levelIndex) {
        return "sig:" + id + ":" + levelIndex;
    }
}
