package net.phoenix.core.integration.conflux.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HookRule(String id, boolean enabled, List<String> on, Map<String, String> conditions, Once once,
                       int cooldownTicks, List<Action> actions) {

    public enum Once {
        NEVER,
        PLAYER,
        TEAM
    }

    public record Action(String type, String value, String as, Map<String, String> data) {}

    public record Context(String type, String dimension, String discipline, double y, Map<String, Object> data) {}

    public boolean listensTo(String type) {
        for (String t : on) if (t.equals(type) || t.equals("*")) return true;
        return false;
    }

    public boolean matches(Context ctx) {
        for (Map.Entry<String, String> c : conditions.entrySet()) {
            String key = c.getKey();
            String want = c.getValue();
            switch (key) {
                case "discipline" -> {
                    if (!want.equals(ctx.discipline())) return false;
                }
                case "dimension" -> {
                    if (!want.equals(ctx.dimension())) return false;
                }
                case "y_min" -> {
                    if (ctx.y() < parse(want)) return false;
                }
                case "y_max" -> {
                    if (ctx.y() > parse(want)) return false;
                }
                default -> {
                    if (key.startsWith("min_")) {
                        if (number(ctx.data().get(key.substring(4))) < parse(want)) return false;
                    } else if (key.startsWith("max_")) {
                        if (number(ctx.data().get(key.substring(4))) > parse(want)) return false;
                    } else {
                        Object have = ctx.data().get(key);
                        if (have == null || !want.equals(String.valueOf(have))) return false;
                    }
                }
            }
        }
        return true;
    }

    private static double parse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static double number(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        return o == null ? Double.NaN : parse(String.valueOf(o));
    }

    public static HookRule fromJson(String id, JsonObject json) {
        List<String> on = new ArrayList<>();
        JsonElement onElement = json.get("on");
        if (onElement != null && onElement.isJsonArray()) {
            for (JsonElement e : onElement.getAsJsonArray()) on.add(e.getAsString());
        } else if (onElement != null) {
            on.add(onElement.getAsString());
        }

        Map<String, String> conditions = new LinkedHashMap<>();
        if (json.has("conditions")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("conditions").entrySet()) {
                conditions.put(e.getKey(), e.getValue().getAsString());
            }
        }

        Once once = Once.NEVER;
        if (json.has("once")) {
            once = switch (json.get("once").getAsString().toLowerCase()) {
                case "player" -> Once.PLAYER;
                case "team" -> Once.TEAM;
                default -> Once.NEVER;
            };
        }

        List<Action> actions = new ArrayList<>();
        JsonArray list = json.has("actions") ? json.getAsJsonArray("actions") : new JsonArray();
        for (JsonElement e : list) {
            JsonObject a = e.getAsJsonObject();
            Map<String, String> data = new LinkedHashMap<>();
            if (a.has("data")) {
                for (Map.Entry<String, JsonElement> d : a.getAsJsonObject("data").entrySet()) {
                    data.put(d.getKey(), d.getValue().getAsString());
                }
            }
            actions.add(new Action(a.get("type").getAsString(), a.has("value") ? a.get("value").getAsString() : "",
                    a.has("as") ? a.get("as").getAsString() : "player", data));
        }

        return new HookRule(id, !json.has("enabled") || json.get("enabled").getAsBoolean(), on, conditions, once,
                json.has("cooldown_ticks") ? json.get("cooldown_ticks").getAsInt() : 0, actions);
    }
}
