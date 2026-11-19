package net.phoenix.core.api;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RecipeReplacements {

    /**
     * Swaps one ingredient for another in the recipes the rule matches. {@code from} and {@code to} are item ids, or
     * tags when they start with {@code #}: {@code "#c:plates/bronze"} swaps every ingredient that is that tag, for an
     * item or for another tag. {@code mustContain} is the same kind of id.
     */
    public record Rule(String mod, Pattern idPattern, String mustContain, String from, String to) {}

    public static final List<Rule> RULES = new ArrayList<>();

    private RecipeReplacements() {}

    public static void apply(Map<ResourceLocation, JsonElement> recipes) {
        if (RULES.isEmpty()) return;
        for (Map.Entry<ResourceLocation, JsonElement> entry : recipes.entrySet()) {
            ResourceLocation id = entry.getKey();
            for (Rule rule : RULES) {
                if (rule.mod() != null && !rule.mod().equals(id.getNamespace())) continue;
                if (rule.idPattern() != null && !rule.idPattern().matcher(id.toString()).find()) continue;
                if (rule.mustContain() != null && !uses(entry.getValue(), rule.mustContain())) continue;
                replace(entry.getValue(), rule.from(), rule.to());
            }
        }
    }

    private static boolean isTag(String id) {
        return id.startsWith("#");
    }

    private static boolean uses(JsonElement e, String item) {
        String key = isTag(item) ? "tag" : "item";
        String value = isTag(item) ? item.substring(1) : item;
        if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> x : e.getAsJsonObject().entrySet()) {
                if (x.getKey().equals("result") || x.getKey().equals("results")) continue;
                if (x.getKey().equals(key) && x.getValue().isJsonPrimitive() &&
                        x.getValue().getAsString().equals(value))
                    return true;
                if (uses(x.getValue(), item)) return true;
            }
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                if (uses(x, item)) return true;
            }
        }
        return false;
    }

    private static void replace(JsonElement e, String from, String to) {
        String fromKey = isTag(from) ? "tag" : "item";
        String fromValue = isTag(from) ? from.substring(1) : from;
        String toKey = isTag(to) ? "tag" : "item";
        String toValue = isTag(to) ? to.substring(1) : to;
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (Map.Entry<String, JsonElement> x : new ArrayList<>(o.entrySet())) {
                if (x.getKey().equals("result") || x.getKey().equals("results")) continue;
                if (x.getKey().equals(fromKey) && x.getValue().isJsonPrimitive() &&
                        x.getValue().getAsString().equals(fromValue)) {
                    // an ingredient is {"item": ...} or {"tag": ...}; one can become the other
                    o.remove(fromKey);
                    o.addProperty(toKey, toValue);
                } else {
                    replace(x.getValue(), from, to);
                }
            }
        } else if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (JsonElement x : a) replace(x, from, to);
        }
    }
}
